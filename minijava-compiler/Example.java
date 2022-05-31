class Example {
    public static void main(String[] args) {
    }
}

class A{
    int i;
    A a;
    
    public int foo(int i, int j) { int k; int l;  return i+j; }
    public boolean boo(int i, int j) {  return true; }
    public int bar(){ return 1; }
}

class B extends A {
    int i;

    public int foo(int i, int j) { return i+j; }
    public int[] foobar(boolean k){ int[] a; return a[1]; }
}
