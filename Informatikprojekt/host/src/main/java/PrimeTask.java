class PrimeTask {
    Integer numberToCheck;
    boolean completed = false;
    boolean isPrime = false;
    public RegisteredClient assignedClient;

    PrimeTask(int numberToCheck) {
        this.numberToCheck = numberToCheck;
    }

    public Integer getNumber() {
        return numberToCheck;
    }
}
