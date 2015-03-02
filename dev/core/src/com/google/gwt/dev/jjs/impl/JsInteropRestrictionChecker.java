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
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

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

  private final Set<String> currentJsTypeMemberNames = Sets.newHashSet();
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
    currentJsTypeMemberNames.clear();
  }

  @Override
  public boolean visit(JDeclaredType x, Context ctx) {
    assert currentType == null;
    assert currentJsTypeMemberNames.isEmpty();
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
            x.getQualifiedName(), x.getQualifiedExportName()));
        throw new UnsupportedOperationException();
      }
    } else if (jprogram.typeOracle.isJsTypeField(x)) {
      boolean success = addJsTypeMemberName(x.getName());
      if (!success) {
        logger.log(TreeLogger.ERROR, String.format(
            "Instance field '%s' can't be exported because the member name '%s' is already taken.",
            x.getQualifiedName(), x.getName()));
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
            x.getQualifiedName(), x.getQualifiedExportName()));
        throw new UnsupportedOperationException();
      }
    } else if (jprogram.typeOracle.isJsTypeMethod(x)) {
      if (isDirectOrTransitiveJsProperty(x)) {
        // JsProperty methods are mangled and obfuscated and so do not consume an unobfuscated
        // collidable name slot.
        return true;
      } else if (x.isSynthetic()) {
        // A name slot taken up by a synthetic method, such as a bridge method for a generic method,
        // is not the fault of the user and so should not be reported as an error. JS generation
        // should take responsibility for ensuring that only the correct method version (in this
        // particular set of colliding method names) is exported.
        return true;
      }

      boolean success = addJsTypeMemberName(x.getName());
      if (!success) {
        logger.log(TreeLogger.ERROR, String.format(
            "Instance method '%s' can't be exported because the member name '%s' is already taken.",
            x.getQualifiedName(), x.getName()));
        throw new UnsupportedOperationException();
      }
    }

    return true;
  }

  private boolean addJsTypeMemberName(String exportedMemberName) {
    return currentJsTypeMemberNames.add(exportedMemberName);
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
