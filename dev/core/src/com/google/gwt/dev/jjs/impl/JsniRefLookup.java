/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.dev.jjs.ast.HasEnclosingType;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.JsniRef;

import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeSet;

/**
 * A utility class that can look up a {@link JsniRef} in a {@link JProgram}.
 */
public class JsniRefLookup {

  /**
   * A callback used to indicate the reason for a failed JSNI lookup.
   */
  public interface ErrorReporter {
    void reportError(String error);
  }

  /**
   * Look up a JSNI reference.
   * 
   * @param ref The reference to look up
   * @param program The program to look up the reference in
   * @param errorReporter A callback used to indicate the reason for a failed
   *          JSNI lookup
   * @return The item referred to, or <code>null</code> if it could not be
   *         found. If the return value is <code>null</code>,
   *         <code>errorReporter</code> will have been invoked.
   */
  public static HasEnclosingType findJsniRefTarget(JsniRef ref,
      JProgram program, JsniRefLookup.ErrorReporter errorReporter) {
    String className = ref.className();
    JType type = null;
    if (!className.equals("null")) {
      type = program.getTypeFromJsniRef(className);
      if (type == null) {
        errorReporter.reportError("Unresolvable native reference to type '"
            + className + "'");
        return null;
      }
    }

    if (!ref.isMethod()) {
      // look for a field
      String fieldName = ref.memberName();
      if (type == null) {
        if (fieldName.equals("nullField")) {
          return program.getNullField();
        }

      } else if (fieldName.equals("class")) {
        JClassLiteral lit = program.getLiteralClass(type);
        return lit.getField();

      } else if (type instanceof JPrimitiveType) {
        errorReporter.reportError("May not refer to fields on primitive types");
        return null;

      } else if (type instanceof JArrayType) {
        errorReporter.reportError("May not refer to fields on array types");
        return null;

      } else {
        for (JField field : ((JDeclaredType) type).getFields()) {
          if (field.getName().equals(fieldName)) {
            return field;
          }
        }
      }

      errorReporter.reportError("Unresolvable native reference to field '"
          + fieldName + "' in type '" + className + "'");
      return null;

    } else if (type instanceof JPrimitiveType) {
      errorReporter.reportError("May not refer to methods on primitive types");
      return null;

    } else {
      // look for a method
      TreeSet<String> almostMatches = new TreeSet<String>();
      String methodName = ref.memberName();
      String jsniSig = ref.memberSignature();
      if (type == null) {
        if (jsniSig.equals("nullMethod()")) {
          return program.getNullMethod();
        }
      } else {
        Queue<JDeclaredType> workList = new LinkedList<JDeclaredType>();
        workList.add((JDeclaredType) type);
        while (!workList.isEmpty()) {
          JDeclaredType cur = workList.poll();
          for (JMethod method : cur.getMethods()) {
            if (method.getName().equals(methodName)) {
              String sig = JProgram.getJsniSig(method);
              if (sig.equals(jsniSig)) {
                return method;
              } else if (sig.startsWith(jsniSig) && jsniSig.endsWith(")")) {
                return method;
              } else {
                almostMatches.add(sig);
              }
            }
          }
          if (cur.getSuperClass() != null) {
            workList.add(cur.getSuperClass());
          }
          workList.addAll(cur.getImplements());
        }
      }

      if (almostMatches.isEmpty()) {
        errorReporter.reportError("Unresolvable native reference to method '"
            + methodName + "' in type '" + className + "'");
        return null;
      } else {
        StringBuilder suggestList = new StringBuilder();
        String comma = "";
        for (String almost : almostMatches) {
          suggestList.append(comma + "'" + almost + "'");
          comma = ", ";
        }
        errorReporter.reportError("Unresolvable native reference to method '"
            + methodName + "' in type '" + className + "' (did you mean "
            + suggestList.toString() + "?)");
        return null;
      }
    }
  }

}
