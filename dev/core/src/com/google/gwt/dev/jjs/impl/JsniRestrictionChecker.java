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
import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

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
  private final Set<JDeclaredType> typesRequiringTrampolineDispatch;
  private TreeLogger logger;
  private boolean hasErrors;

  public JsniRestrictionChecker(TreeLogger logger, JProgram jprogram) {
    this.logger = logger;
    this.jprogram = jprogram;
    this.typesRequiringTrampolineDispatch = Sets.newHashSet();
    for (JDeclaredType type : jprogram.getRepresentedAsNativeTypes()) {
      collectAllSuperTypes(type , typesRequiringTrampolineDispatch);
    }
  }

  private void collectAllSuperTypes(JDeclaredType type,  Set<JDeclaredType> allSuperTypes) {
    if (type.getSuperClass() != null) {
      allSuperTypes.add(type.getSuperClass());
      collectAllSuperTypes(type.getSuperClass(), allSuperTypes);
    }
    for (JInterfaceType interfaceType : type.getImplements()) {
      allSuperTypes.add(interfaceType);
      collectAllSuperTypes(interfaceType, allSuperTypes);
    }
  }

  @Override
  public boolean visit(JDeclaredType x, Context ctx) {
    TreeLogger currentLogger = this.logger;
    this.logger = this.logger.branch(Type.INFO, "Errors in " + x.getSourceInfo().getFileName());
    accept(x.getMethods());
    this.logger = currentLogger;
    return false;
  }

  @Override
  public boolean visit(JsniMethodBody x, Context ctx) {
    currentJsniMethod = x.getMethod();
    return true;
  }

  @Override
  public boolean visit(JsniMethodRef x, Context ctx) {
    JMethod calledMethod = x.getTarget();
    JDeclaredType enclosingTypeOfCalledMethod = calledMethod.getEnclosingType();

    if (isNonStaticJsoClassDispatch(calledMethod, enclosingTypeOfCalledMethod)) {
      logError(x, "JSNI method %s calls non-static method %s on an instance which is a "
          + "subclass of JavaScriptObject. Only static method calls on JavaScriptObject subclasses "
          + "are allowed in JSNI.",
          currentJsniMethod.getQualifiedName(),
          calledMethod.getQualifiedName());
    } else if (isJsoInterface(enclosingTypeOfCalledMethod)) {
      logError(x, "JSNI method %s calls method %s on an instance which might be a "
          + "JavaScriptObject. Such a method call is only allowed in pure Java (non-JSNI) "
          + "functions.",
          currentJsniMethod.getQualifiedName(),
          calledMethod.getQualifiedName());
    } else if (jprogram.isRepresentedAsNativeJsPrimitive(enclosingTypeOfCalledMethod)
        && !calledMethod.isStatic()
        && !calledMethod.isConstructor()) {
      logError(x, "JSNI method %s calls method %s. Instance methods on %s "
          + "cannot be called from JSNI.",
          currentJsniMethod.getQualifiedName(),
          calledMethod.getQualifiedName(),
          enclosingTypeOfCalledMethod.getName());
    } else if (typesRequiringTrampolineDispatch.contains(enclosingTypeOfCalledMethod)
        && !calledMethod.isStatic()
        && !calledMethod.isConstructor()) {
      log(x, Type.WARN, "JSNI method %s calls method %s. Instance methods from %s should "
          + "not be called on Boolean, Double, String, Array or JSO instances from JSNI.",
          currentJsniMethod.getQualifiedName(),
          calledMethod.getQualifiedName(),
          enclosingTypeOfCalledMethod.getName());
    }
    return true;
  }

  private void log(HasSourceInfo hasSourceInfo, Type type, String format, Object... args) {
    logger.log(type, String.format(
        String.format("Line %d: %s",
            hasSourceInfo.getSourceInfo().getStartLine(),
            format),
        args));
    hasErrors |= type == Type.ERROR;
  }

  private void logError(HasSourceInfo hasSourceInfo, String format, Object... args) {
    log(hasSourceInfo, Type.ERROR, format, args);
  }

  private boolean isJsoInterface(JDeclaredType type) {
    return jprogram.typeOracle.isSingleJsoImpl(type)
        || jprogram.typeOracle.isDualJsoInterface(type);
  }

  private boolean isNonStaticJsoClassDispatch(JMethod method, JDeclaredType enclosingType) {
    return !method.isStatic() && enclosingType.isJsoType();
  }
}
