import java.util.ArrayList;

class PrimeTask {
    String numberToCheck;
    boolean completed = false;
    boolean[] completedArr;
    boolean isPrime = false;
    boolean[] isPrimeArr;
    ArrayList<String> numbers = new ArrayList<>();
    public RegisteredClient assignedClient;

    PrimeTask(String numberToCheck) {
        this.numberToCheck = numberToCheck;
        for (String s:numberToCheck.split(",",0)) {
            numbers.add(s);
        }
        isPrimeArr = new boolean[numbers.size()];
        completedArr = new boolean[numbers.size()];
    }

    public String getNumber() {
        return numberToCheck;
    }
    public boolean containsNumber(int number){
        for (String n:numbers) {
            if(Integer.getInteger(n) == number){
                return true;
            }
        }
        return false;
    }
}
