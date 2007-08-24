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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parameterized type in a declaration.
 */
public class JParameterizedType extends JType {

  private final JClassType parameterized;

  private final List<JType> typeArgs = new ArrayList<JType>();

  JParameterizedType(JClassType parameterized) {
    this.parameterized = parameterized;
  }

  /**
   * The signature of the raw type.
   */
  public String getJNISignature() {
    return getRawType().getJNISignature();
  }

  public JType getLeafType() {
    return parameterized;
  }

  /*
   * Get the name of this type without all of the parameterized information
   */
  public String getNonParameterizedQualifiedSourceName() {
    return parameterized.getQualifiedSourceName();
  }

  @Override
  public String getParameterizedQualifiedSourceName() {
    StringBuffer sb = new StringBuffer();
    sb.append(parameterized.getQualifiedSourceName());
    sb.append('<');
    boolean needComma = false;
    for (JType typeArg : typeArgs) {
      if (needComma) {
        sb.append(", ");
      } else {
        needComma = true;
      }
      sb.append(typeArg.getParameterizedQualifiedSourceName());
    }
    sb.append('>');
    return sb.toString();
  }

  /**
   * Everything is fully qualified and includes the &lt; and &gt; in the
   * signature.
   */
  public String getQualifiedSourceName() {
    return parameterized.getQualifiedSourceName();
  }

  public JClassType getRawType() {
    return parameterized;
  }

  /**
   * In this case, the raw type name.
   */
  public String getSimpleSourceName() {
    return parameterized.getSimpleSourceName();
  }

  public JType[] getTypeArgs() {
    return (JType[]) typeArgs.toArray(TypeOracle.NO_JTYPES);
  }

  public JArrayType isArray() {
    return null;
  }

  public JClassType isClass() {
    return parameterized.isClass();
  }

  public JClassType isInterface() {
    return parameterized.isInterface();
  }

  public JParameterizedType isParameterized() {
    return this;
  }

  public JPrimitiveType isPrimitive() {
    return null;
  }

  void addTypeArg(JType type) {
    assert (type.isPrimitive() == null);
    typeArgs.add(type);
  }
}
