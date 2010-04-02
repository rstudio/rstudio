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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for managing autoboxing of Java primitive types.
 */
public class AutoboxUtils {
  private Set<JPrimitiveType> boxablePrimitiveTypes;
  private Map<JClassType, JPrimitiveType> boxClassToPrimitiveMap;
  private Set<JDeclaredType> boxTypes;
  private final JProgram program;
  private Set<JMethod> unboxMethods;

  public AutoboxUtils(JProgram program) {
    this.program = program;
    computeBoxablePrimitiveTypes();
    computeBoxClassToPrimitiveMap();
    computeBoxTypes();
    computeUnboxMethods();
  }

  /**
   * Box the expression <code>toBox</code> into an instance of
   * <code>wrapperType</code>. If <code>toBox</code> is not already of the
   * primitive corresponding to <code>wrapperType</code>, then a cast may be
   * necessary.
   */
  public JExpression box(JExpression toBox, JClassType wrapperType) {
    return box(toBox, primitiveTypeForBoxClass(wrapperType), wrapperType);
  }

  /**
   * Box the expression <code>toBox</code> into the wrapper type corresponding
   * to <code>primitiveType</code>. If <code>toBox</code> is not already of
   * type <code>primitiveType</code>, then a cast may be necessary.
   */
  public JExpression box(JExpression toBox, JPrimitiveType primitiveType) {
    // Find the wrapper type for this primitive type.
    String wrapperTypeName = primitiveType.getWrapperTypeName();
    JClassType wrapperType = (JClassType) program.getFromTypeMap(wrapperTypeName);
    if (wrapperType == null) {
      throw new InternalCompilerException(toBox, "Cannot find wrapper type '"
          + wrapperTypeName + "' associated with primitive type '"
          + primitiveType.getName() + "'", null);
    }

    return box(toBox, primitiveType, wrapperType);
  }

  /**
   * Return the box class for a given primitive. Note that this can return
   * <code>null</code> if the source program does not actually need the
   * requested box type.
   */
  public JClassType boxClassForPrimitive(JPrimitiveType prim) {
    return (JClassType) program.getFromTypeMap(prim.getWrapperTypeName());
  }

  /**
   * If <code>x</code> is an unbox expression, then return the expression that
   * is being unboxed by it. Otherwise, return <code>null</code>.
   */
  public JExpression undoUnbox(JExpression arg) {
    if (arg instanceof JMethodCall) {
      JMethodCall argMethodCall = (JMethodCall) arg;
      if (unboxMethods.contains(argMethodCall.getTarget())) {
        return argMethodCall.getInstance();
      }
    }
    return null;
  }

  private JExpression box(JExpression toBox, JPrimitiveType primitiveType,
      JClassType wrapperType) {
    // Add a cast to toBox if need be
    if (toBox.getType() != primitiveType) {
      toBox = new JCastOperation(toBox.getSourceInfo(), primitiveType, toBox);
    }

    // Find the correct valueOf() method.
    JMethod valueOfMethod = null;
    for (JMethod method : wrapperType.getMethods()) {
      if ("valueOf".equals(method.getName())) {
        if (method.getParams().size() == 1) {
          JParameter param = method.getParams().get(0);
          if (param.getType() == primitiveType) {
            // Found it.
            valueOfMethod = method;
            break;
          }
        }
      }
    }

    if (valueOfMethod == null || !valueOfMethod.isStatic()
        || valueOfMethod.getType() != wrapperType) {
      throw new InternalCompilerException(toBox,
          "Expected to find a method on '" + wrapperType.getName()
              + "' whose signature matches 'public static "
              + wrapperType.getName() + " valueOf(" + primitiveType.getName()
              + ")'", null);
    }

    // Create the boxing call.
    JMethodCall call = new JMethodCall(toBox.getSourceInfo(), null, valueOfMethod);
    call.addArg(toBox);
    return call;
  }

  private void computeBoxablePrimitiveTypes() {
    boxablePrimitiveTypes = new LinkedHashSet<JPrimitiveType>();
    boxablePrimitiveTypes.add(JPrimitiveType.BOOLEAN);
    boxablePrimitiveTypes.add(JPrimitiveType.BYTE);
    boxablePrimitiveTypes.add(JPrimitiveType.CHAR);
    boxablePrimitiveTypes.add(JPrimitiveType.SHORT);
    boxablePrimitiveTypes.add(JPrimitiveType.INT);
    boxablePrimitiveTypes.add(JPrimitiveType.LONG);
    boxablePrimitiveTypes.add(JPrimitiveType.FLOAT);
    boxablePrimitiveTypes.add(JPrimitiveType.DOUBLE);
  }

  private void computeBoxClassToPrimitiveMap() {
    boxClassToPrimitiveMap = new LinkedHashMap<JClassType, JPrimitiveType>();
    for (JPrimitiveType prim : boxablePrimitiveTypes) {
      boxClassToPrimitiveMap.put(boxClassForPrimitive(prim), prim);
    }
  }

  private void computeBoxTypes() {
    boxTypes = new LinkedHashSet<JDeclaredType>();
    for (JPrimitiveType prim : boxablePrimitiveTypes) {
      boxTypes.add(boxClassForPrimitive(prim));
    }
  }

  private void computeUnboxMethods() {
    unboxMethods = new LinkedHashSet<JMethod>();
    for (JDeclaredType boxType : boxTypes) {
      if (boxType != null) {
        for (JMethod method : boxType.getMethods()) {
          if (!method.isStatic() && method.getParams().isEmpty()
              && method.getName().endsWith("Value")
              && (method.getType() instanceof JPrimitiveType)) {
            unboxMethods.add(method);
          }
        }
      }
    }
  }

  private JPrimitiveType primitiveTypeForBoxClass(JClassType wrapperType) {
    JPrimitiveType primitiveType = boxClassToPrimitiveMap.get(wrapperType);
    if (primitiveType == null) {
      throw new IllegalArgumentException("Not a box class: " + wrapperType);
    }
    return primitiveType;
  }
}
