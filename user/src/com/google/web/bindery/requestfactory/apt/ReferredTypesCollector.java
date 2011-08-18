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

import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/**
 * Given a RequestFactory interface, return all RequestContext and proxy types
 * transitively referenced.
 */
class ReferredTypesCollector extends ExtraTypesScanner<Void> {

  /**
   * Finds TypeElements that we care about from TypeMirror API. This is used to
   * handle generic type signatures and supertypes.
   */
  private class ElementFinder extends TypeVisitorBase<Void> {
    @Override
    public Void visitDeclared(DeclaredType x, State state) {
      // Some generic types don't have elements
      Element elt = state.types.asElement(x);
      if (elt != null) {
        ReferredTypesCollector.this.scan(elt, state);
      }
      // Capture List<FooProxy>
      for (TypeMirror a : x.getTypeArguments()) {
        a.accept(this, state);
      }
      return null;
    }

    @Override
    public Void visitExecutable(ExecutableType x, State state) {
      x.getReturnType().accept(this, state);
      for (TypeMirror p : x.getParameterTypes()) {
        p.accept(this, state);
      }
      for (TypeMirror t : x.getTypeVariables()) {
        t.accept(this, state);
      }
      return null;
    }

    @Override
    public Void visitTypeVariable(TypeVariable x, State state) {
      return state.types.erasure(x).accept(this, state);
    }

    @Override
    public Void visitWildcard(WildcardType x, State state) {
      return state.types.erasure(x).accept(this, state);
    }
  }

  /**
   * Collect all RequestContext and proxy types reachable from the given
   * RequestFactory.
   */
  public static Set<TypeElement> collect(TypeElement requestFactory, State state) {
    ReferredTypesCollector c = new ReferredTypesCollector(state);
    c.scan(requestFactory, state);
    return c.seen;
  }

  private final Stack<TypeElement> currentType = new Stack<TypeElement>();
  private final SortedSet<TypeElement> seen;
  private final State state;

  private ReferredTypesCollector(State state) {
    seen = new TreeSet<TypeElement>(new TypeComparator(state));
    this.state = state;
  }

  @Override
  public Void visitExecutable(ExecutableElement x, State state) {
    if (shouldIgnore(x, state)) {
      return null;
    }
    ExecutableType xType = viewIn(currentType.peek(), x, state);
    xType.accept(new ElementFinder(), state);
    checkForAnnotation(x, state);
    return null;
  }

  @Override
  public Void visitType(TypeElement x, State state) {
    // Only look at proxies and contexts
    boolean isContext = state.types.isAssignable(x.asType(), state.requestContextType);
    boolean isFactory = state.types.isAssignable(x.asType(), state.requestFactoryType);
    boolean isProxy = state.types.isAssignable(x.asType(), state.baseProxyType);
    if (isProxy || isFactory || isContext) {
      currentType.push(x);
      try {
        // Ignore previously-seen types and factories
        if ((isContext || isProxy) && !seen.add(x)) {
          return null;
        }
        // Visit a proxy's supertypes
        if (isProxy) {
          x.getSuperclass().accept(new ElementFinder(), state);
          for (TypeMirror intf : x.getInterfaces()) {
            intf.accept(new ElementFinder(), state);
          }
        }
        // Visit methods
        scanAllInheritedMethods(x, state);
        // Scan for extra types
        checkForAnnotation(x, state);
      } finally {
        currentType.pop();
      }
    }
    return null;
  }

  @Override
  protected void scanExtraType(TypeElement extraType) {
    scan(extraType, state);
  }
}
