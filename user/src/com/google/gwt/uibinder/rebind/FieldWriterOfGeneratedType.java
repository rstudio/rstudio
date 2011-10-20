/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;

/**
 * Implementation of FieldWriter for fields whose type we haven't genereated
 * yet, e.g. locally defined CssResources.
 */
class FieldWriterOfGeneratedType extends AbstractFieldWriter {

  private final String typePackage;
  private final String typeName;
  private final JClassType assignableType;

  public FieldWriterOfGeneratedType(FieldManager manager, JClassType assignableType, String typePackage,
      String typeName, String name, MortalLogger logger) {
    super(manager, FieldWriterType.GENERATED_BUNDLE, name, logger);
    if (assignableType == null) {
      throw new RuntimeException("assignableType must not be null");
    }
    if (typeName == null) {
      throw new RuntimeException("typeName must not be null");
    }
    if (typePackage == null) {
      throw new RuntimeException("typePackage must not be null");
    }

    this.assignableType = assignableType;
    this.typeName = typeName;
    this.typePackage = typePackage;
  }

  public JClassType getAssignableType() {
    return assignableType;
  }

  public JClassType getInstantiableType() {
    return null;
  }

  public String getQualifiedSourceName() {
    if (typePackage.length() == 0) {
      return typeName;
    }

    return String.format("%s.%s", typePackage, typeName);
  }
}
