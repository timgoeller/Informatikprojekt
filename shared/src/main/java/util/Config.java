package util;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Config {

    @Parameter(names = { "-rh"}, description = "RabbitMQ host", required = true)
    public String hostIP;

    @Parameter(names = { "-u"}, description = "RabbitMQ username", required = true)
    public String username;

    @Parameter(names = { "-pa"}, description = "RabbitMQ password", required = true)
    public String password;

    @Parameter(names = { "-p"}, description = "RabbitMQ port")
    public Integer port = 5672;

    private Config() {}

    /**
     * Creates a config object using the CLI arguments
     * @param argv CLI arguments
     * @return util.Config instance instantiated with CLI arguments
     */
    public static Config readConfigFromCLIArgs(@NotNull String[] argv) {

        Config config = new Config();

        JCommander.newBuilder()
                .addObject(config)
                .build()
                .parse(argv);

        return config;
    }

    public static Config readConfigFromCLIArgsWithAdditionalConfig(@NotNull String[] argv, @NotNull List<?> additonalConfig) {

        Config config = new Config();

        JCommander.Builder builder = JCommander.newBuilder().addObject(config);

        additonalConfig.forEach(builder::addObject);

        builder.build().parse(argv);

        return config;
    }
}
