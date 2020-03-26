import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.lang3.SerializationUtils;
import util.RabbitMQUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static util.RabbitMQUtils.PRODUCER_EXCHANGE_NAME;

class Scheduler {
    private final Queue<PrimeTask> openTasks = new LinkedList<>();
    private final List<PrimeTask> currentlyExecutingTasks = Collections.synchronizedList(new ArrayList<>());
    private final List<PrimeTask> closedTasks = Collections.synchronizedList(new ArrayList<>());
    boolean listening = false;
    private ScheduleingStrategy strategy;

    public Scheduler(ScheduleingStrategy strategy) {
        this.strategy = strategy;
    }

    void addTask(String numberRow) {
        openTasks.add(new PrimeTask(numberRow));
    }

    /**
     * Runs the scheduler once with the assigned strategy
     *
     * @param clients
     * @param channel
     * @throws IOException
     */
    void scheduleTasks(List<RegisteredClient> clients, Channel channel) throws IOException {
        switch (strategy) {
            case EqualScheduleing:
                scheduleTasksEqually(clients, channel);
                break;
            case WattScheduleing:
                scheduleTasksByWattUsage(clients, channel);
                break;
        }
    }

    public void scheduleTasksEqually(List<RegisteredClient> clients, Channel channel) throws IOException {
        if (!listening) {
            listenForReturns(channel);
            listening = true;
        }

        synchronized (currentlyExecutingTasks) {
            List<PrimeTask> finishedTasks = currentlyExecutingTasks.stream().filter(task -> task.completed).collect(Collectors.toList());
            currentlyExecutingTasks.removeAll(finishedTasks);
            closedTasks.addAll(finishedTasks);
        }

        List<RegisteredClient> availableClients = clients.stream().filter(client -> client.tasksAssigned <= 10).collect(Collectors.toList());
        for (RegisteredClient client : availableClients) {
            if (openTasks.isEmpty()) {
                break;
            }
            assignAndStartTask(client, openTasks.poll(), channel);
        }
    }

    public void scheduleTasksByWattUsage(List<RegisteredClient> clients, Channel channel) throws IOException {

        if (!listening) {
            listenForReturns(channel);
            listening = true;
        }
        synchronized (currentlyExecutingTasks) {
            List<PrimeTask> finishedTasks = currentlyExecutingTasks.stream().filter(task -> task.completed).collect(Collectors.toList());
            currentlyExecutingTasks.removeAll(finishedTasks);
            closedTasks.addAll(finishedTasks);
        }
        List<RegisteredClient> sortedClients = clients.stream().sorted(Comparator.comparingDouble(RegisteredClient::getWattUsage)).collect(Collectors.toList());

        for (RegisteredClient sortedClient : sortedClients) {
            //int maxTasksForClient = sortedClient.getWattUsage()
            if (openTasks.isEmpty()) {
                break;
            }
            assignAndStartTask(sortedClient, openTasks.poll(), channel);
        }
    }

    /**
     * Send task to client for execution
     *
     * @param client
     * @param task
     * @param channel
     * @throws IOException
     */
    private void assignAndStartTask(RegisteredClient client, PrimeTask task, Channel channel) throws IOException {
        task.assignedClient = client;
        client.tasksAssigned++;
        currentlyExecutingTasks.add(task);
        channel.basicPublish(PRODUCER_EXCHANGE_NAME, client.getProductionQueueName(), null, task.numberRowToCheck.getBytes());
    }

    private void listenForReturns(Channel channel) throws IOException {
        channel.basicConsume(RabbitMQUtils.Queue.CONSUMER_DATA_RETURN_QUEUE.getName(), true, "myConsumerTag2",getClientReturnConsumer(channel));
    }

    private DefaultConsumer getClientReturnConsumer(Channel channel) {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                ClientReturn clientReturn = SerializationUtils.deserialize(body);
                Optional<PrimeTask> primeTask = getCurrentlyExecutedTask(clientReturn.numberToCheck);

                if (primeTask.isPresent()) {
                    PrimeTask returnedTask = primeTask.get();
                    returnedTask.isPrimeArr[returnedTask.numbers.indexOf(String.valueOf(clientReturn.numberToCheck))] = clientReturn.isPrime;
                    returnedTask.completedArr[returnedTask.numbers.indexOf(String.valueOf(clientReturn.numberToCheck))] = true;
                    returnedTask.assignedClient.tasksAssigned--;

                    if (allTrue(returnedTask.completedArr)) {
                        returnedTask.completed = true;
                        closedTasks.add(returnedTask);
                        currentlyExecutingTasks.remove(returnedTask);
                    }
                }

            }
        };
    }

    boolean allTrue(boolean[] arr) {
        for (boolean b : arr) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    public Optional<PrimeTask> getCurrentlyExecutedTask(int number) {
        synchronized (currentlyExecutingTasks) {
            return currentlyExecutingTasks.stream().filter(currentTask -> currentTask.numbers.contains(String.valueOf(number))).findFirst();
        }
    }

    boolean tasksLeft() {
        return (!openTasks.isEmpty() || !currentlyExecutingTasks.isEmpty());
    }

    public enum ScheduleingStrategy {
        EqualScheduleing,
        WattScheduleing,
        PerformanceScheduleing
    }
}
