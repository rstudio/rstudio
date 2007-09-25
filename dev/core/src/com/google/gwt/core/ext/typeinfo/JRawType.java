/*
 * Copyright 2007 Google Inc.
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
 * 
 */
public class JRawType extends JDelegatingClassType {

  public JRawType(JGenericType genericType) {
    super.setBaseType(genericType);
  }

  @Override
  public JField findField(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JMethod findMethod(String name, JType[] paramTypes) {
    // TODO Auto-generated method stub
    return null;
  }

  public JGenericType getBaseType() {
    return (JGenericType) baseType;
  }

  @Override
  public JField getField(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JField[] getFields() {
    // TODO Auto-generated method stub
    return null;
  }

  public JGenericType getGenericType() {
    return getBaseType();
  }

  @Override
  public JMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JMethod[] getMethods() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getParameterizedQualifiedSourceName() {
    return getQualifiedSourceName();
  }

  @Override
  public String getQualifiedSourceName() {
    return baseType.getQualifiedSourceName();
  }

  @Override
  public String getSimpleSourceName() {
    return baseType.getSimpleSourceName();
  }

  @Override
  public JGenericType isGenericType() {
    return null;
  }

  @Override
  public JParameterizedType isParameterized() {
    return null;
  }

  @Override
  public JRawType isRawType() {
    return this;
  }

}
