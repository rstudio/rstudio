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

import java.io.Serializable;

/**
 * Test data factory used by the
 * {@link com.google.gwt.user.client.rpc.InheritanceTest InheritanceTest} unit
 * test.
 */
public class InheritanceTestSetFactory {

  /**
   * Used to test <a
   * href="http://code.google.com/p/google-web-toolkit/issues/detail?id=1163">Issue
   * 1163</a>.
   */
  public static class AbstractClass implements IsSerializable {
  }

  /**
   * TODO: document me.
   */
  public static interface AnonymousClassInterface extends IsSerializable {
    void foo();
  }

  /**
   * This class is here to make the code generator think that there is at least
   * one serializable subclass of the AnonymousClassInterface.
   */
  public static class AnonymousClassInterfaceImplementor implements
      AnonymousClassInterface {
    public void foo() {
    }
  }

  /**
   * TODO: document me.
   */
  public static class Circle extends Shape {
    private String name;

    public native void doStuff() /*-{
      alert("foo");
    }-*/;
  }

  /**
   * TODO: document me.
   */
  public static class JavaSerializableBaseClass implements Serializable {
    private int field1 = -1;

    public JavaSerializableBaseClass() {
    }

    public JavaSerializableBaseClass(int field1) {
      this.field1 = field1;
    }

    @Override
    public int hashCode() {
      return field1;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      JavaSerializableBaseClass other = (JavaSerializableBaseClass) obj;
      return field1 == other.field1;
    }
  }

  /**
   * TODO: document me.
   */
  public static class JavaSerializableClass extends JavaSerializableBaseClass {
    private int field2 = -2;
    private boolean field3 = true;

    public JavaSerializableClass() {
    }

    public JavaSerializableClass(int field2) {
      this.field2 = field2;
    }

    @Override
    public int hashCode() {
      return super.hashCode() << 19 + field2;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      JavaSerializableClass other = (JavaSerializableClass) obj;
      return super.equals(other) && field2 == other.field2 && field3 == other.field3;
    }
  }

  /**
   * Used to test <a
   * href="http://code.google.com/p/google-web-toolkit/issues/detail?id=1163">Issue
   * 1163</a>.
   */
  public static interface MySerializableInterface extends IsSerializable {
  }

  /**
   * Used to test <a
   * href="http://code.google.com/p/google-web-toolkit/issues/detail?id=1163">Issue
   * 1163</a>.
   */
  public static interface MySerializableInterfaceSubtype extends
      MySerializableInterface {
  }

  /**
   * This class is here to make the code generator think that there is at least
   * one serializable subclass of the MySerializableInterfaceSubtype.
   */
  public static class MySerializableInterfaceSubtypeImplementor implements
      MySerializableInterfaceSubtype {
  }

  /**
   * TODO: document me.
   */
  public static class SerializableClass implements IsSerializable {
    protected int d = 4;

    int e = 5;

    private int a = 1;

    private int b = 2;

    private int c = 3;

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
  }

  /**
   * TODO: document me.
   */
  public static class SerializableClassWithTransientField extends
      SerializableClass {
    private transient Object obj;

    public Object getObj() {
      return obj;
    }

    public void setObj(Object obj) {
      this.obj = obj;
    }
  }

  /**
   * TODO: document me.
   */
  public static class SerializableSubclass extends SerializableClass {
    private int d = 4;

    public int getD() {
      return d;
    }

    public void setD(int d) {
      this.d = d;
    }
  }

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

  public static Circle createCircle() {
    Circle circle = new Circle();
    circle.setName("Circle");
    return circle;
  }

  public static SerializableClass createNonStaticInnerClass() {
    return new SerializableClass() {
      public String toString() {
        return "foo";
      }
    };
  }

  public static SerializableClass createSerializableClass() {
    return new SerializableClass();
  }

  public static SerializableClassWithTransientField createSerializableClassWithTransientField() {
    SerializableClassWithTransientField cls = new SerializableClassWithTransientField();
    cls.setObj("hello");
    return cls;
  }

  public static SerializableClass createSerializableSubclass() {
    return new SerializableSubclass();
  }
}
