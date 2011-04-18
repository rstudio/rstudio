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
package com.google.web.bindery.autobean.vm.impl;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Implements an AutoBean's shim interface that intercepts calls to the backing
 * object.
 * 
 * @param <T> the interface type of the AutoBean
 */
class ShimHandler<T> implements InvocationHandler {
  private final ProxyAutoBean<T> bean;
  private final Method interceptor;

  public ShimHandler(ProxyAutoBean<T> bean, T toWrap) {
    this.bean = bean;

    Method maybe = null;
    for (Class<?> clazz : bean.getConfiguration().getCategories()) {
      try {
        maybe = clazz.getMethod("__intercept", AutoBean.class, Object.class);
        break;
      } catch (SecurityException expected) {
      } catch (NoSuchMethodException expected) {
      }
    }
    interceptor = maybe;
  }

  @Override
  public boolean equals(Object couldBeShim) {
    if (couldBeShim == null) {
      return false;
    }
    // Handles the foo.equals(foo) case
    if (Proxy.isProxyClass(couldBeShim.getClass())
        && this == Proxy.getInvocationHandler(couldBeShim)) {
      return true;
    }
    return bean.getWrapped().equals(couldBeShim);
  }

  @Override
  public int hashCode() {
    return bean.getWrapped().hashCode();
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    method.setAccessible(true);
    Object toReturn;
    String name = method.getName();
    method.setAccessible(true);
    try {
      if (BeanMethod.OBJECT.matches(method)) {
        return method.invoke(this, args);
      } else if (BeanMethod.GET.matches(method)) {
        toReturn = method.invoke(bean.getWrapped(), args);
        toReturn = bean.get(name, toReturn);
      } else if (BeanMethod.SET.matches(method) || BeanMethod.SET_BUILDER.matches(method)) {
        toReturn = method.invoke(bean.getWrapped(), args);
        bean.set(name, args[0]);
      } else {
        // XXX How should freezing and calls work together?
        toReturn = method.invoke(bean.getWrapped(), args);
        bean.call(name, toReturn, args);
      }
      Class<?> intf = method.getReturnType();
      if (!Object.class.equals(intf)) {
        // XXX Need to deal with resolving generic T return types
        toReturn = maybeWrap(intf, toReturn);
      }
      if (interceptor != null) {
        toReturn = interceptor.invoke(null, bean, toReturn);
      }
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
    return toReturn;
  }

  @Override
  public String toString() {
    return bean.getWrapped().toString();
  }

  private Object maybeWrap(Class<?> intf, Object toReturn) {
    if (toReturn == null) {
      return null;
    }
    AutoBean<?> returnBean = AutoBeanUtils.getAutoBean(toReturn);
    if (returnBean != null) {
      return returnBean.as();
    }
    if (TypeUtils.isValueType(intf) || TypeUtils.isValueType(toReturn.getClass())
        || bean.getConfiguration().getNoWrap().contains(intf)) {
      return toReturn;
    }
    if (toReturn.getClass().isArray()) {
      /*
       * We can't reliably wrap arrays, but the only time we typically see an
       * array is with toArray() call on a collection, since arrays aren't
       * supported property types.
       */
      return toReturn;
    }
    ProxyAutoBean<Object> newBean =
        new ProxyAutoBean<Object>(bean.getFactory(), intf, bean.getConfiguration(), toReturn);
    return newBean.as();
  }
}
