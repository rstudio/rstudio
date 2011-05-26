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

import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilities for managing autoboxing of Java primitive types.
 */
public class AutoboxUtils {
  private static final JPrimitiveType[] TYPES = {
      JPrimitiveType.BOOLEAN, JPrimitiveType.BYTE, JPrimitiveType.CHAR, JPrimitiveType.SHORT,
      JPrimitiveType.INT, JPrimitiveType.LONG, JPrimitiveType.FLOAT, JPrimitiveType.DOUBLE};

  private final Map<JPrimitiveType, JMethod> boxMethods =
      new LinkedHashMap<JPrimitiveType, JMethod>();

  private final Map<JDeclaredType, JMethod> unboxMethods =
      new LinkedHashMap<JDeclaredType, JMethod>();

  public AutoboxUtils(JProgram program) {
    for (JPrimitiveType primType : TYPES) {
      JDeclaredType wrapperType = program.getFromTypeMap(primType.getWrapperTypeName());
      String boxSig =
          "valueOf(" + primType.getJsniSignatureName() + ")" + wrapperType.getJsniSignatureName();
      String unboxSig = primType.getName() + "Value()" + primType.getJsniSignatureName();
      for (JMethod method : wrapperType.getMethods()) {
        if (method.isStatic()) {
          if (method.getSignature().equals(boxSig)) {
            boxMethods.put(primType, method);
          }
        } else {
          if (method.getSignature().equals(unboxSig)) {
            unboxMethods.put(wrapperType, method);
          }
        }
      }
    }
    assert boxMethods.size() == TYPES.length;
    assert unboxMethods.size() == TYPES.length;
  }

  /**
   * Box the expression <code>toBox</code> into the wrapper type corresponding
   * to <code>primitiveType</code>. If <code>toBox</code> is not already of type
   * <code>primitiveType</code>, then a cast may be necessary.
   */
  public JExpression box(JExpression toBox, JPrimitiveType primitiveType) {
    // Add a cast to toBox if need be
    if (toBox.getType() != primitiveType) {
      toBox = new JCastOperation(toBox.getSourceInfo(), primitiveType, toBox);
    }
    JMethod method = boxMethods.get(primitiveType);
    assert method != null;
    JMethodCall call = new JMethodCall(toBox.getSourceInfo(), null, method);
    call.addArg(toBox);
    return call;
  }

  public Collection<JMethod> getBoxMethods() {
    return boxMethods.values();
  }

  public Collection<JMethod> getUnboxMethods() {
    return unboxMethods.values();
  }

  /**
   * If <code>x</code> is an unbox expression, then return the expression that
   * is being unboxed by it. Otherwise, return <code>null</code>.
   */
  public JExpression undoUnbox(JExpression arg) {
    if (arg instanceof JMethodCall) {
      JMethodCall argMethodCall = (JMethodCall) arg;
      assert unboxMethods.values().contains(argMethodCall.getTarget());
      return argMethodCall.getInstance();
    }
    return null;
  }
}
