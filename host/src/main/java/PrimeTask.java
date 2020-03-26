import java.util.ArrayList;
import java.util.Arrays;

class PrimeTask {
    public RegisteredClient assignedClient;
    String numberRowToCheck;
    boolean completed = false;
    boolean[] completedArr;
    boolean isPrime = false;
    boolean[] isPrimeArr;
    ArrayList<String> numbers = new ArrayList<>();

    PrimeTask(String numberRowToCheck) {
        this.numberRowToCheck = numberRowToCheck;
        numbers.addAll(Arrays.asList(numberRowToCheck.split(",", 0)));
        isPrimeArr = new boolean[numbers.size()];
        completedArr = new boolean[numbers.size()];
    }
}
