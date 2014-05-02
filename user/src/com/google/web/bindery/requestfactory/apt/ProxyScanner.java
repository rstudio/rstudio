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

import com.google.gwt.dev.util.Name.BinaryName;
import com.google.web.bindery.requestfactory.shared.JsonRpcProxy;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ProxyForName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

/**
 * Examines the methods declared in a proxy interface. Also records the client
 * to domain mapping for the proxy type.
 */
class ProxyScanner extends ScannerBase<Void> {

  private TypeElement checkedElement;

  @Override
  public Void visitExecutable(ExecutableElement x, State state) {
    if (shouldIgnore(x, state)) {
      return null;
    }

    ExecutableType xType = viewIn(checkedElement, x, state);
    if (isGetter(x, state)) {
      TypeMirror returnType = xType.getReturnType();
      if (!state.isTransportableType(returnType)) {
        // XXX(t.broyer): should we really pass the "resolved" type? that could
        // result in several errors being reported on the same method, but on
        // the other hand tells exactly which type it is that isn't
        // transportable.
        // For instance, a List<T> might be transportable if T is
        // java.lang.String in a sub-interface, but not if T is some
        // untransportable type in another sub-interface
        state.poison(x, Messages.untransportableType(returnType));
      }
    } else if (!isSetter(x, state)) {
      state.poison(x, Messages.proxyOnlyGettersSetters());
    }

    // check parameters (we do not defer to visitVariable, as we need the
    // resolved generics)
    int i = 0;
    for (TypeMirror parameterType : xType.getParameterTypes()) {
      if (!state.isTransportableType(parameterType)) {
        // see comments above about the returnType
        state.poison(x.getParameters().get(i), Messages.untransportableType(parameterType));
      }
      i++;
    }
    return null;
  }

  @Override
  public Void visitType(TypeElement x, State state) {
    checkedElement = x;
    ProxyFor proxyFor = x.getAnnotation(ProxyFor.class);
    ProxyForName proxyForName = x.getAnnotation(ProxyForName.class);
    JsonRpcProxy jsonRpcProxy = x.getAnnotation(JsonRpcProxy.class);
    if (proxyFor != null) {
      poisonIfAnnotationPresent(state, x, proxyForName, jsonRpcProxy);

      // See javadoc on Element.getAnnotation() for why it works this way
      try {
        proxyFor.value();
        throw new RuntimeException("Should not reach here");
      } catch (MirroredTypeException expected) {
        TypeMirror type = expected.getTypeMirror();
        state.addMapping(x, (TypeElement) state.types.asElement(type));
      }
    }
    if (proxyForName != null) {
      poisonIfAnnotationPresent(state, x, jsonRpcProxy);
      TypeElement domain =
          state.elements.getTypeElement(BinaryName.toSourceName(proxyForName.value()));
      if (domain == null) {
        state.warn(x, Messages.proxyMissingDomainType(proxyForName.value()));
      }
      state.addMapping(x, domain);
    }

    scanAllInheritedMethods(x, state);
    state.checkExtraTypes(x);
    return null;
  }

  @Override
  public Void visitVariable(VariableElement x, State state) {
    if (!state.isTransportableType(x.asType())) {
      state.poison(x, Messages.untransportableType(x.asType()));
    }
    return super.visitVariable(x, state);
  }

  @Override
  protected boolean shouldIgnore(ExecutableElement x, State state) {
    // Ignore overrides of stableId()
    if (x.getSimpleName().contentEquals("stableId") && x.getParameters().isEmpty()) {
      return true;
    }
    return super.shouldIgnore(x, state);
  }
}