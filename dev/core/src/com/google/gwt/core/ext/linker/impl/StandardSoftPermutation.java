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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.SoftPermutation;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The standard implementation of {@link SoftPermutation}.
 */
public class StandardSoftPermutation extends SoftPermutation {
  private final int id;
  private final SortedMap<SelectionProperty, String> propertyMap = new TreeMap<SelectionProperty, String>(
      StandardLinkerContext.SELECTION_PROPERTY_COMPARATOR);

  public StandardSoftPermutation(int id,
      Map<SelectionProperty, String> propertyMap) {
    this.id = id;
    this.propertyMap.putAll(propertyMap);
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public SortedMap<SelectionProperty, String> getPropertyMap() {
    return Collections.unmodifiableSortedMap(propertyMap);
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ID ").append(getId()).append(" = {");
    for (Map.Entry<SelectionProperty, String> entry : propertyMap.entrySet()) {
      sb.append(" ").append(entry.getKey().getName()).append(":").append(
          entry.getValue());
    }
    sb.append(" }");
    return sb.toString();
  }
}