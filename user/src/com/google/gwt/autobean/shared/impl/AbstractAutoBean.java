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
package com.google.gwt.autobean.shared.impl;

import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanUtils;
import com.google.gwt.autobean.shared.AutoBeanVisitor;
import com.google.gwt.autobean.shared.AutoBeanVisitor.Context;
import com.google.gwt.core.client.impl.WeakMapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Basic implementation.
 * 
 * @param <T> the wrapper type
 */
public abstract class AbstractAutoBean<T> implements AutoBean<T> {
  /**
   * Used to avoid cycles when visiting.
   */
  public static class OneShotContext implements Context {
    private final Set<AbstractAutoBean<?>> seen = new HashSet<AbstractAutoBean<?>>();

    public boolean hasSeen(AbstractAutoBean<?> bean) {
      return !seen.add(bean);
    }
  }

  protected static final Object[] EMPTY_OBJECT = new Object[0];

  /**
   * Used by {@link #createSimplePeer()}.
   */
  protected final Map<String, Object> values;

  private boolean frozen;

  /**
   * Lazily initialized by {@link #setTag(String, Object)} because not all
   * instances will make use of tags.
   */
  private Map<String, Object> tags;
  private final boolean usingSimplePeer;
  private T wrapped;

  /**
   * Constructor that will use a generated simple peer.
   */
  protected AbstractAutoBean() {
    usingSimplePeer = true;
    values = new HashMap<String, Object>();
  }

  /**
   * Clone constructor.
   */
  protected AbstractAutoBean(AbstractAutoBean<T> toClone, boolean deep) {
    if (!toClone.usingSimplePeer) {
      throw new IllegalStateException("Cannot clone wrapped bean");
    }
    if (toClone.tags != null) {
      tags = new HashMap<String, Object>(toClone.tags);
    }
    usingSimplePeer = true;
    values = new HashMap<String, Object>(toClone.values);

    if (deep) {
      for (Map.Entry<String, Object> entry : values.entrySet()) {
        AutoBean<?> auto = AutoBeanUtils.getAutoBean(entry.getValue());
        if (auto != null) {
          entry.setValue(auto.clone(true).as());
        }
      }
    }
  }

  /**
   * Constructor that wraps an existing object.
   */
  protected AbstractAutoBean(T wrapped) {
    usingSimplePeer = false;
    values = null;
    this.wrapped = wrapped;

    // Used by AutoBeanUtils
    WeakMapping.set(wrapped, AutoBean.class.getName(), this);
  }

  public void accept(AutoBeanVisitor visitor) {
    traverse(visitor, new OneShotContext());
  }

  public abstract T as();

  public abstract AutoBean<T> clone(boolean deep);

  @SuppressWarnings("unchecked")
  public <Q> Q getTag(String tagName) {
    return tags == null ? null : (Q) tags.get(tagName);
  }

  public boolean isFrozen() {
    return frozen;
  }

  public boolean isWrapper() {
    return !usingSimplePeer;
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

  protected T getWrapped() {
    if (wrapped == null) {
      assert usingSimplePeer : "checkWrapped should have failed";
      wrapped = createSimplePeer();
    }
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

  protected abstract void traverseProperties(AutoBeanVisitor visitor,
      OneShotContext ctx);
}
