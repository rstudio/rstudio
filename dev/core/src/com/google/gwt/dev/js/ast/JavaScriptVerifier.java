/*
 * Copyright 2015 Google Inc.
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

package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.JsUtils;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Verifies that the JavaScript AST and the map are consistent.
 */
public class JavaScriptVerifier {

  public static void verify(JsProgram jsProgram, JavaToJavaScriptMap map) {
    if (!JavaScriptVerifier.class.desiredAssertionStatus()) {
      return;
    }
    verifyTopLevelMethodMapping(jsProgram, map);
    verifyGlobalNameOrdering(jsProgram, map);
  }

  private static void verifyGlobalNameOrdering(JsProgram jsProgram, final JavaToJavaScriptMap map) {
    final Set<JsName> declaredEntities = Sets.newHashSet();
    new JsVisitor() {
      @Override
      public boolean visit(JsFunction x, JsContext ctx) {
        declaredEntities.add(x.getName());
        // Do not examine function bodies.
        return false;
      }
    }.accept(jsProgram);

    new JsVisitor() {
      @Override
      public boolean visit(JsFunction x, JsContext ctx) {
        // Do not examine function bodies.
        return false;
      }

      @Override
      public boolean visit(JsVars x, JsContext ctx) {
        for (JsVar var : x) {
          declaredEntities.add(var.getName());
        }
        return true;
      }

      @Override
      public boolean visit(JsBinaryOperation x, JsContext ctx) {
        if (x.getOperator().isAssignment()) {
          JsNameRef nameRef = (JsNameRef) x.getArg1();
          if (nameRef.getQualifier() == null) {
            declaredEntities.add(nameRef.getName());
          }
        }
        return true;
      }

      @Override
      public boolean visit(JsObjectLiteral x, JsContext ctx) {
        for (JsPropertyInitializer propertyInitializer : x.getPropertyInitializers()) {
          accept(propertyInitializer.getValueExpr());
        }
        return false;
      }

      @Override
      public void endVisit(JsNameRef x, JsContext ctx) {
        if (x.getQualifier() != null || !x.getName().isObfuscatable()) {
          return;
        }
        assert declaredEntities.contains(x.getName()) : x.getName() + " reference found before " +
            " definition.";
        map.nameToField(x.getName());
      }
    }.accept(jsProgram);
  }

  public static void verifyTopLevelMethodMapping(JsProgram jsProgram, JavaToJavaScriptMap map) {
    for (JsProgramFragment fragment : jsProgram.getFragments()) {
      for (JsStatement statement : fragment.getGlobalBlock().getStatements()) {
        JsFunction function = JsUtils.isFunctionDeclaration(statement);
        if (function == null) {
          continue;
        }
        assert map.nameToMethod(function.getName()) == map.methodForStatement(statement);
      }
    }
  }

  private JavaScriptVerifier() {
  }
}
