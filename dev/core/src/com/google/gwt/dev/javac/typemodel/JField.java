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
package com.google.gwt.dev.javac.typemodel;

import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.util.StringInterner;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Represents a field declaration.
 */
public class JField implements com.google.gwt.core.ext.typeinfo.JField {

  private final ImmutableAnnotations annotations;

  private final JClassType enclosingType;

  private int modifierBits;

  private final String name;

  private JType type;

  JField(JClassType enclosingType, JField srcField) {
    this.annotations = srcField.annotations;
    this.enclosingType = enclosingType;
    this.modifierBits = srcField.modifierBits;
    this.name = srcField.name;
    this.type = srcField.type;
  }

  JField(JClassType enclosingType, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    assert (enclosingType != null);
    this.enclosingType = enclosingType;
    this.name = StringInterner.get().intern(name);
    this.enclosingType.addField(this);
    annotations = ImmutableAnnotations.EMPTY.plus(declaredAnnotations);
  }

  @Override
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return annotations.getAnnotation(annotationClass);
  }

  @Override
  public Annotation[] getAnnotations() {
    return annotations.getAnnotations();
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return annotations.getDeclaredAnnotations();
  }

  @Override
  public JClassType getEnclosingType() {
    return enclosingType;
  }

  @Override
  @Deprecated
  public final String[][] getMetaData(String tagName) {
    return TypeOracle.NO_STRING_ARR_ARR;
  }

  @Override
  @Deprecated
  public final String[] getMetaDataTags() {
    return TypeOracle.NO_STRINGS;
  }

  @Override
  public String getName() {
    assert (name != null);
    return name;
  }

  @Override
  public JType getType() {
    assert (type != null);
    return type;
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return annotations.isAnnotationPresent(annotationClass);
  }

  @Override
  public boolean isDefaultAccess() {
    return 0 == (modifierBits & (TypeOracle.MOD_PUBLIC | TypeOracle.MOD_PRIVATE | TypeOracle.MOD_PROTECTED));
  }

  @Override
  public JEnumConstant isEnumConstant() {
    return null;
  }

  @Override
  public boolean isFinal() {
    return 0 != (modifierBits & TypeOracle.MOD_FINAL);
  }

  @Override
  public boolean isPrivate() {
    return 0 != (modifierBits & TypeOracle.MOD_PRIVATE);
  }

  @Override
  public boolean isProtected() {
    return 0 != (modifierBits & TypeOracle.MOD_PROTECTED);
  }

  @Override
  public boolean isPublic() {
    return 0 != (modifierBits & TypeOracle.MOD_PUBLIC);
  }

  @Override
  public boolean isStatic() {
    return 0 != (modifierBits & TypeOracle.MOD_STATIC);
  }

  @Override
  public boolean isTransient() {
    return 0 != (modifierBits & TypeOracle.MOD_TRANSIENT);
  }

  @Override
  public boolean isVolatile() {
    return 0 != (modifierBits & TypeOracle.MOD_VOLATILE);
  }

  @Override
  public String toString() {
    String[] names = TypeOracle.modifierBitsToNamesForField(modifierBits);
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
    sb.append(type.getParameterizedQualifiedSourceName());
    sb.append(" ");
    sb.append(getName());
    return sb.toString();
  }

  void addModifierBits(int modifierBits) {
    this.modifierBits |= modifierBits;
  }

  void setType(JType type) {
    this.type = type;
  }
}
