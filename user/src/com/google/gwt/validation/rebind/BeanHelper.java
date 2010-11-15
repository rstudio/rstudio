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

import javax.validation.metadata.BeanDescriptor;

/**
 * A simple struct for the various values associated with a Bean that can be
 * validated.
 */
final class BeanHelper {

  private final BeanDescriptor beanDescriptor;

  private final JClassType jClass;
  public BeanHelper(JClassType jClass,
      BeanDescriptor beanDescriptor) {
    super();
    this.beanDescriptor = beanDescriptor;
    this.jClass = jClass;
  }

  public BeanDescriptor getBeanDescriptor() {
    return beanDescriptor;
  }

  /*
   * The server side validator needs an actual class.
   */
  public Class<?> getClazz() throws ClassNotFoundException {
    return Class.forName(jClass.getQualifiedSourceName());
  }

  public String getDescriptorName() {

    return jClass.getName() + "Descriptor";
  }

  public String getFullyQualifiedValidatorName() {
    return getPackage() + "." + getValidatorName();
  }

  public String getPackage() {
    return jClass.getPackage().getName();
  }

  public String getTypeCanonicalName() {
    return jClass.getQualifiedSourceName();
  }

  public String getValidatorInstanceName() {
    return makeJavaSafe(jClass.getName().toLowerCase() + "Validator");
  }

  public String getValidatorName() {
    return makeJavaSafe(jClass.getName() + "Validator");
  }

  @Override
  public String toString() {
    return getTypeCanonicalName();
  }

  private String makeJavaSafe(String in) {
      return in.replaceAll("\\.", "_");
    }

}
