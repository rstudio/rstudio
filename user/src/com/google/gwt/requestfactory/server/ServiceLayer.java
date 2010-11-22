/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.requestfactory.server;

import com.google.gwt.requestfactory.shared.BaseProxy;
import com.google.gwt.requestfactory.shared.Locator;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * The ServiceLayer mediates all interactions between the
 * {@link SimpleRequestProcessor} and the domain environment. The core service
 * logic can be decorated by extending an {@link ServiceLayerDecorator}.
 * <p>
 * This API is subject to change in future releases.
 */
public abstract class ServiceLayer {
  /*
   * NB: This type cannot be directly extended by the user since it has a
   * package-protected constructor. This means that any API-compatibility work
   * that needs to happen can be done in ServiceLayerDecorator in order to keep
   * this interface as clean as possible.
   */

  /**
   * Create a RequestFactory ServiceLayer that is optionally modified by the
   * given decorators.
   * 
   * @param decorators the decorators that will modify the behavior of the core
   *          service layer implementation
   * @return a ServiceLayer instance
   */
  public static ServiceLayer create(ServiceLayerDecorator... decorators) {
    List<ServiceLayerDecorator> list = new ArrayList<ServiceLayerDecorator>();
    // Always hit the cache first
    ServiceLayerCache cache = new ServiceLayerCache();
    list.add(cache);
    // The the user-provided decorators
    if (decorators != null) {
      list.addAll(Arrays.asList(decorators));
    }
    // Support for Locator objects
    list.add(new LocatorServiceLayer());
    // Interact with domain objects
    list.add(new ReflectiveServiceLayer());
    // Locate domain objects
    list.add(new ResolverServiceLayer());

    // Make the last layer point to the cache
    list.get(list.size() - 1).top = cache;

    // Point each entry at the next
    for (int i = list.size() - 2; i >= 0; i--) {
      ServiceLayerDecorator layer = list.get(i);
      layer.next = list.get(i + 1);
      layer.top = cache;
    }

    return cache;
  }

  /**
   * A pointer to the top-most ServiceLayer instance.
   */
  ServiceLayer top;

  /**
   * Not generally-extensible.
   */
  ServiceLayer() {
  }

  /**
   * Create an instance of the requested domain type.
   * 
   * @param <T> the requested domain type
   * @param clazz the requested domain type
   * @return an instance of the requested domain type
   */
  public abstract <T> T createDomainObject(Class<T> clazz);

  /**
   * Create an instance of the requested {@link Locator} type.
   * 
   * @param <T> the requested Locator type
   * @param clazz the requested Locator type
   * @return an instance of the requested Locator type
   */
  public abstract <T extends Locator<?, ?>> T createLocator(Class<T> clazz);

  /**
   * Return the persistent id for a domain object. May return {@code null} to
   * indicate that the domain object has not been persisted. The value returned
   * from this method must be a simple type (e.g. Integer, String) or a domain
   * type for which a mapping to an EntityProxy or Value proxy exists.
   * <p>
   * The values returned from this method may be passed to
   * {@link #loadDomainObject(Class, Object)} in the future.
   * 
   * @param domainObject a domain object
   * @return the persistent id of the domain object or {@code null} if the
   *         object is not persistent
   */
  public abstract Object getId(Object domainObject);

  /**
   * Returns the type of object the domain type's {@code findFoo()} or
   * {@link com.google.gwt.requestfactory.shared.Locator#getId(Object)
   * Locator.getId()} expects to receive.
   * 
   * @param domainType a domain entity type
   * @return the type of the persistent id value used to represent the domain
   *         type
   */
  public abstract Class<?> getIdType(Class<?> domainType);

  /**
   * Retrieve the named property from the domain object.
   * 
   * @param domainObject the domain object being examined
   * @param property the property name
   * @return the value of the property
   */
  public abstract Object getProperty(Object domainObject, String property);

  /**
   * Compute the return type for a method declared in a RequestContext by
   * analyzing the generic method declaration.
   */
  public abstract Type getRequestReturnType(Method contextMethod);

  /**
   * May return {@code null} to indicate that the domain object has not been
   * persisted. The value returned from this method must be a simple type (e.g.
   * Integer, String) or a domain type for which a mapping to an EntityProxy or
   * Value proxy exists.
   * 
   * @param domainObject a domain object
   * @return the version of the domain object or {@code null} if the object is
   *         not persistent
   */
  public abstract Object getVersion(Object domainObject);

