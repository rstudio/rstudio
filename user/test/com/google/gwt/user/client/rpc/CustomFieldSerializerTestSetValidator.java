/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;

/**
 * Data validator used by the
 * {@link com.google.gwt.user.client.rpc.CustomFieldSerializerTest
 * CustomFieldSerializerTest} unit test.
 */
public class CustomFieldSerializerTestSetValidator {
  public static boolean isValid(ManuallySerializedClass manuallySerializedClass) {
    if (manuallySerializedClass == null) {
      return false;
    }

    StackTraceElement ste = manuallySerializedClass.getStackTraceElement();

    boolean toReturn = manuallySerializedClass.getA() == 4
        && manuallySerializedClass.getB() == 5
        && manuallySerializedClass.getC() == 6
        && manuallySerializedClass.getString().equals("bye")
        && ste.getClassName().equals("HighClass")
        && ste.getMethodName().equals("highClassMethod")
        && ste.getFileName().equals("HighClass.java");

    // Let the custom serializer restore this state
    manuallySerializedClass.setStackTraceElement(new StackTraceElement(
        "Should", "Not", "See", -1));

    return toReturn;
  }

  // Must be a non-null array with two == elements
  public static boolean isValid(
      ManuallySerializedImmutableClass[] manuallySerializedImmutables) {
    if (manuallySerializedImmutables == null) {
      return false;
    }

    if (manuallySerializedImmutables.length != 2) {
      return false;
    }

    return manuallySerializedImmutables[0] == manuallySerializedImmutables[1];
  }

  public static boolean isValid(SerializableSubclass serializableSubclass) {
    if (serializableSubclass == null) {
      return false;
    }

    if (serializableSubclass.getD() != 4) {
      return false;
    }

    return isValid((ManuallySerializedClass) serializableSubclass);
  }
}
