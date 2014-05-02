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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
    final Class<?> elementType;
    final Type genericType;
    final Method getter;
    final Class<?> keyType;
    final PropertyType propertyType;
    Method setter;
    final Class<?> type;
    final Class<?> valueType;

    Data(Method getter, Type genericType, Class<?> type, PropertyType propertyType) {
      this.getter = getter;
      this.genericType = genericType;
      this.type = type;
      this.propertyType = propertyType;

      if (propertyType == PropertyType.COLLECTION) {
        elementType =
            TypeUtils.ensureBaseType(TypeUtils.getSingleParameterization(Collection.class,
                genericType, type));
        keyType = valueType = null;
      } else if (propertyType == PropertyType.MAP) {
        elementType = null;
        Type[] types = TypeUtils.getParameterization(Map.class, genericType, type);
        keyType = TypeUtils.ensureBaseType(types[0]);
        valueType = TypeUtils.ensureBaseType(types[1]);
      } else {
        elementType = keyType = valueType = null;
      }
    }
  }

  private enum PropertyType {
    VALUE, REFERENCE, COLLECTION, MAP;
  }

  private static final Map<Class<?>, Map<String, Data>> cache =
      new WeakHashMap<Class<?>, Map<String, Data>>();

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

  private static Map<String, Data> calculateData(Class<?> beanType) {
    Map<String, Data> toReturn;
    synchronized (cache) {
      toReturn = cache.get(beanType);
      if (toReturn == null) {
        Map<String, Data> getters = new HashMap<String, Data>();
        List<Method> setters = new ArrayList<Method>();
        for (Method method : beanType.getMethods()) {
          if (BeanMethod.GET.matches(method)) {
            // match methods on their name for now, to find the most specific
            // override
            String name = method.getName();

            Type genericReturnType = TypeUtils.resolveGenerics(beanType, method.getGenericReturnType());
            Class<?> returnType = TypeUtils.ensureBaseType(genericReturnType);

            Data data = getters.get(name);
            if (data == null || data.type.isAssignableFrom(returnType)) {
              // no getter seen yet for the property, or a less specific one
              PropertyType propertyType;
              if (TypeUtils.isValueType(returnType)) {
                propertyType = PropertyType.VALUE;
              } else if (Collection.class.isAssignableFrom(returnType)) {
                propertyType = PropertyType.COLLECTION;
              } else if (Map.class.isAssignableFrom(returnType)) {
                propertyType = PropertyType.MAP;
              } else {
                propertyType = PropertyType.REFERENCE;
              }
              data = new Data(method, genericReturnType, returnType, propertyType);

              getters.put(name, data);
            }
          } else if (BeanMethod.SET.matches(method) || BeanMethod.SET_BUILDER.matches(method)) {
            setters.add(method);
          }
        }

        toReturn = new HashMap<String, Data>(getters.size());

        // Now take @PropertyName into account
        for (Map.Entry<String, Data> entry : getters.entrySet()) {
          Data data = entry.getValue();
          toReturn.put(BeanMethod.GET.inferName(data.getter), data);
        }

        // Associate setters to getters
        for (Method setter : setters) {
          String name = BeanMethod.SET.inferName(setter);
          Data data = toReturn.get(name);
          if (data != null && data.setter == null
              && data.getter.getReturnType().isAssignableFrom(setter.getParameterTypes()[0])) {
            data.setter = setter;
          }
        }

        cache.put(beanType, toReturn);
      }
    }
    return toReturn;
  }

  private final Class<T> beanType;
  private final Configuration configuration;
  private final Map<String, Data> propertyData;
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
    this.propertyData = calculateData(beanType);
  }

  @SuppressWarnings("unchecked")
  public ProxyAutoBean(AutoBeanFactory factory, Class<?> beanType, Configuration configuration,
      T toWrap) {
    super(toWrap, factory);
    this.beanType = (Class<T>) beanType;
    this.configuration = configuration;
    this.propertyData = calculateData(beanType);
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
      wrapped = ProxyAutoBean.<T> makeProxy(beanType, new SimpleBeanHandler<T>(this));
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
    for (Map.Entry<String, Data> entry : propertyData.entrySet()) {
      String name = entry.getKey();
      Data data = entry.getValue();
      Method getter = data.getter;
      PropertyType propertyType = data.propertyType;

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
      MethodPropertyContext x;
      if (isUsingSimplePeer()) {
        x =
            new BeanPropertyContext(this, name, data.genericType, data.type, data.elementType,
                data.keyType, data.valueType);
      } else {
        x =
            new GetterPropertyContext(this, getter, data.genericType, data.type, data.elementType,
                data.keyType, data.valueType);
      }

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

  private T createShim() {
    T toReturn = ProxyAutoBean.makeProxy(beanType, new ShimHandler<T>(this, getWrapped()));
    WeakMapping.setWeak(toReturn, AutoBean.class.getName(), this);
    return toReturn;
  }
}
