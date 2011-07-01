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

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 * Looks for all types assignable to {@code RequestFactory} and adds them to the
 * output state. This is necessary to support factory types declared as inner
 * classes.
 */
class Finder extends ScannerBase<Void> {
  @Override
  public Void visitType(TypeElement x, State state) {
    // Ignore anything other than interfaces
    if (x.getKind().equals(ElementKind.INTERFACE)) {
      if (state.types.isAssignable(x.asType(), state.requestFactoryType)) {
        state.maybeScanFactory(x);
      }
      if (state.types.isAssignable(x.asType(), state.requestContextType)) {
        state.maybeScanContext(x);
      }
      if (state.types.isAssignable(x.asType(), state.entityProxyType)
          || state.types.isAssignable(x.asType(), state.valueProxyType)) {
        state.maybeScanProxy(x);
      }
    }
    return super.visitType(x, state);
  }
}