import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import util.ByteConverter;
import util.PrimeUtil;
import util.RabbitMQUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static util.RabbitMQUtils.CONSUMER_EXCHANGE_NAME;

class Scheduler {
    private Queue<PrimeTask> openTasks = new LinkedList<>();
    private List<PrimeTask> currentlyExecutingTasks = Collections.synchronizedList(new ArrayList<>());
    private List<PrimeTask> closedTasks = Collections.synchronizedList(new ArrayList<>());

    boolean listening = false;

    void addTask(int number) {
        openTasks.add(new PrimeTask(number));
    }

    void scheduleTasks(List<RegisteredClient> clients, Channel channel) throws IOException {

        if(!listening) {
            listenForReturns(channel);
            listening = true;
        }

        List<PrimeTask> finishedTasks = currentlyExecutingTasks.stream().filter(task -> task.completed).collect(Collectors.toList());
        currentlyExecutingTasks.removeAll(finishedTasks);
        closedTasks.addAll(finishedTasks);

        List<RegisteredClient> availableClients = clients.stream().filter(client -> client.tasksAssigned == 0).collect(Collectors.toList());
        for (RegisteredClient client : availableClients) {
            if(openTasks.isEmpty())
                break;
            assignAndStartTask(client, openTasks.poll(), channel);
        }
    }

    private void assignAndStartTask(RegisteredClient client, PrimeTask task, Channel channel) throws IOException {
        currentlyExecutingTasks.add(task);
        channel.basicPublish(client.getProductionQueueName(), client.getProductionQueueName(), null, task.numberToCheck.toString().getBytes());
    }

    private void listenForReturns(Channel channel) throws IOException {
        channel.basicConsume(RabbitMQUtils.Queue.CONSUMER_DATA_RETURN_QUEUE.getName(), true, "myConsumerTag2",
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        ClientReturn clientReturn = ByteConverter.bytesToObjects(body);
                        System.out.println("Client Return: " + clientReturn.numberToCheck + " " + clientReturn.isPrime);
                    }
                });
    }

    boolean tasksLeft() {
        return !openTasks.isEmpty() || !currentlyExecutingTasks.isEmpty();
    }
}
