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
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;

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
  private Set<JReferenceType> boxTypes;
  private final JProgram program;
  private Set<JMethod> unboxMethods;

  public AutoboxUtils(JProgram program) {
    this.program = program;
    computeBoxablePrimitiveTypes();
    computeBoxClassToPrimitiveMap();
    computeBoxTypes();
    computeUnboxMethods();
  }

  public JExpression box(JExpression toBox, JClassType wrapperType) {
    return box(toBox, primitiveTypeForBoxClass(wrapperType), wrapperType);
  }

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
   * Return the box class for a given primitive.  Note that this can return <code>null</code>
   * if the source program does not actually need the requested box type.
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
    // Find the correct valueOf() method.
    JMethod valueOfMethod = null;
    for (JMethod method : wrapperType.methods) {
      if ("valueOf".equals(method.getName())) {
        if (method.params.size() == 1) {
          JParameter param = method.params.get(0);
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
    JMethodCall call = new JMethodCall(program, toBox.getSourceInfo(), null,
        valueOfMethod);
    call.getArgs().add(toBox);
    return call;
  }

  private void computeBoxablePrimitiveTypes() {
    boxablePrimitiveTypes = new LinkedHashSet<JPrimitiveType>();
    boxablePrimitiveTypes.add(program.getTypePrimitiveBoolean());
    boxablePrimitiveTypes.add(program.getTypePrimitiveByte());
    boxablePrimitiveTypes.add(program.getTypePrimitiveChar());
    boxablePrimitiveTypes.add(program.getTypePrimitiveShort());
    boxablePrimitiveTypes.add(program.getTypePrimitiveInt());
    boxablePrimitiveTypes.add(program.getTypePrimitiveLong());
    boxablePrimitiveTypes.add(program.getTypePrimitiveFloat());
    boxablePrimitiveTypes.add(program.getTypePrimitiveDouble());
  }

  private void computeBoxClassToPrimitiveMap() {
    boxClassToPrimitiveMap = new LinkedHashMap<JClassType, JPrimitiveType>();
    for (JPrimitiveType prim : boxablePrimitiveTypes) {
      boxClassToPrimitiveMap.put(boxClassForPrimitive(prim), prim);
    }
  }

  private void computeBoxTypes() {
    boxTypes = new LinkedHashSet<JReferenceType>();
    for (JPrimitiveType prim : boxablePrimitiveTypes) {
      boxTypes.add(boxClassForPrimitive(prim));
    }
  }

  private void computeUnboxMethods() {
    unboxMethods = new LinkedHashSet<JMethod>();
    for (JReferenceType boxType : boxTypes) {
      if (boxType != null) {
        for (JMethod method : boxType.methods) {
          if (!method.isStatic() && method.params.isEmpty()
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
