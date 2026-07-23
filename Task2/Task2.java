/*
    Create all the primitives (except long and double) with diff values.
    Concatenate them into a string and print it to the screen so it will print: H3110 w0rld 2.0 true

    byte zero = 0;
    string output = "W" + zero "w"
    System.out.println(output);
 */

void main() {

        /*
            double and long r not used
            so im use byte, short, int, float, char, and boolean
          */
    byte three = 3; // reference byte zero = 0;
    short one = 1;
    int nul = 0;
    float twoPointZero = 2.0f; // float that can display 2.0 and 'f' it's a float instead double

        /*
            stores the characters
            reference 'string out = "W" + zero "w"'
         */
    char W = 'H';
    char w = 'w';

    boolean t = true; // print as text "true"

                /*
                    ("")starts with an empty string. Treat everything that follow as text
                    (+)concatenation operator
                 */
    String output = "" + W + three + one + one + nul + " " +
            w + nul + "rld " + // 'rld' Last three letters of "world" typed directly into the string.
            twoPointZero + " " +
            t;

    IO.println(output);
}
