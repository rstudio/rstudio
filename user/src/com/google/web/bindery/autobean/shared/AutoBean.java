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
package com.google.web.bindery.autobean.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A controller for an implementation of a bean interface. Instances of
 * AutoBeans are obtained from an {@link AutoBeanFactory}.
 * 
 * @param <T> the type of interface that will be wrapped.
 */
public interface AutoBean<T> {
  /**
   * An annotation that allows inferred property names to be overridden.
   * <p>
   * This annotation is asymmetric, applying it to a getter will not affect the
   * setter. The asymmetry allows existing users of an interface to read old
   * {@link AutoBeanCodex} messages, but write new ones.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(value = {ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
  public @interface PropertyName {
    String value();
  }

  /**
   * Accept an AutoBeanVisitor.
   * 
   * @param visitor an {@link AutoBeanVisitor}
   */
  void accept(AutoBeanVisitor visitor);

  /**
   * Returns a proxy implementation of the <code>T</code> interface which will
   * delegate to the underlying wrapped object, if any.
   * 
   * @return a proxy that delegates to the wrapped object
   */
  T as();

  /**
   * This method always throws an {@link UnsupportedOperationException}. The
   * implementation of this method in previous releases was not sufficiently
   * robust and there are no further uses of this method within the GWT code
   * base. Furthermore, there are many different semantics that can be applied
   * to a cloning process that cannot be adequately addressed with a single
   * implementation.
   * <p>
   * A simple clone of an acyclic datastructure can be created by using
   * {@link AutoBeanCodex} to encode and decode the root object. Other cloning
   * algorithms are best implemented by using an {@link AutoBeanVisitor}.
   * 
   * @throws UnsupportedOperationException
   * @deprecated with no replacement
   */
  @Deprecated
  AutoBean<T> clone(boolean deep);

  /**
   * Returns the AutoBeanFactory that created the AutoBean.
   * 
   * @return an AutoBeanFactory
   */
  AutoBeanFactory getFactory();

  /**
   * Retrieve a tag value that was previously provided to
   * {@link #setTag(String, Object)}.
   * 
   * @param tagName the tag name
   * @return the tag value
   * @see #setTag(String, Object)
   */
  <Q> Q getTag(String tagName);

  /**
   * Returns the wrapped interface type.
   */
  Class<T> getType();

  /**
   * Returns the value most recently passed to {@link #setFrozen}, or
   * {@code false} if it has never been called.
   * 
   * @return {@code true} if this instance is frozen
   */
  boolean isFrozen();

  /**
   * Returns {@code true} if the AutoBean was provided with an external object.
   * 
   * @return {@code true} if this instance is a wrapper
   */
  boolean isWrapper();

  /**
   * Disallows any method calls other than getters. All setter and call
   * operations will throw an {@link IllegalStateException}.
   * 
   * @param frozen if {@code true}, freeze this instance
   */
  void setFrozen(boolean frozen);

  /**
   * A tag is an arbitrary piece of external metadata to be associated with the
   * wrapped value.
   * 
   * @param tagName the tag name
   * @param value the wrapped value
   * @see #getTag(String)
   */
  void setTag(String tagName, Object value);

  /**
   * If the AutoBean wraps an object, return the underlying object. The AutoBean
   * will no longer function once unwrapped.
   * 
   * @return the previously-wrapped object
   * @throws IllegalStateException if the AutoBean is not a wrapper
   */
  T unwrap();
}
