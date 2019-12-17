import com.rabbitmq.client.*;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import util.RabbitMQUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static util.RabbitMQUtils.*;

class Host {

    private Channel channel;
    private final List<RegisteredClient> registeredClients = Collections.synchronizedList(new ArrayList<>());
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
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        synchronized (registeredClients) {
                            registeredClients.add(new RegisteredClient(new String(body), channel));
                        }
                    }
                });
    }

    void startTaskExecution(List<Integer> numbersToCheck) throws IOException {
        numbersToCheck.forEach(e -> scheduler.addTask(e));

        listenForClientInfo();

        while(scheduler.tasksLeft()) {
            synchronized (registeredClients) {
                scheduler.scheduleTasks(registeredClients, channel);
            }
        }
        System.out.println("Finished!");
    }

    public void listenForClientInfo() throws IOException {
        channel.basicConsume(RabbitMQUtils.Queue.CONSUMER_INFO_QUEUE.getName(), true, "myConsumerTag4",
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        ClientDataReturn clientDataReturn = SerializationUtils.deserialize(body);

                        Optional<RegisteredClient> registeredClientOptional;
                        synchronized (registeredClients) {
                            registeredClientOptional = registeredClients.stream().filter(client -> client.getName().equals(clientDataReturn.clientName)).findFirst();
                        }

                        if(registeredClientOptional.isPresent()) {
                            RegisteredClient registeredClient = registeredClientOptional.get();
                            synchronized (registeredClient.executionDurations) {
                                registeredClient.executionDurations.addAll(clientDataReturn.latestExecutionTimes);
                                System.out.println("Client: " + registeredClient.executionDurations.size() + " " + registeredClient.getName());
                            }
                        }
                    }
                });
    }
}
