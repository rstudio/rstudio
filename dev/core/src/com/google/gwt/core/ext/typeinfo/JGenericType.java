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
package com.google.gwt.core.ext.typeinfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 
 */
public class JGenericType extends JRealClassType implements HasTypeParameters {
  /**
   * Returns <code>true</code> if lhsType is assignable to rhsType.
   */
  private static boolean isAssignable(JClassType lhsType, JClassType rhsType) {
    if (lhsType == rhsType) {
      return true;
    }

    Set<JClassType> supertypes = getFlattenedSuperTypeHierarchy(rhsType);
    for (JClassType supertype : supertypes) {
      if (supertype.isParameterized() == null) {
        continue;
      }

      if (supertype.isParameterized().getBaseType() == lhsType) {
        return true;
      }
    }

    return false;
  }

  private JRawType lazyRawType = null;

  private final List<JTypeParameter> typeParams = new ArrayList<JTypeParameter>();

  public JGenericType(TypeOracle oracle, CompilationUnitProvider cup,
      JPackage declaringPackage, JClassType enclosingType, boolean isLocalType,
      String name, int declStart, int declEnd, int bodyStart, int bodyEnd,
      boolean isInterface, JTypeParameter[] jtypeParameters) {
    super(oracle, cup, declaringPackage, enclosingType, isLocalType, name,
        declStart, declEnd, bodyStart, bodyEnd, isInterface);
    
     if (jtypeParameters != null) {
      for (JTypeParameter jtypeParameter : jtypeParameters) {
        addTypeParameter(jtypeParameter);            
      }
    }
  }

  @Override
  public JClassType getErasedType() {
    return getRawType();
  }

  @Override
  public String getParameterizedQualifiedSourceName() {
    StringBuffer sb = new StringBuffer();

    if (getEnclosingType() != null) {
      sb.append(getEnclosingType().getParameterizedQualifiedSourceName());
      sb.append(".");
      sb.append(getSimpleSourceName());
    } else {
      sb.append(getQualifiedSourceName());
    }

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
      lazyRawType = new JRawType(this, getEnclosingType());
    }

    return lazyRawType;
  }

  public JTypeParameter[] getTypeParameters() {
    return typeParams.toArray(new JTypeParameter[typeParams.size()]);
  }

  @Override
  public boolean isAssignableFrom(JClassType otherType) {
    otherType = maybeGetGenericBaseType(otherType);

    return isAssignable(this, otherType);
  }

  @Override
  public boolean isAssignableTo(JClassType otherType) {
    otherType = maybeGetGenericBaseType(otherType);

    return isAssignable(otherType, this);
  }

  @Override
  public JGenericType isGenericType() {
    return this;
  }

  @Override
  public String toString() {
    if (isInterface() != null) {
      return "interface " + getParameterizedQualifiedSourceName();
    }

    return "class " + getParameterizedQualifiedSourceName();
  }

  private void addTypeParameter(JTypeParameter typeParameter) {
    typeParams.add(typeParameter);
  }
}
