import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.jetbrains.annotations.NotNull;

public class Config {

    @Parameter(names = { "-rh"}, description = "RabbitMQ host", required = true)
    String hostIP;

    @Parameter(names = { "-u"}, description = "RabbitMQ username", required = true)
    String username;

    @Parameter(names = { "-pa"}, description = "RabbitMQ password", required = true)
    String password;

    @Parameter(names = { "-p"}, description = "RabbitMQ port")
    Integer port = 5672;

    private Config() {}

    /**
     * Creates a config object using the CLI arguments
     * @param argv CLI arguments
     * @return Config instance instantiated with CLI arguments
     */
    public static Config readConfigFromCLIArgs(@NotNull String[] argv) {

        Config config = new Config();

        JCommander.newBuilder()
                .addObject(config)
                .build()
                .parse(argv);

        return config;
    }
}
