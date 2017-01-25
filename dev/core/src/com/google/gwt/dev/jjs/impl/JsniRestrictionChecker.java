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
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Map;
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
      JjsUtils.addAllSuperTypes(type, typesRequiringTrampolineDispatch);
    }
    new JVisitor() {
      @Override
      public boolean visit(JMethodBody x, Context ctx) {
        // Skip non jsni methods.
        return false;
      }

      @Override
      public boolean visit(final JsniMethodBody x, Context ctx) {
        final Map<String, JsniMethodRef> methodsByJsniReference = Maps.newHashMap();
        for (JsniMethodRef ref : x.getJsniMethodRefs()) {
          methodsByJsniReference.put(ref.getIdent(), ref);
        }
        if (methodsByJsniReference.isEmpty()) {
          return false;
        }

        // Examine the JS AST that represents the JSNI method body to check for devirtualizable
        // methods references that are not directly called.
        new JsModVisitor() {
          @Override
          public boolean visit(JsInvocation x, JsContext ctx) {
            if (!(x.getQualifier() instanceof JsNameRef)) {
              // If the invocation does not have a name as a qualifier (it might be an
              // expression), the it is certainly not a JSNI method reference; but it might
              // contain one so explore its subnodes the usual way.
              return true;
            }
            JsNameRef ref = (JsNameRef) x.getQualifier();
            if (!ref.isJsniReference()) {
              // The invocation is not to a JSNI method; but its subnodes might contain one
              // hence explore them the usual way.
              return true;
            }

            // Skip the method JsNameRef but check the qualifier.
            JsExpression methodQualifier = ref.getQualifier();
            if (methodQualifier != null) {
              // Even if it is a direct call, there might be a reference in the qualifier.
              accept(methodQualifier);
            }

            // This is a direct call so if it was a JSNI reference to a devirtualized method
            // it is safe, as it will be rewritten by {@see Devirtualizer}.
            return false;
          }

          @Override
          public void endVisit(JsNameRef x, JsContext ctx) {
            JsniMethodRef jsniMethodReference = methodsByJsniReference.get(x.getIdent());
            if (jsniMethodReference != null) {
              // This is a JSNI reference that is not in a direct call, so check if it is valid.
              checkJsniMethodReference(jsniMethodReference);
            }
          }
        }.accept(x.getFunc());
        return false;
      }

      private void checkJsniMethodReference(JsniMethodRef jsniMethodReference) {
        JMethod method = jsniMethodReference.getTarget();
        JDeclaredType enclosingType = method.getEnclosingType();

        if (isNonStaticJsoClassDispatch(method, enclosingType)
            || isJsoInterface(enclosingType)) {
          logError(jsniMethodReference,
              "Method %s is implemented by a JSO and can only be used in calls "
                  + "within a JSNI method body.",
              getDescription(method));
        } else if (program.isRepresentedAsNativeJsPrimitive(enclosingType)
            && !method.isStatic()
            && !method.isConstructor()) {
          logError(jsniMethodReference,
              "Method %s is implemented by devirtualized type %s JSO and can only be used in "
                  + "calls within a JSNI method body.",
              getDescription(method),
              getDescription(enclosingType));
        } else if (typesRequiringTrampolineDispatch.contains(enclosingType)
            && !method.isStatic()
            && !method.isConstructor()) {
          logWarning(jsniMethodReference,
              "Unsafe reference to method %s. Instance methods from %s should "
                  + "not be called on Boolean, Double, String, Array or JSO instances "
                  + "from  within a JSNI method body.",
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

  private static boolean isNonStaticJsoClassDispatch(JMethod method, JDeclaredType enclosingType) {
    return !method.isStatic() && enclosingType.isJsoType();
  }

  private JsniRestrictionChecker() {
  }
}
