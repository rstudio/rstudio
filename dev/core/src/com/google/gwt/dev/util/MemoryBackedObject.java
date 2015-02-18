/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.thirdparty.guava.common.base.Preconditions;

import java.io.Serializable;

/**
 * A Noop PersistentBackedObject that stores a reference to the data it's provided. Used only
 * in per file compiles.
 * <p>
 * NOTE: Use this class with care as it does not provide a fresh deserialized copy of the data
 * provided,
 *
 * @param <T> the type of object serialized into the file
 */
public class MemoryBackedObject<T extends Serializable> implements PersistenceBackedObject<T> {

  private T data;
  private final Class<T> clazz;

  public MemoryBackedObject(Class<T> clazz) {
    this.clazz = clazz;
  }

  @Override
  public boolean exists() {
    return data != null;
  }

  @Override
  public String getPath() {
    return null;
  }

  @Override
  public T newInstance(TreeLogger logger) throws UnableToCompleteException {
    T result = data;
    data = null;
    return result;
  }

  @Override
  public void set(TreeLogger logger, T object)
      throws IllegalStateException, UnableToCompleteException {
    assert clazz.isInstance(object);
    Preconditions.checkState(data == null);
    data = object;
  }

  @Override
  public String toString() {
    return "MemoryBackedObject" + "<" + clazz.getName() + ">";
  }
}
