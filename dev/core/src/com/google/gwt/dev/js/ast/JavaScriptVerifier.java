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

/**
 * Verifies that the JavaScript AST and the map are consistent.
 */
public class JavaScriptVerifier {

  public static void verify(JsProgram jsProgram, JavaToJavaScriptMap map) {
    if (!JavaScriptVerifier.class.desiredAssertionStatus()) {
      return;
    }
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
