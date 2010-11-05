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
package com.google.gwt.autobean.server;

import com.google.gwt.autobean.server.impl.TypeUtils;
import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanUtils;
import com.google.gwt.autobean.shared.AutoBeanVisitor;
import com.google.gwt.autobean.shared.impl.AbstractAutoBean;
import com.google.gwt.core.client.impl.WeakMapping;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of an AutoBean that uses reflection.
 * 
 * @param <T> the type of interface being wrapped
 */
class ProxyAutoBean<T> extends AbstractAutoBean<T> {
  private final Class<T> beanType;
  private final Configuration configuration;
  private final List<Method> getters;
  private final T shim;

  // These constructors mirror the generated constructors.
  @SuppressWarnings("unchecked")
  public ProxyAutoBean(Class<?> beanType, Configuration configuration) {
    super();
    this.beanType = (Class<T>) beanType;
    this.configuration = configuration;
    this.getters = calculateGetters();
    this.shim = createShim();
  }

  @SuppressWarnings("unchecked")
  public ProxyAutoBean(Class<?> beanType, Configuration configuration, T toWrap) {
    super(toWrap);
    if (Proxy.isProxyClass(toWrap.getClass())) {
      System.out.println("blah");
    }
    this.beanType = (Class<T>) beanType;
    this.configuration = configuration;
    this.getters = calculateGetters();
    this.shim = createShim();
  }

  private ProxyAutoBean(ProxyAutoBean<T> toClone, boolean deep) {
    super(toClone, deep);
    this.beanType = toClone.beanType;
    this.configuration = toClone.configuration;
    this.getters = toClone.getters;
    this.shim = createShim();
  }

  @Override
  public T as() {
    return shim;
  }

  @Override
  public AutoBean<T> clone(boolean deep) {
    return new ProxyAutoBean<T>(this, deep);
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public Class<T> getType() {
    return beanType;
  }

  /**
   * Allow access by {@link ShimHandler}.
   */
  @Override
  protected void call(String method, Object returned, Object... parameters) {
    super.call(method, returned, parameters);
  }

  /**
   * Allow access by {@link ShimHandler}.
   */
  @Override
  protected void checkFrozen() {
    super.checkFrozen();
  }

  /**
   * Allow access by {@link ShimHandler}.
   */
  @Override
  protected void checkWrapped() {
    super.checkWrapped();
  }

  @Override
  protected T createSimplePeer() {
    return AutoBeanFactoryMagic.makeProxy(beanType, new SimpleBeanHandler<T>(
        this));
  }

  /**
   * Allow access by {@link ShimHandler}.
   */
  @Override
  protected <V> V get(String method, V toReturn) {
    return super.get(method, toReturn);
  }

  /**
   * Allow access by {@link BeanMethod}.
   */
  protected Map<String, Object> getValues() {
    return values;
  }

  /**
   * Allow access by {@link ShimHandler}.
   */
  protected T getWrapped() {
    return super.getWrapped();
  }

  /**
   * Allow access by {@link ShimHandler}.
   */
  @Override
  protected void set(String method, Object value) {
    super.set(method, value);
  }

  // TODO: Port to model-based when class-based TypeOracle is available.
  @Override
  protected void traverseProperties(AutoBeanVisitor visitor, OneShotContext ctx) {
    for (final Method getter : getters) {
      String name;
      PropertyName annotation = getter.getAnnotation(PropertyName.class);
      if (annotation != null) {
        name = annotation.value();
      } else {
        name = getter.getName();
        name = Character.toLowerCase(name.charAt(3))
            + (name.length() >= 5 ? name.substring(4) : "");
      }

      // Use the shim to handle automatic wrapping
      Object value;
      try {
        getter.setAccessible(true);
        value = getter.invoke(shim);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e.getCause());
      }

      // Create the context used for the property visitation
      MethodPropertyContext x = isUsingSimplePeer() ? new BeanPropertyContext(
          this, getter) : new GetterPropertyContext(this, getter);

      if (TypeUtils.isValueType(x.getType())) {
        if (visitor.visitValueProperty(name, value, x)) {
        }
        visitor.endVisitValueProperty(name, value, x);
      } else if (Collection.class.isAssignableFrom(x.getType())) {
        // Workaround for generics bug in mac javac 1.6.0_22
        @SuppressWarnings("rawtypes")
        AutoBean temp = AutoBeanUtils.getAutoBean((Collection) value);
        @SuppressWarnings("unchecked")
        AutoBean<Collection<?>> bean = (AutoBean<Collection<?>>) temp;
        if (visitor.visitCollectionProperty(name, bean, x)) {
          if (value != null) {
            ((ProxyAutoBean<?>) bean).traverse(visitor, ctx);
          }
        }
        visitor.endVisitCollectionProperty(name, bean, x);
      } else if (Map.class.isAssignableFrom(x.getType())) {
        // Workaround for generics bug in mac javac 1.6.0_22
        @SuppressWarnings("rawtypes")
        AutoBean temp = AutoBeanUtils.getAutoBean((Map) value);
        @SuppressWarnings("unchecked")
        AutoBean<Map<?, ?>> bean = (AutoBean<Map<?, ?>>) temp;
        if (visitor.visitMapProperty(name, bean, x)) {
          if (value != null) {
            ((ProxyAutoBean<?>) bean).traverse(visitor, ctx);
          }
        }
        visitor.endVisitMapProperty(name, bean, x);
      } else {
        ProxyAutoBean<?> bean = (ProxyAutoBean<?>) AutoBeanUtils.getAutoBean(value);
        if (visitor.visitReferenceProperty(name, bean, x)) {
          if (value != null) {
            bean.traverse(visitor, ctx);
          }
        }
        visitor.endVisitReferenceProperty(name, bean, x);
      }
    }
  }

  Class<?> getBeanType() {
    return beanType;
  }

  Map<String, Object> getPropertyMap() {
    return values;
  }

  private List<Method> calculateGetters() {
    List<Method> toReturn = new ArrayList<Method>();
    for (Method method : beanType.getMethods()) {
      if (BeanMethod.GET.matches(method)) {
        toReturn.add(method);
      }
    }
    return Collections.unmodifiableList(toReturn);
  }

  private T createShim() {
    T toReturn = AutoBeanFactoryMagic.makeProxy(beanType, new ShimHandler<T>(
        this, getWrapped()));
    WeakMapping.set(toReturn, AutoBean.class.getName(), this);
    return toReturn;
  }
}