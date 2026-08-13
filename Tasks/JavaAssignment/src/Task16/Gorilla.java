interface Animal {
    boolean feed(boolean timeToEat);
    void groom();
    void pet();
}

public class Gorilla implements Animal {

    @Override
    public boolean feed(boolean timeToEat) {
        if (timeToEat) {
            System.out.println("Feeding time! Putting gorilla food into cage.");
            return true;
        }

        System.out.println("Not feeding the gorilla right now.");
        return false;
    }

    @Override
    public void groom() {
        System.out.println("Grooming: lather, rinse, repeat.");
    }

    @Override
    public void pet() {
        System.out.println("Careful — petting the gorilla at your own risk!");
    }

    public static void main(String[] args) {
        Gorilla gorilla = new Gorilla();

        gorilla.feed(true);
        gorilla.groom();
        gorilla.pet();
    }
}
