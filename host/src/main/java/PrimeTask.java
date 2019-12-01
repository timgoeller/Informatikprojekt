class PrimeTask {
    Integer numberToCheck;
    boolean completed = false;
    boolean isPrime = false;

    PrimeTask(int numberToCheck) {
        this.numberToCheck = numberToCheck;
    }

    public Integer getNumber() {
        return numberToCheck;
    }
}
