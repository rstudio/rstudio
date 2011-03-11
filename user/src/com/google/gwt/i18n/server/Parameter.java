/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.i18n.server;

import com.google.gwt.i18n.shared.AlternateMessageSelector;

import java.lang.annotation.Annotation;

/**
 * A parameter in a Messages method. 
 */
public interface Parameter {

  /**
   * Get an {@link AlternateMessageSelector} instance associated with this
   * parameter, or null if none.
   * 
   * @return {@link AlternateMessageSelector} instance or null
   */
  AlternateMessageSelector getAlternateMessageSelector();

  /**
   * Get an annotation from this parameter.
   * 
   * @param <A>
   * @param annotClass
   * @return annotation instance or null if not present
   */
  <A extends Annotation> A getAnnotation(Class<A> annotClass);

  /**
   * Get parameter index, starting from 0.
   * 
   * @return parameter index
   */
  int getIndex();

  /**
   * Get the name of this parameter.  Note that this may be expensive (ie, if
   * based on reflection this will require reading/parsing source), so this
   * should only be called if actually needed.
   *
   * @return parameter name, or "arg" + {@link #getIndex()} if it cannot be
   *     determined
   */
  String getName();

  /**
   * Get the type of this parameter.
   * 
   * @return the parameter type
   */
  Type getType();

  /**
   * Check if an annotation is present on this parameter.
   * 
   * @param annotClass
   * @return true if present, otherwise false
   */
  boolean isAnnotationPresent(Class<? extends Annotation> annotClass);
}