  /**
   * Invoke a domain service method. The underlying eventually calls
   * {@link Method#invoke(Object, Object...)}.
   * 
   * @param domainMethod the method to invoke
   * @param args the arguments to pass to the method
   * @return the value returned from the method invocation
   */
  public abstract Object invoke(Method domainMethod, Object... args);

  /**
   * Returns {@code true} if the given domain object is still live (i.e. not
   * deleted) in the backing store.
   * 
   * @param domainObject a domain entity
   * @return {@code true} if {@code domainObject} could be retrieved at a later
   *         point in time
   */
  public abstract boolean isLive(Object domainObject);

  /**
   * Load an object from the backing store. This method may return {@code null}
   * to indicate that the requested object is no longer available.
   * 
   * @param <T> the type of object to load
   * @param clazz the type of object to load
   * @param domainId an id previously returned from {@link #getId(Object)}
   * @return the requested object or {@code null} if it is irretrievable
   */
  public abstract <T> T loadDomainObject(Class<T> clazz, Object domainId);

  /**
   * Given a type token previously returned from
   * {@link #resolveTypeToken(Class)}, return the Class literal associated with
   * the token.
   * 
   * @param typeToken a string token
   * @return the type represented by the token
   */
  public abstract Class<? extends BaseProxy> resolveClass(String typeToken);

  /**
   * Determine the type used by the client code to represent a given domain
   * type. If multiple proxy types have been mapped to the same domain type, the
   * {@code clientType} parameter is used to ensure assignability.
   * 
   * @param domainClass the server-side type to be transported to the client
   * @param clientType the type to which the returned type must be assignable
   * @param required if {@code true} and no mapping is available, throw an
   *          {@link UnexpectedException}, othrewise the method will return
   *          {@code null}
   * @return a class that represents {@code domainClass} on the client which is
   *         assignable to {@code clientType}
   */
  public abstract <T> Class<? extends T> resolveClientType(
      Class<?> domainClass, Class<T> clientType, boolean required);

  /**
   * Determine the domain (server-side) type that the given client type is
   * mapped to.
   * 
   * @param clientType a client-side type
   * @return the domain type that {@code clientType} represents
   */
  public abstract Class<?> resolveDomainClass(Class<?> clientType);

  /**
   * Return the domain service method associated with a RequestContext method
   * declaration. The {@code requestContextMethod} will have been previously
   * resolved by {@link #resolveRequestContextMethod(String, String)}.
   * 
   * @param requestContextMethod a RequestContext method declaration.
   * @return the domain service method that should be invoked
   */
  public abstract Method resolveDomainMethod(Method requestContextMethod);

  /**
   * Return the type of {@link Locator} that should be used to access the given
   * domain type.
   * 
   * @param domainType a domain (server-side) type
   * @return the type of Locator to use, or {@code null} if the type conforms to
   *         the RequestFactory entity protocol
   */
  public abstract Class<? extends Locator<?, ?>> resolveLocator(
      Class<?> domainType);

  /**
   * Find a RequestContext method declaration by name.
   * 
   * @param requestContextClass the fully-qualified binary name of the
   *          RequestContext
   * @param methodName the name of the service method declared within the
   *          RequestContext
   * @return the method declaration, or {@code null} if the method does not
   *         exist
   */
  public abstract Method resolveRequestContextMethod(
      String requestContextClass, String methodName);

  /**
   * Return a string used to represent the given type in the wire protocol.
   * 
   * @param proxyType a client-side EntityProxy or ValueProxy type
   * @return the type token used to represent the proxy type
   */
  public abstract String resolveTypeToken(Class<? extends BaseProxy> proxyType);

  /**
   * Sets a property on a domain object.
   * 
   * @param domainObject the domain object to operate on
   * @param property the name of the property to set
   * @param expectedType the type of the property
   * @param value the new value
   */
  public abstract void setProperty(Object domainObject, String property,
      Class<?> expectedType, Object value);

  /**
   * Invoke a JSR 303 validator on the given domain object. If no validator is
   * available, this method is a no-op.
   * 
   * @param <T> the type of data being validated
   * @param domainObject the domain objcet to validate
   * @return the violations associated with the domain object
   */
  public abstract <T> Set<ConstraintViolation<T>> validate(T domainObject);
}