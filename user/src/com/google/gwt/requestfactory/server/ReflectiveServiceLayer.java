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

import com.google.gwt.autobean.server.impl.TypeUtils;
import com.google.gwt.autobean.shared.ValueCodex;
import com.google.gwt.requestfactory.server.SimpleRequestProcessor.ServiceLayer;
import com.google.gwt.requestfactory.shared.BaseProxy;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.InstanceRequest;
import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.ProxyForName;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.Service;
import com.google.gwt.requestfactory.shared.ServiceName;
import com.google.gwt.requestfactory.shared.ValueProxy;
import com.google.gwt.requestfactory.shared.messages.EntityCodex.EntitySource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * A reflection-based implementation of ServiceLayer.
 */
public class ReflectiveServiceLayer implements ServiceLayer {

  private static final Validator jsr303Validator;

  private static final Logger log = Logger.getLogger(ReflectiveServiceLayer.class.getName());

  /**
   * All instances of the service layer that are loaded by the same classloader
   * can use a shared validator. The use of the validator should be
   * synchronized, since it is stateful.
   */
  private static final RequestFactoryInterfaceValidator validator = new RequestFactoryInterfaceValidator(
      log, new RequestFactoryInterfaceValidator.ClassLoaderLoader(
          ReflectiveServiceLayer.class.getClassLoader()));

  static {
    Validator found;
    try {
      ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
      found = validatorFactory.getValidator();
    } catch (ValidationException e) {
      log.log(Level.INFO, "Unable to initialize a JSR 303 Bean Validator", e);
      found = null;
    }
    jsr303Validator = found;
  }

  private static String capitalize(String name) {
    return Character.toUpperCase(name.charAt(0))
        + (name.length() >= 1 ? name.substring(1) : "");
  }

  public Object createDomainObject(Class<?> clazz) {
    Throwable ex;
    try {
      Constructor<?> c = clazz.getConstructor();
      c.setAccessible(true);
      return c.newInstance();
    } catch (InstantiationException e) {
      return report("Could not create a new instance of the requested type");
    } catch (NoSuchMethodException e) {
      return report("The requested type is not default-instantiable");
    } catch (InvocationTargetException e) {
      return report(e);
    } catch (IllegalAccessException e) {
      ex = e;
    } catch (SecurityException e) {
      ex = e;
    } catch (IllegalArgumentException e) {
      ex = e;
    }
    return die(ex, "Could not create a new instance of domain type %s",
        clazz.getCanonicalName());
  }

  public Class<?> getClientType(Class<?> domainClass, Class<?> clientClass) {
    String name;
    synchronized (validator) {
      name = validator.getEntityProxyTypeName(domainClass.getName(),
          clientClass.getName());
    }
    if (name != null) {
      return forName(name).asSubclass(BaseProxy.class);
    }
    if (List.class.isAssignableFrom(domainClass)) {
      return List.class;
    }
    if (Set.class.isAssignableFrom(domainClass)) {
      return Set.class;
    }
    if (TypeUtils.isValueType(domainClass)) {
      return domainClass;
    }
    return die(null, "The domain type %s cannot be sent to the client",
        domainClass.getCanonicalName());
  }

  public Class<?> getDomainClass(Class<?> clazz) {
    if (List.class.equals(clazz)) {
      return List.class;
    } else if (Set.class.equals(clazz)) {
      return Set.class;
    } else if (BaseProxy.class.isAssignableFrom(clazz)) {
      ProxyFor pf = clazz.getAnnotation(ProxyFor.class);
      if (pf != null) {
        return pf.value();
      }
      ProxyForName pfn = clazz.getAnnotation(ProxyForName.class);
      if (pfn != null) {
        Class<?> toReturn = forName(pfn.value());
        return toReturn;
      }
    }
    return die(null, "Could not resolve a domain type for client type %s",
        clazz.getCanonicalName());
  }

  public Object getId(Object domainObject) {
    return getProperty(domainObject, "id");
  }

  public Class<?> getIdType(Class<?> domainType) {
    return getFind(domainType).getParameterTypes()[0];
  }

