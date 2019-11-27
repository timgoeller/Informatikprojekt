import com.rabbitmq.client.*;
import javafx.concurrent.Task;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static util.RabbitMQUtils.*;

class Host {

    private Channel channel;
    private List<RegisteredClient> registeredClients;
    private Scheduler scheduler;

    Host(@NotNull String rabbitMQHost, @NotNull String rabbitMQUser, @NotNull String rabbitMQPass, @NotNull Integer rabbitMQPort) throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        scheduler = new Scheduler();
        factory.setHost(rabbitMQHost);
        factory.setPort(rabbitMQPort);
        factory.setUsername(rabbitMQUser);
        factory.setPassword(rabbitMQPass);

        initializeRabbitMQConnection(factory);
        listenForNewClients();
    }

    private void initializeRabbitMQConnection(@NotNull ConnectionFactory factory) throws IOException {
        try {
            System.out.println("Creating connection...");
            Connection connection = factory.newConnection();
            System.out.println("Connection created successfully");

            System.out.println("Creating channel...");
            channel = connection.createChannel();
            System.out.println("Channel created successfully with number " + channel.getChannelNumber());

            CreateDefaultExchanges(channel);
            CreateDefaultQueues(channel);
        }
        catch (TimeoutException e) {
            System.out.println("Timeout while trying to connect to the RabbitMQ server");
        }
    }

    private void listenForNewClients() throws IOException {
        channel.basicConsume(Queue.CONSUMER_REGISTRATION_QUEUE.getName(), true, "myConsumerTag",
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    {
                        registeredClients.add(new RegisteredClient(new String(body)));
                    }
                });
    }

    private void startTaskExecution(List<Integer> numbersToCheck) {
        numbersToCheck.forEach(e -> scheduler.addTask(e));
    }

}
