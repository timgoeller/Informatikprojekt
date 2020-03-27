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

    private final List<RegisteredClient> registeredClients = Collections.synchronizedList(new ArrayList<>());
    private Channel channel;
    private Scheduler scheduler;

    /**
     * @param rabbitMQHost IP-String for the RabbitMQ-Server
     * @param rabbitMQUser RabbitMQ-Username
     * @param rabbitMQPass RabbitMQ-Password
     * @param rabbitMQPort Port of the RabbitMQ-Server
     * @throws IOException
     */
    Host(@NotNull String rabbitMQHost, @NotNull String rabbitMQUser, @NotNull String rabbitMQPass, @NotNull Integer rabbitMQPort) throws IOException {
        System.out.println(Runtime.getRuntime().availableProcessors());
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQHost);
        factory.setPort(rabbitMQPort);
        factory.setUsername(rabbitMQUser);
        factory.setPassword(rabbitMQPass);

        initializeRabbitMQConnection(factory);
        startListeningForNewClients();
    }

    /**
     * Triggers creations of all defaults
     *
     * @param factory
     * @throws IOException
     */
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
        } catch (TimeoutException e) {
            System.out.println("Timeout while trying to connect to the RabbitMQ server");
        }
    }

    /**
     * Listen for client registrations in queue
     *
     * @throws IOException
     */
    private void startListeningForNewClients() throws IOException {
        channel.basicConsume(Queue.CONSUMER_REGISTRATION_QUEUE.getName(), true, "myConsumerTag", getNewClientConsumer());
    }

    public DefaultConsumer getNewClientConsumer() {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                synchronized (registeredClients) {
                    registeredClients.add(new RegisteredClient(new String(body), channel));
                }
            }
        };
    }

    /**
     * Start scheduleing of tasks until they are finished
     *
     * @param numberRowsToCheck Numbers to initialize the scheduler with
     * @throws IOException
     */
    void startTaskExecution(@NotNull List<Integer> numberRowsToCheck) throws IOException {
        scheduler = new Scheduler(Scheduler.ScheduleingStrategy.PerformanceScheduleing);
        numberRowsToCheck.forEach(e -> scheduler.addTask(e));

        startListeningForClientInfo();

        System.out.println("Press Enter key to continue...");
        try {
            System.in.read();
        } catch (Exception e) {
        }

        long startTime = System.currentTimeMillis();
        while (scheduler.tasksLeft()) {
            synchronized (registeredClients) {
                scheduler.scheduleTasks(registeredClients, channel);
            }
        }
        long endTime = System.currentTimeMillis();

        System.out.println("Finished! " + "(In " + (endTime - startTime) + "ms)");
    }

    /**
     * Listen for metadata from clients and write it to the client object
     *
     * @throws IOException
     */
    public void startListeningForClientInfo() throws IOException {
        channel.basicConsume(RabbitMQUtils.Queue.CONSUMER_INFO_QUEUE.getName(), true, "myConsumerTag4", getClientInfoConsumer());
    }

    public DefaultConsumer getClientInfoConsumer() {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                ClientDataReturn clientDataReturn = SerializationUtils.deserialize(body);

                Optional<RegisteredClient> registeredClientOptional = tryFindClientByName(clientDataReturn.clientName);

                if (registeredClientOptional.isPresent()) {
                    RegisteredClient registeredClient = registeredClientOptional.get();

                    synchronized (registeredClient) {
                        if(registeredClient.executionDurations.size() > 5000) {
                            registeredClient.executionDurations.clear();
                        }
                        registeredClient.executionDurations.addAll(clientDataReturn.latestExecutionTimes);
                        registeredClient.setWattUsage(clientDataReturn.wattUsage);
                    }
                }
            }
        };
    }

    public Optional<RegisteredClient> tryFindClientByName(@NotNull String name) {
        synchronized (registeredClients) {
            return registeredClients.stream()
                    .filter(client -> client.getName().equals(name)).findFirst();
        }
    }
}
