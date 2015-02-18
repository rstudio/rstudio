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

import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collections;
import java.util.Set;

/**
 * Represents a method. Its goal is to keep as minimal information as possible and track
 * dependencies between them. The signature is in jsni format, including the return type and the
 * parameter types.
 *
 */
public class MethodDescriptor extends MemberDescriptor {

  /**
   * Creates a method descriptor from a JMethod with its original signature, and set its enclosing
   * class.
   */
  public static MethodDescriptor from(ClassDescriptor classDescriptor, JMethod method,
      String signature) {
    MethodDescriptor methodDescriptor = new MethodDescriptor(classDescriptor, signature);
    methodDescriptor.methodReferences.add(method);
    return methodDescriptor;
  }

  private final Set<MethodDescriptor> dependentMethods = Sets.newIdentityHashSet();
  private int uniqueId;
  private final String paramTypes;
  private final Set<JMethod> methodReferences = Sets.newIdentityHashSet();

  private MethodDescriptor(ClassDescriptor owner, String[] signatureComponents) {
    super(owner, signatureComponents[0]);
    this.paramTypes = signatureComponents[1];
    // fix for wrong jsni signature for constructors in JMethod.getSignature()
    this.type = normalizeMethodSignature(signatureComponents[2]);
  }

  private static final String CONSTRUCTOR_POSTFIX = " <init>";

  public static String normalizeMethodSignature(String methodSignature) {
    if (methodSignature.endsWith(CONSTRUCTOR_POSTFIX)) {
      return methodSignature.substring(0,
          methodSignature.length() - CONSTRUCTOR_POSTFIX.length()) + "V";
    }
    return methodSignature;
  }

  public MethodDescriptor(ClassDescriptor owner, String jsniSignature) {
    this(owner, jsniSignature.split("\\(|\\)"));
  }

  public void addDependant(MethodDescriptor methodDescriptor) {
    dependentMethods.add(methodDescriptor);
  }

  public void addReference(JMethod methodRef) {
    methodReferences.add(methodRef);
  }

  /**
   * Returns the dependent list ids.
   */
  public int[] getDependentPointers() {
    int[] ps = new int[dependentMethods.size()];
    int c = 0;
    for (MethodDescriptor dependant : dependentMethods) {
      ps[c++] = dependant.getUniqueId();
    }
    return ps;
  }

  public Set<MethodDescriptor> getDependentMethods() {
    return Collections.unmodifiableSet(dependentMethods);
  }

  @Override
  public String getJsniSignature() {
    return name + "(" + paramTypes + ")" + type;
  }

  public String getParamTypes() {
    return paramTypes;
  }

  /**
   * Returns the set of JMethods that share the same signature.
   */
  public Set<JMethod> getMethodReferences() {
    return Collections.unmodifiableSet(methodReferences);
  }

  public int getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(int uniqueId) {
    this.uniqueId = uniqueId;
  }
}