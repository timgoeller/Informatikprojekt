package util;

import java.util.ArrayList;
import java.util.List;

public class PrimeUtil {
    final static private int ANZAHLZAHLENPROMESSAGE = 1000;
    public static boolean isPrimeNumber(int number) {
        if(number == 2){
            return true;
        }

        if(number % 2 == 0){
            return false;
        }

        for(int i = 3; i * i <= number; i += 2) {
            if(number % i == 0) {
                return false;
            }
        }
        return true;
    }

    public static List<String> generateNumbers(int max) {
        System.out.println("Starting to generate numbers from 0 to " + max + "...");

        List<String> numbers = new ArrayList<>();
        String temp = "";

        for (int i = 1; i <= max; i++) {
            if(i%ANZAHLZAHLENPROMESSAGE != 0){
                temp += i + ",";
            }
            else{
                temp += i;
                numbers.add(temp);
                temp = "";
            }
        }

        System.out.println("Finished generating numbers");

        return numbers;
    }

}
