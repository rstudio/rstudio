/*
 * Copyright 2006 Google Inc.
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
import java.util.Iterator;
import java.util.List;

/**
 * Common superclass for {@link JMethod} and {@link JConstructor}.
 */
public abstract class JAbstractMethod implements HasMetaData {

  private int bodyEnd;

  private int bodyStart;

  private final int declEnd;

  private final int declStart;

  private final HasMetaData metaData = new MetaData();

  private int modifierBits;

  private final String name;

  private final List params = new ArrayList();

  private final List thrownTypes = new ArrayList();

  // Only the builder can construct
  JAbstractMethod(String name, int declStart, int declEnd, int bodyStart,
      int bodyEnd) {
    this.name = name;
    this.declStart = declStart;
    this.declEnd = declEnd;
    this.bodyStart = bodyStart;
    this.bodyEnd = bodyEnd;
  }

  public void addMetaData(String tagName, String[] values) {
    metaData.addMetaData(tagName, values);
  }

  public void addModifierBits(int bits) {
    modifierBits |= bits;
  }

  public void addThrows(JType type) {
    thrownTypes.add(type);
  }

  public JParameter findParameter(String name) {
    for (Iterator iter = params.iterator(); iter.hasNext();) {
      JParameter param = (JParameter) iter.next();
      if (param.getName().equals(name)) {
        return param;
      }
    }
    return null;
  }

  public int getBodyEnd() {
    return bodyEnd;
  }

  public int getBodyStart() {
    return bodyStart;
  }

  public int getDeclEnd() {
    return declEnd;
  }

  public int getDeclStart() {
    return declStart;
  }

  /**
   * Gets the type in which this method or constructor was declared.
   */
  public abstract JClassType getEnclosingType();

  public String[][] getMetaData(String tagName) {
    return metaData.getMetaData(tagName);
  }

  public String[] getMetaDataTags() {
    return metaData.getMetaDataTags();
  }

  public String getName() {
    return name;
  }

  public JParameter[] getParameters() {
    return (JParameter[]) params.toArray(TypeOracle.NO_JPARAMS);
  }

  public abstract String getReadableDeclaration();

  public JType[] getThrows() {
    return (JType[]) thrownTypes.toArray(TypeOracle.NO_JTYPES);
  }

  public abstract JConstructor isConstructor();

  public boolean isDefaultAccess() {
    return 0 == (modifierBits & (TypeOracle.MOD_PUBLIC | TypeOracle.MOD_PRIVATE | TypeOracle.MOD_PROTECTED));
  }
  public abstract JMethod isMethod();
  public boolean isPrivate() {
    return 0 != (modifierBits & TypeOracle.MOD_PRIVATE);
  }
  public boolean isProtected() {
    return 0 != (modifierBits & TypeOracle.MOD_PROTECTED);
  }
  public boolean isPublic() {
    return 0 != (modifierBits & TypeOracle.MOD_PUBLIC);
  }
  protected int getModifierBits() {
    return modifierBits;
  }
  protected void toStringParamsAndThrows(StringBuffer sb) {
    sb.append("(");
    boolean needComma = false;
    for (Iterator iter = params.iterator(); iter.hasNext();) {
      JParameter param = (JParameter) iter.next();
      if (needComma) {
        sb.append(", ");
      } else {
        needComma = true;
      }
      sb.append(param.getType().getParameterizedQualifiedSourceName());
      sb.append(" ");
      sb.append(param.getName());
    }
    sb.append(")");

    if (!thrownTypes.isEmpty()) {
      sb.append(" throws ");
      needComma = false;
      for (Iterator iter = thrownTypes.iterator(); iter.hasNext();) {
        JClassType thrownType = (JClassType) iter.next();
        if (needComma) {
          sb.append(", ");
        } else {
          needComma = true;
        }
        sb.append(thrownType.getParameterizedQualifiedSourceName());
      }
    }
  }
  void addParameter(JParameter param) {
    params.add(param);
  }
  boolean hasParamTypes(JType[] paramTypes) {
    if (params.size() != paramTypes.length) {
      return false;
    }

    for (int i = 0; i < paramTypes.length; i++) {
      JParameter candidate = (JParameter) params.get(i);
      // Identity tests are ok since identity is durable within an oracle.
      //
      if (candidate.getType() != paramTypes[i]) {
        return false;
      }
    }
    return true;
  }
}
