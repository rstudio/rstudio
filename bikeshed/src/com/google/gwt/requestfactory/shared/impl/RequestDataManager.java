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

import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory.ServerSideOperation;

import java.util.HashMap;
import java.util.Map;

/**
 * An utility class to manage the encoding and decoding of parameters and
 * methodNames.
 * 
 * TODO: add appropriate unit tests.
 */
public class RequestDataManager {

  public static final String CONTENT_TOKEN = "contentData";
  public static final String METHOD_TOKEN = "methodName";
  public static final String OPERATION_TOKEN = "operation";
  public static final String PARAM_TOKEN = "param";

  public static Object[] getObjectsFromParameterMap(
      Map<String, String> parameterMap, Class<?> parameterClasses[]) {
    assert parameterClasses != null;
    Object args[] = new Object[parameterClasses.length];
    for (int i = 0; i < parameterClasses.length; i++) {
      args[i] = encodeParameterValue(parameterClasses[i].getName(),
          parameterMap.get("param" + i));
    }
    return args;
  }

  /**
   * Returns the string that encodes the request data.
   * 
   */
  public static Map<String, String> getRequestMap(
      ServerSideOperation operation, Object values[], String content) {
    Map<String, String> requestMap = new HashMap<String, String>();
    requestMap.put(OPERATION_TOKEN, operation.name());
    if (values != null) {
      for (int i = 0; i < values.length; i++) {
        Object value = values[i];
        requestMap.put(PARAM_TOKEN + i, value.toString());
      }
    }
    if (content != null) {
      requestMap.put(CONTENT_TOKEN, content);
    }
    return requestMap;
  }

  /**
   * Encodes parameter value.
   * 
   */
  private static Object encodeParameterValue(String parameterType,
      String parameterValue) {
    if ("java.lang.String".equals(parameterType)) {
      return parameterValue;
    }
    if ("java.lang.Integer".equals(parameterType)
        || "int".equals(parameterType)) {
      return new Integer(parameterValue);
    }
    if ("java.lang.Long".equals(parameterType) || "long".equals(parameterType)) {
      return new Long(parameterValue);
    }
    throw new IllegalArgumentException("Unknown parameter type: "
        + parameterType);
  }
}
