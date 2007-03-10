/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.client.rpc;

/**
 * TODO: document me.
 */
public class InheritanceTestSetFactory {

  /**
   * TODO: document me.
   */
  public static class Shape implements IsSerializable {
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  /**
   * TODO: document me.
   */
  public static class Circle extends Shape {
    private String name;
  }

  /**
   * TODO: document me.
   */
  public static interface AnonymousClassInterface extends IsSerializable {
    void foo();
  }

  /**
   * This class is here to make the code generator think that there is atleast
   * on serializable subclass of the AnonymousClassInterface
   */
  public static class MyClass implements AnonymousClassInterface {
    public void foo() {
    }
  }

  /**
   * TODO: document me.
   */
  public class NonStaticInnerClass implements IsSerializable {
  }

  /**
   * TODO: document me.
   */
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

  /**
   * TODO: document me.
   */
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

  /**
   * TODO: document me.
   */
  public static class SerializableClassWithUnserializableClassField extends
      SerializableClass {
    UnserializableClass cls;
  }

  /**
   * TODO: document me.
   */
  public static class SerializableClassWithUnserializableObjectField extends
      SerializableClass {
    Object obj;
  }

  /**
   * TODO: document me.
   */
  public static class SerializableSubclass extends SerializableClass {
    public int getD() {
      return d;
    }

    public void setD(int d) {
      this.d = d;
    }

    private int d = 4;
  }

  /**
   * TODO: document me.
   */
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
