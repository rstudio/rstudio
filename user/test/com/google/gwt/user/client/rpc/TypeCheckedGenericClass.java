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

import java.util.HashMap;

/**
 * A class with a server custom fields serializer that does not inherit from
 * {@link com.google.gwt.user.server.rpc.ServerCustomFieldSerializer}, but that
 * does include type checking instantiate and deserialize methods.
 * 
 * This class has the ability to record a marker field. Its intended use is for
 * a server side instantiate/deserialize to set the marker to verify that the
 * instantiate/deserialize was invoked correctly.
 * 
 * @param <X> the key type for the hash map field
 * @param <Y> the value type for the hash map field
 */
public class TypeCheckedGenericClass<X, Y> implements IsSerializable {
  /**
   * Public hash map to insert test elements.
   */
  public HashMap<X, Y> hashField = null;

  private X markerKey = null;
  private Y markerValue = null;

  public TypeCheckedGenericClass() {
    hashField = new HashMap<X, Y>();
  }

  public void setMarker(X key, Y value) {
    markerKey = key;
    markerValue = value;
  }

  public X getMarkerKey() {
    return markerKey;
  }

  public Y getMarkerValue() {
    return markerValue;
  }
}
