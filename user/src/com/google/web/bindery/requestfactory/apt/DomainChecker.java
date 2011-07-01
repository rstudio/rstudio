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

import com.google.web.bindery.requestfactory.apt.ClientToDomainMapper.UnmappedTypeException;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ProxyForName;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.ServiceName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

/**
 * Checks client to domain mappings.
 */
class DomainChecker extends ScannerBase<Void> {

  /**
   * Attempt to find the most specific method that conforms to a given
   * signature.
   */
  static class MethodFinder extends ScannerBase<ExecutableElement> {
    private TypeElement domainType;
    private ExecutableElement found;
    private final CharSequence name;
    private final TypeMirror returnType;
    private final List<TypeMirror> params;

    public MethodFinder(CharSequence name, TypeMirror returnType, List<TypeMirror> params,
        State state) {
      this.name = name;
      this.returnType = TypeSimplifier.simplify(returnType, true, state);
      List<TypeMirror> temp = new ArrayList<TypeMirror>(params.size());
      for (TypeMirror param : params) {
        temp.add(TypeSimplifier.simplify(param, false, state));
      }
      this.params = Collections.unmodifiableList(temp);
    }

    @Override
    public ExecutableElement visitExecutable(ExecutableElement domainMethodElement, State state) {
      // Quick check for name, paramer count, and return type assignability
      if (domainMethodElement.getSimpleName().contentEquals(name)
          && domainMethodElement.getParameters().size() == params.size()) {
        // Pick up parameterizations in domain type
        ExecutableType domainMethod = viewIn(domainType, domainMethodElement, state);

        boolean returnTypeMatches;
        if (returnType == null) {
          /*
           * This condition is for methods that we don't really care about the
           * domain return types (for getId(), getVersion()).
           */
          returnTypeMatches = true;
        } else {
          TypeMirror domainReturn =
              TypeSimplifier.simplify(domainMethod.getReturnType(), true, state);
          // The isSameType handles the NONE case.
          returnTypeMatches =
              state.types.isSameType(domainReturn, returnType)
                  || state.types.isAssignable(domainReturn, returnType);
        }
        if (returnTypeMatches) {
          boolean paramsMatch = true;
          Iterator<TypeMirror> lookFor = params.iterator();
          Iterator<? extends TypeMirror> domainParam = domainMethod.getParameterTypes().iterator();
          while (lookFor.hasNext()) {
            assert domainParam.hasNext();
            TypeMirror requestedType = lookFor.next();
            TypeMirror paramType = TypeSimplifier.simplify(domainParam.next(), false, state);
            if (!state.types.isSameType(requestedType, paramType)
                && !state.types.isAssignable(requestedType, paramType)) {
              paramsMatch = false;
            }
          }

          if (paramsMatch) {
            // Keep most-specific method signature
            if (found == null
                || state.types.isSubsignature(domainMethod, (ExecutableType) found.asType())) {
              found = domainMethodElement;
            }
          }
        }
      }

      return found;
    }

    @Override
    public ExecutableElement visitType(TypeElement domainType, State state) {
      this.domainType = domainType;
      return scanAllInheritedMethods(domainType, state);
    }
  }

  private static ExecutableType viewIn(TypeElement lookIn, ExecutableElement methodElement,
      State state) {
    try {
      return (ExecutableType) state.types.asMemberOf(state.types.getDeclaredType(lookIn),
          methodElement);
    } catch (IllegalArgumentException e) {
      return (ExecutableType) methodElement.asType();
    }
  }

  /**
   * This is used as the target for errors since generic methods show up as
   * synthetic elements that don't correspond to any source.
   */
  private TypeElement checkedType;
  private boolean currentTypeIsProxy;
  private boolean requireInstanceDomainMethods;
  private boolean requireStaticDomainMethods;
  private TypeElement domainType;

