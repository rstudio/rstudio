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

import com.google.gwt.event.shared.HandlerRegistration;

import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * Magic hookup between the Editor and the backing service. The code that
 * creates an EditorDelegate is responsible for calling
 * {@link ValueAwareEditor#setDelegate}.
 * 
 * @param <T> the type of object the delegate can accept
 */
public interface EditorDelegate<T> {
  T ensureMutable(T object);

  /**
   * Returns the Editor's path, relative to the root object.
   */
  String getPath();

  /**
   * Register for notifications if object being edited is updated. The
   * notification will occur via {@link ValueAwareEditor#onPropertyChange} if
   * the backend supports in-place property updates, otherwise updates will be
   * passed via {@link ValueAwareEditor#setValue}.
   * 
   * @return a HandlerRegistration to unsubscribe from the notifications or
   *         <code>null</code> if the EditorDelegate does not support
   *         notifications
   */
  HandlerRegistration subscribe();

  Set<ConstraintViolation<T>> validate(T object);
}
