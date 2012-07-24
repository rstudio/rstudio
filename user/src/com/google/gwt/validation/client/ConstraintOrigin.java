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
package com.google.gwt.validation.client;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Visibility looked at when discovering constraints.
 * <p>
 * Exactly the same as the
 * <a href="http://docs.jboss.org/hibernate/validator/4.3/api/org/hibernate/validator/internal/metadata/core/ConstraintOrigin.html">
 * Hibernate implementation</a>.
 */
public enum ConstraintOrigin {
  /**
   * Constraint is defined on the root class
   */
  DEFINED_LOCALLY,
  /**
   * Constraint is defined in a super-class or interface of the root class.
   */
  DEFINED_IN_HIERARCHY
}
