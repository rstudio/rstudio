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
package com.google.gwt.sample.expenses.gen;

import java.util.Map;

/**
 * An utitlity class to manage the encoding and decoding of parameters.
 *
 * TODO: add appropriate unit tests.
 */
public class UrlParameterManager {

  private static final String TOKEN = "param";

  public static Object[] getObjectsFromFragment(
      Map<String, String[]> parameterMap, Class<?> parameterClasses[]) {
    assert parameterClasses != null;
    Object args[] = new Object[parameterClasses.length];
    for (int i = 0; i < parameterClasses.length; i++) {
      args[i] = encodeParameterValue(parameterClasses[i].getName(),
          parameterMap.get("param" + i));
    }
    return args;
  }

  /**
   * Returns the string that encodes the values. The string has a leading &.
   * 
   * @param values
   * @return
   */
  public static String getUrlFragment(Object values[]) {
    assert values != null;
    StringBuffer fragment = new StringBuffer();
    for (int i = 0; i < values.length; i++) {
      Object value = values[i];
      fragment.append("&");
      fragment.append(TOKEN);
      fragment.append(i);
      fragment.append("=");
      fragment.append(value.toString());
    }
    return fragment.toString();
  }

  /**
   * Encodes parameter value.
   * 
   */
  private static Object encodeParameterValue(String parameterType,
      String parameterValues[]) {
    assert parameterValues != null;
    assert parameterValues.length == 1;
    String parameterValue = parameterValues[0];
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
