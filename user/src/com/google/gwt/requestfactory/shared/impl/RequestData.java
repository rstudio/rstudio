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
package com.google.gwt.requestfactory.shared.impl;

import java.util.Set;

/**
 * A class that encapsulates the parameters and method name to be invoked on the
 * server.
 */
public class RequestData {
  private final Class<?> elementType;
  private final String operation;
  private final Object[] parameters;
  private final Set<String> propertyRefs;
  private final Class<?> returnType;

  public RequestData(String operation, Object[] parameters,
      Set<String> propertyRefs, Class<?> returnType, Class<?> elementType) {
    this.operation = operation;
    this.parameters = parameters;
    this.propertyRefs = propertyRefs;
    this.returnType = returnType;
    this.elementType = elementType;
  }

  /**
   * Used to interpret the returned payload.
   */
  public Class<?> getElementType() {
    return elementType;
  }

  public String getOperation() {
    return operation;
  }

  /**
   * Used by InstanceRequest subtypes to reset the instance object in the
   * <code>using</code> method.
   */
  public Object[] getParameters() {
    return parameters;
  }

  public Set<String> getPropertyRefs() {
    return propertyRefs;
  }

  /**
   * Used to interpret the returned payload.
   */
  public Class<?> getReturnType() {
    return returnType;
  }
}
