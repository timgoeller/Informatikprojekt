package util;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import java.io.IOException;

public class RabbitMQUtils {
    public final static String PRODUCER_EXCHANGE_NAME = "producer_events";
    public final static String CONSUMER_EXCHANGE_NAME = "consumer_events";

    //docker run -d --restart always --ip="10.0.0.8" -p 5672:5672 -p 15672:15672 rabbitmq:3.6.6-management
    //docker run --ip="10.0.0.8" -p 5672:5672 -p 15672:15672 rabbitmq:3.6.6-management

    public enum Queue
    {
        CONSUMER_REGISTRATION_QUEUE("consumer_registration"), //registration of new consumers
        CONSUMER_PRODUCTION_QUEUE("consumer_prod"), //sending data to the clients
        CONSUMER_DATA_RETURN_QUEUE("data_return"), //return data to producer
        CONSUMER_INFO_QUEUE("consumer_info"); //send consumer info

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
        channel.queueDeclare(Queue.CONSUMER_REGISTRATION_QUEUE.getName(), false, false, false, null);
        channel.queueDeclare(Queue.CONSUMER_DATA_RETURN_QUEUE.getName(), false, false, false, null);
        channel.queueDeclare(Queue.CONSUMER_INFO_QUEUE.getName(), false, false, false, null);
        System.out.println("Queues declared successfully");

        System.out.println("Binding queues...");
        channel.queueBind(Queue.CONSUMER_REGISTRATION_QUEUE.getName(), CONSUMER_EXCHANGE_NAME, Queue.CONSUMER_REGISTRATION_QUEUE.getName());
        channel.queueBind(Queue.CONSUMER_DATA_RETURN_QUEUE.getName(), CONSUMER_EXCHANGE_NAME, Queue.CONSUMER_DATA_RETURN_QUEUE.getName());
        channel.queueBind(Queue.CONSUMER_INFO_QUEUE.getName(), CONSUMER_EXCHANGE_NAME, Queue.CONSUMER_INFO_QUEUE.getName());
        System.out.println("Binding of queues completed successfully");
    }
}
