import java.util.ArrayList;
import java.util.Arrays;

class PrimeTask {
    public RegisteredClient assignedClient;
    Integer numberToCheck;
    boolean completed = false;
    boolean isPrime = false;
    ArrayList<String> numbers = new ArrayList<>();

    PrimeTask(Integer numerToCheck) {
        this.numberToCheck = numerToCheck;
    }
}
