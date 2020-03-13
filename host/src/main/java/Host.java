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



    /**
     *
     * @param rabbitMQHost IP-String for the RabbitMQ-Server
     * @param rabbitMQUser RabbitMQ-Username
     * @param rabbitMQPass RabbitMQ-Password
     * @param rabbitMQPort Port of the RabbitMQ-Server
     * @throws IOException
     */
    Host(@NotNull String rabbitMQHost, @NotNull String rabbitMQUser, @NotNull String rabbitMQPass, @NotNull Integer rabbitMQPort) throws IOException {
        System.out.println(Runtime.getRuntime().availableProcessors());
        ConnectionFactory factory = new ConnectionFactory();
        scheduler = new Scheduler();
        factory.setHost(rabbitMQHost);
        factory.setPort(rabbitMQPort);
        factory.setUsername(rabbitMQUser);
        factory.setPassword(rabbitMQPass);

        initializeRabbitMQConnection(factory);
        listenForNewClients();
    }

    /**
     * Triggers creations of all defaults
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
        }
        catch (TimeoutException e) {
            System.out.println("Timeout while trying to connect to the RabbitMQ server");
        }
    }

    /**
     * Listen for client registrations in queue
     * @throws IOException
     */
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

    /**
     * Start scheduleing of tasks until they are finished
     * @param numbersToCheck
     * @throws IOException
     */
    void startTaskExecution(List<String> numbersToCheck) throws IOException {


        numbersToCheck.forEach(e -> scheduler.addTask(e));

        listenForClientInfo();
        getClientInfo();

        System.out.println("Press Enter key to continue...");
        try
        {
            System.in.read();
        }
        catch(Exception e)
        {}
        long startTime = System.currentTimeMillis();
        while(scheduler.tasksLeft()) {
            synchronized (registeredClients) {
                scheduler.scheduleTasks(registeredClients, channel);
//                scheduler.scheduleTasksWattUsage(registeredClients, channel);
            }
        }
        System.out.println("Finished!");
        System.out.println(registeredClients.get(0).executionDurations.size());
        long endTime = System.currentTimeMillis();
        System.out.println(endTime-startTime);
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
                                if(scheduler.tasksLeft()) {
                                    System.out.println("Client: " + registeredClient.executionDurations.size() + " " + registeredClient.getName());
                                }
                            }
                        }
                    }
                });
    }



    public void getClientInfo()throws IOException{
        channel.basicConsume(RabbitMQUtils.Queue.CONSUMER_INFO_QUEUE.getName(), true, "myConsumerTag6",
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException{
                        ClientDataReturn clientDataReturn = SerializationUtils.deserialize(body);

                        for(RegisteredClient rc : registeredClients){
                            if(clientDataReturn.clientName.equals(rc.getName())){
                                rc.setWattUsage(clientDataReturn.wattUsage);
                                System.out.println(rc.getName() + ", "+ rc.getWattUsage() );
                            }
                        }

                    }
                });

    }
}
