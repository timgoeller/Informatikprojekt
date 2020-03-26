import java.io.Serializable;
import java.util.List;

public class ClientDataReturn implements Serializable {

    public double wattUsage;

    public List<Long> latestExecutionTimes;

    String clientName;

    public ClientDataReturn(String clientName) {
        this.clientName = clientName;
    }
}
