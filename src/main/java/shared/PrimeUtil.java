package shared;

public class PrimeUtil {

    public static boolean isPrimeNumber(int number) {

        if (number % 2 == 0) return false;

        for(int i = 3; i * i <= number; i += 2) {
            if(number % i == 0)
                return false;
        }
        return true;
    }

}
