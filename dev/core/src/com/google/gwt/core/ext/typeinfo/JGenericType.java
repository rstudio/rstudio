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
 * 
 */
public class JGenericType extends JRealClassType implements HasTypeParameters {
  private JRawType lazyRawType = null;
  private final List<JTypeParameter> typeParams = new ArrayList<JTypeParameter>();

  public JGenericType(TypeOracle oracle, CompilationUnitProvider cup,
      JPackage declaringPackage, JClassType enclosingType, boolean isLocalType,
      String name, int declStart, int declEnd, int bodyStart, int bodyEnd,
      boolean isInterface) {
    super(oracle, cup, declaringPackage, enclosingType, isLocalType, name,
        declStart, declEnd, bodyStart, bodyEnd, isInterface);
  }

  @Override
  public JClassType getErasedType() {
    return getRawType();
  }

  @Override
  public String getParameterizedQualifiedSourceName() {
    StringBuffer sb = new StringBuffer();
    sb.append(getQualifiedSourceName());

    sb.append('<');
    boolean needComma = false;
    for (JClassType typeParam : typeParams) {
      if (needComma) {
        sb.append(", ");
      } else {
        needComma = true;
      }
      sb.append(typeParam.getParameterizedQualifiedSourceName());
    }
    sb.append('>');
    return sb.toString();
  }

  public JRawType getRawType() {
    if (lazyRawType == null) {
      lazyRawType = new JRawType(this);
    }
    return lazyRawType;
  }

  public JTypeParameter[] getTypeParameters() {
    return typeParams.toArray(new JTypeParameter[typeParams.size()]);
  }

  @Override
  public boolean isDefaultInstantiable() {
    /*
     * By definition, you cannot instantiate a generic type, only a
     * parameterized type or a raw type?
     */
    return false;
  }

  @Override
  public JGenericType isGenericType() {
    return this;
  }

  protected boolean isDefaultInstantiableIfParameterized() {
    return super.isDefaultInstantiable();
  }

  void addTypeParameter(JTypeParameter typeParameter) {
    typeParams.add(typeParameter);
  }
}
