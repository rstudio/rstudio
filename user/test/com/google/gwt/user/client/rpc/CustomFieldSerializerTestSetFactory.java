// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.rpc;

public class CustomFieldSerializerTestSetFactory {
  public static class SerializableSubclass extends UnserializableClass
      implements IsSerializable {
    public int getD() {
      return d;
    }

    public void setD(int d) {
      this.d = d;
    }

    private int d = 4;
  }

  public static class UnserializableSubclass extends UnserializableClass {
  }

  public static SerializableSubclass createSerializableSubclass() {
    return new SerializableSubclass();
  }

  public static UnserializableClass createUnserializableClass() {
    return new UnserializableClass();
  }

  public static UnserializableSubclass createUnserializableSubclass() {
    return new UnserializableSubclass();
  }
}
