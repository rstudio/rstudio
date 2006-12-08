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

/**
 * Represents a field declaration.
 */
public class JField implements HasMetaData {

  private final JClassType enclosingType;

  private final HasMetaData metaData = new MetaData();

  private int modifierBits;

  private final String name;

  private JType type;

  public JField(JClassType enclosingType, String name) {
    this.enclosingType = enclosingType;
    this.name = name;

    assert (enclosingType != null);
    enclosingType.addField(this);
  }

  public void addMetaData(String tagName, String[] values) {
    metaData.addMetaData(tagName, values);
  }

  public void addModifierBits(int modifierBits) {
    this.modifierBits |= modifierBits;
  }

  public JClassType getEnclosingType() {
    return enclosingType;
  }

  public String[][] getMetaData(String tagName) {
    return metaData.getMetaData(tagName);
  }

  public String[] getMetaDataTags() {
    return metaData.getMetaDataTags();
  }

  public String getName() {
    assert (name != null);
    return name;
  }

  public JType getType() {
    assert (type != null);
    return type;
  }

  public boolean isDefaultAccess() {
    return 0 == (modifierBits & (TypeOracle.MOD_PUBLIC | TypeOracle.MOD_PRIVATE | TypeOracle.MOD_PROTECTED));
  }

  public boolean isFinal() {
    return 0 != (modifierBits & TypeOracle.MOD_FINAL);
  }

  public boolean isPrivate() {
    return 0 != (modifierBits & TypeOracle.MOD_PRIVATE);
  }

  public boolean isProtected() {
    return 0 != (modifierBits & TypeOracle.MOD_PROTECTED);
  }

  public boolean isPublic() {
    return 0 != (modifierBits & TypeOracle.MOD_PUBLIC);
  }

  public boolean isStatic() {
    return 0 != (modifierBits & TypeOracle.MOD_STATIC);
  }

  public boolean isTransient() {
    return 0 != (modifierBits & TypeOracle.MOD_TRANSIENT);
  }

  public boolean isVolatile() {
    return 0 != (modifierBits & TypeOracle.MOD_VOLATILE);
  }

  public void setType(JType type) {
    this.type = type;
  }

  public String toString() {
    String[] names = TypeOracle.modifierBitsToNames(modifierBits);
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < names.length; i++) {
      if (i > 0) {
        sb.append(" ");
      }
      sb.append(names[i]);
    }
    if (names.length > 0) {
      sb.append(" ");
    }
    sb.append(type.getQualifiedSourceName());
    sb.append(" ");
    sb.append(getName());
    return sb.toString();
  }
}
