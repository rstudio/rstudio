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
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parsed Java reference from within a JSNI method.
 */
public class JsniRef {
  /**
   * Special field name for referring to a class literal.
   */
  public static final String CLASS = "class";

  /**
   * Special method name for a class constructor.
   */
  public static final String NEW = "new";

  /**
   * A parameter list indicating a match to any overload.
   */
  public static final String WILDCARD_PARAM_LIST = "*";

  /**
   * A regex pattern for a Java reference in JSNI code. Its groups are:
   * <ol>
   * <li>the class name
   * <li>the field or method name
   * <li>the method parameter types, including the surrounding parentheses
   * <li>the method parameter types, excluding the parentheses
   * </ol>
   */
  private static Pattern JsniRefPattern = Pattern.compile("@?([^:@\\[\\]]*)((?:\\[\\])*)::([^(]+)(\\((.*)\\))?");

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
    int arrayDimensions = matcher.group(2).length() / 2;
    String memberName = matcher.group(3);
    String paramTypesString = null;
    String[] paramTypes = null;
    if (matcher.group(4) != null) {
      paramTypesString = matcher.group(5);
      if (!paramTypesString.equals(WILDCARD_PARAM_LIST)) {
        paramTypes = computeParamTypes(paramTypesString);
        if (paramTypes == null) {
          return null;
        }
      }
    }
    return new JsniRef(className, arrayDimensions, memberName, paramTypesString, paramTypes);
  }

  private static String[] computeParamTypes(String paramTypesString) {
    List<String> types = Lists.newArrayList();
    StringBuilder nextType = new StringBuilder();
    boolean inRef = false;
    for (char c : paramTypesString.toCharArray()) {
      nextType.append(c);
      if (inRef) {
        if (c == JniConstants.DESC_REF_END) {
          types.add(StringInterner.get().intern(nextType.toString()));
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
            types.add(StringInterner.get().intern(nextType.toString()));
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
  private String resolvedClassName;
  private String resolvedNemberSignature;
  private final String memberName;
  private final String[] paramTypes;
  private final String paramTypesString;
  private final int arrayDimensions;

  protected JsniRef(String className, int arrayDimensions, String memberName,
      String paramTypesString, String[] paramTypes) {
    this.className = className;
    this.memberName = memberName;
    this.arrayDimensions = arrayDimensions;
    this.paramTypesString = paramTypesString;
    this.paramTypes = paramTypes;
  }

  public String className() {
    return className;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof JsniRef) && toString().equals(obj.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  public boolean isField() {
    return paramTypesString == null;
  }

  public boolean isMethod() {
    return paramTypesString != null;
  }

  /**
   * Whether this method reference matches all overloads of the specified class
   * and method name. Only valid for method references.
   */
  public boolean matchesAnyOverload() {
    return paramTypesString.equals(WILDCARD_PARAM_LIST);
  }

  public String memberName() {
    return memberName;
  }

  public String memberSignature() {
    String ret = memberName;
    if (isMethod()) {
      ret += "(" + paramTypesString + ")";
    }
    return ret;
  }

  /**
   * Return the list of parameter types for the method referred to by this
   * reference. Only valid for method references where
   * {@link #matchesAnyOverload()} is false.
   */
  public String[] paramTypes() {
    assert !matchesAnyOverload();
    return paramTypes;
  }

  public String paramTypesString() {
    return paramTypesString;
  }

  public void setResolvedClassName(String resolvedClassName) {
    this.resolvedClassName = StringInterner.get().intern(resolvedClassName);
  }

  public void setResolvedMemberWithSignature(String resolvedMemberSignature) {
    this.resolvedNemberSignature = StringInterner.get().intern(resolvedMemberSignature);
  }

  public String getResolvedClassName() {
    return resolvedClassName;
  }

  public String getFullResolvedClassName() {
    return resolvedClassName == null ? null :
        resolvedClassName + Strings.repeat("[]", arrayDimensions);
  }

  public String getResolvedReference() {
    String fullResolvedClassName = getFullResolvedClassName();
    return fullResolvedClassName == null || resolvedClassName == null ? null :
        "@" + fullResolvedClassName + "::" + resolvedNemberSignature;
  }

  public String getResolvedMemberSignature() {
    return resolvedNemberSignature;
  }

  public String fullClassName() {
    return className + Strings.repeat("[]", arrayDimensions);
  }

  @Override
  public String toString() {
    return "@" + fullClassName() + "::" + memberSignature();
  }

  public boolean isArray() {
    return arrayDimensions > 0;
  }

  public int getDimensions() {
    return arrayDimensions;
  }
}
