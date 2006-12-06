/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.core.ext.typeinfo;

public abstract class JType {
  public abstract String getQualifiedSourceName();

  public abstract String getSimpleSourceName();
  
  public String getParameterizedQualifiedSourceName() {
    return  getQualifiedSourceName();
  }

  public abstract JArrayType isArray();

  public abstract JClassType isClass();

  public JClassType isClassOrInterface() {
    JClassType type = isClass();
    if (type != null)
      return type;
    return isInterface();
  }

  public abstract JClassType isInterface();

  public abstract JParameterizedType isParameterized();

  public abstract JPrimitiveType isPrimitive();
  
  public abstract String getJNISignature();
  
  public JType getLeafType() {
    return this;
  }
}
