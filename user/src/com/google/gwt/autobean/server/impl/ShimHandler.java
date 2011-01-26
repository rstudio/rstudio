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
package com.google.gwt.autobean.server.impl;

import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanUtils;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
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
  private final T toWrap;

  public ShimHandler(ProxyAutoBean<T> bean, T toWrap) {
    this.bean = bean;
    this.toWrap = toWrap;

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

  public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable {
    method.setAccessible(true);
    Object toReturn;
    String name = method.getName();
    bean.checkWrapped();
    method.setAccessible(true);
    if (BeanMethod.OBJECT.matches(method)) {
      return method.invoke(this, args);
    } else if (BeanMethod.GET.matches(method)) {
      toReturn = method.invoke(toWrap, args);
      toReturn = bean.get(name, toReturn);
    } else if (BeanMethod.SET.matches(method)) {
      bean.checkFrozen();
      toReturn = method.invoke(toWrap, args);
      bean.set(name, args[0]);
    } else {
      // XXX How should freezing and calls work together?
      // bean.checkFrozen();
      toReturn = method.invoke(toWrap, args);
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
    if (TypeUtils.isValueType(intf)
        || TypeUtils.isValueType(toReturn.getClass())
        || AutoBeanUtils.getAutoBean(toReturn) != null
        || bean.getConfiguration().getNoWrap().contains(intf)) {
      return toReturn;
    }
    if (toReturn.getClass().isArray()) {
      for (int i = 0, j = Array.getLength(toReturn); i < j; i++) {
        Object value = Array.get(toReturn, i);
        if (value != null) {
          Array.set(toReturn, i, maybeWrap(value.getClass(), value));
        }
      }
      return toReturn;
    }
    ProxyAutoBean<Object> newBean = new ProxyAutoBean<Object>(
        bean.getFactory(), intf, bean.getConfiguration(), toReturn);
    return newBean.as();
  }
}