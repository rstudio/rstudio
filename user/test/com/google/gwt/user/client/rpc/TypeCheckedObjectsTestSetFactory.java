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

import java.io.Serializable;
import java.util.HashSet;

/**
 * Generated test data for the
 * {@link com.google.gwt.user.client.rpc.TypeCheckedObjectsTest} unit tests.
 */
public class TypeCheckedObjectsTestSetFactory {

  /**
   * Used to test that the type checked field for an unchecked class is actually
   * type checked.
   */
  public static class TypeCheckedFieldClass<X, Y> implements Serializable {
    private TypeCheckedGenericClass<X, Y> checkedField = null;

    public TypeCheckedFieldClass() {
    }

    public TypeCheckedGenericClass<X, Y> getCheckedField() {
      return checkedField;
    }

    public void setCheckedField(TypeCheckedGenericClass<X, Y> value) {
      checkedField = value;
    }
  }

  /**
   * Used to test that the type checked base class for an unchecked class is
   * actually type checked.
   */
  public static class TypeCheckedSuperClass<X, Y> extends TypeCheckedGenericClass<X, Y> {
  }

  public static TypeCheckedFieldClass<HashSet<Integer>, String> createInvalidCheckedFieldClass() {
    TypeCheckedFieldClass<HashSet<Integer>, String> result =
      new TypeCheckedFieldClass<HashSet<Integer>, String>();
    TypeCheckedGenericClass<HashSet<Integer>, String> field =
      new TypeCheckedGenericClass<HashSet<Integer>, String>();
    field.hashField.put(TypeCheckedObjectsTestSetValidator.invalidMarkerKey,
        TypeCheckedObjectsTestSetValidator.markerValue);
    result.setCheckedField(field);
    return result;
  }

  public static
  TypeCheckedGenericClass<Integer, HashSet<String>> createInvalidCheckedGenericClass() {
    TypeCheckedGenericClass<Integer, HashSet<String>> result =
        new TypeCheckedGenericClass<Integer, HashSet<String>>();
    result.hashField.put(TypeCheckedObjectsTestSetValidator.markerKey,
        TypeCheckedObjectsTestSetValidator.invalidMarkerValue);
    return result;
  }

  public static TypeCheckedSuperClass<HashSet<Integer>, String> createInvalidCheckedSuperClass() {
    TypeCheckedSuperClass<HashSet<Integer>, String> result =
      new TypeCheckedSuperClass<HashSet<Integer>, String>();
    result.hashField.put(TypeCheckedObjectsTestSetValidator.invalidMarkerKey,
        TypeCheckedObjectsTestSetValidator.markerValue);
    return result;
  }

  public static TypeCheckedFieldClass<Integer, String> createTypeCheckedFieldClass() {
    TypeCheckedFieldClass<Integer, String> result =
      new TypeCheckedFieldClass<Integer, String>();
    TypeCheckedGenericClass<Integer, String> field =
      new TypeCheckedGenericClass<Integer, String>();
    field.hashField.put(TypeCheckedObjectsTestSetValidator.markerKey,
        TypeCheckedObjectsTestSetValidator.markerValue);
    result.setCheckedField(field);
    return result;
  }

  public static TypeCheckedGenericClass<Integer, String> createTypeCheckedGenericClass() {
    TypeCheckedGenericClass<Integer, String> result =
        new TypeCheckedGenericClass<Integer, String>();
    result.hashField.put(TypeCheckedObjectsTestSetValidator.markerKey,
        TypeCheckedObjectsTestSetValidator.markerValue);
    return result;
  }

  public static TypeCheckedSuperClass<Integer, String> createTypeCheckedSuperClass() {
    TypeCheckedSuperClass<Integer, String> result = new TypeCheckedSuperClass<Integer, String>();
    result.hashField.put(TypeCheckedObjectsTestSetValidator.markerKey,
        TypeCheckedObjectsTestSetValidator.markerValue);
    return result;
  }

  public static
  TypeUncheckedGenericClass<Integer, HashSet<String>> createInvalidUncheckedGenericClass() {
    TypeUncheckedGenericClass<Integer, HashSet<String>> result =
        new TypeUncheckedGenericClass<Integer, HashSet<String>>();
    result.setMarker(TypeCheckedObjectsTestSetValidator.markerKey,
        TypeCheckedObjectsTestSetValidator.invalidMarkerValue);
    result.checkedField.hashField.put(TypeCheckedObjectsTestSetValidator.markerKey,
        TypeCheckedObjectsTestSetValidator.invalidMarkerValue);
    return result;
  }

  public static TypeUncheckedGenericClass<Integer, String> createTypeUncheckedGenericClass() {
    TypeUncheckedGenericClass<Integer, String> result =
        new TypeUncheckedGenericClass<Integer, String>();
    result.setMarker(TypeCheckedObjectsTestSetValidator.markerKey,
        TypeCheckedObjectsTestSetValidator.markerValue);
    result.checkedField.hashField.put(TypeCheckedObjectsTestSetValidator.markerKey,
        TypeCheckedObjectsTestSetValidator.markerValue);
    return result;
  }
}
