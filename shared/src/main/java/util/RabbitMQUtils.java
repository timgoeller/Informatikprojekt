package util;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import java.io.IOException;

public class RabbitMQUtils {
    public final static String PRODUCER_EXCHANGE_NAME = "producer_events";
    public final static String CONSUMER_EXCHANGE_NAME = "consumer_events";

    public enum Queue
    {
        CONSUMER_REGISTRATION_QUEUE("consumer_registration"),
        CONSUMER_PRODUCTION_QUEUE("consumer_prod"),
        CONSUMER_DATA_RETURN_QUEUE("data_return");

        private String name;

        Queue(String channelName) {
            this.name = channelName;
        }

        public String getName() {
            return name;
        }
    }

    public static void CreateDefaultExchanges(Channel channel) throws IOException {
        System.out.println("Declaring exchanges...");
        //Producer->Consumer
        channel.exchangeDeclare(PRODUCER_EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        //Consumer->Producer
        channel.exchangeDeclare(CONSUMER_EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        System.out.println("Exchanges declared successfully");
    }

    public static void CreateDefaultQueues(Channel channel) throws IOException {
        System.out.println("Declaring queues...");
        //queueDeclare(name, durable, exclusive, autoDelete, arguments)
        channel.queueDeclare(Queue.CONSUMER_REGISTRATION_QUEUE.getName(), false, false, true, null);
        channel.queueDeclare(Queue.CONSUMER_PRODUCTION_QUEUE.getName(), false, false, true, null);
        System.out.println("Queues declared successfully");

        System.out.println("Binding queues...");
        channel.queueBind(Queue.CONSUMER_REGISTRATION_QUEUE.getName(), CONSUMER_EXCHANGE_NAME, Queue.CONSUMER_REGISTRATION_QUEUE.getName());
        channel.queueBind(Queue.CONSUMER_PRODUCTION_QUEUE.getName(), CONSUMER_EXCHANGE_NAME, Queue.CONSUMER_REGISTRATION_QUEUE.getName());
        System.out.println("Binding of queues completed successfully");
    }
}
