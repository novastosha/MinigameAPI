package dev.nova.gameapi.utils.api.general;

public class ModifiablePair<A,B> {

    private B b;
    private A a;

    public ModifiablePair(){}

    public ModifiablePair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public void setA(A a) {
        this.a = a;
    }

    public void setB(B b) {
        this.b = b;
    }
}
