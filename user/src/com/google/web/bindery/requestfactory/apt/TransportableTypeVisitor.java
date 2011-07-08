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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/**
 * Scans a TypeMirror to determine if it can be transported by RequestFactory.
 */
class TransportableTypeVisitor extends TypeVisitorBase<Boolean> {

  /**
   * AutoBeans supports arbitrary parameterizations, but there's work that needs
   * to be done on the Request serialization code to support arbitrarily-complex
   * parameterizations. For the moment, we'll disallow anything other than a
   * one-level parameterization.
   */
  private boolean allowNestedParameterization = true;

  @Override
  public Boolean visitDeclared(DeclaredType t, State state) {
    if (t.asElement().getKind().equals(ElementKind.ENUM)) {
      return true;
    }
    if (state.types.isAssignable(t, state.entityProxyType)
        || state.types.isAssignable(t, state.valueProxyType)) {
      TypeElement proxyElement = (TypeElement) t.asElement();
      state.maybeScanProxy(proxyElement);
      state.requireMapping(proxyElement);
      return true;
    }
    if (state.types.isAssignable(t, state.entityProxyIdType)) {
      DeclaredType asId = (DeclaredType) State.viewAs(state.entityProxyIdType, t, state);
      if (asId.getTypeArguments().isEmpty()) {
        return false;
      }
      return asId.getTypeArguments().get(0).accept(this, state);
    }
    for (DeclaredType valueType : getValueTypes(state)) {
      if (state.types.isAssignable(t, valueType)) {
        return true;
      }
    }
    if (state.types.isAssignable(t, state.findType(List.class))
        || state.types.isAssignable(t, state.findType(Set.class))) {
      if (!allowNestedParameterization) {
        return false;
      }
      allowNestedParameterization = false;
      DeclaredType asCollection =
          (DeclaredType) State.viewAs(state.findType(Collection.class), t, state);
      if (asCollection.getTypeArguments().isEmpty()) {
        return false;
      }
      return t.getTypeArguments().get(0).accept(this, state);
    }
    return false;
  }

  @Override
  public Boolean visitPrimitive(PrimitiveType x, State state) {
    return true;
  }

  @Override
  public Boolean visitTypeVariable(TypeVariable t, State state) {
    if (t.equals(t.getUpperBound())) {
      /*
       * Weird case seen in Eclipse with self-parameterized type variables such
       * as <T extends Enum<T>>.
       * 
       * TODO(bobv): Should intersection types be allowed at all? They don't
       * seem to make much sense in the most-derived interface types, since the
       * RF Generator won't know how to implement them.
       */
      return state.types.erasure(t).accept(this, state);
    }
    // Allow <T extends FooProxy>
    return t.getUpperBound().accept(this, state);
  }

  @Override
  public Boolean visitWildcard(WildcardType t, State state) {
    // Allow List<? extends FooProxy>
    return state.types.erasure(t).accept(this, state);
  }

  @Override
  protected Boolean defaultAction(TypeMirror arg0, State arg1) {
    return false;
  }

}