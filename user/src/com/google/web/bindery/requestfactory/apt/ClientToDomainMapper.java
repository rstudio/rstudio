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
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/**
 * Uses information in a State object to convert client types to their domain
 * equivalents. This types assumes that any incoming type has already been
 * determined to be a transportable type.
 */
class ClientToDomainMapper extends TypeVisitorBase<TypeMirror> {
  public static class UnmappedTypeException extends RuntimeException {
    private final TypeMirror clientType;

    public UnmappedTypeException() {
      super();
      clientType = null;
    }

    public UnmappedTypeException(TypeMirror clientType) {
      super("No domain type resolved for " + clientType.toString());
      this.clientType = clientType;
    }

    public TypeMirror getClientType() {
      return clientType;
    }
  }

  @Override
  public TypeMirror visitDeclared(DeclaredType x, State state) {
    if (x.asElement().getKind().equals(ElementKind.ENUM)) {
      // Enums map to enums
      return x;
    }
    if (state.types.isAssignable(x, state.entityProxyType)
        || state.types.isAssignable(x, state.valueProxyType)) {
      // FooProxy -> FooDomain
      /*
       * TODO(bobv): This if statement should be widened to baseProxy to support
       * heterogenous collections of any proxy type. The BaseProxy interface
       * would also need to be annotated with an @ProxyFor mapping. This can be
       * done once RFIV is removed, since it only allows homogenous collections.
       */
      TypeElement domainType =
          (TypeElement) state.getClientToDomainMap().get(state.types.asElement(x));
      if (domainType == null) {
        return defaultAction(x, state);
      }
      return domainType.asType();
    }
    if (state.types.isAssignable(x, state.entityProxyIdType)) {
      // EntityProxyId<FooProxy> -> FooDomain
      return convertSingleParamType(x, state.entityProxyIdType, 0, state);
    }
    if (state.types.isAssignable(x, state.requestType)) {
      // Request<FooProxy> -> FooDomain
      return convertSingleParamType(x, state.requestType, 0, state);
    }
    if (state.types.isAssignable(x, state.instanceRequestType)) {
      // InstanceRequest<FooProxy, X> -> FooDomain
      return convertSingleParamType(x, state.instanceRequestType, 1, state);
    }
    for (DeclaredType valueType : getValueTypes(state)) {
      if (state.types.isAssignable(x, valueType)) {
        // Value types map straight through
        return x;
      }
    }
    if (state.types.isAssignable(x, state.findType(List.class))
        || state.types.isAssignable(x, state.findType(Set.class))) {
      // Convert Set,List<FooProxy> to Set,List<FooDomain>
      TypeMirror param = convertSingleParamType(x, state.findType(Collection.class), 0, state);
      return state.types.getDeclaredType((TypeElement) state.types.asElement(x), param);
    }
    return defaultAction(x, state);
  }

  @Override
  public TypeMirror visitNoType(NoType x, State state) {
    if (x.getKind().equals(TypeKind.VOID)) {
      // Pass void through
      return x;
    }
    // Here, x would be NONE or PACKAGE, neither of which make sense
    return defaultAction(x, state);
  }

  @Override
  public TypeMirror visitPrimitive(PrimitiveType x, State state) {
    // Primitives pass through
    return x;
  }

  @Override
  public TypeMirror visitTypeVariable(TypeVariable x, State state) {
    // Convert <T extends FooProxy> to FooDomain
    return x.getUpperBound().accept(this, state);
  }

  @Override
  public TypeMirror visitWildcard(WildcardType x, State state) {
    // Convert <? extends FooProxy> to FooDomain
    return state.types.erasure(x).accept(this, state);
  }

  /**
   * Utility method to convert a {@code Foo<BarProxy> -> BarDomain}. The
   * {@code param} parameter specifies the index of the type paramater to
   * extract.
   */
  protected TypeMirror convertSingleParamType(DeclaredType x, DeclaredType convertTo, int param,
      State state) {
    DeclaredType converted = (DeclaredType) State.viewAs(convertTo, x, state);
    if (converted == null) {
      return state.types.getNoType(TypeKind.NONE);
    }
    if (converted.getTypeArguments().isEmpty()) {
      return defaultAction(x, state);
    }
    return converted.getTypeArguments().get(param).accept(this, state);
  }

  @Override
  protected TypeMirror defaultAction(TypeMirror x, State state) {
    throw new UnmappedTypeException(x);
  }
}
