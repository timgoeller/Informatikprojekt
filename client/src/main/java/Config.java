import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class Config {

    @Parameter(names = { "-rh"}, description = "RabbitMQ host", required = true)
    String hostIP;

    @Parameter(names = { "-u"}, description = "RabbitMQ username", required = true)
    String username;

    @Parameter(names = { "-p"}, description = "RabbitMQ password", required = true)
    String password;

    private Config() {

    }

    public static Config readConfigFromCLIArgs(String[] argv) {

        Config config = new Config();

        JCommander.newBuilder()
                .addObject(config)
                .build()
                .parse(argv);

        return config;
    }
}
