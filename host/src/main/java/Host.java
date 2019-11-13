import com.rabbitmq.client.*;
import com.sun.org.apache.xml.internal.security.Init;
import org.jetbrains.annotations.NotNull;
import util.RabbitMQUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static util.RabbitMQUtils.*;

class Host {

    private Channel channel;

    Host(@NotNull String rabbitMQHost, @NotNull String rabbitMQUser, @NotNull String rabbitMQPass, @NotNull Integer rabbitMQPort) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQHost);
        factory.setPort(rabbitMQPort);
        factory.setUsername(rabbitMQUser);
        factory.setPassword(rabbitMQPass);

        InitializeRabbitMQConnection(factory);
    }

    private void InitializeRabbitMQConnection(@NotNull ConnectionFactory factory) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void StartTaskExecution(List<Integer> numbersToCheck) {

    }

}
