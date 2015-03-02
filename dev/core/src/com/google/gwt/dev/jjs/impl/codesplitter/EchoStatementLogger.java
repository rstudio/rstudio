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
package com.google.gwt.dev.jjs.impl.codesplitter;

import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVars;

/**
 * A statement logger that immediately prints out everything live that it
 * sees.
 */
class EchoStatementLogger implements FragmentExtractor.StatementLogger {
  private final JavaToJavaScriptMap map;

  public EchoStatementLogger(JavaToJavaScriptMap map) {
    this.map = map;
  }

  @Override
  public void log(JsStatement statement, boolean include) {
    if (!include) {
      return;
    }
    if (statement instanceof JsExprStmt) {
      JsExpression expr = ((JsExprStmt) statement).getExpression();
      if (!(expr instanceof JsFunction)) {
        return;
      }
      JsFunction func = (JsFunction) expr;
      if (func.getName() == null) {
        return;
      }
      JMethod method = map.nameToMethod(func.getName());
      if (method == null) {
        return;
      }
      System.out.println(JProgram.getFullName(method));

    } else if (statement instanceof JsVars) {
      JsVars vars = (JsVars) statement;
      for (JsVars.JsVar var : vars) {
        JField field = map.nameToField(var.getName());
        if (field != null) {
          System.out.println(field.getQualifiedName());
        }
      }
    }
  }
}
