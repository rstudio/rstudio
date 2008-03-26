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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JniConstants;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.util.Jsni;
import com.google.gwt.dev.util.JsniRef;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.util.Util;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Tests for access to Java longs from JSNI. Issues a warning for:
 * <ul>
 * <li> JSNI methods with a parameter or return type of long.
 * <li> Access from JSNI to a field whose type is long.
 * <li> Access from JSNI to a method with a parameter or return type of long.
 * </ul>
 * All tests also apply for arrays of longs, arrays of arrays of longs, etc.
 */
public class LongFromJSNIChecker {
  private class CheckingVisitor extends ASTVisitor implements
      ClassFileConstants {
    @Override
    public void endVisit(MethodDeclaration meth, ClassScope scope) {
      if (meth.isNative() && !suppressingWarnings(meth, scope)) {
        // check return type
        final TypeReference returnType = meth.returnType;
        if (containsLong(returnType, scope)) {
          warn(meth, "JSNI method with return type of " + returnType);
        }

        // check parameter types
        if (meth.arguments != null) {
          for (Argument arg : meth.arguments) {
            if (containsLong(arg.type, scope)) {
              warn(arg, "JSNI method with a parameter of type " + arg.type);
            }
          }
        }

        // check JSNI references
        FindJsniRefVisitor jsniRefsVisitor = new FindJsniRefVisitor();
        meth.traverse(jsniRefsVisitor, scope);
        Set<String> jsniRefs = jsniRefsVisitor.getJsniRefs();

        for (String jsniRefString : jsniRefs) {
          JsniRef jsniRef = JsniRef.parse(jsniRefString);
          if (hasLongParam(jsniRef)) {
            warn(meth, "Passing a long into Java from JSNI (method "
                + jsniRef.memberName() + ")");
          }
          JType jsniRefType = getType(jsniRef);
          if (containsLong(jsniRefType)) {
            if (jsniRef.isMethod()) {
              warn(meth, "Method " + jsniRef.memberName() + " returns type "
                  + jsniRefType + ", which cannot be processed in JSNI code");
            } else {
              warn(meth, "Field " + jsniRef.memberName() + " has type "
                  + jsniRefType + ", which cannot be processed in JSNI code");
            }
          }
        }
      }
    }

    private boolean containsLong(JType type) {
      if (type != null && type.isArray() != null) {
        return containsLong(type.isArray().getLeafType());
      }
      return type == JPrimitiveType.LONG;
    }

    /**
     * Check whether the argument type is long or an array of (arrays of...)
     * long. If the argument is <code>null</code>, returns <code>false</code>.
     */
    private boolean containsLong(TypeBinding type) {
      if (type instanceof BaseTypeBinding) {
        BaseTypeBinding btb = (BaseTypeBinding) type;
        if (btb.id == TypeIds.T_long) {
          return true;
        }
      }

      if (type instanceof ArrayBinding) {
        ArrayBinding ab = (ArrayBinding) type;
        if (containsLong(ab.elementsType())) {
          return true;
        }
      }

      return false;
    }

    private boolean containsLong(final TypeReference returnType,
        ClassScope scope) {
      return returnType != null && containsLong(returnType.resolveType(scope));
    }

    /**
     * Returns either the type returned if this reference is "read". For a
     * field, returns the field's type. For a method, returns the method's
     * return type. If the reference cannot be resolved, returns null.
     */
    private JType getType(JsniRef jsniRef) {
      JClassType type = typeOracle.findType(jsniRef.className());
      if (type == null) {
        return null;
      }

      if (jsniRef.isMethod()) {
        for (JMethod method : type.getMethods()) {
          if (paramTypesMatch(method, jsniRef)) {
            return method.getReturnType();
          }
        }
        // no method matched
        return null;
      } else {
        JField field = type.getField(jsniRef.memberName());
        if (field != null) {
          return field.getType();
        }
        // field not found
        return null;
      }
    }

    private boolean hasLongParam(JsniRef jsniRef) {
      if (!jsniRef.isMethod()) {
        return false;
      }
      for (String type : jsniRef.paramTypes()) {
        if (type.charAt(type.length() - 1) == JniConstants.DESC_LONG) {
          return true;
        }
      }
      return false;
    }

    private boolean paramTypesMatch(JMethod method, JsniRef jsniRef) {
      JsniRef methodJsni = JsniRef.parse(Jsni.getJsniSignature(method));
      return methodJsni.equals(jsniRef);
    }

    private boolean suppressingWarnings(MethodDeclaration meth, ClassScope scope) {
      CompilationResult result = scope.referenceCompilationUnit().compilationResult;
      long[] suppressWarningIrritants;
      long[] suppressWarningScopePositions; // (start << 32) + end
      int suppressWarningsCount;

      try {
        {
          Field field = CompilationResult.class.getDeclaredField("suppressWarningIrritants");
          field.setAccessible(true);
          suppressWarningIrritants = (long[]) field.get(result);
        }
        {
          Field field = CompilationResult.class.getDeclaredField("suppressWarningScopePositions");
          field.setAccessible(true);
          suppressWarningScopePositions = (long[]) field.get(result);
        }
        {
          Field field = CompilationResult.class.getDeclaredField("suppressWarningsCount");
          field.setAccessible(true);
          suppressWarningsCount = (Integer) field.get(result);
        }
      } catch (NoSuchFieldException e) {
        throw new InternalCompilerException(
            "Failed to read suppress warnings data from JDT", e);
      } catch (IllegalAccessException e) {
        throw new InternalCompilerException(
            "Failed to read suppress warnings data from JDT", e);
      }

      for (int i = 0; i < suppressWarningsCount; i++) {
        if ((suppressWarningIrritants[i] & CompilerOptions.DiscouragedReference) != 0) {
          long start = suppressWarningScopePositions[i] >> 32;
          long end = suppressWarningScopePositions[i] & 0xFFFFFFFF;
          if (meth.bodyStart >= start && meth.bodyStart <= end) {
            return true;
          }
        }
      }

      return false;
    }

    private void warn(ASTNode node, String message) {
      CompilationResult compResult = cud.compilationResult();
      int[] lineEnds = compResult.getLineSeparatorPositions();
      int startLine = Util.getLineNumber(node.sourceStart(), lineEnds, 0,
          lineEnds.length - 1);
      String fileName = String.valueOf(cud.getFileName());
      logger.log(TreeLogger.WARN, fileName + "(" + startLine + "): " + message,
          null);
    }
  }

  /**
   * Checks an entire
   * {@link org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration}.
   * 
   */
  public static void check(TypeOracle typeOracle,
      CompilationUnitDeclaration cud, TreeLogger logger) {
    LongFromJSNIChecker checker = new LongFromJSNIChecker(typeOracle, cud,
        logger);
    checker.check();
  }

  private final CompilationUnitDeclaration cud;
  private final TreeLogger logger;
  private final TypeOracle typeOracle;

  private LongFromJSNIChecker(TypeOracle typeOracle,
      CompilationUnitDeclaration cud, TreeLogger logger) {
    this.typeOracle = typeOracle;
    this.cud = cud;
    this.logger = logger;
  }

  private void check() {
    cud.traverse(new CheckingVisitor(), cud.scope);
  }
}
