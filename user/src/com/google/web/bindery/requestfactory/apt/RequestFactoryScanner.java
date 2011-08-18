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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Scans a RequestFactory declaration for errors. This visitor will call out to
 * the State object to validate the types that it encounters.
 */
class RequestFactoryScanner extends ScannerBase<Void> {
  @Override
  public Void visitExecutable(ExecutableElement x, State state) {
    if (shouldIgnore(x, state)) {
      // Ignore initializers and methods from Object and RequestFactory
      return null;
    }
    if (!x.getParameters().isEmpty()) {
      state.poison(x, Messages.factoryNoMethodParameters());
    }
    TypeMirror returnType = x.getReturnType();
    if (state.types.isAssignable(returnType, state.requestContextType)) {
      Element returnTypeElement = state.types.asElement(returnType);
      if (!returnTypeElement.getKind().equals(ElementKind.INTERFACE)) {
        state.poison(x, Messages.factoryMustReturnInterface(returnTypeElement.getSimpleName()));
      } else {
        TypeElement contextElement = (TypeElement) returnTypeElement;
        state.maybeScanContext(contextElement);
        state.requireMapping(contextElement);
      }
    } else {
      state.poison(x, Messages.factoryMustBeAssignable(state.requestContextType.asElement()
          .getSimpleName()));
    }
    return null;
  }

  @Override
  public Void visitType(TypeElement x, State state) {
    // Ignore RequestFactory itself
    if (state.types.isSameType(state.requestFactoryType, x.asType())) {
      return null;
    }

    scanAllInheritedMethods(x, state);
    state.checkExtraTypes(x);
    return null;
  }

}