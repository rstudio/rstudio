/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.core.ext.typeinfo;

import java.util.HashMap;
import java.util.Map;

public class JPackage {

  private final String name;

  private final Map types = new HashMap();

  JPackage(String name) {
    this.name = name;
  }

  public JClassType findType(String typeName) {
    String[] parts = typeName.split("\\.");
    return findType(parts);
  }

  public JClassType findType(String[] typeName) {
    return findTypeImpl(typeName, 0);
  }

  public String getName() {
    return name;
  }

  public JClassType getType(String typeName) throws NotFoundException {
    JClassType result = findType(typeName);
    if (result == null) {
      throw new NotFoundException();
    }
    return result;
  }

  public JClassType[] getTypes() {
    return (JClassType[]) types.values().toArray(TypeOracle.NO_JCLASSES);
  }

  public boolean isDefault() {
    return "".equals(name);
  }

  public String toString() {
    return "package " + name;
  }

  void addType(JClassType type) {
    types.put(type.getSimpleSourceName(), type);
  }

  JClassType findTypeImpl(String[] typeName, int index) {
    JClassType found = (JClassType) types.get(typeName[index]);
    if (found == null) {
      return null;
    } else if (index < typeName.length - 1) {
      return found.findNestedTypeImpl(typeName, index + 1);
    } else {
      return found;
    }
  }

  void remove(JClassType type) {
    Object removed = types.remove(type.getSimpleSourceName());
    // JDT will occasionally remove non-existent items, such as packages.
  }
}
