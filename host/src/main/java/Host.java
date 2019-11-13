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

    public void StartTaskExecution(List<Integer> numbersToCheck) {

    }


    public void InitializeRabbitMQConnection(ConnectionFactory factory) {
        try {
            System.out.println("Creating connection...");
            Connection connection = factory.newConnection();
            System.out.println("Connection created successfully");

            System.out.println("Creating channel...");
            channel = connection.createChannel();
            System.out.println("Channel created successfully with number " + channel.getChannelNumber());

            System.out.println("Declaring exchanges...");
            //Producer->Consumer
            channel.exchangeDeclare(PRODUCER_EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
            //Consumer->Producer
            channel.exchangeDeclare(CONSUMER_EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
            System.out.println("Exchanges declared successfully");

            System.out.println("Declaring queues...");
            //queueDeclare(name, durable, exclusive, autoDelete, arguments)
            channel.queueDeclare(Queue.CONSUMER_REGISTRATION_QUEUE.getName(), false, false, true, null);
            System.out.println("Queues declared successfully");

            System.out.println("Binding queues...");
            channel.queueBind(Queue.CONSUMER_REGISTRATION_QUEUE.getName(), CONSUMER_EXCHANGE_NAME, Queue.CONSUMER_REGISTRATION_QUEUE.getName());
            System.out.println("Binding of queues completed successfully");
        }
        catch (TimeoutException e) {
            System.out.println("Timeout while trying to connect to the RabbitMQ server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
