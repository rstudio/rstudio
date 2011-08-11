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

/**
 * A class with no server custom field serializer with a member that does
 * have a type checked server custom field serializer.
 * 
 * The class tests deserialization in which type information is lost when
 * deserializing the class, but the system then tries to use a deserializaer
 * that expects type information (when deserializing the type checked member).
 * 
 * This class has the ability to record a marker field. Its intended use is for
 * a server side instantiate/deserialize to set the marker to verify that the
 * instantiate/deserialize was invoked correctly.
 * 
 * @param <X> the key type for the hash map field of the checkedField member
 * @param <Y> the value type for the hash map field of the checkedField member
 */
public class TypeUncheckedGenericClass<X, Y> implements IsSerializable {
  /**
   * Public hash map to insert test elements.
   */
  public TypeCheckedGenericClass<X, Y> checkedField = null;

  public TypeUncheckedGenericClass() {
    checkedField = new TypeCheckedGenericClass<X, Y>();
  }

  public void setMarker(X key, Y value) {
    checkedField.setMarker(key, value);
  }

  public X getMarkerKey() {
    return checkedField.getMarkerKey();
  }

  public Y getMarkerValue() {
    return checkedField.getMarkerValue();
  }
}
