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
package com.google.web.bindery.requestfactory.server;

import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.Locator;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.ServiceLocator;

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
   * Provides a flag to disable the ServiceLayerCache for debugging purposes.
   */
  private static final boolean ENABLE_CACHE = Boolean.valueOf(System.getProperty(
      "gwt.rf.ServiceLayerCache", "true"));

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
    ServiceLayerDecorator cache =
        ENABLE_CACHE ? new ServiceLayerCache() : new ServiceLayerDecorator();
    list.add(cache);
    // The the user-provided decorators
    if (decorators != null) {
      list.addAll(Arrays.asList(decorators));
    }
    // Support for Locator objects
    list.add(new LocatorServiceLayer());
    // Interact with domain objects
    list.add(new ReflectiveServiceLayer());
    // Add shortcut for find's operation
    list.add(new FindServiceLayer());
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
   * Create an instance of a service object that can be used as the target for
   * the given method invocation.
   * 
   * @param requestContext the RequestContext type for which a service object
   *          must be instantiated.
   * @return an instance of the requested service object
   */
  public abstract Object createServiceInstance(Class<? extends RequestContext> requestContext);

  /**
   * Create an instance of the requested {@link ServiceLocator} type.
   * 
   * @param <T> the requested ServiceLocator type
   * @param clazz the requested ServiceLocator type
   * @return an instance of the requested ServiceLocator type
   */
  public abstract <T extends ServiceLocator> T createServiceLocator(Class<T> clazz);

  /**
   * Returns the ClassLoader that should be used when attempting to access
   * domain classes or resources.
   * <p>
   * The default implementation returns
   * {@code Thread.currentThread().getContextClassLoader()}.
   */
  public abstract ClassLoader getDomainClassLoader();

  /**
   * Determine the method to invoke when retrieving the given property.
   * 
   * @param domainType a domain entity type
   * @param property the name of the property to be retrieved
   * @return the Method that should be invoked to retrieve the property or
   *         {@code null} if the method could not be located
   */
  public abstract Method getGetter(Class<?> domainType, String property);

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
   * {@link com.google.web.bindery.requestfactory.shared.Locator#getId(Object)
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
   * Determine the method to invoke when setting the given property.
   * 
   * @param domainType a domain entity type
   * @param property the name of the property to be set
   * @return the Method that should be invoked to set the property or
   *         {@code null} if the method could not be located
   */
  public abstract Method getSetter(Class<?> domainType, String property);

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
   * Load multiple objects from the backing store. This method is intended to
   * allow more efficient access to the backing store by providing all objects
   * referenced in an incoming payload.
   * <p>
   * The default implementation of this method will delegate to
   * {@link #loadDomainObject(Class, Object)}.
   * 
   * @param classes type type of each object to load
   * @param domainIds the ids previously returned from {@link #getId(Object)}
   * @return the requested objects, elements of which may be {@code null} if the
   *         requested objects were irretrievable
   */
  public abstract List<Object> loadDomainObjects(List<Class<?>> classes, List<Object> domainIds);

  /**
   * Determines if the invocation of a domain method requires a
   * {@link ServiceLocator} as the 0th parameter when passed into
   * {@link #invoke(Method, Object...)}.
   * 
   * @param contextMethod a method defined in a RequestContext
   * @param domainMethod a domain method
   * @return {@code true} if a ServiceLocator is required
   */
  public abstract boolean requiresServiceLocator(Method contextMethod, Method domainMethod);

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
   *          exception, otherwise the method will return {@code null}
   * @return a class that represents {@code domainClass} on the client which is
   *         assignable to {@code clientType}
   */
  public abstract <T> Class<? extends T> resolveClientType(Class<?> domainClass,
      Class<T> clientType, boolean required);

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
   * @param requestContext the RequestContext requested by the client
   * @param requestContextMethod a RequestContext method declaration. Note that
   *          this Method may be defined in a supertype of
   *          {@code requestContext}
   * @return the domain service method that should be invoked
   */
  public abstract Method resolveDomainMethod(String operation);

  /**
   * Return the type of {@link Locator} that should be used to access the given
   * domain type.
   * 
   * @param domainType a domain (server-side) type
   * @return the type of Locator to use, or {@code null} if the type conforms to
   *         the RequestFactory entity protocol
   */
  public abstract Class<? extends Locator<?, ?>> resolveLocator(Class<?> domainType);

  /**
   * Find a RequestContext that should be used to fulfill the requested
   * operation.
   * 
   * @param operation the operation
   * @return the RequestContext or {@code null} if no RequestContext exists that
   *         can fulfill the operation
   */
  public abstract Class<? extends RequestContext> resolveRequestContext(String operation);

  /**
   * Find a RequestContext method declaration by name.
   * 
   * @param operation the operation's name
   * @return the method declaration, or {@code null} if the method does not
   *         exist
   */
  public abstract Method resolveRequestContextMethod(String operation);

  /**
   * Loads and validates a RequestFactory interface.
   * 
   * @param token the RequestFactory's type token (usually the type's binary
   *          name)
   * @return the RequestFactory type
   */
  public abstract Class<? extends RequestFactory> resolveRequestFactory(String token);

  /**
   * Given a {@link RequestContext} method, find the service class referenced in
   * the {@link com.google.web.bindery.requestfactory.shared.Service Service} or
   * {@link com.google.web.bindery.requestfactory.shared.ServiceName
   * ServiceName} annotation.
   * 
   * @param requestContextClass a RequestContext interface
   * @return the type of service to use
   */
  public abstract Class<?> resolveServiceClass(Class<? extends RequestContext> requestContextClass);

  /**
   * Given a RequestContext method declaration, resolve the
   * {@link ServiceLocator} that should be used when invoking the domain method.
   * This method will only be called if
   * {@link #requiresServiceLocator(Method, Method)} returned {@code true} for
   * the associated domain method.
   * 
   * @param requestContext the RequestContext for which a ServiceLocator must be
   *          located
   * @return the type of ServiceLocator to use
   */
  public abstract Class<? extends ServiceLocator> resolveServiceLocator(
      Class<? extends RequestContext> requestContext);

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
  public abstract void setProperty(Object domainObject, String property, Class<?> expectedType,
      Object value);

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
