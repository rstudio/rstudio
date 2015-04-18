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
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Ordering;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Checks and throws errors for invalid JsInterop constructs.
 */
// TODO: handle custom JsType field/method names when that feature exists.
// TODO: move JsInterop checks from JSORestrictionsChecker to here.
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

  private Map<String, String> currentJsTypeMethodNameByGetterNames;
  private Map<String, String> currentJsTypeMethodNameByMemberNames;
  private Map<String, String> currentJsTypeMethodNameBySetterNames;
  private Set<JMethod> currentJsTypeProcessedMethods;
  private Map<String, JType> currentJsTypePropertyTypeByName;
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
    currentJsTypePropertyTypeByName = Maps.newHashMap();
    currentJsTypeMethodNameByMemberNames = Maps.newHashMap();
    currentJsTypeMethodNameByGetterNames = Maps.newHashMap();
    currentJsTypeMethodNameBySetterNames = Maps.newHashMap();
    minimalRebuildCache.removeJsInteropNames(x.getName());
    currentType = x;

    checkJsFunctionHierarchy(x);
    checkJsFunctionJsTypeCollision(x);
    if (currentType instanceof JInterfaceType) {
      checkJsTypeHierarchy((JInterfaceType) currentType);
    }

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

    if (currentType == x.getEnclosingType()) {
      if (jprogram.typeOracle.isJsPropertyMethod(x) && !jprogram.typeOracle.isJsType(currentType)) {
        if (currentType instanceof JInterfaceType) {
          logError("Method '%s' can't be a JsProperty since interface '%s' is not a JsType.",
              x.getName(), x.getEnclosingType().getName());
        } else {
          logError("Method '%s' can't be a JsProperty since '%s' "
              + "is not an interface.", x.getName(), x.getEnclosingType().getName());
        }
      }
    }

    return false;
  }

  private void checkExportName(JMember x) {
    boolean success = minimalRebuildCache.addExportedGlobalName(x.getQualifiedExportName(),
        currentType.getName());
    if (!success) {
      logError("Member '%s' can't be exported because the global name '%s' is already taken.",
          x.getQualifiedName(), x.getQualifiedExportName());
    }
  }

  private void checkInconsistentPropertyType(String propertyName, String enclosingTypeName,
      JType parameterType) {
    JType recordedType = currentJsTypePropertyTypeByName.put(propertyName, parameterType);
    if (recordedType != null && recordedType != parameterType) {
      logError("The setter and getter for JsProperty '%s' in type '%s' must have consistent types.",
          propertyName, enclosingTypeName);
    }
  }

  private void checkJsTypeHierarchy(JInterfaceType interfaceType) {
    if (jprogram.typeOracle.isJsType(currentType)) {
      for (JDeclaredType superInterface : interfaceType.getImplements()) {
        if (!jprogram.typeOracle.isJsType(superInterface)) {
          logWarning(
              "JsType interface '%s' extends non-JsType interface '%s'. This is not recommended.",
              interfaceType.getName(), superInterface.getName());
        }
      }
    }
  }

  private void checkJsTypeFieldName(JField field, String memberName) {
    boolean success =
        currentJsTypeMethodNameByMemberNames.put(memberName, field.getQualifiedName()) == null;
    if (!success) {
      logError("Field '%s' can't be exported in type '%s' because the member name "
          + "'%s' is already taken.", field.getQualifiedName(), currentType.getName(), memberName);
    }
  }

  private void checkJsTypeMethod(JMethod method) {
    if (method.isSynthetic() && !method.isForwarding()) {
      // A name slot taken up by a synthetic method, such as a bridge method for a generic method,
      // is not the fault of the user and so should not be reported as an error. JS generation
      // should take responsibility for ensuring that only the correct method version (in this
      // particular set of colliding method names) is exported. Forwarding synthetic methods
      // (such as an accidental override forwarding method that occurs when a JsType interface
      // starts exposing a method in class B that is only ever implemented in its parent class A)
      // though should be checked since they are exported and do take up an name slot.
      return;
    }

    String jsMemberName = method.getImmediateOrTransitiveJsMemberName();
    String qualifiedMethodName = method.getQualifiedName();
    String typeName = method.getEnclosingType().getName();
    JsPropertyType jsPropertyType = method.getImmediateOrTransitiveJsPropertyType();

    if (jsMemberName == null) {
      logError("'%s' can't be exported because the method overloads multiple methods with "
          + "different names.", qualifiedMethodName);
    }

    if (jsPropertyType == JsPropertyType.GET) {
      if (!method.getParams().isEmpty() || method.getType() == JPrimitiveType.VOID) {
        logError("There can't be void return type or any parameters for the JsProperty getter"
            + " '%s'.", qualifiedMethodName);
        return;
      }
      if (method.getType() != JPrimitiveType.BOOLEAN && method.getName().startsWith("is")) {
        logError("There can't be non-booelean return for the JsProperty 'is' getter '%s'.",
            qualifiedMethodName);
        return;
      }
      if (currentJsTypeMethodNameByGetterNames.put(jsMemberName, qualifiedMethodName) != null) {
        // Don't allow multiple getters for the same property name.
        logError("There can't be more than one getter for JsProperty '%s' in type '%s'.",
            jsMemberName, typeName);
        return;
      }
      checkNameCollisionForGetterAndRegular(jsMemberName, typeName);
      checkInconsistentPropertyType(jsMemberName, typeName, method.getOriginalReturnType());
    } else if (jsPropertyType == JsPropertyType.SET) {
      if (method.getParams().size() != 1 || method.getType() != JPrimitiveType.VOID) {
        logError("There needs to be single parameter and void return type for the JsProperty setter"
            + " '%s'.", qualifiedMethodName);
        return;
      }
      if (currentJsTypeMethodNameBySetterNames.put(jsMemberName, qualifiedMethodName) != null) {
        // Don't allow multiple setters for the same property name.
        logError("There can't be more than one setter for JsProperty '%s' in type '%s'.",
            jsMemberName, typeName);
        return;
      }
      checkNameCollisionForSetterAndRegular(jsMemberName, typeName);
      checkInconsistentPropertyType(jsMemberName, typeName,
          Iterables.getOnlyElement(method.getParams()).getType());
    } else if (jsPropertyType == JsPropertyType.UNDEFINED) {
      // We couldn't extract the JsPropertyType.
      logError("JsProperty '%s' doesn't follow Java Bean naming conventions.", qualifiedMethodName);
    } else {
      // If it's just an regular JsType method.
      if (currentJsTypeMethodNameByMemberNames.put(jsMemberName, qualifiedMethodName) != null) {
        logError("Method '%s' can't be exported in type '%s' because the member name "
            + "'%s' is already taken.", qualifiedMethodName, currentType.getName(), jsMemberName);
      }
      checkNameCollisionForGetterAndRegular(jsMemberName, typeName);
      checkNameCollisionForSetterAndRegular(jsMemberName, typeName);
    }
  }

  private void checkNameCollisionForGetterAndRegular(String getterName, String typeName) {
    if (currentJsTypeMethodNameByGetterNames.containsKey(getterName)
        && currentJsTypeMethodNameByMemberNames.containsKey(getterName)) {
      logError("The JsType member '%s' and JsProperty '%s' can't both be named "
          + "'%s' in type '%s'.", currentJsTypeMethodNameByMemberNames.get(getterName),
          currentJsTypeMethodNameByGetterNames.get(getterName), getterName, typeName);
    }
  }

  private void checkNameCollisionForSetterAndRegular(String setterName, String typeName) {
    if (currentJsTypeMethodNameBySetterNames.containsKey(setterName)
        && currentJsTypeMethodNameByMemberNames.containsKey(setterName)) {
      logError("The JsType member '%s' and JsProperty '%s' can't both be named "
          + "'%s' in type '%s'.", currentJsTypeMethodNameByMemberNames.get(setterName),
          currentJsTypeMethodNameBySetterNames.get(setterName), setterName, typeName);
    }
  }

  private void checkJsFunctionHierarchy(JDeclaredType type) {
    SortedSet<String> implementedJsFunctions =
        FluentIterable.from(jprogram.typeOracle.getImplementedJsFunctions(type)).transform(
        new Function<JInterfaceType, String>() {
          @Override
          public String apply(JInterfaceType type) {
            return type.getName();
          }
        }).toSortedSet(Ordering.natural());
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

  private void logWarning(String format, Object... args) {
    logger.log(TreeLogger.WARN, String.format(format, args));
  }
}
