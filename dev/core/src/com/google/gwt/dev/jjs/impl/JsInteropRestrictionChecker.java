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
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethod.JsPropertyType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Checks and throws errors for invalid JsInterop constructs.
 */
// TODO: handle custom JsType field/method names when that feature exists.
// TODO: move JsInterop checks from JSORestrictionsChecker to here.
// TODO: check setter and getter for the same property has compatible types.
// TODO: check for name collisions between regular members and accidental-override methods.
// TODO: a JsType interface can only extend another JsType interface.
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

  private Set<JMethod> currentJsTypeProcessedMethods;
  private Map<String, String> currentJsTypeMethodNameByGetterNames;
  private Map<String, String> currentJsTypeMethodNameByMemberNames;
  private Map<String, String> currentJsTypeMethodNameBySetterNames;
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
  }

  @Override
  public boolean visit(JDeclaredType x, Context ctx) {
    assert currentType == null;
    currentJsTypeProcessedMethods = Sets.newHashSet();
    currentJsTypeMethodNameByMemberNames = Maps.newHashMap();
    currentJsTypeMethodNameByGetterNames = Maps.newHashMap();
    currentJsTypeMethodNameBySetterNames = Maps.newHashMap();
    minimalRebuildCache.removeJsInteropNames(x.getName());
    currentType = x;
    checkJsFunctionInheritance(x);
    checkJsFunctionJsTypeCollision(x);
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
      checkJsTypeFieldName(x, x.getJsMemberName());
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
      checkJsTypeMethod(x);
    }

    if (jprogram.typeOracle.isJsPropertyMethod(x)
        && !jprogram.typeOracle.isJsType(x.getEnclosingType())) {
      logError("Method '%s' can't be explicitly annotated @JsProperty since enclosing type '%s' "
          + "is not explicitly annotated @JsType. If the method is already overriding "
          + "a JsProperty method then there is no need to restate the annotation.", x.getName(),
          x.getEnclosingType().getName());
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

  private void checkJsTypeFieldName(JField field, String memberName) {
    boolean success =
        currentJsTypeMethodNameByMemberNames.put(memberName, field.getQualifiedName()) == null;
    if (!success) {
      logError("'%s' can't be exported because the member name '%s' is already taken.",
          field.getQualifiedName(), memberName);
    }
  }

  private void checkJsTypeMethod(JMethod method) {
    if (method.isSynthetic()) {
      // A name slot taken up by a synthetic method, such as a bridge method for a generic method,
      // is not the fault of the user and so should not be reported as an error. JS generation
      // should take responsibility for ensuring that only the correct method version (in this
      // particular set of colliding method names) is exported.
      return;
    }

    String jsMemberName = method.getImmediateOrTransitiveJsMemberName();
    String qualifiedMethodName = method.getQualifiedName();
    String typeName = method.getEnclosingType().getName();
    JsPropertyType jsPropertyType = method.getImmediateOrTransitiveJsPropertyType();

    if (jsMemberName == null) {
      logError("'%s' can't be exported because the method overloads multiple methods with "
          + "different names.", qualifiedMethodName);
    } else if (jsPropertyType == JsPropertyType.HAS) {
      // Has JS dispatch consumes no named slot on the prototype and so can not cause or suffer from
      // any collisions.
    } else if (jsPropertyType == JsPropertyType.GET) {
      // If it's a getter.
      if (currentJsTypeMethodNameByGetterNames.put(jsMemberName, qualifiedMethodName) != null) {
        // Don't allow multiple getters for the same property name.
        logError("There can't be more than one getter for JS property '%s' in type '%s'.",
            jsMemberName, typeName);
      }
      checkNameCollisionForGetterAndRegular(jsMemberName, typeName);
    } else if (jsPropertyType == JsPropertyType.SET) {
      // If it's a setter.
      if (currentJsTypeMethodNameBySetterNames.put(jsMemberName, qualifiedMethodName) != null) {
        // Don't allow multiple setters for the same property name.
        logError("There can't be more than one setter for JS property '%s' in type '%s'.",
            jsMemberName, typeName);
      }
      checkNameCollisionForSetterAndRegular(jsMemberName, typeName);
    } else {
      // If it's just an regular JsType method.
      if (currentJsTypeMethodNameByMemberNames.put(jsMemberName, qualifiedMethodName) != null) {
        logError("'%s' can't be exported because the member name '%s' is already taken.",
            qualifiedMethodName, jsMemberName);
      }
      checkNameCollisionForGetterAndRegular(jsMemberName, typeName);
      checkNameCollisionForSetterAndRegular(jsMemberName, typeName);
    }
  }

  private void checkNameCollisionForGetterAndRegular(String getterName, String typeName) {
    if (currentJsTypeMethodNameByGetterNames.containsKey(getterName)
        && currentJsTypeMethodNameByMemberNames.containsKey(getterName)) {
      logError("The JsType member '%s' and JsProperty property '%s' name can't both be named "
          + "'%s' in type '%s'.", currentJsTypeMethodNameByMemberNames.get(getterName),
          currentJsTypeMethodNameByGetterNames.get(getterName), getterName, typeName);
    }
  }

  private void checkNameCollisionForSetterAndRegular(String setterName, String typeName) {
    if (currentJsTypeMethodNameBySetterNames.containsKey(setterName)
        && currentJsTypeMethodNameByMemberNames.containsKey(setterName)) {
      logError("The JsType member '%s' and JsProperty property '%s' name can't both be named "
          + "'%s' in type '%s'.", currentJsTypeMethodNameByMemberNames.get(setterName),
          currentJsTypeMethodNameBySetterNames.get(setterName), setterName, typeName);
    }
  }

  private void checkJsFunctionInheritance(JDeclaredType type) {
    Collection<JInterfaceType> implementedJsFunctions =
        jprogram.typeOracle.getImplementedJsFunctions(type);
    if (implementedJsFunctions.size() > 1) {
      logError("'%s' implements more than one JsFunction interfaces: %s", type.getName(),
          implementedJsFunctions);
    }
  }

  // To prevent potential name collisions, we disallow JsFunction implementations to be also a
  // JsType.
  private void checkJsFunctionJsTypeCollision(JDeclaredType type) {
    if (type.isOrExtendsJsType() && type.isOrExtendsJsFunction()) {
      logError("'%s' cannot be annotated as (or extend) both a @JsFunction and a @JsType at the "
          + "same time.", type.getName());
    }
  }

  private void logError(String format, Object... args) {
    logger.log(TreeLogger.ERROR, String.format(format, args));
    hasErrors = true;
  }
}
