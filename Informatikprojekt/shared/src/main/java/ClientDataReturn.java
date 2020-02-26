import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.SerializationUtils;
import util.RabbitMQUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import static util.RabbitMQUtils.CONSUMER_EXCHANGE_NAME;

public class ClientDataReturn implements Serializable {

    public String wattUsage;

    public List<Long> latestExecutionTimes;

    String clientName;

    public ClientDataReturn(String clientName) {
        this.clientName = clientName;
    }
}
