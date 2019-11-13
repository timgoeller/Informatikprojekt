import com.beust.jcommander.Parameter;

public class ClientConfig {
    @Parameter(names = { "-name"}, description = "Client name", required = true)
    public String name;
}
