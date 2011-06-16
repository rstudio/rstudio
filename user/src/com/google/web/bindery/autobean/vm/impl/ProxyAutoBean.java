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

import com.google.gwt.core.client.impl.WeakMapping;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor;
import com.google.web.bindery.autobean.shared.impl.AbstractAutoBean;
import com.google.web.bindery.autobean.vm.Configuration;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
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
public class ProxyAutoBean<T> extends AbstractAutoBean<T> {
  private static class Data {
    final List<Method> getters = new ArrayList<Method>();
    final List<String> getterNames = new ArrayList<String>();
    final List<PropertyType> propertyType = new ArrayList<PropertyType>();
  }

  private enum PropertyType {
    VALUE, REFERENCE, COLLECTION, MAP;
  }

  private static final Map<Class<?>, Data> cache = new WeakHashMap<Class<?>, Data>();

  /**
   * Utility method to crete a new {@link Proxy} instance.
   * 
   * @param <T> the interface type to be implemented by the Proxy
   * @param intf the Class representing the interface type
   * @param handler the implementation of the interface
   * @param extraInterfaces additional interface types the Proxy should
   *          implement
   * @return a Proxy instance
   */
  public static <T> T makeProxy(Class<T> intf, InvocationHandler handler,
      Class<?>... extraInterfaces) {
    Class<?>[] intfs;
    if (extraInterfaces == null) {
      intfs = new Class<?>[] {intf};
    } else {
      intfs = new Class<?>[extraInterfaces.length + 1];
      intfs[0] = intf;
      System.arraycopy(extraInterfaces, 0, intfs, 1, extraInterfaces.length);
    }

    return intf.cast(Proxy.newProxyInstance(intf.getClassLoader(), intfs, handler));
  }

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
              name = BeanMethod.GET.inferName(method);
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
  /**
   * Because the shim and the ProxyAutoBean are related through WeakMapping, we
   * need to ensure that the ProxyAutoBean doesn't artificially extend the
   * lifetime of the shim. If there are no external references to the shim, it's
   * ok if it's deallocated, since it has no interesting state.
   * 
   * <pre>
   * _________________            ______________
   * | ProxyAutoBean |            |    Shim    |
   * |               | <----------+-bean       |
   * |          shim-+---X------> |            |
   * |_______________|            |____________|
   *         ^                           ^  ^
   *         X                           X  |
   *         |__value__WeakMapping__key__|  |
   *                      ^                 |
   *                      |                 |
   *                   GC Roots -> Owner____|
   * </pre>
   * <p>
   * In the case of a wrapped object (for example, an ArrayList), the weak
   * reference from WeakMapping to the ProxyAutoBean may cause the AutoBean to
   * be prematurely collected if neither the bean nor the shim are referenced
   * elsewhere. The alternative is a massive memory leak.
   */
  private WeakReference<T> shim;

  // These constructors mirror the generated constructors.
  @SuppressWarnings("unchecked")
  public ProxyAutoBean(AutoBeanFactory factory, Class<?> beanType, Configuration configuration) {
    super(factory);
    this.beanType = (Class<T>) beanType;
    this.configuration = configuration;
    this.data = calculateData(beanType);
  }

  @SuppressWarnings("unchecked")
  public ProxyAutoBean(AutoBeanFactory factory, Class<?> beanType, Configuration configuration,
      T toWrap) {
    super(toWrap, factory);
    this.beanType = (Class<T>) beanType;
    this.configuration = configuration;
    this.data = calculateData(beanType);
  }

  @Override
  public T as() {
    T toReturn = shim == null ? null : shim.get();
    if (toReturn == null) {
      toReturn = createShim();
      shim = new WeakReference<T>(toReturn);
    }
    return toReturn;
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

  /**
   * Not used in this implementation. Instead, the simple implementation is
   * created lazily in {@link #getWrapped()}.
   */
  @Override
  protected T createSimplePeer() {
    return null;
  }

  /**
   * Allow access by {@link ShimHandler}.
   */
  @Override
  protected <V> V get(String method, V toReturn) {
    return super.get(method, toReturn);
  }

  /**
   * Allow access by BeanMethod.
   */
  @Override
  protected <V> V getOrReify(String propertyName) {
    return super.<V> getOrReify(propertyName);
  }

  /**
   * Allow access by {@link ShimHandler}.
   */
  @Override
  protected T getWrapped() {
    if (wrapped == null && isUsingSimplePeer()) {
      wrapped = (T) ProxyAutoBean.makeProxy(beanType, new SimpleBeanHandler<T>(this));
    }
    return super.getWrapped();
  }

  /**
   * Allow access by {@link ShimHandler}.
   */
  @Override
  protected void set(String method, Object value) {
    super.set(method, value);
  }

  @Override
  protected void setProperty(String propertyName, Object value) {
    super.setProperty(propertyName, value);
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
        value = getter.invoke(as());
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e.getCause());
      }

      // Create the context used for the property visitation
      MethodPropertyContext x =
          isUsingSimplePeer() ? new BeanPropertyContext(this, getter) : new GetterPropertyContext(
              this, getter);

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

  private T createShim() {
    T toReturn = ProxyAutoBean.makeProxy(beanType, new ShimHandler<T>(this, getWrapped()));
    WeakMapping.setWeak(toReturn, AutoBean.class.getName(), this);
    return toReturn;
  }
}
