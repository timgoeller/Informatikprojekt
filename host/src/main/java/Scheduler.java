import java.util.List;

class Scheduler {
    private List<PrimeTask> tasks;

    void addTask(int number) {
        tasks.add(new PrimeTask(number));
    }
}
