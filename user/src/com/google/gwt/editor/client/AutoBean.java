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
package com.google.gwt.editor.client;

/**
 * A controller for an implementation of a bean interface.
 * 
 * @param <T> the type of interface that will be wrapped.
 */
public interface AutoBean<T> {
  /**
   * Accept an AutoBeanVisitor.
   */
  void accept(AutoBeanVisitor visitor);

  /**
   * Returns a proxy implementation of the <code>T</code> interface which will
   * delegate to the underlying wrapped object, if any.
   */
  T as();

  /**
   * Creates a copy of the AutoBean.
   * <p>
   * If the AutoBean has tags, the tags will be copied into the cloned AutoBean.
   * If any of the tag values are AutoBeans, they will not be cloned, regardless
   * of the value of <code>deep</code>.
   * 
   * @param deep indicates if all referenced AutoBeans should be cloned
   * @throws IllegalStateException if the AutoBean is a wrapper type
   */
  AutoBean<T> clone(boolean deep);

  /**
   * Retrieve a tag value that was previously provided to
   * {@link #setTag(String, Object)}.
   */
  <Q> Q getTag(String tagName);

  /**
   * Returns the value most recently passed to {@link #setFrozen}, or false
   * if it has never been called.
   */
  boolean isFrozen();

  /**
   * Returns <code>true</code> if the AutoBean was provided with an external
   * object.
   */
  boolean isWrapper();

  /**
   * Disallows any method calls other than getters. All setter and call
   * operations will throw an {@link UnsupportedOperationException}.
   */
  void setFrozen(boolean frozen);

  /**
   * A tag is an arbitrary piece of external metadata to be associated with the
   * wrapped value.
   */
  void setTag(String tagName, Object value);

  /**
   * If the AutoBean wraps an object, return the underlying object.
   * 
   * @throws IllegalStateException if the AutoBean is not a wrapper
   */
  T unwrap();
}
