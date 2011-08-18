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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementScanner6;

/**
 * Contains utility methods for traversing RequestFactory declarations.
 */
class ScannerBase<R> extends ElementScanner6<R, State> {

  /**
   * Poisons the given type if one or more of the annotation values are
   * non-null.
   */
  protected static void poisonIfAnnotationPresent(State state, TypeElement x,
      Annotation... annotations) {
    for (Annotation a : annotations) {
      if (a != null) {
        state.poison(x, Messages.redundantAnnotation(a.annotationType().getSimpleName()));
      }
    }
  }

  protected static ExecutableType viewIn(TypeElement lookIn, ExecutableElement methodElement, State state) {
    try {
      return (ExecutableType) state.types.asMemberOf(state.types.getDeclaredType(lookIn),
          methodElement);
    } catch (IllegalArgumentException e) {
      return (ExecutableType) methodElement.asType();
    }
  }

  @Override
  public final R scan(Element x, State state) {
    try {
      return super.scan(x, state);
    } catch (HaltException e) {
      throw e;
    } catch (Throwable e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      state.poison(x, sw.toString());
      throw new HaltException();
    }
  }

  /**
   * No parameters, name stars with "get" or is a boolean / Boolean isFoo hasFoo
   * method.
   */
  protected boolean isGetter(ExecutableElement x, State state) {
    String name = x.getSimpleName().toString();
    TypeMirror returnType = x.getReturnType();
    if (!x.getParameters().isEmpty()) {
      return false;
    }
    if (name.startsWith("get")) {
      return true;
    }
    if (name.startsWith("is") || name.startsWith("has")) {
      TypeMirror javaLangBoolean =
          state.types.boxedClass(state.types.getPrimitiveType(TypeKind.BOOLEAN)).asType();
      if (returnType.getKind().equals(TypeKind.BOOLEAN)
          || state.types.isSameType(returnType, javaLangBoolean)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Name starts with set, has one parameter, returns either null or something
   * assignable from the element's enclosing type.
   */
  protected boolean isSetter(ExecutableElement x, State state) {
    String name = x.getSimpleName().toString();
    TypeMirror returnType = x.getReturnType();

    if (x.getParameters().size() != 1) {
      return false;
    }
    if (!name.startsWith("set")) {
      return false;
    }
    if (returnType.getKind().equals(TypeKind.VOID)) {
      return true;
    }
    if (x.getEnclosingElement() != null
        && state.types.isAssignable(x.getEnclosingElement().asType(), returnType)) {
      return true;
    }
    return false;
  }

  protected R scanAllInheritedMethods(TypeElement x, State state) {
    R toReturn = DEFAULT_VALUE;
    List<ExecutableElement> methods = ElementFilter.methodsIn(state.elements.getAllMembers(x));
    for (ExecutableElement method : methods) {
      toReturn = scan(method, state);
    }
    return toReturn;
  }

  /**
   * Ignore all static initializers and methods defined in the base
   * RequestFactory interfaces
   */
  protected boolean shouldIgnore(ExecutableElement x, State state) {
    TypeMirror enclosingType = x.getEnclosingElement().asType();
    return x.getKind().equals(ElementKind.STATIC_INIT)
        || state.types.isSameType(state.objectType, enclosingType)
        || state.types.isSameType(state.requestFactoryType, enclosingType)
        || state.types.isSameType(state.requestContextType, enclosingType)
        || state.types.isSameType(state.entityProxyType, enclosingType)
        || state.types.isSameType(state.valueProxyType, enclosingType);
  }
}