public class Task8 {

    public static int sumOfSums(int... numbers) {
        int cumulativeTotal = 0;

        for (int number : numbers) {
            int triangularSum = number * (number + 1) / 2;

            cumulativeTotal += triangularSum;

            System.out.println("Parameter " + number +
                    ": sum = " + triangularSum +
                    ", cumulative sum = " + cumulativeTotal);
        }

        return cumulativeTotal;
    }

    public static void main(String[] args) {

        int total = sumOfSums(4, 5, 10);

        System.out.println("Total sum: " + total);
    }
}
