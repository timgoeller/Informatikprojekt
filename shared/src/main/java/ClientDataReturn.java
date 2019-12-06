import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.SerializationUtils;
import util.RabbitMQUtils;

import java.io.IOException;
import java.io.Serializable;

import static util.RabbitMQUtils.CONSUMER_EXCHANGE_NAME;

public class ClientDataReturn implements Serializable {

    private String wattUsage;
    private Channel channel;

    public ClientDataReturn(Channel channel, String wattUsage){
        this.wattUsage = wattUsage;
        this.channel = channel;
    }

    public void sendDataToMQ() throws IOException{

        channel.basicPublish(CONSUMER_EXCHANGE_NAME, RabbitMQUtils.Queue.CONSUMER_INFO_QUEUE.getName(), null, SerializationUtils.serialize(this));

    }

}
