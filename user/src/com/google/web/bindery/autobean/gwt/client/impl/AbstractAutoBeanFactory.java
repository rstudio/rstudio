/*
 * Copyright 2010 Google Inc.
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
package com.google.web.bindery.autobean.gwt.client.impl;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.impl.EnumMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides base implementations of AutoBeanFactory methods.
 */
public abstract class AbstractAutoBeanFactory implements AutoBeanFactory, EnumMap {

  protected Map<Enum<?>, String> enumToStringMap;
  // This map is almost always one-to-one
  protected Map<String, List<Enum<?>>> stringsToEnumsMap;
  private JsniCreatorMap creatorMap;

  public <T> AutoBean<T> create(Class<T> clazz) {
    maybeInitializeCreatorMap();
    return creatorMap.create(clazz, this);
  }

  public <T, U extends T> AutoBean<T> create(Class<T> clazz, U delegate) {
    maybeInitializeCreatorMap();
    return creatorMap.create(clazz, this, delegate);
  }

  /**
   * EnumMap support.
   */
  public <E extends Enum<?>> E getEnum(Class<E> clazz, String token) {
    maybeInitializeEnumMap();
    List<Enum<?>> list = stringsToEnumsMap.get(token);
    if (list == null) {
      throw new IllegalArgumentException(token);
    }
    for (Enum<?> e : list) {
      if (e.getDeclaringClass().equals(clazz)) {
        @SuppressWarnings("unchecked")
        E toReturn = (E) e;
        return toReturn;
      }
    }
    throw new IllegalArgumentException(clazz.getName());
  }

  /**
   * EnumMap support.
   */
  public String getToken(Enum<?> e) {
    maybeInitializeEnumMap();
    String toReturn = enumToStringMap.get(e);
    if (toReturn == null) {
      throw new IllegalArgumentException(e.toString());
    }
    return toReturn;
  }

  protected abstract void initializeCreatorMap(JsniCreatorMap creatorMap);

  protected abstract void initializeEnumMap();

  private void maybeInitializeCreatorMap() {
    if (creatorMap == null) {
      creatorMap = JsniCreatorMap.createMap();
      initializeCreatorMap(creatorMap);
    }
  }

  private void maybeInitializeEnumMap() {
    if (enumToStringMap == null) {
      enumToStringMap = new HashMap<Enum<?>, String>();
      stringsToEnumsMap = new HashMap<String, List<Enum<?>>>();
      initializeEnumMap();
    }
  }
}
