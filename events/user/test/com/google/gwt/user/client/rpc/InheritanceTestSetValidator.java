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

import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableSubclass;

/**
 * TODO: document me.
 */
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
