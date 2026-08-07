import java.util.Scanner;

public class Task7 {

	static int add(int a, int b) {
		return a + b;
	}

	static int subtract(int a, int b) {
		return a - b;
	}

	static int multiply(int a, int b) {
		return a * b;
	}

	static int devide(int a, int b) {
		if (b == 0) {
			System.out.println("Cannot devide by zero.");
			return 0;
		}
		return a / b;
	}

	public static void main(String[] args) {
		Scanner scn = new Scanner(System.in);
        // Task7 obj = new Task7();

		System.out.print("First number: ");
		int num1 = scn.nextInt();

		System.out.print("Second number: ");
		int num2 = scn.nextInt();

        System.out.println();
		System.out.println("Results");
		System.out.println("Addition: " + add(num1, num2));
		System.out.println("Subraction: " + subtract(num1, num2));
		System.out.println("Multiply: " + multiply(num1, num2));
		System.out.println("Devide: " + devide(num1, num2));

		scn.close();
	}
}
