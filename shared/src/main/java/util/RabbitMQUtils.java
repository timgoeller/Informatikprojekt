package util;

public class RabbitMQUtils {
    public final static String PRODUCER_EXCHANGE_NAME = "producer_events";
    public final static String CONSUMER_EXCHANGE_NAME = "consumer_events";

    public static enum Queue
    {
        CONSUMER_REGISTRATION_QUEUE("consumer_registration");

        private String name;

        Queue(String channelName) {
            this.name = channelName;
        }

        public String getName() {
            return name;
        }
    }
}
