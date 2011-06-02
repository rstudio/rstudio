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

import com.google.web.bindery.autobean.shared.ValueCodex;
import com.google.web.bindery.autobean.vm.impl.BeanMethod;
import com.google.web.bindery.autobean.vm.impl.TypeUtils;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.InstanceRequest;
import com.google.web.bindery.requestfactory.shared.Request;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
 * Implements all methods that interact with domain objects.
 */
final class ReflectiveServiceLayer extends ServiceLayerDecorator {
  /*
   * NB: All calls that ReflectiveServiceLayer makes to public APIs inherited
   * from ServiceLayer should be made to use the instance returned from
   * getTop().
   */

  private static final Validator jsr303Validator;
  private static final Logger log = Logger.getLogger(ServiceLayer.class.getName());

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

  /**
   * Linear search, but we want to handle getFoo, isFoo, and hasFoo. The result
   * of this method will be cached by the ServiceLayerCache.
   */
  private static Method getBeanMethod(BeanMethod methodType, Class<?> domainType, String property) {
    for (Method m : domainType.getMethods()) {
      if (methodType.matches(m) && property.equals(methodType.inferName(m))) {
        m.setAccessible(true);
        return m;
      }
    }
    return null;
  }

  @Override
  public <T> T createDomainObject(Class<T> clazz) {
    Throwable ex;
    try {
      Constructor<T> c = clazz.getConstructor();
      c.setAccessible(true);
      return c.newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();
      return this.<T> report("Could not create a new instance of the requested type");
    } catch (NoSuchMethodException e) {
      return this.<T> report("The requested type is not default-instantiable");
    } catch (InvocationTargetException e) {
      return this.<T> report(e);
    } catch (IllegalAccessException e) {
      ex = e;
    } catch (SecurityException e) {
      ex = e;
    } catch (IllegalArgumentException e) {
      ex = e;
    }
    return this.<T> die(ex, "Could not create a new instance of domain type %s", clazz
        .getCanonicalName());
  }

  @Override
  public Method getGetter(Class<?> domainType, String property) {
    return getBeanMethod(BeanMethod.GET, domainType, property);
  }

  @Override
  public Object getId(Object domainObject) {
    return getTop().getProperty(domainObject, "id");
  }

  @Override
  public Class<?> getIdType(Class<?> domainType) {
    return getFind(domainType).getParameterTypes()[0];
  }

  @Override
  public Object getProperty(Object domainObject, String property) {
    try {
      Method getter = getTop().getGetter(domainObject.getClass(), property);
      if (getter == null) {
        die(null, "Could not determine getter for property %s on type %s", property, domainObject
            .getClass().getCanonicalName());
      }
      Object value = getter.invoke(domainObject);
      return value;
    } catch (IllegalAccessException e) {
      return die(e, "Could not retrieve property %s", property);
    } catch (InvocationTargetException e) {
      return report(e);
    }
  }

  @Override
  public Type getRequestReturnType(Method contextMethod) {
    Class<?> returnClass = contextMethod.getReturnType();
    if (InstanceRequest.class.isAssignableFrom(returnClass)) {
      Type[] params =
          TypeUtils
              .getParameterization(InstanceRequest.class, contextMethod.getGenericReturnType());
      assert params.length == 2;
      return params[1];
    } else if (Request.class.isAssignableFrom(returnClass)) {
      Type param =
          TypeUtils.getSingleParameterization(Request.class, contextMethod.getGenericReturnType());
      return param;
    } else {
      return die(null, "Unknown RequestContext return type %s", returnClass.getCanonicalName());
    }
  }

  @Override
  public Method getSetter(Class<?> domainType, String property) {
    Method setter = getBeanMethod(BeanMethod.SET, domainType, property);
    if (setter == null) {
      setter = getBeanMethod(BeanMethod.SET_BUILDER, domainType, property);
    }
    return setter;
  }

  @Override
  public Object getVersion(Object domainObject) {
    return getTop().getProperty(domainObject, "version");
  }

  @Override
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
  @Override
  public boolean isLive(Object domainObject) {
    Object id = getTop().getId(domainObject);
    return getTop().invoke(getFind(domainObject.getClass()), id) != null;
  }

  @Override
  public <T> T loadDomainObject(Class<T> clazz, Object id) {
    if (id == null) {
      die(null, "Cannot invoke find method with a null id");
    }
    return clazz.cast(getTop().invoke(getFind(clazz), id));
  }

  @Override
  public List<Object> loadDomainObjects(List<Class<?>> classes, List<Object> domainIds) {
    if (classes.size() != domainIds.size()) {
      die(null, "Size mismatch in paramaters. classes.size() = %d domainIds.size=%d", classes
          .size(), domainIds.size());
    }
    List<Object> toReturn = new ArrayList<Object>(classes.size());
    Iterator<Class<?>> classIt = classes.iterator();
    Iterator<Object> idIt = domainIds.iterator();
    while (classIt.hasNext()) {
      toReturn.add(getTop().loadDomainObject(classIt.next(), idIt.next()));
    }
    return toReturn;
  }

  @Override
  public void setProperty(Object domainObject, String property, Class<?> expectedType, Object value) {
    try {
      Method setter = getTop().getSetter(domainObject.getClass(), property);
      if (setter == null) {
        die(null, "Could not locate setter for property %s in type %s", property, domainObject
            .getClass().getCanonicalName());
      }
      setter.invoke(domainObject, value);
      return;
    } catch (IllegalAccessException e) {
      die(e, "Could not set property %s", property);
    } catch (InvocationTargetException e) {
      report(e);
    }
  }

  @Override
  public <T> Set<ConstraintViolation<T>> validate(T domainObject) {
    if (jsr303Validator != null) {
      return jsr303Validator.validate(domainObject);
    }
    return Collections.emptySet();
  }

  private Method getFind(Class<?> clazz) {
    if (clazz == null) {
      return die(null, "Could not find static method with a single" + " parameter of a key type");
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

    return BaseProxy.class.isAssignableFrom(getTop().resolveClientType(domainClass,
        BaseProxy.class, true));
  }
}
