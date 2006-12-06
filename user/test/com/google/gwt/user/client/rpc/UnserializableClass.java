// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.rpc;

/*
 * This class is defined outside of the CustomFieldSerializerTestSetFactory
 * because of a bug where custom field serializers cannot be inner classes and
 * custom field serializers must be in the same package as the class that they
 * serializer. Once we fix this bug we can move this class into the test set
 * factory.
 */
public class UnserializableClass {
  public int getA() {
    return a;
  }
  public int getB() {
    return b;
  }
  public int getC() {
    return c;
  }
  public Object getObj() {
    return obj;
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

  public void setObj(Object obj) {
    this.obj = obj;
  }

  private int a = 1;

  private int b = 2;

  private int c = 3;

  private Object obj = "hello";
}