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
package com.google.gwt.view.client;

/**
 * Interface for classes that have a {@link ProvidesKey}. Must be implemented by
 * {@link com.google.gwt.cell.client.Cell} containers.
 *
 * @param <T> the data type
 */
public interface HasKeyProvider<T> {

  /**
   * Return the key provider.
   *
   * @return the {@link ProvidesKey} instance
   */
  ProvidesKey<T> getKeyProvider();
}
