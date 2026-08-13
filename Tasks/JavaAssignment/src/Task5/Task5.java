import java.util.Scanner;
import java.util.Arrays;

public class Task5 {
	public static void main(String[] args){
		Scanner scan = new Scanner(System.in);

		int[] nom = new int[3];

			// collectiong data input
		for (int i = 0; i < nom.length; i++){
			System.out.println("Enter the nom🤑" + (i + 1) + ": ");
			nom[i] = scan.nextInt();
		}

			// checking data noms if equal
		boolean  allEqual = true;
		for (int i = 1; i < nom.length; i++){
			if (nom[i] != nom[0]){
				allEqual = false;
				break;
			}
		}

			// print noms data
		System.out.println("\nNoms entered: " + Arrays.toString(nom));

		if (allEqual){
			System.out.println("All noms are equal!🤑🤑🤑. ");
		} else {
			int large = nom[0];
			for (int i = 1; i < nom.length; i++){
				if(nom[i] > large){
					large = nom[i];
				}
			}

			System.out.println("The largest nom is🤑: " + large + "👍");

				//  i'm just add this for view typd numbers in nom
			System.out.print("Largest: 🫸");
                    for (int i = 0; i < nom.length; i++){
                        if (nom[i] == large){
                            System.out.print("👉" + nom[i] + "👈");
						} else {
                            System.out.print(nom[i]);
                    	}
                    	if (i < nom.length - 1){
                        	System.out.print(", ");
                	}
            	}
        	System.out.println("🫷");
        }
		scan.close();
	}
}
