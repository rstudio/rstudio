// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.inneroutersuper;

public class Main {

  public void onModuleLoad() {
    main(null);
  }

  public static void print(int x) {
//    System.out.println(x);
  }
  
  public static void main(String[] args) {
    Outer outer = new Outer(1);
    Outer.OuterIsSuper outerIsSuper = outer.new OuterIsSuper(2);

    {
      // QualifiedAlloc: outer becomes Outer or OuterIsSuper???
      Outer.OuterIsNotSuper outerIsNotSuper = outerIsSuper.new OuterIsNotSuper();
      int whatIsIt = outerIsNotSuper.getValue();
      print(whatIsIt);
    }

    {
      // [unqualified]Alloc: outer becomes Outer or OuterIsSuper???
      Outer.OuterIsNotSuper outerIsNotSuper = outerIsSuper.unqualifiedAlloc();
      int whatIsIt = outerIsNotSuper.getValue();
      print(whatIsIt);
    }

    {
      // QualifiedSupercall: outer becomes Outer or OuterIsSuper???
      Outer.TestQualifiedSuperCall testQualifiedSuperCall = new Outer.TestQualifiedSuperCall();
      int whatIsIt = testQualifiedSuperCall.getValue();
      print(whatIsIt);
    }
    
    {
      // UnqualifiedSupercall: outer becomes Outer or OuterIsSuper???
      Outer.TestUnqualifiedSuperCall testUnqualifiedSuperCall = outerIsSuper.new TestUnqualifiedSuperCall();
      int whatIsIt = testUnqualifiedSuperCall.getValue();
      print(whatIsIt);
    }
  }

  public static class Outer {

    protected final int value;

    public Outer(int i) {
      value = i;
    }

    public class OuterIsSuper extends Outer {

      public OuterIsSuper(int i) {
        super(i);
      }

      public OuterIsNotSuper unqualifiedAlloc() {
        return new OuterIsNotSuper();
      }

    }

    public class OuterIsNotSuper {

      public int getValue() {
        return value;
      }

    }
    
    public static class TestQualifiedSuperCall extends OuterIsNotSuper {
      public TestQualifiedSuperCall() {
        new Outer(1).new OuterIsSuper(2).super();
      }
    }

    public class TestUnqualifiedSuperCall extends OuterIsNotSuper {
      public TestUnqualifiedSuperCall() {
        super();
      }
    }
  }

}