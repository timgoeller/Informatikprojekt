import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

class Scheduler {
    private Queue<PrimeTask> openTasks;
    private List<PrimeTask> currentlyExecutingTasks;
    private List<PrimeTask> closedTasks;

    void addTask(int number) {
        openTasks.add(new PrimeTask(number));
    }

    void scheduleTasks(List<RegisteredClient> clients) {
        List<PrimeTask> finishedTasks = currentlyExecutingTasks.stream().filter(task -> task.completed).collect(Collectors.toList());
        currentlyExecutingTasks.removeAll(finishedTasks);
        closedTasks.addAll(finishedTasks);

        List<RegisteredClient> availableClients = clients.stream().filter(client -> client.tasksAssigned == 0).collect(Collectors.toList());
        for (RegisteredClient client : availableClients) {
            if(openTasks.isEmpty())
                break;
            assignAndStartTask(client, openTasks.poll());
        }
    }

    void assignAndStartTask(RegisteredClient client, PrimeTask task) {
        currentlyExecutingTasks.add(task);
    }

    public boolean tasksLeft() {
        return !closedTasks.isEmpty() || !currentlyExecutingTasks.isEmpty();
    }
}
