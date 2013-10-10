/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.util;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.io.Serializable;

/**
 * A handle for interacting with a persisted or to be persisted object.
 *
 * @param <T> the type of object being serialized
 */
public interface PersistenceBackedObject<T> extends Serializable {

  /**
   * Returns whether the intended persistence location is already populated. For example if
   * persisting to a file this would be whether or not the file already exists.
   */
  boolean exists();

  /**
   * Returns the path to the intended persistence location.
   */
  String getPath();

  /**
   * Constructs and returns a new instance of the object stored at the path.
   *
   * @throws UnableToCompleteException if the backing store does not contain an object of type
   *           <code>T</code>
   */
  T newInstance(TreeLogger logger) throws UnableToCompleteException;

  /**
   * Serializes the provided object to the intended path. Can only be set once.
   *
   * @throws UnableToCompleteException if the object could not be serialized
   */
  void set(TreeLogger logger, T object) throws IllegalStateException, UnableToCompleteException;
}
