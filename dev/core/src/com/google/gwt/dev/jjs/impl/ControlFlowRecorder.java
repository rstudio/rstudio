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

import com.google.gwt.dev.StringAnalyzableTypeEnvironment;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Records control flow information.
 * <p>
 * Collects caller->callee, instantiating method->instantiated type, overridden method->overriding
 * method, exported methods and other control flow information in TypeEnvironment indexes to support
 * control flow based link time pruning.
 */
public class ControlFlowRecorder extends JVisitor {

  public static void exec(JProgram program,
      StringAnalyzableTypeEnvironment stringAnalyzableTypeEnvironment, boolean onlyUpdate) {
    new ControlFlowRecorder(stringAnalyzableTypeEnvironment, onlyUpdate, program).execImpl();
  }

  private static String computeName(JMethod method) {
    return method.getJsniSignature(true, true);
  }

  private final Set<String> bannedMethodNames = Sets.newHashSet();
  private String currentMethodName;
  private final boolean onlyUpdate;
  private final JProgram program;
  private final StringAnalyzableTypeEnvironment stringAnalyzableTypeEnvironment;

  public ControlFlowRecorder(StringAnalyzableTypeEnvironment stringAnalyzableTypeEnvironment,
      boolean onlyUpdate, JProgram program) {
    this.stringAnalyzableTypeEnvironment = stringAnalyzableTypeEnvironment;
    this.onlyUpdate = onlyUpdate;
    this.program = program;

    bannedMethodNames.add(computeName(program.getTypeClassLiteralHolder().getClinitMethod()));
  }

  @Override
  public void endVisit(JClassLiteral x, Context ctx) {
    recordClassLiteralReferenced(x.getRefType());
  }

  @Override
  public void endVisit(JNewArray x, Context ctx) {
    JType type = x.getArrayType().getLeafType();
    recordClassLiteralReferenced(type);
  }

  @Override
  public void endVisit(JFieldRef x, Context ctx) {
    processJFieldRef(x);
  }

  @Override
  public void endVisit(JsniFieldRef x, Context ctx) {
    processJFieldRef(x);
  }

  @Override
  public void endVisit(JsniMethodRef x, Context ctx) {
    processMethodCall(x);
  }

  @Override
  public boolean visit(JDeclaredType x, Context ctx) {
    if (!onlyUpdate) {
      stringAnalyzableTypeEnvironment.removeControlFlowIndexesFor(x.getName());
    }

    return true;
  }

  @Override
  public boolean visit(JField x, Context ctx) {
    String typeName = x.getEnclosingType().getName();
    if (x.isJsInteropEntryPoint()) {
      stringAnalyzableTypeEnvironment.recordExportedStaticReferenceInType(typeName);
    }

    return true;
  }

  @Override
  public boolean visit(JMethod x, Context ctx) {
    String typeName = x.getEnclosingType().getName();
    currentMethodName = computeName(x);

    if (bannedMethodNames.contains(currentMethodName)) {
      return false;
    }

    stringAnalyzableTypeEnvironment.recordTypeEnclosesMethod(typeName, currentMethodName);

    for (JMethod overriddenMethod : x.getOverriddenMethods()) {
      String overriddenMethodName = computeName(overriddenMethod);
      stringAnalyzableTypeEnvironment.recordMethodOverridesMethod(currentMethodName,
          overriddenMethodName);
    }

    if (x.canBeReferencedExternally()) {
      stringAnalyzableTypeEnvironment.recordExportedMethodInType(currentMethodName, typeName);
    }

    if (x.isJsInteropEntryPoint()) {
      stringAnalyzableTypeEnvironment.recordExportedStaticReferenceInType(typeName);
    }

    if (x.isConstructor()) {
      // Constructor calls if reachable are deemed to instantiate the class.
      recordCurrentMethodInstantiatesType(x.getEnclosingType());
    }

    for (JParameter parameter : x.getParams()) {
      if (x.isJsMethodVarargs() && parameter.isVarargs()) {
        recordClassLiteralReferenced(
            ((JArrayType) parameter.getType().getUnderlyingType()).getLeafType());
      }
    }
    return true;
  }

  @Override
  public boolean visit(JMethodCall x, Context ctx) {
    processMethodCall(x);
    return true;
  }

  private void execImpl() {
    accept(program);
  }

  private JMethod getValueOfMethod(JDeclaredType type) {
    for (JMethod method : type.getMethods()) {
      if (method.getName().equals("valueOf")) {
        return method;
      }
    }
    return null;
  }

  private void maybeRecordClinitCall(String typeName) {
    String typeClinitMethod = typeName + "::$clinit()V";
    if (!typeClinitMethod.equals(currentMethodName)) {
      stringAnalyzableTypeEnvironment.recordMethodCallsMethod(currentMethodName, typeClinitMethod);
    }
  }

  private void recordClassLiteralReferenced(JType type) {
    if (type instanceof JDeclaredType) {
      String typeName = type.getName();
      stringAnalyzableTypeEnvironment.recordStaticReferenceInMethod(typeName, currentMethodName);
      maybeRecordClinitCall(typeName);
    }
    // Any Enum subtype whose class literal is referenced might have its enumValueOfFunc
    // reflectively called at runtime (see Enum.valueOf()). So to be safe the enumValueOfFunc
    // must be assumed to be called.
    if (type.isEnumOrSubclass() != null && !type.getName().equals("java.lang.Enum")) {
      JMethod valueOfMethod = getValueOfMethod((JDeclaredType) type);
      if (valueOfMethod != null) {
        stringAnalyzableTypeEnvironment.recordMethodCallsMethod(currentMethodName,
            computeName(valueOfMethod));
      }
    }
  }

  private void processJFieldRef(JFieldRef x) {
    if (x.getTarget() instanceof JField) {
      JField field = (JField) x.getTarget();
      if (field.isStatic()) {
        String typeName = field.getEnclosingType().getName();
        stringAnalyzableTypeEnvironment.recordStaticReferenceInMethod(typeName, currentMethodName);
        maybeRecordClinitCall(typeName);
      }
    }
  }

  private void processMethodCall(JMethodCall x) {
    JMethod targetMethod = x.getTarget();
    String calleeMethodName = computeName(targetMethod);
    stringAnalyzableTypeEnvironment.recordMethodCallsMethod(currentMethodName, calleeMethodName);

    if (targetMethod.isStatic()) {
      String typeName = targetMethod.getEnclosingType().getName();
      stringAnalyzableTypeEnvironment.recordStaticReferenceInMethod(typeName, currentMethodName);
      maybeRecordClinitCall(typeName);
    }

    // Instantiations in JSNI don't use JNewInstance and must be recognized by method calls on
    // Constructor functions.
    if (targetMethod.isConstructor()) {
      String typeName = targetMethod.getEnclosingType().getName();
      stringAnalyzableTypeEnvironment.recordMethodInstantiatesType(currentMethodName, typeName);
      maybeRecordClinitCall(typeName);
    }
  }

  private void recordCurrentMethodInstantiatesType(JDeclaredType type) {
    String typeName = type.getName();
    stringAnalyzableTypeEnvironment.recordMethodInstantiatesType(currentMethodName, typeName);
    maybeRecordClinitCall(typeName);

    JClassType superClass = type.getSuperClass();
    if (superClass != null) {
      recordCurrentMethodInstantiatesType(superClass);
    }

    for (JInterfaceType interfaceType : type.getImplements()) {
      recordCurrentMethodInstantiatesType(interfaceType);
    }
  }
}
