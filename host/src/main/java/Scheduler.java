import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.SerializationUtils;
import util.RabbitMQUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static util.RabbitMQUtils.PRODUCER_EXCHANGE_NAME;

class Scheduler {
    private final Queue<PrimeTask> openTasks = new LinkedList<>();
    private final List<PrimeTask> currentlyExecutingTasks = Collections.synchronizedList(new ArrayList<>());
    private final List<PrimeTask> closedTasks = Collections.synchronizedList(new ArrayList<>());

    boolean listening = false;

    void addTask(String number) {
        openTasks.add(new PrimeTask(number));
    }

    /**
     * Runs the scheduler once
     * @param clients
     * @param channel
     * @throws IOException
     */
    void scheduleTasks(List<RegisteredClient> clients, Channel channel) throws IOException {
        if(!listening) {
            listenForReturns(channel);
            listening = true;
        }

        synchronized (currentlyExecutingTasks) {
            List<PrimeTask> finishedTasks = currentlyExecutingTasks.stream().filter(task -> task.completed).collect(Collectors.toList());
            currentlyExecutingTasks.removeAll(finishedTasks);
            closedTasks.addAll(finishedTasks);
        }

        List<RegisteredClient> availableClients = clients.stream().filter(client -> client.tasksAssigned <= 10).collect(Collectors.toList());
        for (RegisteredClient client : availableClients) {
            if(openTasks.isEmpty()){
                break;
            }
            assignAndStartTask(client, openTasks.poll(), channel);

        }
    }

    public void scheduleTasksWattUsage(List<RegisteredClient> clients, Channel channel) throws IOException {

        if(!listening) {
            listenForReturns(channel);
            listening = true;
        }
        synchronized (currentlyExecutingTasks) {
            List<PrimeTask> finishedTasks = currentlyExecutingTasks.stream().filter(task -> task.completed).collect(Collectors.toList());
            currentlyExecutingTasks.removeAll(finishedTasks);
            closedTasks.addAll(finishedTasks);
        }
        List<RegisteredClient> availableClients = clients.stream().filter(client -> client.tasksAssigned <= 10).collect(Collectors.toList());

        List<RegisteredClient> sortedRegClientList = availableClients.stream().sorted(Comparator.comparingDouble(RegisteredClient::getWattUsage)).collect(Collectors.toList());

        for (RegisteredClient sortedClient : sortedRegClientList) {
            if(openTasks.isEmpty()) {
                break;
            }
            assignAndStartTask(sortedClient, openTasks.poll(), channel);

        }
    }

    /**
     * Send task to client for execution
     * @param client
     * @param task
     * @param channel
     * @throws IOException
     */
    private void assignAndStartTask(RegisteredClient client, PrimeTask task, Channel channel) throws IOException {
        task.assignedClient = client;
        client.tasksAssigned++;
        currentlyExecutingTasks.add(task);
        channel.basicPublish(PRODUCER_EXCHANGE_NAME, client.getProductionQueueName(), null, task.numberToCheck.getBytes());
    }

    private void listenForReturns(Channel channel) throws IOException {
        channel.basicConsume(RabbitMQUtils.Queue.CONSUMER_DATA_RETURN_QUEUE.getName(), true, "myConsumerTag2",
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        ClientReturn clientReturn = SerializationUtils.deserialize(body);
                        Optional<PrimeTask> primeTask = getCurrentlyExecutedTask(clientReturn.numberToCheck);

                        if(primeTask.isPresent()) {
                            PrimeTask returnedTask = primeTask.get();
                            if (clientReturn.isPrime){
                                returnedTask.isPrimeArr[returnedTask.numbers.indexOf(String.valueOf(clientReturn.numberToCheck))] = true;
                            }else{
                                returnedTask.isPrimeArr[returnedTask.numbers.indexOf(String.valueOf(clientReturn.numberToCheck))] = false;
                            }
                            returnedTask.completedArr[returnedTask.numbers.indexOf(String.valueOf(clientReturn.numberToCheck))] = true;
//                                returnedTask.completed = true;
                            //sollte nur aus taskliste raus wenn alle zahlen completed sind, gerade nur wenn einer ist. ist noch ff von aenderung von 1 int pro task auf mehrere string pro task
                            returnedTask.assignedClient.tasksAssigned--;
                            //in closedtask muss noch hinzugefuegt werden

//                            Arrays.asList(returnedTask.isPrimeArr).contains(true);
                            if(allTrue(returnedTask.completedArr)){
                                returnedTask.completed = true;
                                closedTasks.add(returnedTask);
                                currentlyExecutingTasks.remove(returnedTask);
                            }
//                            failedTasks.removeIf(task -> task.getNumber().equals(returnedTask.getNumber()));
//                            System.out.println(clientReturn.numberToCheck + " is a Prime: " + returnedTask.isPrimeArr[returnedTask.numbers.indexOf(String.valueOf(clientReturn.numberToCheck))]);
                        }

                    }
                });



//        for (int i = 0; i <= 10; i++) {
//            new Task().run(channel);
//        }
    }

    boolean allTrue(boolean[] arr) {
        for(boolean b : arr){
            if(!b){
                return false;
            }
        }
        return true;
    }

    public Optional<PrimeTask> getCurrentlyExecutedTask(int number) {
        synchronized (currentlyExecutingTasks) {
//            return currentlyExecutingTasks.stream().filter(currentTask -> currentTask.getNumber().contains(String.valueOf(number))).findFirst();
            return currentlyExecutingTasks.stream().filter(currentTask -> currentTask.numbers.contains(String.valueOf(number))).findFirst();
//            return currentlyExecutingTasks.stream().filter(currentTask -> currentTask.numbers.get(currentTask.numbers.indexOf(number)).contains(String.valueOf(number))).findFirst();
//            return currentlyExecutingTasks.stream().filter(currentTask -> currentTask.containsNumber(number)).findFirst();

        }
    }

    boolean tasksLeft() {
        return (!openTasks.isEmpty() || !currentlyExecutingTasks.isEmpty());
    }

    void test(Function<String, String> test) {

    }
}
