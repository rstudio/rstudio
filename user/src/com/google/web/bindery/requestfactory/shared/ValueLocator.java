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

package com.google.web.bindery.requestfactory.shared;

/**
 * A {@link Locator} for use with value types (as opposed to entities), which
 * are not persisted. Abstract methods from the {@link Locator} class that are
 * not relevant for value types are implemented to return {@code null}.
 * 
 * @param <T> the type of domain object the Locator will operate on
 */
public abstract class ValueLocator<T> extends Locator<T, Void> {

  /**
   * Returns {@code null}.
   */
  @Override
  public final Class<T> getDomainType() {
    return null;
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public final Void getId(T domainObject) {
    return null;
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public final Class<Void> getIdType() {
    return null;
  }

  /**
   * Returns {@code null}.
   */
  @Override
  public final Object getVersion(T domainObject) {
    return null;
  }
}
