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

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
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
    private final boolean boxReturnType;
    private final CharSequence name;
    private final TypeMirror returnType;
    private final List<TypeMirror> params;

    public MethodFinder(CharSequence name, TypeMirror returnType, List<TypeMirror> params,
        boolean boxReturnType, State state) {
      this.boxReturnType = boxReturnType;
      this.name = name;
      this.returnType = TypeSimplifier.simplify(returnType, boxReturnType, state);
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
              TypeSimplifier.simplify(domainMethod.getReturnType(), boxReturnType, state);
          // The isSameType handles the NONE case.
          returnTypeMatches = state.types.isSubtype(domainReturn, returnType);
        }
        if (returnTypeMatches) {
          boolean paramsMatch = true;
          Iterator<TypeMirror> lookFor = params.iterator();
          Iterator<? extends TypeMirror> domainParam = domainMethod.getParameterTypes().iterator();
          while (lookFor.hasNext()) {
            assert domainParam.hasNext();
            TypeMirror requestedType = lookFor.next();
            TypeMirror paramType = TypeSimplifier.simplify(domainParam.next(), false, state);
            if (!state.types.isSubtype(requestedType, paramType)) {
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

  /**
   * This is used as the target for errors since generic methods show up as
   * synthetic elements that don't correspond to any source.
   */
  private TypeElement checkedElement;
  private boolean currentTypeIsProxy;
  private TypeElement domainElement;
  private boolean requireInstanceDomainMethods;
  private boolean requireStaticDomainMethods;

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

    ExecutableType clientMethod = viewIn(checkedElement, clientMethodElement, state);
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
          new MethodFinder(name, state.types.getNoType(TypeKind.VOID), lookFor, false, state).scan(
              domainElement, state);
      if (domainMethod == null) {
        // Try a builder style
        domainMethod =
            new MethodFinder(name, domainElement.asType(), lookFor, false, state).scan(
                domainElement, state);
      }
    } else {
      /*
       * The usual case for getters and all service methods. Only box return
       * types when matching context methods since there's a significant
       * semantic difference between a null Integer and 0.
       */
      domainMethod =
          new MethodFinder(name, returnType, lookFor, !currentTypeIsProxy, state).scan(
              domainElement, state);
    }

    if (domainMethod == null) {
      // Did not find a service method
      StringBuilder sb = new StringBuilder();
      sb.append(returnType).append(" ").append(name).append("(");
      boolean first = true;
      for (TypeMirror param : lookFor) {
        if (!first) {
          sb.append(", ");
        }
        sb.append(param);
        first = false;
      }
      sb.append(")");

      state.poison(clientMethodElement, Messages.domainMissingMethod(sb));
      return null;
    }

    // Check if the method is public
    if (!domainMethod.getModifiers().contains(Modifier.PUBLIC)) {
      state.poison(clientMethodElement, Messages.domainMethodNotPublic(
          domainMethod.getSimpleName()));
    }

    /*
     * Check the domain method for any requirements for it to be static.
     * InstanceRequests assume instance methods on the domain type.
     */
    boolean isInstanceRequest =
        state.types.isSubtype(clientMethod.getReturnType(), state.instanceRequestType);

    if ((isInstanceRequest || requireInstanceDomainMethods)
        && domainMethod.getModifiers().contains(Modifier.STATIC)) {
      state.poison(clientMethodElement, Messages.domainMethodWrongModifier(false, domainMethod
          .getSimpleName()));
    }
    if (!isInstanceRequest && requireStaticDomainMethods
        && !domainMethod.getModifiers().contains(Modifier.STATIC)) {
      state.poison(clientMethodElement, Messages.domainMethodWrongModifier(true, domainMethod
          .getSimpleName()));
    }

    // Record the mapping
    state.addMapping(clientMethodElement, domainMethod);
    return null;
  }

  @Override
  public Void visitType(TypeElement clientTypeElement, State state) {
    TypeMirror clientType = clientTypeElement.asType();
    checkedElement = clientTypeElement;
    boolean isEntityProxy = state.types.isSubtype(clientType, state.entityProxyType);
    currentTypeIsProxy = isEntityProxy || state.types.isSubtype(clientType, state.valueProxyType);
    domainElement = (TypeElement) state.getClientToDomainMap().get(clientTypeElement);
    if (domainElement == null) {
      // A proxy with an unresolved domain type (e.g. ProxyForName(""))
      return null;
    }

    requireInstanceDomainMethods = false;
    requireStaticDomainMethods = false;

    if (currentTypeIsProxy) {
      // Require domain property methods to be instance methods
      requireInstanceDomainMethods = true;
      if (!hasProxyLocator(clientTypeElement, state)) {
        // Domain types without a Locator should be default-instantiable
        if (!isDefaultInstantiable(domainElement)) {
          state.warn(clientTypeElement, Messages.domainNotDefaultInstantiable(domainElement
              .getSimpleName(), clientTypeElement.getSimpleName(), state.requestContextType
              .asElement().getSimpleName()));
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
        new MethodFinder("getId", null, Collections.<TypeMirror> emptyList(), false, state).scan(
            domainElement, state);
    if (getId == null) {
      state.poison(checkedElement, Messages.domainNoGetId(domainElement.asType()));
    } else {
      if (getId.getModifiers().contains(Modifier.STATIC)) {
        state.poison(checkedElement, Messages.domainGetIdStatic());
      }

      // Can only check findFoo() if we have a getId
      ExecutableElement find =
          new MethodFinder("find" + domainElement.getSimpleName(), domainElement.asType(),
              Collections.singletonList(getId.getReturnType()), false, state).scan(domainElement,
              state);
      if (find == null) {
        state.warn(checkedElement, Messages.domainMissingFind(domainElement.asType(), domainElement
            .getSimpleName(), getId.getReturnType(), checkedElement.getSimpleName()));
      } else if (!find.getModifiers().contains(Modifier.STATIC)) {
        state.poison(checkedElement, Messages.domainFindNotStatic(domainElement.getSimpleName()));
      }
    }

    ExecutableElement getVersion =
        new MethodFinder("getVersion", null, Collections.<TypeMirror> emptyList(), false, state)
            .scan(domainElement, state);
    if (getVersion == null) {
      state.poison(checkedElement, Messages.domainNoGetVersion(domainElement.asType()));
    } else if (getVersion.getModifiers().contains(Modifier.STATIC)) {
      state.poison(checkedElement, Messages.domainGetVersionStatic());
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
      state.warn(warnTo, Messages.methodNoDomainPeer(e.getClientType(), false));
    }
    for (TypeMirror param : clientMethod.getParameterTypes()) {
      try {
        parameterAccumulator.add(param.accept(new ClientToDomainMapper(), state));
      } catch (UnmappedTypeException e) {
        parameterAccumulator.add(null);
        error = true;
        state.warn(warnTo, Messages.methodNoDomainPeer(e.getClientType(), true));
      }
    }
    if (error) {
      throw new UnmappedTypeException();
    }
    return returnType;
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

  /**
   * Looks for a no-arg constructor or no constructors at all. Instance
   * initializers are ignored.
   */
  private boolean isDefaultInstantiable(TypeElement x) {
    if (x.getKind() != ElementKind.CLASS) {
      return false;
    }
    if (x.getModifiers().contains(Modifier.ABSTRACT)) {
      return false;
    }
    if (x.getNestingKind() == NestingKind.ANONYMOUS || x.getNestingKind() == NestingKind.LOCAL
        || (x.getNestingKind() == NestingKind.MEMBER && !x.getModifiers().contains(Modifier.STATIC))) {
      // anonymous and local shouldn't ever happen, but just in case...
      return false;
    }
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
}
