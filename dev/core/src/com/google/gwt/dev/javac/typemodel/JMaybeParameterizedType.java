/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.javac.typemodel;

/**
 * Super class for parameterized types or raw types.
 */
abstract class JMaybeParameterizedType extends JDelegatingClassType {

  public JMaybeParameterizedType() {
    super();
  }

  @Override
  public JGenericType getBaseType() {
    JGenericType genericType = super.getBaseType().isGenericType();
    assert (genericType != null);
    return genericType;
  }

  @Override
  protected JMaybeParameterizedType isMaybeParameterizedType() {
    return this;
  }
}