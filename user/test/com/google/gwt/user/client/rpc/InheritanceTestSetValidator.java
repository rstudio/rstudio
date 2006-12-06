// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableSubclass;

public class InheritanceTestSetValidator {
  public static boolean isValid(SerializableClass serializableClass) {
    if (serializableClass == null) {
      return false;
    }

    return serializableClass.getA() == 1 && serializableClass.getB() == 2
        && serializableClass.getC() == 3 && serializableClass.d == 4
        && serializableClass.e == 5;
  }

  public static boolean isValid(SerializableClassWithTransientField cls) {
    if (cls == null) {
      return false;
    }

    if (!isValid((SerializableClass) cls)) {
      return false;
    }

    if (cls.getObj() != null) {
      return false;
    }

    return true;
  }

  public static boolean isValid(SerializableSubclass serializableSubclass) {
    if (serializableSubclass == null) {
      return false;
    }

    if (serializableSubclass.getD() != 4) {
      return false;
    }

    return isValid((SerializableClass) serializableSubclass);
  }
}
