import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import util.RabbitMQUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientInfo {

    static String wattUsage;

    public static void clientWatt(String name) throws IOException, InterruptedException {

        Runtime rt = Runtime.getRuntime();

        Process pr = Runtime.getRuntime().exec(
                new String[] { "bash", "-c", "echo jm*8FcpX | sudo -S powermetrics --show-process-energy" });


        new Thread(new Runnable() {
            public void run() {
                BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                String line = null;
                String[] trimLine;
                try {
                    while ((line = input.readLine()) != null) {
                        //Intel energy model derived package power (CPUs+GT+SA):
                        if(line.contains("(CPUs+GT+SA):")){
                            trimLine = line.trim().split(" ");
                            wattUsage = trimLine[7];
                            System.out.println(trimLine[7] + ", " + name);
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();

        pr.waitFor();
        pr.destroy();
    }

}


