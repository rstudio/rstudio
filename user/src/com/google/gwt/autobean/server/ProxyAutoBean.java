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
import com.google.gwt.autobean.shared.AutoBeanFactory;
import com.google.gwt.autobean.shared.AutoBeanUtils;
import com.google.gwt.autobean.shared.AutoBeanVisitor;
import com.google.gwt.autobean.shared.impl.AbstractAutoBean;
import com.google.gwt.core.client.impl.WeakMapping;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * An implementation of an AutoBean that uses reflection.
 * 
 * @param <T> the type of interface being wrapped
 */
class ProxyAutoBean<T> extends AbstractAutoBean<T> {
  private static class Data {
    final List<Method> getters = new ArrayList<Method>();
    final List<String> getterNames = new ArrayList<String>();
    final List<PropertyType> propertyType = new ArrayList<PropertyType>();
  }

  private enum PropertyType {
    VALUE, REFERENCE, COLLECTION, MAP;
  }

  private static final Map<Class<?>, Data> cache = new WeakHashMap<Class<?>, Data>();

  private static Data calculateData(Class<?> beanType) {
    Data toReturn;
    synchronized (cache) {
      toReturn = cache.get(beanType);
      if (toReturn == null) {
        toReturn = new Data();
        for (Method method : beanType.getMethods()) {
          if (BeanMethod.GET.matches(method)) {
            toReturn.getters.add(method);

            String name;
            PropertyName annotation = method.getAnnotation(PropertyName.class);
            if (annotation != null) {
              name = annotation.value();
            } else {
              name = method.getName();
              name = Character.toLowerCase(name.charAt(3))
                  + (name.length() >= 5 ? name.substring(4) : "");
            }
            toReturn.getterNames.add(name);

            Class<?> returnType = method.getReturnType();
            if (TypeUtils.isValueType(returnType)) {
              toReturn.propertyType.add(PropertyType.VALUE);
            } else if (Collection.class.isAssignableFrom(returnType)) {
              toReturn.propertyType.add(PropertyType.COLLECTION);
            } else if (Map.class.isAssignableFrom(returnType)) {
              toReturn.propertyType.add(PropertyType.MAP);
            } else {
              toReturn.propertyType.add(PropertyType.REFERENCE);
            }
          }
        }
        cache.put(beanType, toReturn);
      }
    }
    return toReturn;
  }

  private final Class<T> beanType;
  private final Configuration configuration;
  private final Data data;
  private final T shim;

  // These constructors mirror the generated constructors.
  @SuppressWarnings("unchecked")
  public ProxyAutoBean(AutoBeanFactory factory, Class<?> beanType,
      Configuration configuration) {
    super(factory);
    this.beanType = (Class<T>) beanType;
    this.configuration = configuration;
    this.data = calculateData(beanType);
    this.shim = createShim();
  }

  @SuppressWarnings("unchecked")
  public ProxyAutoBean(AutoBeanFactory factory, Class<?> beanType,
      Configuration configuration, T toWrap) {
    super(factory, toWrap);
    if (Proxy.isProxyClass(toWrap.getClass())) {
      System.out.println("blah");
    }
    this.beanType = (Class<T>) beanType;
    this.configuration = configuration;
    this.data = calculateData(beanType);
    this.shim = createShim();
  }

  private ProxyAutoBean(ProxyAutoBean<T> toClone, boolean deep) {
    super(toClone, deep);
    this.beanType = toClone.beanType;
    this.configuration = toClone.configuration;
    this.data = toClone.data;
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
    assert data.getters.size() == data.getterNames.size()
        && data.getters.size() == data.propertyType.size();
    Iterator<Method> getterIt = data.getters.iterator();
    Iterator<String> nameIt = data.getterNames.iterator();
    Iterator<PropertyType> typeIt = data.propertyType.iterator();
    while (getterIt.hasNext()) {
      Method getter = getterIt.next();
      String name = nameIt.next();
      PropertyType propertyType = typeIt.next();

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

      switch (propertyType) {
        case VALUE: {
          if (visitor.visitValueProperty(name, value, x)) {
          }
          visitor.endVisitValueProperty(name, value, x);
          break;
        }
        case COLLECTION: {
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
          break;
        }
        case MAP: {
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
          break;
        }
        case REFERENCE: {
          ProxyAutoBean<?> bean = (ProxyAutoBean<?>) AutoBeanUtils.getAutoBean(value);
          if (visitor.visitReferenceProperty(name, bean, x)) {
            if (value != null) {
              bean.traverse(visitor, ctx);
            }
          }
          visitor.endVisitReferenceProperty(name, bean, x);
          break;
        }
      }
    }
  }

  Class<?> getBeanType() {
    return beanType;
  }

  Map<String, Object> getPropertyMap() {
    return values;
  }

  private T createShim() {
    T toReturn = AutoBeanFactoryMagic.makeProxy(beanType, new ShimHandler<T>(
        this, getWrapped()));
    WeakMapping.set(toReturn, AutoBean.class.getName(), this);
    return toReturn;
  }
}