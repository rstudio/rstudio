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
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMember;
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
    jsInteropRestrictionChecker.accept(jprogram);
    if (jsInteropRestrictionChecker.hasErrors) {
      throw new UnableToCompleteException();
    }
  }

  private final Set<JMethod> currentJsTypeProcessedMethods = Sets.newHashSet();
  private final Set<String> currentJsTypeMemberNames = Sets.newHashSet();
  private JDeclaredType currentType;
  private boolean hasErrors;
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
    currentJsTypeProcessedMethods.clear();
  }

  @Override
  public boolean visit(JDeclaredType x, Context ctx) {
    assert currentType == null;
    assert currentJsTypeMemberNames.isEmpty();
    assert currentJsTypeProcessedMethods.isEmpty();
    minimalRebuildCache.removeJsInteropNames(x.getName());
    currentType = x;

    // Perform custom class traversal to examine fields and methods of this class and all
    // superclasses so that name collisions between local and inherited members can be found.
    do {
      acceptWithInsertRemoveImmutable(x.getFields());
      acceptWithInsertRemoveImmutable(x.getMethods());
      x = x.getSuperClass();
    } while (x != null);

    // Skip the default class traversal.
    return false;
  }

  @Override
  public boolean visit(JField x, Context ctx) {
    if (currentType == x.getEnclosingType() && jprogram.typeOracle.isExportedField(x)) {
      checkExportName(x);
    } else if (jprogram.typeOracle.isJsTypeField(x)) {
      checkJsTypeMemberName(x, x.getJsMemberName());
    }

    return false;
  }

  @Override
  public boolean visit(JMethod x, Context ctx) {
    if (!currentJsTypeProcessedMethods.add(x)) {
      return false;
    }
    currentJsTypeProcessedMethods.addAll(x.getOverriddenMethods());

    if (currentType == x.getEnclosingType() && jprogram.typeOracle.isExportedMethod(x)) {
      checkExportName(x);
    } else if (jprogram.typeOracle.isJsTypeMethod(x)) {
      if (x.isOrOverridesJsProperty()) {
        // JsProperty methods are mangled and obfuscated and so do not consume an unobfuscated
        // collidable name slot.
      } else if (x.isSynthetic()) {
        // A name slot taken up by a synthetic method, such as a bridge method for a generic method,
        // is not the fault of the user and so should not be reported as an error. JS generation
        // should take responsibility for ensuring that only the correct method version (in this
        // particular set of colliding method names) is exported.
      } else {
        checkJsTypeMethod(x);
      }
    }

    return false;
  }

  private void checkExportName(JMember x) {
    boolean success = minimalRebuildCache.addExportedGlobalName(x.getQualifiedExportName(),
        currentType.getName());
    if (!success) {
      logError("'%s' can't be exported because the global name '%s' is already taken.",
          x.getQualifiedName(), x.getQualifiedExportName());
    }
  }

  private void checkJsTypeMemberName(JMember x, String memberName) {
    boolean success = currentJsTypeMemberNames.add(memberName);
    if (!success) {
      logError("'%s' can't be exported because the member name '%s' is already taken.",
          x.getQualifiedName(), memberName);
    }
  }

  private void checkJsTypeMethod(JMethod x) {
    String name = x.getJsMemberName();
    for (JMethod override : x.getOverriddenMethods()) {
      String overrideName = override.getJsMemberName();
      if (overrideName == null) {
        continue;
      }
      if (name != null && !name.equals(overrideName)) {
        logError("'%s' can't be exported because the method overloads multiple methods with "
            + "different names: %s and %s.", x.getQualifiedName(), name, overrideName);
      }
      name = overrideName;
    }
    assert name != null;
    checkJsTypeMemberName(x, name);
  }

  private void logError(String format, Object... args) {
    logger.log(TreeLogger.ERROR, String.format(format, args));
    hasErrors = true;
  }
}
