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

package org.hibernate.validator.engine;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashSet;

/**
 * Exposes Hibernate Validator Implementation Classes so they can be serialized.
 * <p>
 * Create a dummy method like the following in your RemoteService
 *
 * <pre>
 * org.hibernate.validator.engine.ValidationSupport dummy();
 * </pre>
 *
 * The following classes are included.
 * <ul>
 * <li>{@link ConstraintViolationImpl}</li>
 * <li>{@link PathImpl}</li>
 * <li>{@link HashSet}</li>
 * </ul>
 *
 */
public class ValidationSupport implements IsSerializable {

  @SuppressWarnings("unused")
  private ConstraintViolationImpl<?> constraintViolationImpl;

  @SuppressWarnings("unused")
  private PathImpl pathIpml;

  @SuppressWarnings("unused")
  private HashSet<?> hashSet;
}
