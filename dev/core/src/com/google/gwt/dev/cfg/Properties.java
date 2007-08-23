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
package com.google.gwt.dev.cfg;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A typed map of deferred binding properties.
 */
public class Properties {

  private final Map<String, Property> map = new HashMap<String, Property>();

  private Property[] propertiesLazyArray;

  /**
   * Creates the specified property, or returns an existing one by the specified
   * name if present.
   */
  public Property create(String name) {
    if (name == null) {
      throw new NullPointerException("name");
    }

    Property property = find(name);
    if (property == null) {
      property = new Property(name);
      map.put(name, property);
    }

    return property;
  }

  public Property find(String name) {
    return map.get(name);
  }

  public Iterator<Property> iterator() {
    return map.values().iterator();
  }

  /**
   * Lists all properties in sorted order.
   */
  public Property[] toArray() {
    if (propertiesLazyArray == null) {
      Collection<Property> properties = map.values();
      int n = properties.size();
      propertiesLazyArray = properties.toArray(new Property[n]);
      Arrays.sort(propertiesLazyArray);
    }
    return propertiesLazyArray;
  }
}
