package me.darknet.assembler;

public class Test {

    public static void main(String[] args) {
        exec(Test::example);
    }

    // the code we want to run
    static void example() {
        System.out.println("hi");
        throw new RuntimeException();
    }

    // runs a runnable
    static void exec(Runnable r) {
        r.run();
    }

}
