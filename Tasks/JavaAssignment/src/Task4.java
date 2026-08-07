/** write a program that takes a string as input and checks if it is a palindrome A palindrome is a word, number, or other sequence of characters that reads the same forward and backward.

program should perform the following steps:

* use stringbuilder to create a new string that is a reverse of the input string
* use the equals() method to check if the input string and the reversed string are the same
* if the strings are the same, print "the input string is a palindrome"
* if the strings are not the same, print "the input string is not a palindrome"

*/


import java.util.Scanner;

public class Task4 {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.print("String Here 👉: ");
        String word = input.nextLine();

        //rvd Reverse the string
        String	 rvd = new StringBuilder(word).reverse().toString();

        // Check if it is a palindrome
        if (word.equals(rvd)) {
            System.out.println("The input string is a palindrome");
        } else {
            System.out.println("The input string is not a palindrome");
        }

        input.close();
    }
}
