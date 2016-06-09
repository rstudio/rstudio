/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Checks and throws errors for invalid JSNI constructs.
 */
public class JsniRestrictionChecker extends AbstractRestrictionChecker {

  public static void exec(TreeLogger logger, JProgram program)
      throws UnableToCompleteException {
    new JsniRestrictionChecker().checkProgram(logger, program);
  }

  private void checkProgram(TreeLogger logger, final JProgram program)
      throws UnableToCompleteException {
    final Set<JDeclaredType> typesRequiringTrampolineDispatch = Sets.newHashSet();
    for (JDeclaredType type : program.getRepresentedAsNativeTypes()) {
      collectAllSuperTypes(type, typesRequiringTrampolineDispatch);
    }
    new JVisitor() {
      @Override
      public boolean visit(JMethodBody x, Context ctx) {
        // Skip non jsni methods.
        return false;
      }

      @Override
      public boolean visit(JsniMethodRef x, Context ctx) {
        checkJsniMethodReference(x);
        return true;
      }

      private void checkJsniMethodReference(JsniMethodRef jsniMethodReference) {
        JMethod method = jsniMethodReference.getTarget();
        JDeclaredType enclosingType = method.getEnclosingType();

        if (isNonStaticJsoClassDispatch(method, enclosingType)) {
          logError(jsniMethodReference,
              "Cannot call non-static method %s on an instance which is a "
                  + "subclass of JavaScriptObject. Only static method calls on JavaScriptObject "
                  + "subclasses are allowed in JSNI.",
              getDescription(method));
        } else if (isJsoInterface(enclosingType)) {
          logError(jsniMethodReference,
              "Cannot call method %s on an instance which might be a JavaScriptObject. "
                  + "Such a method call is only allowed in pure Java (non-JSNI) functions.",
              getDescription(method));
        } else if (program.isRepresentedAsNativeJsPrimitive(enclosingType)
            && !method.isStatic()
            && !method.isConstructor()) {
          logError(jsniMethodReference,
              "Cannot call method %s. Instance methods on %s cannot be called from JSNI.",
              getDescription(method),
              getDescription(enclosingType));
        } else if (typesRequiringTrampolineDispatch.contains(enclosingType)
            && !method.isStatic()
            && !method.isConstructor()) {
          logWarning(jsniMethodReference,
              "Unsafe call to method %s. Instance methods from %s should "
                  + "not be called on Boolean, Double, String, Array or JSO instances from JSNI.",
              getDescription(method),
              getDescription(enclosingType));
        }
      }

      private boolean isJsoInterface(JDeclaredType type) {
        return program.typeOracle.isSingleJsoImpl(type)
            || program.typeOracle.isDualJsoInterface(type);
      }
    }.accept(program);

    boolean hasErrors = reportErrorsAndWarnings(logger);
    if (hasErrors) {
      throw new UnableToCompleteException();
    }
  }

  private static void collectAllSuperTypes(JDeclaredType type, Set<JDeclaredType> allSuperTypes) {
    if (type.getSuperClass() != null) {
      allSuperTypes.add(type.getSuperClass());
      collectAllSuperTypes(type.getSuperClass(), allSuperTypes);
    }
    for (JInterfaceType interfaceType : type.getImplements()) {
      allSuperTypes.add(interfaceType);
      collectAllSuperTypes(interfaceType, allSuperTypes);
    }
  }

  private static boolean isNonStaticJsoClassDispatch(JMethod method, JDeclaredType enclosingType) {
    return !method.isStatic() && enclosingType.isJsoType();
  }

  private JsniRestrictionChecker() {
  }
}
