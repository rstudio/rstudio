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
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.ProxyForName;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.Service;
import com.google.gwt.requestfactory.shared.ServiceName;
import com.google.gwt.requestfactory.shared.messages.EntityCodex;
import com.google.gwt.requestfactory.shared.messages.EntityCodex.EntitySource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
      return clazz.newInstance();
    } catch (InstantiationException e) {
      return report("Could not create a new instance of the requested type");
    } catch (IllegalAccessException e) {
      ex = e;
    }
    return die(ex, "Could not create a new instance of domain type %s",
        clazz.getCanonicalName());
  }

  public Class<?> getClientType(Class<?> domainClass) {
    String name;
    synchronized (validator) {
      name = validator.getEntityProxyTypeName(domainClass.getName());
    }
    if (name != null) {
      return forName(name).asSubclass(EntityProxy.class);
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
    } else if (EntityProxy.class.isAssignableFrom(clazz)) {
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

  public String getFlatId(EntitySource source, Object domainObject) {
    Object id = getProperty(domainObject, "id");
    if (id == null) {
      report("Could not retrieve id property for domain object");
    }
    if (!isKeyType(id.getClass())) {
      die(null, "The type %s is not a valid key type",
          id.getClass().getCanonicalName());
    }
    return EntityCodex.encode(source, id).getPayload();
  }

  public Object getProperty(Object domainObject, String property) {
    Throwable toReport;
    try {
      Method method = domainObject.getClass().getMethod(
          "get" + capitalize(property));
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

  public String getTypeToken(Class<?> clazz) {
    return clazz.getName();
  }

  public int getVersion(Object domainObject) {
    // TODO: Make version any value type
    Object version = getProperty(domainObject, "version");
    if (version == null) {
      report("Could not retrieve version property");
    }
    if (!(version instanceof Integer)) {
      die(null, "The getVersion() method on type %s did not return"
          + " int or Integer", domainObject.getClass().getCanonicalName());
    }
    return ((Integer) version).intValue();
  }

  public Object invoke(Method domainMethod, Object[] args) {
    Throwable ex;
    try {
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

  public Object loadDomainObject(EntitySource source, Class<?> clazz,
      String flatId) {
    String searchFor = "find" + clazz.getSimpleName();
    Method found = null;
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
      found = method;
      break;
    }
    if (found == null) {
      die(null, "Could not find static method %s with a single"
          + " parameter of a key type", searchFor);
    }
    Object id = EntityCodex.decode(source, found.getParameterTypes()[0], null,
        flatId);
    if (id == null) {
      report("Cannot load a domain object with a null id");
    }
    Throwable ex;
    try {
      return found.invoke(null, id);
    } catch (IllegalArgumentException e) {
      ex = e;
    } catch (IllegalAccessException e) {
      ex = e;
    } catch (InvocationTargetException e) {
      return report(e);
    }
    return die(ex, "Cauld not load domain object using id", id.toString());
  }

  public Class<? extends EntityProxy> resolveClass(String typeToken) {
    Class<?> found = forName(typeToken);
    if (!EntityProxy.class.isAssignableFrom(found)) {
      die(null, "The requested type %s is not assignable to %s", typeToken,
          EntityProxy.class.getName());
    }
    synchronized (validator) {
      validator.antidote();
      validator.validateEntityProxy(found.getName());
      if (validator.isPoisoned()) {
        die(null, "The type %s did not pass RequestFactory validation",
            found.getCanonicalName());
      }
    }
    return found.asSubclass(EntityProxy.class);
  }

  public Method resolveDomainMethod(Method requestContextMethod) {
    Class<?> enclosing = requestContextMethod.getDeclaringClass();
    synchronized (validator) {
      validator.antidote();
      validator.validateRequestContext(enclosing.getName());
      if (validator.isPoisoned()) {
        die(null, "The type %s did not pass RequestFactory validation",
            enclosing.getCanonicalName());
      }
    }

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
      if (EntityProxy.class.isAssignableFrom(parameterTypes[i])) {
        domainArgs[i] = getDomainClass(parameterTypes[i].asSubclass(EntityProxy.class));
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
    Method getId;
    Throwable ex;
    try {
      getId = domainObject.getClass().getMethod("set" + capitalize(property),
          expectedType);
      getId.invoke(domainObject, value);
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
    die(ex, "Could not locate getter for property %s in type %s", property,
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

  private Class<?> forName(String name) {
    try {
      return Class.forName(name, false,
          Thread.currentThread().getContextClassLoader());
    } catch (ClassNotFoundException e) {
      return die(e, "Could not locate class %s", name);
    }
  }

  private boolean isKeyType(Class<?> clazz) {
    return ValueCodex.canDecode(clazz)
        || EntityProxy.class.isAssignableFrom(clazz);
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
