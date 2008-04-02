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
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
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
        checkDecl(meth, scope);
        checkRefs(meth, scope);
      }
    }

    private void checkDecl(MethodDeclaration meth, ClassScope scope) {
      TypeReference returnType = meth.returnType;
      if (containsLong(returnType, scope)) {
        warn(meth, "Return value of type '" + returnType
            + "' is an opaque, non-numeric value in JS code");
      }

      if (meth.arguments != null) {
        for (Argument arg : meth.arguments) {
          if (containsLong(arg.type, scope)) {
            warn(arg, "Parameter '" + String.valueOf(arg.name) + "': '"
                + arg.type + "' is an opaque, non-numeric value in JS code");
          }
        }
      }
    }

    private void checkFieldRef(MethodDeclaration meth, JsniRef jsniRef) {
      assert jsniRef.isField();
      JField target = getField(jsniRef);
      if (target == null) {
        return;
      }
      if (containsLong(target.getType())) {
        warn(meth, "Referencing field '"
            + target.getEnclosingType().getSimpleSourceName() + "."
            + target.getName() + "': '" + target.getType()
            + "' is an opaque, non-numeric value in JS code");
      }
    }

    private void checkMethodRef(MethodDeclaration meth, JsniRef jsniRef) {
      assert jsniRef.isMethod();
      JMethod target = getMethod(jsniRef);
      if (target == null) {
        return;
      }
      if (containsLong(target.getReturnType())) {
        warn(meth, "Referencing method '"
            + target.getEnclosingType().getSimpleSourceName() + "."
            + target.getName() + "': return type '" + target.getReturnType()
            + "' is an opaque, non-numeric value in JS code");
      }

      for (JParameter param : target.getParameters()) {
        if (containsLong(param.getType())) {
          warn(meth, "Referencing method '"
              + target.getEnclosingType().getSimpleSourceName() + "."
              + target.getName() + "': parameter '" + param.getName() + "': '"
              + param.getType()
              + "' is an opaque, non-numeric value in JS code");
        }
      }
    }

    private void checkRefs(MethodDeclaration meth, ClassScope scope) {
      FindJsniRefVisitor jsniRefsVisitor = new FindJsniRefVisitor();
      meth.traverse(jsniRefsVisitor, scope);
      Set<String> jsniRefs = jsniRefsVisitor.getJsniRefs();

      for (String jsniRefString : jsniRefs) {
        JsniRef jsniRef = JsniRef.parse(jsniRefString);
        if (jsniRef != null) {
          if (jsniRef.isMethod()) {
            checkMethodRef(meth, jsniRef);
          } else {
            checkFieldRef(meth, jsniRef);
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

    private JClassType findType(JsniRef jsniRef) {
      // Use source name.
      String className = jsniRef.className();
      className = className.replace('$', '.');
      JClassType type = typeOracle.findType(className);
      return type;
    }

    /**
     * Returns either the type returned if this reference is "read". For a
     * field, returns the field's type. For a method, returns the method's
     * return type. If the reference cannot be resolved, returns null.
     */
    private JField getField(JsniRef jsniRef) {
      assert jsniRef.isField();
      JClassType type = findType(jsniRef);
      if (type == null) {
        return null;
      }

      JField field = type.findField(jsniRef.memberName());
      if (field != null) {
        return field;
      }
      return null;
    }

    /**
     * Returns either the type returned if this reference is "read". For a
     * field, returns the field's type. For a method, returns the method's
     * return type. If the reference cannot be resolved, returns null.
     */
    private JMethod getMethod(JsniRef jsniRef) {
      assert jsniRef.isMethod();
      JClassType type = findType(jsniRef);
      if (type == null) {
        return null;
      }

      for (JMethod method : type.getMethods()) {
        if (paramTypesMatch(method, jsniRef)) {
          return method;
        }
      }
      return null;
    }

    private boolean paramTypesMatch(JMethod method, JsniRef jsniRef) {
      String methodSig = Jsni.getMemberSignature(method);
      return methodSig.equals(jsniRef.memberSignature());
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
