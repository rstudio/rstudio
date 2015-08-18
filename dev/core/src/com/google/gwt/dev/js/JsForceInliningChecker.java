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
package com.google.gwt.dev.js;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.common.InliningMode;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVisitor;

/**
 * Makes sure methods marked with @ForceInline are actually inlined in optimized mode.
 */
public class JsForceInliningChecker {
  private static final String NAME = JsForceInliningChecker.class.getSimpleName();

  /**
   * Determines if the evaluation of a JsNode may be affected by side effects.
   */
  private static class ForceInliningCheckerVisitor extends JsVisitor {

    private final TreeLogger logger;
    private boolean error = false;
    private JavaToJavaScriptMap javaToJavaScriptMap;

    private ForceInliningCheckerVisitor(
        TreeLogger logger, JavaToJavaScriptMap javaToJavaScriptMap) {
      this.logger = logger;
      this.javaToJavaScriptMap = javaToJavaScriptMap;
    }

    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      if (x.getInliningMode() == InliningMode.FORCE_INLINE) {
        JMethod originalMethod = javaToJavaScriptMap.nameToMethod(x.getName());
        String methodName = originalMethod != null ? originalMethod.toString() :
            x.getName().getShortIdent();
        logger.log(Type.ERROR, "Function " + methodName
            + " is marked as @ForceInline but it could not be inlined");
        error = true;
      }
    }
  }

  /**
   * Static entry point used by JavaToJavaScriptCompiler.
   */
  public static void check(TreeLogger logger, JavaToJavaScriptMap javaToJavaScriptMap,
      JsProgram program) throws UnableToCompleteException {
    ForceInliningCheckerVisitor visitor =
        new ForceInliningCheckerVisitor(logger, javaToJavaScriptMap);
    visitor.accept(program);
    if (visitor.error) {
      throw new UnableToCompleteException();
    }
  }

  /**
   * Utility class.
   */
  private JsForceInliningChecker() {
  }
}
