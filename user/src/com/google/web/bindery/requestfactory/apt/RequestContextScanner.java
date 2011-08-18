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
import com.google.web.bindery.requestfactory.shared.JsonRpcService;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.ServiceName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

/**
 * Scans a RequestContext declaration. This visitor will call out to the State
 * object to validate the types that it encounters.
 */
class RequestContextScanner extends ScannerBase<Void> {

  @Override
  public Void visitExecutable(ExecutableElement x, State state) {
    if (shouldIgnore(x, state)) {
      return null;
    }
    TypeMirror returnType = x.getReturnType();
    if (state.types.isAssignable(returnType, state.requestType)) {
      // Extract Request<Foo> type
      DeclaredType asRequest = (DeclaredType) State.viewAs(state.requestType, returnType, state);
      if (asRequest.getTypeArguments().isEmpty()) {
        state.poison(x, Messages.rawType());
      } else {
        TypeMirror requestReturn = asRequest.getTypeArguments().get(0);
        if (!state.isTransportableType(requestReturn)) {
          state.poison(x, Messages.untransportableType(requestReturn));
        }
      }
    } else if (state.types.isAssignable(returnType, state.instanceRequestType)) {
      // Extract InstanceRequest<FooProxy, String>
      DeclaredType asInstanceRequest =
          (DeclaredType) State.viewAs(state.instanceRequestType, returnType, state);
      if (asInstanceRequest.getTypeArguments().isEmpty()) {
        state.poison(x, Messages.rawType());
      } else {
        TypeMirror instanceType = asInstanceRequest.getTypeArguments().get(0);
        state.maybeScanProxy((TypeElement) state.types.asElement(instanceType));
        TypeMirror requestReturn = asInstanceRequest.getTypeArguments().get(1);
        if (!state.isTransportableType(requestReturn)) {
          state.poison(x, Messages.untransportableType(requestReturn));
        }
      }
    } else if (isSetter(x, state)) {
      // Parameter checked in visitVariable
    } else {
      state.poison(x, Messages.contextRequiredReturnTypes(state.requestType.asElement()
          .getSimpleName(), state.instanceRequestType.asElement().getSimpleName()));
    }
    return super.visitExecutable(x, state);
  }

  @Override
  public Void visitType(TypeElement x, State state) {
    Service service = x.getAnnotation(Service.class);
    ServiceName serviceName = x.getAnnotation(ServiceName.class);
    JsonRpcService jsonRpcService = x.getAnnotation(JsonRpcService.class);
    if (service != null) {
      poisonIfAnnotationPresent(state, x, serviceName, jsonRpcService);

      // See javadoc on Element.getAnnotation() for why it works this way
      try {
        service.value();
        throw new RuntimeException("Should not reach here");
      } catch (MirroredTypeException expected) {
        TypeMirror type = expected.getTypeMirror();
        state.addMapping(x, (TypeElement) state.types.asElement(type));
      }
    }
    if (serviceName != null) {
      poisonIfAnnotationPresent(state, x, jsonRpcService);
      TypeElement domain =
          state.elements.getTypeElement(BinaryName.toSourceName(serviceName.value()));
      if (domain == null) {
        state.warn(x, Messages.contextMissingDomainType(serviceName.value()));
      }
      state.addMapping(x, domain);
    }

    scanAllInheritedMethods(x, state);
    state.checkExtraTypes(x);
    return null;
  }

  @Override
  public Void visitTypeParameter(TypeParameterElement x, State state) {
    for (TypeMirror bound : x.getBounds()) {
      if (!state.isTransportableType(bound)) {
        state.poison(x, Messages.untransportableType(bound));
      }
    }
    return super.visitTypeParameter(x, state);
  }

  @Override
  public Void visitVariable(VariableElement x, State state) {
    if (!state.isTransportableType(x.asType())) {
      state.poison(x, Messages.untransportableType(x.asType()));
    }
    return super.visitVariable(x, state);
  }
}