  @Override
  public Void visitExecutable(ExecutableElement clientMethodElement, State state) {
    if (shouldIgnore(clientMethodElement, state)) {
      return null;
    }
    // Ignore overrides of stableId() in proxies
    Name name = clientMethodElement.getSimpleName();
    if (currentTypeIsProxy && name.contentEquals("stableId")
        && clientMethodElement.getParameters().isEmpty()) {
      return null;
    }

    ExecutableType clientMethod = viewIn(checkedType, clientMethodElement, state);
    List<TypeMirror> lookFor = new ArrayList<TypeMirror>();
    // Convert client method signature to domain types
    TypeMirror returnType;
    try {
      returnType = convertToDomainTypes(clientMethod, lookFor, clientMethodElement, state);
    } catch (UnmappedTypeException e) {
      /*
       * Unusual: this would happen if a RequestContext for which we have a
       * resolved domain service method uses unresolved proxy types. For
       * example, the RequestContext uses a @Service annotation, while one or
       * more proxy types use @ProxyForName("") and specify a domain type not
       * available to the compiler.
       */
      return null;
    }

    ExecutableElement domainMethod;
    if (currentTypeIsProxy && isSetter(clientMethodElement, state)) {
      // Look for void setFoo(...)
      domainMethod =
          new MethodFinder(name, state.types.getNoType(TypeKind.VOID), lookFor, state).scan(
              domainType, state);
      if (domainMethod == null) {
        // Try a builder style
        domainMethod =
            new MethodFinder(name, domainType.asType(), lookFor, state).scan(domainType, state);
      }
    } else {
      // The usual case for getters and all service methods
      domainMethod = new MethodFinder(name, returnType, lookFor, state).scan(domainType, state);
    }

    if (domainMethod == null) {
      // Did not find a service method
      StringBuilder sb = new StringBuilder();
      sb.append(returnType).append(" ").append(name).append("(");
      for (TypeMirror param : lookFor) {
        sb.append(param);
      }
      sb.append(")");

      state.poison(clientMethodElement, "Could not find domain method similar to %s", sb);
      return null;
    }

    /*
     * Check the domain method for any requirements for it to be static.
     * InstanceRequests assume instance methods on the domain type.
     */
    boolean isInstanceRequest =
        state.types.isAssignable(clientMethod.getReturnType(), state.instanceRequestType);

    if ((isInstanceRequest || requireInstanceDomainMethods)
        && domainMethod.getModifiers().contains(Modifier.STATIC)) {
      state.poison(checkedType, "Found static domain method %s when instance method required",
          domainMethod.getSimpleName());
    }
    if (!isInstanceRequest && requireStaticDomainMethods
        && !domainMethod.getModifiers().contains(Modifier.STATIC)) {
      state.poison(checkedType, "Found instance domain method %s when static method required",
          domainMethod.getSimpleName());
    }
    if (state.verbose) {
      state.warn(clientMethodElement, "Found domain method %s", domainMethod.toString());
    }

    return null;
  }

  @Override
  public Void visitType(TypeElement clientTypeElement, State state) {
    TypeMirror clientType = clientTypeElement.asType();
    checkedType = clientTypeElement;
    boolean isEntityProxy = state.types.isAssignable(clientType, state.entityProxyType);
    currentTypeIsProxy =
        isEntityProxy || state.types.isAssignable(clientType, state.valueProxyType);
    domainType = state.getClientToDomainMap().get(clientTypeElement);
    if (domainType == null) {
      // A proxy with an unresolved domain type (e.g. ProxyForName(""))
      return null;
    }

    requireInstanceDomainMethods = false;
    requireStaticDomainMethods = false;

    if (currentTypeIsProxy) {
      // Require domain property methods to be instance methods
      requireInstanceDomainMethods = true;
      if (!hasProxyLocator(clientTypeElement, state)) {
        // Domain types without a Locator should have a no-arg constructor
        if (!hasNoArgConstructor(domainType)) {
          state.warn(clientTypeElement, "The domain type %s has no default constructor."
              + " Calling %s.create(%s.class) will cause a server error.", domainType,
              state.requestContextType.asElement().getSimpleName(), clientTypeElement
                  .getSimpleName());
        }

        /*
         * Check for getId(), getVersion(), and findFoo() for any type that
         * extends EntityProxy, but not on EntityProxy itself, since EntityProxy
         * is mapped to java.lang.Object.
         */
        if (isEntityProxy && !state.types.isSameType(clientType, state.entityProxyType)) {
          checkDomainEntityMethods(state);
        }
      }
    } else if (!hasServiceLocator(clientTypeElement, state)) {
      /*
       * Otherwise, we're looking at a RequestContext. If it doesn't have a
       * ServiceLocator, all methods must be static.
       */
      requireStaticDomainMethods = true;
    }

    scanAllInheritedMethods(clientTypeElement, state);
    return null;
  }

