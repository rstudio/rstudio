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
package com.google.web.bindery.requestfactory.apt;

import java.util.Comparator;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

/**
 * Orders TypeElements by assignability, with most-derived types ordered first,
 * and then by name.
 */
class TypeComparator implements Comparator<TypeElement> {
  private final State state;

  public TypeComparator(State state) {
    this.state = state;
  }

  @Override
  public int compare(TypeElement a, TypeElement b) {
    DeclaredType typeA = state.types.getDeclaredType(a);
    DeclaredType typeB = state.types.getDeclaredType(b);
    if (state.types.isSameType(typeA, typeB)) {
      return 0;
    }
    if (state.types.isSubtype(typeA, typeB)) {
      return -1;
    }
    if (state.types.isSubtype(typeB, typeA)) {
      return 1;
    }
    return state.elements.getBinaryName(a).toString().compareTo(
        state.elements.getBinaryName(b).toString());
  }
}