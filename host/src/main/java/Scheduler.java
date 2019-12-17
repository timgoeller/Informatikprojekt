import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.lang3.SerializationUtils;
import util.ByteConverter;
import util.PrimeUtil;
import util.RabbitMQUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static util.RabbitMQUtils.CONSUMER_EXCHANGE_NAME;
import static util.RabbitMQUtils.PRODUCER_EXCHANGE_NAME;

class Scheduler {
    private final Queue<PrimeTask> openTasks = new LinkedList<>();
    private final List<PrimeTask> currentlyExecutingTasks = Collections.synchronizedList(new ArrayList<>());
    private final List<PrimeTask> closedTasks = Collections.synchronizedList(new ArrayList<>());

    boolean listening = false;

    void addTask(int number) {
        openTasks.add(new PrimeTask(number));
    }

    void scheduleTasks(List<RegisteredClient> clients, Channel channel) throws IOException {

        if(!listening) {
            listenForReturns(channel);
            listening = true;
        }

        synchronized (currentlyExecutingTasks) {
            List<PrimeTask> finishedTasks = currentlyExecutingTasks.stream().filter(task -> task.completed).collect(Collectors.toList());
            currentlyExecutingTasks.removeAll(finishedTasks);
            closedTasks.addAll(finishedTasks);
        }

        List<RegisteredClient> availableClients = clients.stream().filter(client -> client.tasksAssigned == 0).collect(Collectors.toList());
        for (RegisteredClient client : availableClients) {
            if(openTasks.isEmpty())
                break;
            assignAndStartTask(client, openTasks.poll(), channel);
        }
    }

    private void assignAndStartTask(RegisteredClient client, PrimeTask task, Channel channel) throws IOException {
        currentlyExecutingTasks.add(task);
        channel.basicPublish(PRODUCER_EXCHANGE_NAME, client.getProductionQueueName(), null, task.numberToCheck.toString().getBytes());
    }

    private void listenForReturns(Channel channel) throws IOException {
        channel.basicConsume(RabbitMQUtils.Queue.CONSUMER_DATA_RETURN_QUEUE.getName(), true, "myConsumerTag2",
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        ClientReturn clientReturn = SerializationUtils.deserialize(body);
                        Optional<PrimeTask> primeTask = getCurrentlyExecutedTask(clientReturn.numberToCheck);

                        if(primeTask.isPresent()) {
                            PrimeTask returnedTask = primeTask.get();
                            returnedTask.completed = true;
                           //System.out.println("Client Return: " + clientReturn.numberToCheck + " " + clientReturn.isPrime + " Client Name:" + clientReturn.name);
                        }

                    }
                });

        channel.basicConsume(RabbitMQUtils.Queue.CONSUMER_INFO_QUEUE.getName(), true, "myConsumerTag3",
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        ClientDataReturn clientDataReturn = SerializationUtils.deserialize(body);
                        System.out.println("SIZE " + clientDataReturn.latestExecutionTimes.size());
                        
                    }
                });
    }

    public Optional<PrimeTask> getCurrentlyExecutedTask(int number) {
        synchronized (currentlyExecutingTasks) {
            return currentlyExecutingTasks.stream().filter(currentTask -> currentTask.getNumber() == number).findFirst();
        }
    }

    boolean tasksLeft() {
        return !openTasks.isEmpty() || !currentlyExecutingTasks.isEmpty();
    }
}
