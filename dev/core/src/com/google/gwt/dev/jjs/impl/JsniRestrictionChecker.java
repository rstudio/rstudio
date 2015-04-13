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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;

/**
 * Checks and throws errors for invalid JSNI constructs.
 */
public class JsniRestrictionChecker extends JVisitor {

  public static void exec(TreeLogger logger, JProgram jprogram) throws UnableToCompleteException {
    JsniRestrictionChecker jsniRestrictionChecker = new JsniRestrictionChecker(logger, jprogram);
    jsniRestrictionChecker.accept(jprogram);
    if (jsniRestrictionChecker.hasErrors) {
      throw new UnableToCompleteException();
    }
  }

  private JMethod currentJsniMethod;
  private final JProgram jprogram;
  private final TreeLogger logger;
  private boolean hasErrors;

  public JsniRestrictionChecker(TreeLogger logger, JProgram jprogram) {
    this.logger = logger;
    this.jprogram = jprogram;
  }

  @Override
  public boolean visit(JsniMethodBody x, Context ctx) {
    currentJsniMethod = x.getMethod();
    return super.visit(x, ctx);
  }

  @Override
  public boolean visit(JsniMethodRef x, Context ctx) {
    JMethod calledMethod = x.getTarget();
    JDeclaredType enclosingTypeOfCalledMethod = calledMethod.getEnclosingType();

    if (isNonStaticJsoClassDispatch(calledMethod, enclosingTypeOfCalledMethod)) {
      logError("JSNI method %s  attempts to call non-static method %s on an instance which is a "
          + "subclass of JavaScriptObject. Only static method calls on JavaScriptObject subclasses "
          + "are allowed in JSNI.", currentJsniMethod.getQualifiedName(),
          calledMethod.getQualifiedName());
    } else if (isJsoInterface(enclosingTypeOfCalledMethod)) {
      logError("JSNI method %s attempts to call method %s on an instance which might be a "
          + "JavaScriptObject. Such a method call is only allowed in pure Java (non-JSNI) "
          + "functions.", currentJsniMethod.getQualifiedName(), calledMethod.getQualifiedName());
    } else if (jprogram.isJavaLangString(enclosingTypeOfCalledMethod) && !calledMethod.isStatic()) {
      logError("JSNI method %s attempts to call method %s. Only static methods from " +
              "java.lang.String can be called from JSNI.",
          currentJsniMethod.getQualifiedName(),
          calledMethod.getQualifiedName());
    } else if (jprogram.isJavaLangObject(enclosingTypeOfCalledMethod) && !calledMethod.isStatic()) {
      log(Type.WARN, "JSNI method %s calls method %s. Instance java.lang.Object methods should " +
              "not be called on String, Array or JSO instances.",
          currentJsniMethod.getQualifiedName(),
          calledMethod.getQualifiedName());
    }
    return super.visit(x, ctx);
  }

  private void log(Type type, String format, Object... args) {
    logger.log(type, String.format(format, args));
    hasErrors |= type == Type.ERROR;
  }

  private void logError(String format, Object... args) {
    log(Type.ERROR, format, args);
  }

  private boolean isJsoInterface(JDeclaredType type) {
    return jprogram.typeOracle.isSingleJsoImpl(type)
        || jprogram.typeOracle.isDualJsoInterface(type);
  }

  private boolean isNonStaticJsoClassDispatch(JMethod method, JDeclaredType enclosingType) {
    return !method.isStatic() && jprogram.typeOracle.isJavaScriptObject(enclosingType);
  }
}
