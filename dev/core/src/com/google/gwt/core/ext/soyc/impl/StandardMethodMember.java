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
package com.google.gwt.core.ext.soyc.impl;

import com.google.gwt.core.ext.soyc.ClassMember;
import com.google.gwt.core.ext.soyc.MethodMember;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JType;

/**
 * An implementation of MethodMember.
 */
public class StandardMethodMember extends AbstractMemberWithDependencies implements MethodMember {
  private final ClassMember enclosing;
  private final String sourceName;

  /**
   * Constructed by {@link MemberFactory#get(JMethod)}.
   */
  public StandardMethodMember(MemberFactory factory, JMethod method) {
    this.enclosing = factory.get(method.getEnclosingType());

    StringBuilder sb = new StringBuilder();
    sb.append(method.getEnclosingType().getName()).append("::");
    sb.append(method.getName()).append("(");
    for (JType type : method.getOriginalParamTypes()) {
      sb.append(type.getJsniSignatureName());
    }
    sb.append(")");
    sb.append(method.getOriginalReturnType().getJsniSignatureName());
    this.sourceName = sb.toString();
  }

  @Override
  public ClassMember getEnclosing() {
    return enclosing;
  }

  @Override
  public String getSourceName() {
    return sourceName;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return "MethodMember " + sourceName;
  }
}
