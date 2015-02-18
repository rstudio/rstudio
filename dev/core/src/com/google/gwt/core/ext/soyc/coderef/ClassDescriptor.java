/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.ext.soyc.coderef;

import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.util.Util;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * A succinct code representation for classes.
 *
 */
public class ClassDescriptor extends EntityDescriptor {

  /**
   * Creates a class descriptor from a JDeclaredType.
   */
  public static ClassDescriptor from(JDeclaredType classType) {
    return new ClassDescriptor(Util.getClassName(classType.getName()),
                               Util.getPackageName(classType.getName()),
                               classType);
  }

  private final String packageName;
  private final Map<String, MethodDescriptor> methodsByJsniSignature = Maps.newTreeMap();
  private final Map<String, FieldDescriptor> fieldsByName = Maps.newTreeMap();
  private final JDeclaredType typeReference;

  /* TODO(ocallau) for storing hierarchy information, but supporting this has been delayed
  protected int id;
  protected ClassDescriptor superClass;
  protected Collection<ClassDescriptor> interfaces;*/

  private ClassDescriptor(String className, String packageName, JDeclaredType classType) {
    super(className);
    this.packageName = packageName;
    typeReference = classType;
  }

  public ClassDescriptor(String className, String packageName) {
    this(className, packageName, null);
  }

  public void addField(FieldDescriptor fieldDescriptor) {
    this.fieldsByName.put(fieldDescriptor.getName(), fieldDescriptor);
  }

  public void addMethod(MethodDescriptor methodDescriptor) {
    this.methodsByJsniSignature.put(methodDescriptor.getJsniSignature(), methodDescriptor);
  }

  /**
   * Returns a field descriptor from a JField. If the field descriptor is not in the current class
   * descriptor, it will be added.
   */
  public FieldDescriptor fieldFrom(JField field) {
    FieldDescriptor descriptor = this.fieldsByName.get(field.getName());
    if (descriptor == null) {
      descriptor = FieldDescriptor.from(this, field);
      fieldsByName.put(field.getName(), descriptor);
    }
    return descriptor;
  }

  /**
   * Returns the field descriptor associated to the given field name.
   */
  public FieldDescriptor getField(String fieldName) {
    return fieldsByName.get(fieldName);
  }

  public Collection<FieldDescriptor> getFields() {
    return Collections.unmodifiableCollection(fieldsByName.values());
  }

  @Override
  public String getFullName() {
    if (packageName.isEmpty()) {
      return name;
    }
    return packageName + "." + name;
  }

  /**
   * Returns the method descriptor associated to the given original method signature.
   */
  public MethodDescriptor getMethod(String methodSignature) {
    methodSignature = MethodDescriptor.normalizeMethodSignature(methodSignature);
    return methodsByJsniSignature.get(methodSignature);
  }

  public Collection<MethodDescriptor> getMethods() {
    return Collections.unmodifiableCollection(methodsByJsniSignature.values());
  }

  public String getPackageName() {
    return packageName;
  }

  public JDeclaredType getTypeReference() {
    return typeReference;
  }

  /**
   * Returns a method descriptor from a JMethod and its original signature (prior any modifications).
   * If the method descriptor is not in the current class descriptor, it will be added.
   */
  public MethodDescriptor methodFrom(JMethod method, String signature) {
    signature = MethodDescriptor.normalizeMethodSignature(signature);
    MethodDescriptor methodDescriptor = this.methodsByJsniSignature.get(signature);
    if (methodDescriptor == null) {
      methodDescriptor = MethodDescriptor.from(this, method, signature);
      methodsByJsniSignature.put(signature, methodDescriptor);
    } else {
      methodDescriptor.addReference(method);
    }
    return methodDescriptor;
  }
}
