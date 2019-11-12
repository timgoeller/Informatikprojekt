import java.util.ArrayList;
import java.util.List;

public class PrimeUtil {

    public static boolean isPrimeNumber(int number) {

        if (number % 2 == 0) return false;

        for(int i = 3; i * i <= number; i += 2) {
            if(number % i == 0)
                return false;
        }
        return true;
    }

    public static List<Integer> generateNumbers(int max) {
        System.out.println("Starting to generate numbers from 0 to " + max + "...");

        List<Integer> numbers = new ArrayList<Integer>();

        for (int i = 0; i <= max; i++) {
            numbers.add(i);
        }

        System.out.println("Finished generating numbers");

        return numbers;
    }

}
