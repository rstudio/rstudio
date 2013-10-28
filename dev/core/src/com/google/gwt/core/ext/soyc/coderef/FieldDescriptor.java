/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.ext.soyc.coderef;

import com.google.gwt.dev.jjs.ast.JField;

/**
 * Represents a field.
 *
 */
public class FieldDescriptor extends MemberDescriptor {

  /**
   * Creates a field descriptor from a JField and set its enclosing class.
   */
  public static FieldDescriptor from(ClassDescriptor classDescriptor, JField jField) {
    FieldDescriptor fieldDescriptor = new FieldDescriptor(classDescriptor, jField.getName(),
        jField.getType().getJsniSignatureName());
    fieldDescriptor.fieldReference = jField;
    return fieldDescriptor;
  }

  private JField fieldReference;

  public FieldDescriptor(ClassDescriptor classDescriptor, String name, String type) {
    super(classDescriptor, name);
    this.type = type;
  }

  public JField getFieldReference() {
    return fieldReference;
  }

  @Override
  public String getJsniSignature() {
    return this.name + ":" + this.type;
  }
}
