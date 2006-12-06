// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.rpc;

public class InheritanceTestSetFactory {
  
  public static class Shape implements IsSerializable {
    private String name;
    
    public String getName() {
      return name;
    }
    
    public void setName(String name) {
      this.name = name;
    }
  }
  
  public static class Circle extends Shape {
    private String name;
  }
  
  public static interface AnonymousClassInterface extends IsSerializable {
    void foo();
  }

  // This class is here to make the code generator think that there is atleast
  // on serializable subclass of the AnonymousClassInterface
  public static class MyClass implements AnonymousClassInterface {
    public void foo() {
    }
  }

  public class NonStaticInnerClass implements IsSerializable {
  }

  public static class SerializableClass implements IsSerializable {
    public int getA() {
      return a;
    }

    public int getB() {
      return b;
    }

    public int getC() {
      return c;
    }

    public void setA(int a) {
      this.a = a;
    }

    public void setB(int b) {
      this.b = b;
    }

    public void setC(int c) {
      this.c = c;
    }

    private int a = 1;

    private int b = 2;

    private int c = 3;
    
    protected int d = 4;
    
    int e = 5;
  }

  public static class SerializableClassWithTransientField extends
      SerializableClass {
    public Object getObj() {
      return obj;
    }

    public void setObj(Object obj) {
      this.obj = obj;
    }

    private transient Object obj;
  }

  public static class SerializableClassWithUnserializableClassField extends
      SerializableClass {
    UnserializableClass cls;
  }

  public static class SerializableClassWithUnserializableObjectField extends
      SerializableClass {
    Object obj;
  }

  public static class SerializableSubclass extends SerializableClass {
    public int getD() {
      return d;
    }

    public void setD(int d) {
      this.d = d;
    }

    private int d = 4;
  }

  public static class UnserializableClass {
  }

  public static Circle createCircle() {
    Circle circle = new Circle();
    circle.setName("Circle");
    return circle;
  }
  
  public static SerializableClass createSerializableClass() {
    return new SerializableClass();
  }

  public static SerializableClassWithTransientField createSerializableClassWithTransientField() {
    SerializableClassWithTransientField cls = new SerializableClassWithTransientField();
    cls.setObj("hello");
    return cls;
  }

  public static SerializableClassWithUnserializableClassField createSerializableClassWithUnserializableClassField() {
    return new SerializableClassWithUnserializableClassField();
  }

  public static SerializableClassWithUnserializableObjectField createSerializableClassWithUnserializableObjectField() {
    return new SerializableClassWithUnserializableObjectField();
  }

  public static SerializableClass createSerializableSubclass() {
    return new SerializableSubclass();
  }
}
