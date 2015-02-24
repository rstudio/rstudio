/*
 * Copyright 2015 Google Inc.
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
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;

/**
 * Checks and throws errors for invalid JsInterop constructs.
 */
// TODO: prevent the existence of more than 1 (x/is/get/has) getter for the same property name.
// TODO: handle custom JsType field/method names when that feature exists.
// TODO: prevent regular Java JsType (not JsProperty) method names like ".x()" colliding with raw JS
// property names like ".x".
public class JsInteropRestrictionChecker extends JVisitor {

  public static void exec(TreeLogger logger, JProgram jprogram,
      MinimalRebuildCache minimalRebuildCache) throws UnableToCompleteException {
    JsInteropRestrictionChecker jsInteropRestrictionChecker =
        new JsInteropRestrictionChecker(logger, jprogram, minimalRebuildCache);
    try {
      jsInteropRestrictionChecker.accept(jprogram);
    } catch (InternalCompilerException e) {
      // Already logged.
      throw new UnableToCompleteException();
    }
  }

  private JDeclaredType currentType;
  private final JProgram jprogram;
  private final TreeLogger logger;
  private final MinimalRebuildCache minimalRebuildCache;

  public JsInteropRestrictionChecker(TreeLogger logger, JProgram jprogram,
      MinimalRebuildCache minimalRebuildCache) {
    this.logger = logger;
    this.jprogram = jprogram;
    this.minimalRebuildCache = minimalRebuildCache;
  }

  @Override
  public void endVisit(JDeclaredType x, Context ctx) {
    assert currentType == x;
    currentType = null;
  }

  @Override
  public boolean visit(JDeclaredType x, Context ctx) {
    assert currentType == null;
    minimalRebuildCache.removeJsInteropNames(x.getName());
    currentType = x;

    return true;
  }

  @Override
  public boolean visit(JField x, Context ctx) {
    if (jprogram.typeOracle.isExportedField(x)) {
      boolean success = minimalRebuildCache.addExportedGlobalName(x.getQualifiedExportName(),
          currentType.getName());
      if (!success) {
        logger.log(TreeLogger.ERROR, String.format(
            "Static field '%s' can't be exported because the global name '%s' is already taken.",
            computeReadableSignature(x), x.getQualifiedExportName()));
        throw new UnsupportedOperationException();
      }
    } else if (jprogram.typeOracle.isJsTypeField(x)) {
      boolean success = minimalRebuildCache.addJsTypeMemberName(x.getName(), currentType.getName());
      if (!success) {
        logger.log(TreeLogger.ERROR, String.format(
            "Instance field '%s' can't be exported because the member name '%s' is already taken.",
            computeReadableSignature(x), x.getName()));
        throw new UnsupportedOperationException();
      }
    }

    return true;
  }

  @Override
  public boolean visit(JMethod x, Context ctx) {
    if (jprogram.typeOracle.isExportedMethod(x)) {
      boolean success = minimalRebuildCache.addExportedGlobalName(x.getQualifiedExportName(),
          currentType.getName());
      if (!success) {
        logger.log(TreeLogger.ERROR, String.format(
            "Static method '%s' can't be exported because the global name '%s' is already taken.",
            computeReadableSignature(x), x.getQualifiedExportName()));
        throw new UnsupportedOperationException();
      }
    } else if (jprogram.typeOracle.isJsTypeMethod(x) && isDirectOrTransitiveJsProperty(x)) {
      // JsProperty methods are mangled and obfuscated and so do not consume an unobfuscated
      // collidable name slot.
    } else if (jprogram.typeOracle.isJsTypeMethod(x)) {
      boolean success = minimalRebuildCache.addJsTypeMemberName(x.getName(), currentType.getName());
      if (!success) {
        logger.log(TreeLogger.ERROR, String.format(
            "Instance method '%s' can't be exported because the member name '%s' is already taken.",
            computeReadableSignature(x), x.getName()));
        throw new UnsupportedOperationException();
      }
    }

    return true;
  }

  private String computeReadableSignature(JField field) {
    return field.getEnclosingType().getName() + "." + field.getName();
  }

  private String computeReadableSignature(JMethod method) {
    return method.getEnclosingType().getName() + "." + method.getName();
  }

  private boolean isDirectOrTransitiveJsProperty(JMethod method) {
    if (method.isJsProperty()) {
      return true;
    }
    for (JMethod overrideMethod : method.getOverriddenMethods()) {
      if (overrideMethod.isJsProperty()) {
        return true;
      }
    }
    return false;
  }
}
