import util.Config;
import util.PrimeUtil;

public class Program {

    public static void main(String[] argv) {
        Config config = Config.readConfigFromCLIArgs(argv);
        Client host = new Client(config.hostIP, config.username, config.password, config.port);
    }

}
