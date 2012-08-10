/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.validation.client.impl;


import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Does shallow group-specific validation. Group sequences and Default group overriding are not
 * directly supported by implementations of this interface. Instead, this is used by higher-level
 * validators to delegate the validation of specific areas.
 * 
 */
public interface GroupValidator {

  /**
   * Validates the given group(s) (may not include group sequences)
   * and adds any violations to the set.
   */
  <T> void validateGroups(GwtValidationContext<T> context, //
      Set<ConstraintViolation<T>> violations, Group... groups);
}
