import com.rabbitmq.client.Channel;
import util.RabbitMQUtils;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import static util.RabbitMQUtils.CONSUMER_EXCHANGE_NAME;

class Scheduler {
    private Queue<PrimeTask> openTasks;
    private List<PrimeTask> currentlyExecutingTasks;
    private List<PrimeTask> closedTasks;

    void addTask(int number) {
        openTasks.add(new PrimeTask(number));
    }

    void scheduleTasks(List<RegisteredClient> clients, Channel channel) throws IOException {
        List<PrimeTask> finishedTasks = currentlyExecutingTasks.stream().filter(task -> task.completed).collect(Collectors.toList());
        currentlyExecutingTasks.removeAll(finishedTasks);
        closedTasks.addAll(finishedTasks);

        List<RegisteredClient> availableClients = clients.stream().filter(client -> client.tasksAssigned == 0).collect(Collectors.toList());
        for (RegisteredClient client : availableClients) {
            if(openTasks.isEmpty())
                break;
            assignAndStartTask(client, openTasks.poll(), channel);
        }
    }

    void assignAndStartTask(RegisteredClient client, PrimeTask task, Channel channel) throws IOException {
        currentlyExecutingTasks.add(task);
        channel.basicPublish(client.getProductionQueueName(), client.getProductionQueueName(), null, task.toString().getBytes());
    }

    public boolean tasksLeft() {
        return !closedTasks.isEmpty() || !currentlyExecutingTasks.isEmpty();
    }
}
