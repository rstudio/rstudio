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
package com.google.web.bindery.requestfactory.shared.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A class that encapsulates the parameters and method name to be invoked on the
 * server.
 */
public class RequestData {
  private final Class<?> elementType;
  private final String operation;
  private final Object[] parameters;
  private Set<String> propertyRefs;
  private final Class<?> returnType;
  private Map<String, Object> requestParameters;
  private Object requestContent;
  private String apiVersion;

  public RequestData(String operation, Object[] parameters,
      Class<?> returnType, Class<?> elementType) {
    this.operation = operation;
    this.parameters = parameters;
    this.returnType = returnType;
    this.elementType = elementType;
  }

  /**
   * Used by generated code.
   */
  public RequestData(String operation, Object[] parameters,
      Set<String> propertyRefs, Class<?> returnType, Class<?> elementType) {
    this(operation, parameters, returnType, elementType);
    setPropertyRefs(propertyRefs);
  }

  public String getApiVersion() {
    return apiVersion;
  }

  /**
   * Used to interpret the returned payload.
   */
  public Class<?> getElementType() {
    return elementType;
  }

  public Map<String, Object> getNamedParameters() {
    return requestParameters == null ? Collections.<String, Object> emptyMap()
        : requestParameters;
  }

  public String getOperation() {
    return operation;
  }

  /**
   * Used by standard-mode payloads and InstanceRequest subtypes to reset the
   * instance object in the <code>using</code> method.
   */
  public Object[] getOrderedParameters() {
    return parameters;
  }

  public Set<String> getPropertyRefs() {
    return propertyRefs;
  }

  public Object getRequestResource() {
    return requestContent;
  }

  /**
   * Used to interpret the returned payload.
   */
  public Class<?> getReturnType() {
    return returnType;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public void setNamedParameter(String key, Object value) {
    if (requestParameters == null) {
      requestParameters = new HashMap<String, Object>();
    }
    requestParameters.put(key, value);
  }

  public void setPropertyRefs(Set<String> propertyRefs) {
    this.propertyRefs = propertyRefs;
  }

  /**
   * Represents the {@code request} object in a JSON-RPC request.
   * 
   * @see com.google.web.bindery.requestfactory.shared.JsonRpcContent
   */
  public void setRequestContent(Object requestContent) {
    this.requestContent = requestContent;
  }
}