  public Object getProperty(Object domainObject, String property) {
    Throwable toReport;
    try {
      Method method = domainObject.getClass().getMethod(
          "get" + capitalize(property));
      method.setAccessible(true);
      Object value = method.invoke(domainObject);
      return value;
    } catch (SecurityException e) {
      toReport = e;
    } catch (NoSuchMethodException e) {
      toReport = e;
    } catch (IllegalArgumentException e) {
      toReport = e;
    } catch (IllegalAccessException e) {
      toReport = e;
    } catch (InvocationTargetException e) {
      return report(e);
    }
    return die(toReport, "Could not retrieve property %s", property);
  }

  public Type getRequestReturnType(Method contextMethod) {
    Class<?> returnClass = contextMethod.getReturnType();
    if (InstanceRequest.class.isAssignableFrom(returnClass)) {
      Type[] params = TypeUtils.getParameterization(InstanceRequest.class,
          contextMethod.getGenericReturnType());
      assert params.length == 2;
      return params[1];
    } else if (Request.class.isAssignableFrom(returnClass)) {
      Type param = TypeUtils.getSingleParameterization(Request.class,
          contextMethod.getGenericReturnType());
      return param;
    } else {
      throw new IllegalArgumentException("Unknown RequestContext return type "
          + returnClass.getCanonicalName());
    }
  }

  public String getTypeToken(Class<?> clazz) {
    return clazz.getName();
  }

  public Object getVersion(Object domainObject) {
    return getProperty(domainObject, "version");
  }

  public Object invoke(Method domainMethod, Object... args) {
    Throwable ex;
    try {
      domainMethod.setAccessible(true);
      if (Modifier.isStatic(domainMethod.getModifiers())) {
        return domainMethod.invoke(null, args);
      } else {
        Object[] realArgs = new Object[args.length - 1];
        System.arraycopy(args, 1, realArgs, 0, realArgs.length);
        return domainMethod.invoke(args[0], realArgs);
      }
    } catch (IllegalArgumentException e) {
      ex = e;
    } catch (IllegalAccessException e) {
      ex = e;
    } catch (InvocationTargetException e) {
      return report(e);
    }
    return die(ex, "Could not invoke method %s", domainMethod.getName());
  }

  /**
   * This implementation attempts to re-load the object from the backing store.
   */
  public boolean isLive(EntitySource source, Object domainObject) {
    Object id = getId(domainObject);
    return invoke(getFind(domainObject.getClass()), id) != null;
  }

  public Object loadDomainObject(EntitySource source, Class<?> clazz, Object id) {
    if (id == null) {
      die(null, "Cannot invoke find method with a null id");
    }
    return invoke(getFind(clazz), id);
  }

  public Class<? extends BaseProxy> resolveClass(String typeToken) {
    Class<?> found = forName(typeToken);
    if (!EntityProxy.class.isAssignableFrom(found)
        && !ValueProxy.class.isAssignableFrom(found)) {
      die(null, "The requested type %s is not assignable to %s or %s",
          typeToken, EntityProxy.class.getCanonicalName(),
          ValueProxy.class.getCanonicalName());
    }
    synchronized (validator) {
      validator.antidote();
      validator.validateProxy(found.getName());
      if (validator.isPoisoned()) {
        die(null, "The type %s did not pass RequestFactory validation",
            found.getCanonicalName());
      }
    }
    return found.asSubclass(BaseProxy.class);
  }

  public Method resolveDomainMethod(Method requestContextMethod) {
    Class<?> enclosing = requestContextMethod.getDeclaringClass();

    Class<?> searchIn = null;
    Service s = enclosing.getAnnotation(Service.class);
    if (s != null) {
      searchIn = s.value();
    }
    ServiceName sn = enclosing.getAnnotation(ServiceName.class);
    if (sn != null) {
      searchIn = forName(sn.value());
    }
    if (searchIn == null) {
      die(null, "The %s type %s did not specify a service type",
          RequestContext.class.getSimpleName(), enclosing.getCanonicalName());
    }

    Class<?>[] parameterTypes = requestContextMethod.getParameterTypes();
    Class<?>[] domainArgs = new Class<?>[parameterTypes.length];
    for (int i = 0, j = domainArgs.length; i < j; i++) {
      if (BaseProxy.class.isAssignableFrom(parameterTypes[i])) {
        domainArgs[i] = getDomainClass(parameterTypes[i].asSubclass(BaseProxy.class));
      } else if (EntityProxyId.class.isAssignableFrom(parameterTypes[i])) {
        domainArgs[i] = TypeUtils.ensureBaseType(TypeUtils.getSingleParameterization(
            EntityProxyId.class,
            requestContextMethod.getGenericParameterTypes()[i]));
      } else {
        domainArgs[i] = parameterTypes[i];
      }
    }

    Throwable ex;
    try {
      return searchIn.getMethod(requestContextMethod.getName(), domainArgs);
    } catch (SecurityException e) {
      ex = e;
    } catch (NoSuchMethodException e) {
      return report("Could not locate domain method %s",
          requestContextMethod.getName());
    }
    return die(ex, "Could not get domain method %s in type %s",
        requestContextMethod.getName(), searchIn.getCanonicalName());
  }

