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

import com.google.gwt.core.ext.soyc.impl.DependencyRecorder;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.io.OutputStream;
import java.util.Map;

/**
 * Builds the model for the (new) soyc through reading method dependencies. The code model is in a
 * mapping from fully qualified class names (eg. com.google.gwt.MyClass, see
 * {@link com.google.gwt.dev.jjs.ast.JDeclaredType#getName()}) to class descriptors. Where
 * each class descriptor has methods and their dependents.
 *
 */
public class DependencyGraphRecorder extends DependencyRecorder {

  private Map<String, ClassDescriptor> codeModel = Maps.newTreeMap();
  private String currentGraph;
  private int nextUniqueId = 0;
  private JProgram jProgram;

  public DependencyGraphRecorder(OutputStream out, JProgram jProgram) {
    super(out);
    this.jProgram = jProgram;
  }

  protected int nextPointerId() {
    return ++nextUniqueId;
  }

  /**
   * Returns the code model that maps fully qualified class names (eg. com.google.gwt.MyClass, see
   * {@link com.google.gwt.dev.jjs.ast.JDeclaredType#getName()}) to class descriptors.
   */
  public Map<String, ClassDescriptor> getCodeModel() {
    return this.codeModel;
  }

  public void startDependencyGraph(String name, String extendz) {
    super.startDependencyGraph(name, extendz);

    currentGraph = name;
  }

  protected void printMethodDependencyBetween(JMethod curMethod, JMethod depMethod) {
    super.printMethodDependencyBetween(curMethod, depMethod);

    methodDescriptorFrom(curMethod).addDependant(methodDescriptorFrom(depMethod));
  }

  protected String signatureFor(JMethod method) {
    JMethod original = jProgram.staticImplFor(method);
    if (original == null) { //method is the original
      return method.getSignature();
    }
    return original.getSignature();
  }

  public MethodDescriptor methodDescriptorFrom(JMethod method) {
    MethodDescriptor mth = classDescriptorFrom(method.getEnclosingType())
                             .methodFrom(method, signatureFor(method));
    if (!isValid(mth.getUniqueId())) {
      mth.setUniqueId(nextPointerId());
    }
    return mth;
  }

  protected boolean isValid(int n) {
    return n > 0;
  }

  /**
   * Returns a class descriptor from a JDeclaredType. If the class descriptor is not in the code
   * model, it will be added.
   */
  public ClassDescriptor classDescriptorFrom(JDeclaredType classType) {
    // JDeclaredType.getName returns the fully qualified name
    ClassDescriptor classDescriptor = codeModel.get(classType.getName());
    if (classDescriptor == null) {
      classDescriptor = ClassDescriptor.from(classType);
      codeModel.put(classType.getName(), classDescriptor);
    }
    return classDescriptor;
  }
}
