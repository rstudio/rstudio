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
package com.google.gwt.validation.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;

/**
 * A simple struct for the various values associated with a Bean that can be
 * validated.
 */
final class BeanHelper {

  private final JClassType jClass;

  public BeanHelper(JClassType jClass) {
    super();
    this.jClass = jClass;
  }

  public String getDescriptorName() {

    return jClass.getName() + "Descriptor";
  }

  public String getTypeCanonicalName() {
    return jClass.getQualifiedSourceName();
  }

  public String getValidatorInstanceName() {
    return jClass.getName().toLowerCase() + "Validator";
  }

  public String getValidatorName() {
    return jClass.getName() + "Validator";
  }

  @Override
  public String toString() {
    return getTypeCanonicalName();
  }
}