package proxy;

public class Name {
    String name;

    public Name() {
    }

    public Name(String name) {
        this.name = name;
    }

    String sayName() {
        return "I'm " + name;
    }
}
