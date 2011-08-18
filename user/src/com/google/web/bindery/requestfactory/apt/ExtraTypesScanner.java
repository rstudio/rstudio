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

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Looks for {@code ExtraTypes} annotations and calls
 * {@link #scanExtraType(TypeElement)}.
 */
abstract class ExtraTypesScanner<T> extends ScannerBase<T> {
  /**
   * Check an element for a declaration.
   */
  protected void checkForAnnotation(Element x, State state) {
    // Bug similar to Eclipse 261969 makes ExtraTypes.value() unreliable.
    for (AnnotationMirror mirror : x.getAnnotationMirrors()) {
      if (!state.types.isSameType(mirror.getAnnotationType(), state.extraTypesAnnotation)) {
        continue;
      }
      // The return of the Class[] value() method
      AnnotationValue value = mirror.getElementValues().values().iterator().next();
      // which is represented by a list
      @SuppressWarnings("unchecked")
      List<? extends AnnotationValue> valueList =
          (List<? extends AnnotationValue>) value.getValue();
      for (AnnotationValue clazz : valueList) {
        TypeMirror type = (TypeMirror) clazz.getValue();
        scanExtraType((TypeElement) state.types.asElement(type));
      }
    }
  }

  /**
   * Check a type and all of its supertypes for the annotation.
   */
  protected void checkForAnnotation(TypeElement x, State state) {
    // Check type's declaration
    checkForAnnotation((Element) x, state);
    // Look at superclass, if it exists
    if (!x.getSuperclass().getKind().equals(TypeKind.NONE)) {
      checkForAnnotation((TypeElement) state.types.asElement(x.getSuperclass()), state);
    }
    // Check super-interfaces
    for (TypeMirror intf : x.getInterfaces()) {
      checkForAnnotation((TypeElement) state.types.asElement(intf), state);
    }
  }

  protected abstract void scanExtraType(TypeElement extraType);
}
