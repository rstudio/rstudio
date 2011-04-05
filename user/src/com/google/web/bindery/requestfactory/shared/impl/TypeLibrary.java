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
package com.google.web.bindery.requestfactory.shared.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for querying, encoding, and decoding typed
 * payload data.
 */
public class TypeLibrary {

  public static final Collection<Class<?>> VALUE_TYPES;

  static {
    HashSet<Class<?>> valueTypes = new HashSet<Class<?>>();
    valueTypes.add(BigDecimal.class);
    valueTypes.add(BigInteger.class);
    valueTypes.add(Boolean.class);
    valueTypes.add(Byte.class);
    valueTypes.add(Character.class);
    valueTypes.add(Date.class);
    valueTypes.add(Double.class);
    valueTypes.add(Enum.class);
    valueTypes.add(Float.class);
    valueTypes.add(Integer.class);
    valueTypes.add(Long.class);
    valueTypes.add(Short.class);
    valueTypes.add(String.class);
    VALUE_TYPES = Collections.unmodifiableSet(valueTypes);
  }

  public static boolean isCollectionType(Class<?> type) {
    return type == List.class || type == Set.class;
  }

  public static boolean isProxyType(Class<?> type) {
    return !isValueType(type) && !isCollectionType(type);
  }

  public static boolean isValueType(Class<?> type) {
    return VALUE_TYPES.contains(type);
  }  
}
