package util;

import java.util.ArrayList;
import java.util.List;

public class PrimeUtil {
    final static private int ANZAHL_ZAHLEN_PRO_MESSAGE = 1000;

    public static boolean isPrimeNumber(int number) {
        if (number == 2) {
            return true;
        }

        if (number % 2 == 0) {
            return false;
        }

        for (int i = 3; i * i <= number; i += 2) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Generate String-rows seperated by ',' of numbers from 0 to max
     *
     * @param max
     * @return
     */
    public static List<String> generateNumberRows(int max) {
        System.out.println("Starting to generate numbers from 0 to " + max + "...");

        List<String> numbers = new ArrayList<>();
        StringBuilder temp = new StringBuilder();

        for (int i = 1; i <= max; i++) {
            if (i % ANZAHL_ZAHLEN_PRO_MESSAGE != 0) {
                temp.append(i).append(",");
            } else {
                temp.append(i);
                numbers.add(temp.toString());
                temp = new StringBuilder();
            }
        }

        System.out.println("Finished generating numbers");

        return numbers;
    }

}