  /**
   * Check that {@code getId()} and {@code getVersion()} exist and that they are
   * non-static. Check that {@code findFoo()} exists, is static, returns an
   * appropriate type, and its parameter is assignable from the return value
   * from {@code getId()}.
   */
  private void checkDomainEntityMethods(State state) {
    ExecutableElement getId =
        new MethodFinder("getId", null, Collections.<TypeMirror> emptyList(), state).scan(
            domainType, state);
    if (getId == null) {
      state.poison(checkedType, "Domain type %s does not have a getId() method", domainType
          .asType());
    } else {
      if (getId.getModifiers().contains(Modifier.STATIC)) {

        state.poison(checkedType, "The domain type's getId() method must not be static");
      }

      // Can only check findFoo() if we have a getId
      ExecutableElement find =
          new MethodFinder("find" + domainType.getSimpleName(), domainType.asType(), Collections
              .singletonList(getId.getReturnType()), state).scan(domainType, state);
      if (find == null) {
        state.warn(checkedType, "The domain type %s has no %s find%s(%s) method. "
            + "Attempting to send a %s to the server will result in a server error.", domainType
            .asType(), domainType.getSimpleName(), domainType.getSimpleName(), getId
            .getReturnType(), checkedType.getSimpleName());
      } else if (!find.getModifiers().contains(Modifier.STATIC)) {
        state.poison(checkedType, "The domain object's find%s() method is not static", domainType
            .getSimpleName());
      }
    }

    ExecutableElement getVersion =
        new MethodFinder("getVersion", null, Collections.<TypeMirror> emptyList(), state).scan(
            domainType, state);
    if (getVersion == null) {
      state.poison(checkedType, "Domain type %s does not have a getVersion() method", domainType
          .asType());
    } else if (getVersion.getModifiers().contains(Modifier.STATIC)) {
      state.poison(checkedType, "The domain type's getVersion() method must not be static");
    }
  }

  /**
   * Converts a client method's types to their domain counterparts.
   * 
   * @param clientMethod the RequestContext method to validate
   * @param parameterAccumulator an out parameter that will be populated with
   *          the converted paramater types
   * @param warnTo The element to which warnings should be posted if one or more
   *          client types cannot be converted to domain types for validation
   * @param state the State object
   * @throws UnmappedTypeException if one or more types used in
   *           {@code clientMethod} cannot be resolved to domain types
   */
  private TypeMirror convertToDomainTypes(ExecutableType clientMethod,
      List<TypeMirror> parameterAccumulator, ExecutableElement warnTo, State state)
      throws UnmappedTypeException {
    boolean error = false;
    TypeMirror returnType;
    try {
      returnType = clientMethod.getReturnType().accept(new ClientToDomainMapper(), state);
    } catch (UnmappedTypeException e) {
      error = true;
      returnType = null;
      state.warn(warnTo, "Cannot validate this method because the domain mapping for the"
          + " return type (%s) could not be resolved to a domain type", e.getClientType());
    }
    for (TypeMirror param : clientMethod.getParameterTypes()) {
      try {
        parameterAccumulator.add(param.accept(new ClientToDomainMapper(), state));
      } catch (UnmappedTypeException e) {
        parameterAccumulator.add(null);
        error = true;
        state.warn(warnTo, "Cannot validate this method because the domain mapping for a"
            + " parameter of type (%s) could not be resolved to a domain type", e.getClientType());
      }
    }
    if (error) {
      throw new UnmappedTypeException();
    }
    return returnType;
  }

  /**
   * Looks for a no-arg constructor or no constructors at all. Instance
   * initializers are ignored.
   */
  private boolean hasNoArgConstructor(TypeElement x) {
    List<ExecutableElement> constructors = ElementFilter.constructorsIn(x.getEnclosedElements());
    if (constructors.isEmpty()) {
      return true;
    }
    for (ExecutableElement constructor : constructors) {
      if (constructor.getParameters().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private boolean hasProxyLocator(TypeElement x, State state) {
    ProxyFor proxyFor = x.getAnnotation(ProxyFor.class);
    if (proxyFor != null) {
      // See javadoc on getAnnotation
      try {
        proxyFor.locator();
        throw new RuntimeException("Should not reach here");
      } catch (MirroredTypeException expected) {
        TypeMirror locatorType = expected.getTypeMirror();
        return !state.types.asElement(locatorType).equals(state.locatorType.asElement());
      }
    }
    ProxyForName proxyForName = x.getAnnotation(ProxyForName.class);
    return proxyForName != null && !proxyForName.locator().isEmpty();
  }

  private boolean hasServiceLocator(TypeElement x, State state) {
    Service service = x.getAnnotation(Service.class);
    if (service != null) {
      // See javadoc on getAnnotation
      try {
        service.locator();
        throw new RuntimeException("Should not reach here");
      } catch (MirroredTypeException expected) {
        TypeMirror locatorType = expected.getTypeMirror();
        return !state.types.asElement(locatorType).equals(state.serviceLocatorType.asElement());
      }
    }
    ServiceName serviceName = x.getAnnotation(ServiceName.class);
    return serviceName != null && !serviceName.locator().isEmpty();
  }
}
