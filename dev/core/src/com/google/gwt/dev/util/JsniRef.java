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
package com.google.gwt.dev.util;

import com.google.gwt.core.ext.typeinfo.JniConstants;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parsed Java reference from within a JSNI method.
 */
public class JsniRef {

  /**
   * A regex pattern for a Java reference in JSNI code. Its groups are:
   * <ol>
   * <li> the class name
   * <li> the field or method name
   * <li> the method parameter types, including the surrounding parentheses
   * <li> the method parameter types, excluding the parentheses
   * </ol>
   */
  private static Pattern JsniRefPattern = Pattern.compile("@?([^:]+)::([^(]+)(\\((.*)\\))?");

  /**
   * Parse a Java reference from JSNI code. This parser is forgiving; it does
   * not always detect invalid references. If the refString is improperly
   * formatted, returns null.
   */
  public static JsniRef parse(String refString) {
    Matcher matcher = JsniRefPattern.matcher(refString);
    if (!matcher.matches()) {
      return null;
    }

    String className = matcher.group(1);
    String memberName = matcher.group(2);
    String paramTypesString = null;
    String[] paramTypes = null;
    if (matcher.group(3) != null) {
      paramTypesString = matcher.group(4);
      paramTypes = computeParamTypes(paramTypesString);
      if (paramTypes == null) {
        return null;
      }
    }
    return new JsniRef(className, memberName, paramTypesString, paramTypes);
  }

  private static String[] computeParamTypes(String paramTypesString) {
    ArrayList<String> types = new ArrayList<String>();
    StringBuilder nextType = new StringBuilder();
    boolean inRef = false;
    for (char c : paramTypesString.toCharArray()) {
      nextType.append(c);
      if (inRef) {
        if (c == JniConstants.DESC_REF_END) {
          types.add(nextType.toString());
          nextType.setLength(0);
          inRef = false;
        }
      } else {
        switch (c) {
          case JniConstants.DESC_BOOLEAN:
          case JniConstants.DESC_BYTE:
          case JniConstants.DESC_CHAR:
          case JniConstants.DESC_DOUBLE:
          case JniConstants.DESC_FLOAT:
          case JniConstants.DESC_INT:
          case JniConstants.DESC_LONG:
          case JniConstants.DESC_SHORT:
          case JniConstants.DESC_VOID:
            types.add(nextType.toString());
            nextType.setLength(0);
            break;

          case JniConstants.DESC_ARRAY:
            // Nothing special to do.
            break;

          case JniConstants.DESC_REF:
            inRef = true;
            break;

          default:
            // Bad input.
            return null;
        }
      }
    }

    return types.toArray(Empty.STRINGS);
  }

  private final String className;
  private final String memberName;
  private final String[] paramTypes;
  private final String paramTypesString;

  private JsniRef(String className, String memberName, String paramTypesString,
      String[] paramTypes) {
    this.className = className;
    this.memberName = memberName;
    this.paramTypesString = paramTypesString;
    this.paramTypes = paramTypes;
  }

  public String className() {
    return className;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof JsniRef) {
      return toString().equals(obj.toString());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  public final boolean isMethod() {
    return paramTypesString() != null;
  }

  public String memberName() {
    return memberName;
  }

  /**
   * Return the list of parameter types for the method referred to by this
   * reference.
   */
  public String[] paramTypes() {
    return paramTypes;
  }

  public String paramTypesString() {
    return paramTypesString;
  }

  @Override
  public String toString() {
    String ret = "@" + className + "::" + memberName;
    if (isMethod()) {
      ret += "(" + paramTypesString + ")";
    }
    return ret;
  }
}
