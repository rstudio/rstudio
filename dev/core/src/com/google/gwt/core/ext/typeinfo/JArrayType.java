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

/**
 * Type representing a java array.
 */
public class JArrayType extends JType {

  private JType componentType;

  private String lazyQualifiedName;

  private String lazySimpleName;

  JArrayType(JType componentType) {
    this.componentType = componentType;
  }

  public JType getComponentType() {
    return componentType;
  }

  public String getJNISignature() {
    return "[" + componentType.getJNISignature();
  }

  public JType getLeafType() {
    return componentType.getLeafType();
  }

  public String getQualifiedSourceName() {
    if (lazyQualifiedName == null) {
      lazyQualifiedName = getComponentType().getQualifiedSourceName() + "[]";
    }
    return lazyQualifiedName;
  }

  public int getRank() {
    JArrayType componentArrayType = componentType.isArray();
    if (componentArrayType != null) {
      return 1 + componentArrayType.getRank(); 
    }
    
    return 1;
  }

  public String getSimpleSourceName() {
    if (lazySimpleName == null) {
      lazySimpleName = getComponentType().getSimpleSourceName() + "[]";
    }
    return lazySimpleName;
  }
 
  public JArrayType isArray() {
    return this;
  }
  
  public JClassType isClass() {
    // intentional null
    return null;
  }

  public JClassType isInterface() {
    // intentional null
    return null;
  }
  
  public JParameterizedType isParameterized() {
    // intentional null
    return null;
  }
  
  public JPrimitiveType isPrimitive() {
    // intentional null
    return null;
  }
  public void setLeafType(JType type) {
    JArrayType componentTypeIsArray = componentType.isArray();
    if (componentTypeIsArray != null) {
      componentTypeIsArray.setLeafType(type);
    } else {
      componentType = type;
    }
  }
  public String toString() {
    return getQualifiedSourceName();
  }
}
