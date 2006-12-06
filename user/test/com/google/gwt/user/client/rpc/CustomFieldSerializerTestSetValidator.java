// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;

public class CustomFieldSerializerTestSetValidator {
  public static boolean isValid(SerializableSubclass serializableSubclass) {
    if (serializableSubclass == null) {
      return false;
    }

    if (serializableSubclass.getD() != 4) {
      return false;
    }

    return isValid((UnserializableClass) serializableSubclass);
  }

  public static boolean isValid(UnserializableClass unserializableClass) {
    if (unserializableClass == null) {
      return false;
    }

    return unserializableClass.getA() == 4 && unserializableClass.getB() == 5
      && unserializableClass.getC() == 6
      && unserializableClass.getObj().equals("bye");
  }
}
