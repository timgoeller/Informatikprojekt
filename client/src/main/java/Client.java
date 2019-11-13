import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static util.RabbitMQUtils.*;

public class Client {

    private Channel channel;
    private String name;

    Client(@NotNull String rabbitMQHost, @NotNull String rabbitMQUser, @NotNull String rabbitMQPass, @NotNull Integer rabbitMQPort, @NotNull String clientName) throws IOException {
        name = clientName;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQHost);
        factory.setPort(rabbitMQPort);
        factory.setUsername(rabbitMQUser);
        factory.setPassword(rabbitMQPass);

        InitializeRabbitMQConnection(factory);
        NotifyProducer();
    }

    private void InitializeRabbitMQConnection(@NotNull ConnectionFactory factory) throws IOException {
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

    private void NotifyProducer() throws IOException {
        channel.basicPublish(CONSUMER_EXCHANGE_NAME, Queue.CONSUMER_REGISTRATION_QUEUE.getName(), null, name.getBytes());
    }
}
