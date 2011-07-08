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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.SimpleTypeVisitor6;

/**
 * Provides utility functions for type visitors.
 * 
 * @param <T> the return type for the visitor
 */
class TypeVisitorBase<T> extends SimpleTypeVisitor6<T, State> {
  /**
   * This method should be kept in sync with
   * {@code ValueCodex.getAllValueTypes()}. It doesn't use
   * {@code getAllValueTypes()} because a dependency on {@code ValueCodex} would
   * pull in a large number of dependencies into the minimal
   * {@code requestfactory-apt.jar}.
   */
  protected List<DeclaredType> getValueTypes(State state) {
    List<DeclaredType> types = new ArrayList<DeclaredType>();
    for (TypeKind kind : TypeKind.values()) {
      if (kind.isPrimitive()) {
        PrimitiveType primitiveType = state.types.getPrimitiveType(kind);
        TypeElement boxedClass = state.types.boxedClass(primitiveType);
        types.add((DeclaredType) boxedClass.asType());
      }
    }
    types.add(state.findType(BigDecimal.class));
    types.add(state.findType(BigInteger.class));
    types.add(state.findType(Date.class));
    types.add(state.findType(String.class));
    types.add(state.findType(Void.class));
    // Avoids compile-dependency bloat
    types.add(state.types.getDeclaredType(state.elements
        .getTypeElement("com.google.web.bindery.autobean.shared.Splittable")));
    return Collections.unmodifiableList(types);
  }
}
