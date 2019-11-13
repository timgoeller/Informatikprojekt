import util.Config;
import util.PrimeUtil;

import java.io.IOException;

public class Program {

    public static void main(String[] argv) throws IOException {
        Config config = Config.readConfigFromCLIArgs(argv);
        Host host = new Host(config.hostIP, config.username, config.password, config.port);
        host.StartTaskExecution(PrimeUtil.generateNumbers(100000));
    }

}
