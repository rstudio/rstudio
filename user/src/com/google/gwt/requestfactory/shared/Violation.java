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
package com.google.gwt.requestfactory.shared;

/**
 * A lightweight representation of a
 * {@link javax.validation.ConstraintViolation}.
 */
public interface Violation {
  /**
   * Returns the message associated with this {@link Violation}.
   *
   * @return a String message
   */
  String getMessage();

  /**
   * Returns the path associated with this {@link Violation}.
   *
   * @return a String path
   */
  String getPath();

  /**
   * Returns the proxy id associated with this {@link Violation}.
   *
   * @return an {@link EntityProxyId} instance
   */
  EntityProxyId<?> getProxyId();
}
