import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import util.PrimeUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static util.RabbitMQUtils.Queue;
import static util.RabbitMQUtils.*;

public class Client {

    public final List<Long> latestExecutionTimes = Collections.synchronizedList(new ArrayList<>());
    Timer dataTimer = new Timer();
    Boolean dataTimerRunning = false;

    private ClientInfoCollector clientInfoCollector = new ClientInfoCollector();
    private Channel channel;
    private String name;
    TimerTask sendClientInfo = new TimerTask() {
        @Override
        public void run() {
            ClientDataReturn clientDataReturn = new ClientDataReturn(name);
            clientDataReturn.wattUsage = ClientInfoCollector.getWattUsage();
            synchronized (latestExecutionTimes) {
                clientDataReturn.latestExecutionTimes = new ArrayList<>(latestExecutionTimes);
                latestExecutionTimes.clear();
            }
            try {
                if (!(Double.isNaN(clientDataReturn.wattUsage))) {
                    channel.basicPublish(CONSUMER_EXCHANGE_NAME, Queue.CONSUMER_INFO_QUEUE.getName(), null, SerializationUtils.serialize(clientDataReturn));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * @param rabbitMQHost IP-String for the RabbitMQ-Server
     * @param rabbitMQUser RabbitMQ-Username
     * @param rabbitMQPass RabbitMQ-Password
     * @param rabbitMQPort Port of the RabbitMQ-Server
     * @param clientName   Name of this client. Has to be unique in the host-client connection
     * @throws IOException
     */
    Client(@NotNull String rabbitMQHost, @NotNull String rabbitMQUser, @NotNull String rabbitMQPass, @NotNull Integer rabbitMQPort, @NotNull String clientName) throws IOException, InterruptedException {
        name = clientName;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQHost);
        factory.setPort(rabbitMQPort);
        factory.setUsername(rabbitMQUser);
        factory.setPassword(rabbitMQPass);

        initializeRabbitMQConnection(factory);
        listenForTasks();
        notifyProducer();

        clientInfoCollector.clientWatt();
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
            createClientQueue(channel);
        } catch (TimeoutException e) {
            System.out.println("Timeout while trying to connect to the RabbitMQ server");
        }
    }

    /**
     * Notify producer that this client is now available
     *
     * @throws IOException
     */
    private void notifyProducer() throws IOException {
        System.out.println("Notifying publisher of creation...");
        channel.basicPublish(CONSUMER_EXCHANGE_NAME, Queue.CONSUMER_REGISTRATION_QUEUE.getName(), null, name.getBytes());
    }

    /**
     * Create the queue for sending data to this client
     *
     * @param channel
     * @throws IOException
     */
    private void createClientQueue(@NotNull Channel channel) throws IOException {
        System.out.println("Declaring custom queue for data exchange...");
        //queueDeclare(name, durable, exclusive, autoDelete, arguments)
        channel.queueDeclare(getProductionQueueName(), false, false, true, null);
        System.out.println("Custom queue declared successfully");

        System.out.println("Binding custom queue for data exchange...");
        channel.queueBind(getProductionQueueName(), PRODUCER_EXCHANGE_NAME, getProductionQueueName());
        System.out.println("Binding of custom queue completed successfully");
    }

    /**
     * Listen for incoming data packets
     *
     * @throws IOException
     */
    private void listenForTasks() throws IOException {

        channel.basicQos(1); // accept only one unack-ed message at a time (see below)

        DeliverCallback deliverCallback = (name, delivery) -> {
            String numberToCheck = new String(delivery.getBody());
            try {
                doWork(numberToCheck);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        boolean autoAck = false;
        channel.basicConsume(getProductionQueueName(), autoAck, deliverCallback, consumerTag -> {
        });
    }

    private void doWork(String number) throws IOException, InterruptedException {
        String[] numbers = number.split(",", 0);
        for (String n : numbers) {
            new Task().run(Integer.valueOf(n));
        }
    }

    /**
     * @param numberToCheck
     * @throws IOException
     */
    private void executeTask(Integer numberToCheck) throws IOException {
        if (!dataTimerRunning) {
            triggerDataCollection();
        }

        long startTime = System.nanoTime();
        boolean isPrime = PrimeUtil.isPrimeNumber(numberToCheck);
        long endTime = System.nanoTime();
        synchronized (latestExecutionTimes) {
            latestExecutionTimes.add(endTime - startTime);
        }
        ClientReturn clientReturn = new ClientReturn();
        clientReturn.isPrime = isPrime;
        clientReturn.numberToCheck = numberToCheck;
        clientReturn.name = name;
        channel.basicPublish(CONSUMER_EXCHANGE_NAME, Queue.CONSUMER_DATA_RETURN_QUEUE.getName(), null, SerializationUtils.serialize(clientReturn));
    }

    /**
     * Start collecting client data in a fixed interval
     */
    private void triggerDataCollection() {
        synchronized (dataTimerRunning) {
            if (!dataTimerRunning) {
                dataTimerRunning = true;
                System.out.println("Starting data timers...");
                dataTimer.scheduleAtFixedRate(sendClientInfo, 100, 1000);
                System.out.println("Started data timers");
            }
        }

    }

    private String getProductionQueueName() {
        return Queue.CONSUMER_PRODUCTION_QUEUE.getName() + "_" + name;
    }

    public class Task extends Thread {
        public void run(Integer numberToCheck) throws IOException, InterruptedException {
            boolean b = true;
            if (!dataTimerRunning) {
                triggerDataCollection();
            }

            long startTime = System.nanoTime();
            boolean isPrime = PrimeUtil.isPrimeNumber(numberToCheck);
            long endTime = System.nanoTime();
            synchronized (latestExecutionTimes) {
                latestExecutionTimes.add(endTime - startTime);
            }
            ClientReturn clientReturn = new ClientReturn();
            clientReturn.isPrime = isPrime;
            clientReturn.numberToCheck = numberToCheck;
            clientReturn.name = name;
            channel.basicPublish(CONSUMER_EXCHANGE_NAME, Queue.CONSUMER_DATA_RETURN_QUEUE.getName(), null, SerializationUtils.serialize(clientReturn));
        }
    }
}
