/*
 * Copyright 2016 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Replaces direct calls to native methods that override methods from java.lang.Object to directly
 * call them through java.lang.Object. This makes sure that the calls are routed through the
 * trampoline.
 */
public class ReplaceCallsToNativeJavaLangObjectOverrides {

  public static void exec(final JProgram program) {
    final Set<JMethod> overridableJavaLangObjectMethods = ImmutableSet.of(
        program.getIndexedMethodOrNull(RuntimeConstants.OBJECT_EQUALS),
        program.getIndexedMethodOrNull(RuntimeConstants.OBJECT_HASHCODE),
        program.getIndexedMethodOrNull(RuntimeConstants.OBJECT_TO_STRING));
    new JModVisitor() {
      @Override
      public void endVisit(JMethodCall x, Context ctx) {
        JMethod targetMethod = x.getTarget();
        if (!targetMethod.isJsNative()) {
          return;
        }

        JMethod overridenMethod = Iterables.getOnlyElement(
            Sets.intersection(targetMethod.getOverriddenMethods(),
                overridableJavaLangObjectMethods),
            null);
        if (overridenMethod == null) {
          return;
        }
        ctx.replaceMe(
            new JMethodCall(x.getSourceInfo(), x.getInstance(), overridenMethod, x.getArgs()));
      }
    }.accept(program);
  }

}
