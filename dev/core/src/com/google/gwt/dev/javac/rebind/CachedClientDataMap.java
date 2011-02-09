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
package com.google.gwt.dev.javac.rebind;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple map class for storing cached rebind client data.  It ensures that
 * all stored data implement Serializable.
 */
public class CachedClientDataMap implements Serializable {
  
  private final Map<String, Serializable> dataMap;
  
  public CachedClientDataMap() {
    dataMap = new HashMap<String, Serializable>();
  }
  
  public Object get(String key) {
    return dataMap.get(key);
  }
  
  public void put(String key, Object value) {
    dataMap.put(key, (Serializable) value);
  }
}
