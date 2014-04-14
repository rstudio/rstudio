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

import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.util.collect.Maps;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Represents a logical package.
 */
public class JPackage implements com.google.gwt.core.ext.typeinfo.JPackage {

  private ImmutableAnnotations annotations = ImmutableAnnotations.EMPTY;

  private final String name;

  private Map<String, JRealClassType> types = Maps.create();

  JPackage(String name) {
    this.name = name;
  }

  @Override
  public JClassType findType(String typeName) {
    String[] parts = typeName.split("\\.");
    return findType(parts);
  }

  @Override
  public JClassType findType(String[] typeName) {
    return findTypeImpl(typeName, 0);
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
  public String getName() {
    return name;
  }

  @Override
  public JClassType getType(String typeName) throws NotFoundException {
    JClassType result = findType(typeName);
    if (result == null) {
      throw new NotFoundException();
    }
    return result;
  }

  @Override
  public JClassType[] getTypes() {
    return types.values().toArray(TypeOracle.NO_JCLASSES);
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return annotations.isAnnotationPresent(annotationClass);
  }

  @Override
  public boolean isDefault() {
    return "".equals(name);
  }

  @Override
  public String toString() {
    return "package " + name;
  }

  void addAnnotations(Map<Class<? extends Annotation>, Annotation> additions) {
    annotations = annotations.plus(additions);
  }

  void addType(JRealClassType type) {
    types = Maps.put(types, type.getSimpleSourceName(), type);
  }

  JClassType findTypeImpl(String[] typeName, int index) {
    JClassType found = types.get(typeName[index]);
    if (found == null) {
      return null;
    } else if (index < typeName.length - 1) {
      return found.findNestedTypeImpl(typeName, index + 1);
    } else {
      return found;
    }
  }
}
