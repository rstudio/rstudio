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
package com.google.web.bindery.autobean.shared.impl;

import com.google.gwt.core.client.impl.WeakMapping;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor.Context;
import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.autobean.shared.impl.AutoBeanCodexImpl.Coder;
import com.google.web.bindery.autobean.shared.impl.AutoBeanCodexImpl.EncodeState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Basic implementation.
 * 
 * @param <T> the wrapper type
 */
public abstract class AbstractAutoBean<T> implements AutoBean<T>, HasSplittable {
  /**
   * Used to avoid cycles when visiting.
   */
  public static class OneShotContext implements Context {
    private final Set<AbstractAutoBean<?>> seen = new HashSet<AbstractAutoBean<?>>();

    public boolean hasSeen(AbstractAutoBean<?> bean) {
      return !seen.add(bean);
    }
  }

  public static final String UNSPLITTABLE_VALUES_KEY = "__unsplittableValues";
  protected static final Object[] EMPTY_OBJECT = new Object[0];

  /**
   * Used by {@link #createSimplePeer()}.
   */
  protected Splittable data;
  protected T wrapped;
  private final AutoBeanFactory factory;
  private boolean frozen;
  /**
   * Lazily initialized by {@link #setTag(String, Object)} because not all
   * instances will make use of tags.
   */
  private Map<String, Object> tags;
  private final boolean usingSimplePeer;

  /**
   * Constructor that will use a generated simple peer.
   */
  protected AbstractAutoBean(AutoBeanFactory factory) {
    this(factory, StringQuoter.createSplittable());
  }

  /**
   * Constructor that will use a generated simple peer, backed with existing
   * data.
   */
  protected AbstractAutoBean(AutoBeanFactory factory, Splittable data) {
    this.data = data;
    this.factory = factory;
    usingSimplePeer = true;
    wrapped = createSimplePeer();
  }

  /**
   * Constructor that wraps an existing object. The parameters on this method
   * are reversed to avoid conflicting with the other two-arg constructor for
   * {@code AutoBean<Splittable>} instances.
   */
  protected AbstractAutoBean(T wrapped, AutoBeanFactory factory) {
    this.factory = factory;
    usingSimplePeer = false;
    data = null;
    this.wrapped = wrapped;

    // Used by AutoBeanUtils
    WeakMapping.setWeak(wrapped, AutoBean.class.getName(), this);
  }

  public void accept(AutoBeanVisitor visitor) {
    traverse(visitor, new OneShotContext());
  }

  public abstract T as();

  public AutoBean<T> clone(boolean deep) {
    throw new UnsupportedOperationException();
  }

  public AutoBeanFactory getFactory() {
    return factory;
  }

  public Splittable getSplittable() {
    return data;
  }

  @SuppressWarnings("unchecked")
  public <Q> Q getTag(String tagName) {
    return tags == null ? null : (Q) tags.get(tagName);
  }

  /**
   * Indicates that the value returned from {@link #getSplittable()} may not
   * contain all of the data encapsulated by the AutoBean.
   */
  public boolean hasUnsplittableValues() {
    return data.isReified(UNSPLITTABLE_VALUES_KEY);
  }

  public boolean isFrozen() {
    return frozen;
  }

  public boolean isWrapper() {
    return !usingSimplePeer;
  }

  public void setData(Splittable data) {
    assert data != null : "null data";
    this.data = data;
    /*
     * The simple peer aliases the data object from the enclosing bean to avoid
     * needing to call up the this.this$0 chain.
     */
    wrapped = createSimplePeer();
  }

  public void setFrozen(boolean frozen) {
    this.frozen = frozen;
  }

  public void setTag(String tagName, Object value) {
    if (tags == null) {
      tags = new HashMap<String, Object>();
    }
    tags.put(tagName, value);
  }

  public void traverse(AutoBeanVisitor visitor, OneShotContext ctx) {
    // Avoid cycles
    if (ctx.hasSeen(this)) {
      return;
    }
    if (visitor.visit(this, ctx)) {
      traverseProperties(visitor, ctx);
    }
    visitor.endVisit(this, ctx);
  }

  public T unwrap() {
    if (usingSimplePeer) {
      throw new IllegalStateException();
    }
    try {
      WeakMapping.set(wrapped, AutoBean.class.getName(), null);
      return wrapped;
    } finally {
      wrapped = null;
    }
  }

  /**
   * No-op. Used as a debugger hook point for generated code.
   * 
   * @param method the method name
   * @param returned the returned object
   * @param parameters the parameter list
   */
  protected void call(String method, Object returned, Object... parameters) {
  }

  protected void checkFrozen() {
    if (frozen) {
      throw new IllegalStateException("The AutoBean has been frozen");
    }
  }

  protected void checkWrapped() {
    if (wrapped == null && !usingSimplePeer) {
      throw new IllegalStateException("The AutoBean has been unwrapped");
    }
  }

  protected T createSimplePeer() {
    throw new UnsupportedOperationException();
  }

  /**
   * No-op. Used as a debugger hook point for generated code.
   * 
   * @param method the method name
   * @param toReturn the value to return
   */
  protected <V> V get(String method, V toReturn) {
    return toReturn;
  }

  protected <W> W getFromWrapper(W obj) {
    // Some versions of javac have problem inferring the generics here
    return AutoBeanUtils.<W, W> getAutoBean(obj).as();
  }

  /**
   * Native getters and setters for primitive properties are generated for each
   * type to ensure inlining.
   */
  protected <Q> Q getOrReify(String propertyName) {
    checkWrapped();
    if (data.isReified(propertyName)) {
      @SuppressWarnings("unchecked")
      Q temp = (Q) data.getReified(propertyName);
      return temp;
    }
    if (data.isNull(propertyName)) {
      return null;
    }
    data.setReified(propertyName, null);
    Coder coder = AutoBeanCodexImpl.doCoderFor(this, propertyName);
    @SuppressWarnings("unchecked")
    Q toReturn = (Q) coder.decode(EncodeState.forDecode(factory), data.get(propertyName));
    data.setReified(propertyName, toReturn);
    return toReturn;
  }

  protected T getWrapped() {
    checkWrapped();
    return wrapped;
  }

  protected boolean isUsingSimplePeer() {
    return usingSimplePeer;
  }

  protected boolean isWrapped(Object obj) {
    return AutoBeanUtils.getAutoBean(obj) != null;
  }

  /**
   * No-op. Used as a debugger hook point for generated code.
   * 
   * @param method the method name
   * @param value the Object value to be set
   */
  protected void set(String method, Object value) {
  }

  protected void setProperty(String propertyName, Object value) {
    checkWrapped();
    checkFrozen();
    data.setReified(propertyName, value);
    if (value == null) {
      Splittable.NULL.assign(data, propertyName);
      return;
    }
    Coder coder = AutoBeanCodexImpl.doCoderFor(this, propertyName);
    Splittable backing = coder.extractSplittable(EncodeState.forDecode(factory), value);
    if (backing == null) {
      /*
       * External data type, such as an ArrayList or a concrete implementation
       * of a setter's interface type. This means that a slow serialization pass
       * is necessary.
       */
      data.setReified(UNSPLITTABLE_VALUES_KEY, true);
    } else {
      backing.assign(data, propertyName);
    }
  }

  protected abstract void traverseProperties(AutoBeanVisitor visitor, OneShotContext ctx);
}
