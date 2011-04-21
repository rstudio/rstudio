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
package com.google.web.bindery.requestfactory.shared;

/**
 * A lightweight representation of a
 * {@link javax.validation.ConstraintViolation}.
 * 
 * @deprecated users should upgrade to the full
 *             {@link javax.validation.ConstraintViolation} type by switching
 *             their {@link Receiver} implementations to use
 *             {@link Receiver#onConstraintViolation(java.util.Set)} instead of
 *             {@link Receiver#onViolation(java.util.Set)}.
 */
@Deprecated
public interface Violation {
  /**
   * If the ConstraintViolation occurred while validating a object, this method
   * will return a BaseProxy that contains the invalid values.
   * 
   * @return the BaseProxy that caused the ConstraintViolation
   */
  BaseProxy getInvalidProxy();

  /**
   * Returns the message associated with this {@link Violation}.
   * 
   * @return a String message
   */
  String getMessage();

  /**
   * If the ConstraintViolation occurred while validating a value object that
   * originated from the server, this method will return a BaseProxy that
   * contains the original values.
   * 
   * @return the BaseProxy originally sent by the server or {@code null} if the
   *         BaseProxy was created on the client.
   */
  BaseProxy getOriginalProxy();

  /**
   * Returns the path associated with this {@link Violation}.
   * 
   * @return a String path
   */
  String getPath();

  /**
   * Returns the proxy id associated with this {@link Violation} if the object
   * associated with the violation is an {@link EntityProxy}.
   * 
   * @return an {@link EntityProxyId} instance or {@code null} if the object is
   *         a ValueProxy.
   */
  EntityProxyId<?> getProxyId();
}
