/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedFieldClass;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedNestedLists;

import java.util.HashSet;

/**
 * Data validator used by the
 * {@link com.google.gwt.user.client.rpc.TypeCheckedObjectsTest} unit tests.
 */
public class TypeCheckedObjectsTestSetValidator {
  public static final Integer markerKey = 12345;
  public static final String markerValue = "Marker";

  public static final HashSet<Integer> invalidMarkerKey = new HashSet<Integer>() {
    {
      add(12345);
    }
  };
  public static final HashSet<String> invalidMarkerValue = new HashSet<String>() {
    {
      add("Marker");
    }
  };
  
  public static final String expectedInvalidMessage = "Expected isValid exception";

  public static boolean isValid(TypeCheckedGenericClass<Integer, String> arg1) {
    if (arg1 == null) {
      return false;
    }

    if (!markerKey.equals(arg1.getMarkerKey())) {
      return false;
    }

    if (!markerValue.equals(arg1.getMarkerValue())) {
      return false;
    }

    if (arg1.hashField.size() != 1) {
      return false;
    }

    if (!arg1.hashField.containsKey(markerKey) || !arg1.hashField.containsValue(markerValue)) {
      return false;
    }

    return true;
  }

  public static boolean isValid(TypeCheckedFieldClass<Integer, String> arg1) {
    if (arg1 == null) {
      return false;
    }

    TypeCheckedGenericClass<Integer, String> field = arg1.getCheckedField();

    if (!markerKey.equals(field.getMarkerKey())) {
      return false;
    }

    if (!markerValue.equals(field.getMarkerValue())) {
      return false;
    }

    if (field.hashField.size() != 1) {
      return false;
    }

    if (!field.hashField.containsKey(markerKey) || !field.hashField.containsValue(markerValue)) {
      return false;
    }

    return true;
  }

  public static boolean isValid(TypeCheckedNestedLists arg1) {
    if (arg1 == null) {
      return false;
    }

    if (arg1.values.size() != 1) {
      return false;
    }
    
    TypeCheckedBaseClass baseClass = arg1.values.get(0);
    if (!(baseClass instanceof TypeCheckedInnerClass)) {
      return false;
    }
    TypeCheckedInnerClass innerClass = (TypeCheckedInnerClass) baseClass;
    
    if (innerClass.values.get(0).value != 12345) {
      return false;
    }

    if (innerClass.values.get(1).value != 67890) {
      return false;
    }

    if (!innerClass.name.equals("foo")) {
      return false;
    }
    
    return true;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static boolean isValid(TypeUncheckedGenericClass arg1) {
    if (arg1 == null) {
      return false;
    }

    Object markerKeyObject = arg1.getMarkerKey();
    Object markerValueObject = arg1.getMarkerValue();
    
    if (!(markerKeyObject instanceof Integer)) {
      return false;
    }
    
    if (!(markerValueObject instanceof String)) {
      return false;
    }
    
    return isValid(arg1.checkedField);
  }
}
