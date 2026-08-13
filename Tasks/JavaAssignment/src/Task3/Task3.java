class Task3 {

    public static void main(String[] args) {
        String a = "wow";
        String b = a;       // same as object a
        String c = "wow!";
        String d = c;       // same as object c

        boolean b1 = a == b;                 // true
        boolean b2 = d.equals(b + "!");     // "wow!" .equals("wow!") true
        boolean b3 = !c.equals(a);         // ! "wow!" .equal("wow") true

        if (b1 && b2 && b3) {
            System.out.println("Success");
        }
    }
}
