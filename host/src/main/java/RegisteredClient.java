import util.RabbitMQUtils;

public class RegisteredClient {
    private String name;

    public RegisteredClient(String name) {
        this.name = name;
    }

    public String getProductionQueueName() {
        return RabbitMQUtils.Queue.CONSUMER_PRODUCTION_QUEUE.getName() + "_" + name;
    }
}
