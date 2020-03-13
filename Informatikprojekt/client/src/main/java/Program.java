import util.Config;
import util.PrimeUtil;

import java.io.IOException;
import java.util.ArrayList;

public class Program {

    public static void main(String[] argv) throws IOException, InterruptedException {
        ClientConfig clientConfig = new ClientConfig();
        Config config = Config.readConfigFromCLIArgsWithAdditionalConfig(argv, new ArrayList<Object>() {{add(clientConfig);}});

        Client client = new Client(config.hostIP, config.username, config.password, config.port, clientConfig.name);
    }

}
