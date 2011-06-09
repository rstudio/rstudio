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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintViolation;

/**
 * Users that intend to alter how RequestFactory interacts with the domain
 * environment can extend this type and provide it to
 * {@link ServiceLayer#create(ServiceLayerDecorator...)}. The methods defined in
 * this type will automatically delegate to the next decorator or the root
 * service object after being processed by{@code create()}.
 */
public class ServiceLayerDecorator extends ServiceLayer {
  private static final Logger log = Logger.getLogger(ServiceLayer.class.getName());

  /**
   * A pointer to the next deepest layer.
   */
  ServiceLayer next;

  @Override
  public <T> T createDomainObject(Class<T> clazz) {
    return getNext().createDomainObject(clazz);
  }

  @Override
  public <T extends Locator<?, ?>> T createLocator(Class<T> clazz) {
    return getNext().createLocator(clazz);
  }

  @Override
  public Object createServiceInstance(Class<? extends RequestContext> requestContext) {
    return getNext().createServiceInstance(requestContext);
  }

  @Override
  public <T extends ServiceLocator> T createServiceLocator(Class<T> clazz) {
    return getNext().createServiceLocator(clazz);
  }

  @Override
  public ClassLoader getDomainClassLoader() {
    return getNext().getDomainClassLoader();
  }

  @Override
  public Method getGetter(Class<?> domainType, String property) {
    return getNext().getGetter(domainType, property);
  }

  @Override
  public Object getId(Object domainObject) {
    return getNext().getId(domainObject);
  }

  @Override
  public Class<?> getIdType(Class<?> domainType) {
    return getNext().getIdType(domainType);
  }

  @Override
  public Object getProperty(Object domainObject, String property) {
    return getNext().getProperty(domainObject, property);
  }

  @Override
  public Type getRequestReturnType(Method contextMethod) {
    return getNext().getRequestReturnType(contextMethod);
  }

  @Override
  public Method getSetter(Class<?> domainType, String property) {
    return getNext().getSetter(domainType, property);
  }

  @Override
  public Object getVersion(Object domainObject) {
    return getNext().getVersion(domainObject);
  }

  @Override
  public Object invoke(Method domainMethod, Object... args) {
    return getNext().invoke(domainMethod, args);
  }

  @Override
  public boolean isLive(Object domainObject) {
    return getNext().isLive(domainObject);
  }

  @Override
  public <T> T loadDomainObject(Class<T> clazz, Object domainId) {
    return getNext().loadDomainObject(clazz, domainId);
  }

  @Override
  public List<Object> loadDomainObjects(List<Class<?>> classes, List<Object> domainIds) {
    return getNext().loadDomainObjects(classes, domainIds);
  }

  @Override
  public boolean requiresServiceLocator(Method contextMethod, Method domainMethod) {
    return getNext().requiresServiceLocator(contextMethod, domainMethod);
  }

  @Override
  public Class<? extends BaseProxy> resolveClass(String typeToken) {
    return getNext().resolveClass(typeToken);
  }

  @Override
  public <T> Class<? extends T> resolveClientType(Class<?> domainClass, Class<T> clientType,
      boolean required) {
    return getNext().resolveClientType(domainClass, clientType, required);
  }

  @Override
  public Class<?> resolveDomainClass(Class<?> clazz) {
    return getNext().resolveDomainClass(clazz);
  }

  @Override
  public Method resolveDomainMethod(String operation) {
    return getNext().resolveDomainMethod(operation);
  }

  @Override
  public Class<? extends Locator<?, ?>> resolveLocator(Class<?> domainType) {
    return getNext().resolveLocator(domainType);
  }

  @Override
  public Class<? extends RequestContext> resolveRequestContext(String operation) {
    return getNext().resolveRequestContext(operation);
  }

  @Override
  public Method resolveRequestContextMethod(String operation) {
    return getNext().resolveRequestContextMethod(operation);
  }

  @Override
  public Class<? extends RequestFactory> resolveRequestFactory(String binaryName) {
    return getNext().resolveRequestFactory(binaryName);
  }

  @Override
  public Class<?> resolveServiceClass(Class<? extends RequestContext> requestContextClass) {
    return getNext().resolveServiceClass(requestContextClass);
  }

  @Override
  public Class<? extends ServiceLocator> resolveServiceLocator(
      Class<? extends RequestContext> requestContext) {
    return getNext().resolveServiceLocator(requestContext);
  }

  @Override
  public String resolveTypeToken(Class<? extends BaseProxy> proxyType) {
    return getNext().resolveTypeToken(proxyType);
  }

  @Override
  public void setProperty(Object domainObject, String property, Class<?> expectedType, Object value) {
    getNext().setProperty(domainObject, property, expectedType, value);
  }

  @Override
  public <T> Set<ConstraintViolation<T>> validate(T domainObject) {
    return getNext().validate(domainObject);
  }

  /**
   * Throw a fatal error up into the top-level processing code. This method
   * should be used to provide diagnostic information that will help the
   * end-developer track down problems when that data would expose
   * implementation details of the server to the client.
   * 
   * @param e a throwable with more data, may be {@code null}
   * @param message a printf-style format string
   * @param args arguments for the message
   * @throws UnexpectedException this method never returns normally
   * @see #report(String, Object...)
   */
  protected final <T> T die(Throwable e, String message, Object... args) throws UnexpectedException {
    String msg = String.format(message, args);
    log.log(Level.SEVERE, msg, e);
    throw new UnexpectedException(msg, e);
  }

  /**
   * Returns the top-most service layer. General-purpose ServiceLayer decorators
   * should use the instance provided by {@code getTop()} when calling public
   * methods on the ServiceLayer API to allow higher-level decorators to
   * override behaviors built into lower-level decorators.
   * 
   * @return the ServiceLayer returned by
   *         {@link #create(ServiceLayerDecorator...)}
   */
  protected final ServiceLayer getTop() {
    return top;
  }

  /**
   * Report an exception thrown by code that is under the control of the
   * end-developer.
   * 
   * @param userGeneratedException an {@link InvocationTargetException} thrown
   *          by an invocation of user-provided code
   * @throws ReportableException this method never returns normally
   */
  protected final <T> T report(InvocationTargetException userGeneratedException)
      throws ReportableException {
    throw new ReportableException(userGeneratedException.getCause());
  }

  /**
   * Return a message to the client. This method should not include any data
   * that was not sent to the server by the client to avoid leaking data.
   * 
   * @param msg a printf-style format string
   * @param args arguments for the message
   * @throws ReportableException this method never returns normally
   * @see #die(Throwable, String, Object...)
   */
  protected final <T> T report(String msg, Object... args) throws ReportableException {
    throw new ReportableException(String.format(msg, args));
  }

  /**
   * Retrieves the next service layer. Used only by the server-package code and
   * accessed by used code via {@code super.doSomething()}.
   */
  final ServiceLayer getNext() {
    if (next == null) {
      // Unexpected, all methods should be implemented by some layer
      throw new UnsupportedOperationException();
    }
    return next;
  }
}
