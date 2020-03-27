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

    void addTask(Integer numberRow) {
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
        if (!listening) {
            listenForReturns(channel);
            listening = true;
        }
        synchronized (currentlyExecutingTasks) {
            List<PrimeTask> finishedTasks = currentlyExecutingTasks.stream()
                    .filter(task -> task.completed).collect(Collectors.toList());
            currentlyExecutingTasks.removeAll(finishedTasks);
            closedTasks.addAll(finishedTasks);
        }

        switch (strategy) {
            case EqualScheduleing:
                scheduleTasksEqually(clients, channel);
                break;
            case WattScheduleing:
                scheduleTasksByWattUsage(clients, channel);
                break;
            case PerformanceScheduleing:
                scheduleTasksByPerformance(clients, channel);
                break;
        }
    }

    public void scheduleTasksEqually(List<RegisteredClient> clients, Channel channel) throws IOException {
        List<RegisteredClient> availableClients = clients.stream()
                .filter(client -> client.tasksAssigned <= 10).collect(Collectors.toList());
        for (RegisteredClient client : availableClients) {
            if (openTasks.isEmpty()) {
                break;
            }
            assignAndStartTask(client, openTasks.poll(), channel);
        }
    }

    public void scheduleTasksByWattUsage(List<RegisteredClient> clients, Channel channel) throws IOException {
        List<RegisteredClient> sortedClients = clients.stream()
                .sorted(Comparator.comparingDouble(RegisteredClient::getWattUsage)).collect(Collectors.toList());

        for (RegisteredClient sortedClient : sortedClients) {
            //int maxTasksForClient = sortedClient.getWattUsage()
            if (openTasks.isEmpty()) {
                break;
            }
            assignAndStartTask(sortedClient, openTasks.poll(), channel);
        }
    }

    /**
     * Schedule tasks based on the average of the clients last 50 performance values
     *
     * @param clients
     * @param channel
     * @throws IOException
     */
    public void scheduleTasksByPerformance(List<RegisteredClient> clients, Channel channel) throws IOException {
        final int MAX_TASKS_FOR_CLIENT = 50;

        synchronized (clients) {
            List<Double> clientAveragePerformance = clients.stream().map(client -> {
                synchronized (client.executionDurations) {
                    if (client.executionDurations.isEmpty()) {
                        return 0.0;
                    } else {
                        if (client.executionDurations.size() > 100) {
                            return client.executionDurations.stream().mapToLong(Long::longValue)
                                    .skip(client.executionDurations.size() - 100).average().getAsDouble();
                        } else {
                            return client.executionDurations.stream().mapToLong(Long::longValue).average().getAsDouble();
                        }
                    }
                }
            }).collect(Collectors.toList());

            Optional<Double> bestPerformanceAverageOpt = clientAveragePerformance.stream().min(Double::compareTo);
            Optional<Double> worstPerformanceAverageOpt = clientAveragePerformance.stream().max(Double::compareTo);
            Double bestPerformanceAverage = 0.0;
            Double worstPerformanceAverage = 0.0;
            if (bestPerformanceAverageOpt.isPresent()) {
                bestPerformanceAverage = bestPerformanceAverageOpt.get();
                worstPerformanceAverage = worstPerformanceAverageOpt.get();
            }

            double performanceDiff = worstPerformanceAverage - bestPerformanceAverage;
            double performanceDiffStep = MAX_TASKS_FOR_CLIENT / performanceDiff;

            int clientIndex = 0;
            for (RegisteredClient client : clients) {
                if (openTasks.isEmpty()) {
                    break;
                }
                Double averagePerformance = clientAveragePerformance.get(clientIndex);
                int tasksToAssign = 0;
                if (averagePerformance != 0) {
                    Double clientPerformanceDiff = averagePerformance - bestPerformanceAverage;
                    tasksToAssign = (int) (clientPerformanceDiff * performanceDiffStep);
                }

                if (tasksToAssign <= 0) {
                    tasksToAssign = 1;
                }
                int taskToAssignPre = tasksToAssign;
                tasksToAssign -= client.tasksAssigned;

                if(tasksToAssign > 0)
                System.out.println("Assigning " + tasksToAssign + " to " + client.getName() + ". Total scheduled: " + taskToAssignPre);

                for (int i = 0; i < tasksToAssign; i++) {
                    if (!openTasks.isEmpty()) {
                        assignAndStartTask(client, openTasks.poll(), channel);
                    }
                }

                clientIndex++;
            }
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
        channel.basicPublish(PRODUCER_EXCHANGE_NAME, client.getProductionQueueName(), null, task.numberToCheck.toString().getBytes());
    }

    private void listenForReturns(Channel channel) throws IOException {
        channel.basicConsume(RabbitMQUtils.Queue.CONSUMER_DATA_RETURN_QUEUE.getName(), true, "myConsumerTag2", getClientReturnConsumer(channel));
    }

    private DefaultConsumer getClientReturnConsumer(Channel channel) {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                ClientReturn clientReturn = SerializationUtils.deserialize(body);
                Optional<PrimeTask> primeTask = getCurrentlyExecutedTask(clientReturn.numberToCheck);

                if (primeTask.isPresent()) {
                    //System.out.println("Task returned: " + clientReturn.numberToCheck);
                    PrimeTask returnedTask = primeTask.get();

                    returnedTask.isPrime = clientReturn.isPrime;
                    returnedTask.completed = true;
                    synchronized (returnedTask.assignedClient) {
                        returnedTask.assignedClient.tasksAssigned--;
                    }
                    closedTasks.add(returnedTask);
                    currentlyExecutingTasks.remove(returnedTask);
                }

            }
        };
    }

    public Optional<PrimeTask> getCurrentlyExecutedTask(int number) {
        synchronized (currentlyExecutingTasks) {
            return currentlyExecutingTasks.stream().filter(currentTask -> currentTask.numberToCheck == number).findFirst();
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