  public Method resolveRequestContextMethod(String requestContextClass,
      String methodName) {
    synchronized (validator) {
      validator.antidote();
      validator.validateRequestContext(requestContextClass);
      if (validator.isPoisoned()) {
        die(null, "The RequestContext type %s did not pass validation",
            requestContextClass);
      }
    }
    Class<?> searchIn = forName(requestContextClass);
    for (Method method : searchIn.getMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    return report("Could not locate %s method %s::%s",
        RequestContext.class.getSimpleName(), requestContextClass, methodName);
  }

  public void setProperty(Object domainObject, String property,
      Class<?> expectedType, Object value) {
    Method setter;
    Throwable ex;
    try {
      setter = domainObject.getClass().getMethod("set" + capitalize(property),
          expectedType);
      setter.setAccessible(true);
      setter.invoke(domainObject, value);
      return;
    } catch (SecurityException e) {
      ex = e;
    } catch (NoSuchMethodException e) {
      ex = e;
    } catch (IllegalArgumentException e) {
      ex = e;
    } catch (IllegalAccessException e) {
      ex = e;
    } catch (InvocationTargetException e) {
      report(e);
      return;
    }
    die(ex, "Could not locate setter for property %s in type %s", property,
        domainObject.getClass().getCanonicalName());
  }

  public <T> Set<ConstraintViolation<T>> validate(T domainObject) {
    if (jsr303Validator != null) {
      return jsr303Validator.validate(domainObject);
    }
    return Collections.emptySet();
  }

  /**
   * Throw a fatal error up into the top-level processing code. This method
   * should be used to provide diagnostic information that will help the
   * end-developer track down problems when that data would expose
   * implementation details of the server to the client.
   */
  private <T> T die(Throwable e, String message, Object... args)
      throws UnexpectedException {
    String msg = String.format(message, args);
    log.log(Level.SEVERE, msg, e);
    throw new UnexpectedException(msg, e);
  }

  /**
   * Call {@link Class#forName(String)} and report any errors through
   * {@link #die()}.
   */
  private Class<?> forName(String name) {
    try {
      return Class.forName(name, false,
          Thread.currentThread().getContextClassLoader());
    } catch (ClassNotFoundException e) {
      return die(e, "Could not locate class %s", name);
    }
  }

  private Method getFind(Class<?> clazz) {
    if (clazz == null) {
      return die(null, "Could not find static method with a single"
          + " parameter of a key type");
    }
    String searchFor = "find" + clazz.getSimpleName();
    for (Method method : clazz.getMethods()) {
      if (!Modifier.isStatic(method.getModifiers())) {
        continue;
      }
      if (!searchFor.equals(method.getName())) {
        continue;
      }
      if (method.getParameterTypes().length != 1) {
        continue;
      }
      if (!isKeyType(method.getParameterTypes()[0])) {
        continue;
      }
      return method;
    }
    return getFind(clazz.getSuperclass());
  }

  /**
   * Returns <code>true</code> if the given class can be used as an id or
   * version key.
   */
  private boolean isKeyType(Class<?> domainClass) {
    if (ValueCodex.canDecode(domainClass)) {
      return true;
    }

    return BaseProxy.class.isAssignableFrom(getClientType(domainClass,
        BaseProxy.class));
  }

  /**
   * Report an exception thrown by code that is under the control of the
   * end-developer.
   */
  private <T> T report(InvocationTargetException userGeneratedException)
      throws ReportableException {
    throw new ReportableException(userGeneratedException.getCause());
  }

  /**
   * Return a message to the client. This method should not include any data
   * that was not sent to the server by the client to avoid leaking data.
   * 
   * @see #die()
   */
  private <T> T report(String msg, Object... args) throws ReportableException {
    throw new ReportableException(String.format(msg, args));
  }
}
