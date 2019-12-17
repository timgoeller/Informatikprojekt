import com.rabbitmq.client.Channel;
import util.RabbitMQUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static util.RabbitMQUtils.PRODUCER_EXCHANGE_NAME;

public class RegisteredClient {
    private String name;
    public volatile int tasksAssigned = 0;

    public final List<Long> executionDurations = Collections.synchronizedList(new ArrayList<>());

    public RegisteredClient(String name, Channel channel) throws IOException {
        this.name = name;

        System.out.println("Declaring custom queue for data exchange with client " + name + "...");
        //queueDeclare(name, durable, exclusive, autoDelete, arguments)
        channel.queueDeclare(getProductionQueueName(), false, false, true, null);
        System.out.println("Custom queue declared successfully");

        System.out.println("Binding custom queue for data exchange...");
        channel.queueBind(getProductionQueueName(), PRODUCER_EXCHANGE_NAME, getProductionQueueName());
        System.out.println("Binding of custom queue completed successfully");

    }

    public String getProductionQueueName() {
        return RabbitMQUtils.Queue.CONSUMER_PRODUCTION_QUEUE.getName() + "_" + name;
    }

    public String getName() {
        return name;
    }
}
