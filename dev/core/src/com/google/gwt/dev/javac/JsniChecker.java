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
package com.google.gwt.dev.javac;

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.dev.jdt.FindJsniRefVisitor;
import com.google.gwt.dev.util.InstalledHelpInfo;
import com.google.gwt.dev.util.JsniRef;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests for access to Java from JSNI. Issues a warning for:
 * <ul>
 * <li>JSNI methods with a parameter or return type of long.</li>
 * <li>Access from JSNI to a field whose type is long.</li>
 * <li>Access from JSNI to a method with a parameter or return type of long.</li>
 * <li>JSNI references to anonymous classes.</li>
 * </ul>
 * All tests also apply for arrays of longs, arrays of arrays of longs, etc.
 */
public class JsniChecker {
  private class CheckingVisitor extends ASTVisitor implements
      ClassFileConstants {
    @Override
    public void endVisit(MethodDeclaration meth, ClassScope scope) {
      if (meth.isNative() && !hasUnsafeLongsAnnotation(meth, scope)) {
        checkDecl(meth, scope);
        checkRefs(meth, scope);
      }
    }

    private void checkDecl(MethodDeclaration meth, ClassScope scope) {
      TypeReference returnType = meth.returnType;
      if (containsLong(returnType, scope)) {
        longAccessError(meth, "Type '" + typeString(returnType)
            + "' may not be returned from a JSNI method");
      }

      if (meth.arguments != null) {
        for (Argument arg : meth.arguments) {
          if (containsLong(arg.type, scope)) {
            longAccessError(arg, "Parameter '" + String.valueOf(arg.name)
                + "': type '" + typeString(arg.type)
                + "' is not safe to access in JSNI code");
          }
        }
      }
    }

    private void checkFieldRef(ReferenceBinding clazz, JsniRef jsniRef,
        Set<String> errors) {
      assert jsniRef.isField();
      FieldBinding target = getField(clazz, jsniRef);
      if (target == null) {
        return;
      }
      if (containsLong(target.type)) {
        errors.add("Referencing field '" + jsniRef.className() + "."
            + jsniRef.memberName() + "': type '" + typeString(target.type)
            + "' is not safe to access in JSNI code");
      }
    }

    private void checkMethodRef(ReferenceBinding clazz, JsniRef jsniRef,
        Set<String> errors) {
      assert jsniRef.isMethod();
      MethodBinding target = getMethod(clazz, jsniRef);
      if (target == null) {
        return;
      }
      if (containsLong(target.returnType)) {
        errors.add("Referencing method '" + jsniRef.className() + "."
            + jsniRef.memberName() + "': return type '"
            + typeString(target.returnType)
            + "' is not safe to access in JSNI code");
      }

      if (target.parameters != null) {
        int i = 0;
        for (TypeBinding paramType : target.parameters) {
          ++i;
          if (containsLong(paramType)) {
            // It would be nice to print the parameter name, but how to find it?
            errors.add("Parameter " + i + " of method '" + jsniRef.className()
                + "." + jsniRef.memberName() + "': type '"
                + typeString(paramType)
                + "' may not be passed out of JSNI code");
          }
        }
      }
    }

    private void checkRefs(MethodDeclaration meth, ClassScope scope) {
      Map<String, Set<String>> errors = new LinkedHashMap<String, Set<String>>();

      // first do a sloppy parse, for speed

      FindJsniRefVisitor sloppyRefsVisitor = new FindJsniRefVisitor();
      sloppyRefsVisitor.beSloppy();
      meth.traverse(sloppyRefsVisitor, scope);

      for (String jsniRefString : sloppyRefsVisitor.getJsniRefs()) {
        JsniRef jsniRef = JsniRef.parse(jsniRefString);
        if (jsniRef != null) {
          ReferenceBinding clazz = findClass(jsniRef);
          if (looksLikeAnonymousClass(jsniRef)
              || (clazz != null && clazz.isAnonymousType())) {
            GWTProblem.recordInCud(ProblemSeverities.Warning, meth, cud,
                "Referencing class '" + jsniRef.className()
                    + ": JSNI references to anonymous classes are deprecated",
                null);
          } else if (clazz != null) {
            Set<String> refErrors = new LinkedHashSet<String>();
            if (jsniRef.isMethod()) {
              checkMethodRef(clazz, jsniRef, refErrors);
            } else {
              checkFieldRef(clazz, jsniRef, refErrors);
            }
            if (!refErrors.isEmpty()) {
              errors.put(jsniRefString, refErrors);
            }
          } else if (!jsniRef.className().equals("null")) {
            GWTProblem.recordInCud(ProblemSeverities.Warning, meth, cud,
                "Referencing class '" + jsniRef.className()
                    + ": unable to resolve class, expect subsequent failures",
                null);
          }
        }
      }

      if (!errors.isEmpty()) {
        // do a strict parse to find out which JSNI refs are real
        FindJsniRefVisitor jsniRefsVisitor = new FindJsniRefVisitor();
        meth.traverse(jsniRefsVisitor, scope);
        for (String jsniRefString : jsniRefsVisitor.getJsniRefs()) {
          if (errors.containsKey(jsniRefString)) {
            for (String err : errors.get(jsniRefString)) {
              longAccessError(meth, err);
            }
          }
        }
      }
    }

    /**
     * Check whether the argument type is the <code>long</code> primitive
     * type. If the argument is <code>null</code>, returns <code>false</code>.
     */
    private boolean containsLong(TypeBinding type) {
      if (type instanceof BaseTypeBinding) {
        BaseTypeBinding btb = (BaseTypeBinding) type;
        if (btb.id == TypeIds.T_long) {
          return true;
        }
      }

      return false;
    }

    private boolean containsLong(final TypeReference returnType,
        ClassScope scope) {
      return returnType != null && containsLong(returnType.resolveType(scope));
    }

    private ReferenceBinding findClass(JsniRef jsniRef) {
      char[][] compoundName = getCompoundName(jsniRef);
      TypeBinding binding = cud.scope.getType(compoundName, compoundName.length);

      if (binding instanceof ProblemReferenceBinding) {
        ProblemReferenceBinding prb = (ProblemReferenceBinding) binding;
        if (prb.problemId() == ProblemReasons.NotVisible) {
          // It's just a visibility problem, so try drilling down manually
          ReferenceBinding drilling = prb.closestReferenceMatch();
          for (int i = prb.compoundName.length; i < compoundName.length; i++) {
            drilling = drilling.getMemberType(compoundName[i]);
          }
          binding = drilling;
        }
      }

      if (binding instanceof ReferenceBinding
          && !(binding instanceof ProblemReferenceBinding)) {
        return (ReferenceBinding) binding;
      }
      return null;
    }

    private char[][] getCompoundName(JsniRef jsniRef) {
      String className = jsniRef.className().replace('$', '.');
      char[][] compoundName = CharOperation.splitOn('.',
          className.toCharArray());
      return compoundName;
    }

    private FieldBinding getField(ReferenceBinding clazz, JsniRef jsniRef) {
      assert jsniRef.isField();
      return clazz.getField(jsniRef.memberName().toCharArray(), false);
    }

    private MethodBinding getMethod(ReferenceBinding clazz, JsniRef jsniRef) {
      assert jsniRef.isMethod();
      for (MethodBinding method : clazz.getMethods(jsniRef.memberName().toCharArray())) {
        if (paramTypesMatch(method, jsniRef)) {
          return method;
        }
      }
      return null;
    }

    private boolean hasUnsafeLongsAnnotation(MethodDeclaration meth,
        ClassScope scope) {
      if (meth.annotations != null) {
        for (Annotation annot : meth.annotations) {
          if (isUnsafeLongAnnotation(annot, scope)) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean isUnsafeLongAnnotation(Annotation annot, ClassScope scope) {
      if (annot.type != null) {
        TypeBinding resolved = annot.type.resolveType(scope);
        if (resolved != null) {
          if (resolved instanceof ReferenceBinding) {
            ReferenceBinding rb = (ReferenceBinding) resolved;
            if (CharOperation.equals(rb.compoundName,
                UNSAFE_LONG_ANNOTATION_CHARS)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    private void longAccessError(ASTNode node, String message) {
      GWTProblem.recordInCud(node, cud, message, new InstalledHelpInfo(
          "longJsniRestriction.html"));
    }

    private boolean looksLikeAnonymousClass(JsniRef jsniRef) {
      char[][] compoundName = getCompoundName(jsniRef);
      for (char[] part : compoundName) {
        if (Character.isDigit(part[0])) {
          return true;
        }
      }
      return false;
    }

    private boolean paramTypesMatch(MethodBinding method, JsniRef jsniRef) {
      StringBuilder methodSig = new StringBuilder();
      if (method.parameters != null) {
        for (TypeBinding binding : method.parameters) {
          methodSig.append(binding.signature());
        }
      }
      return methodSig.toString().equals(jsniRef.paramTypesString());
    }

    private String typeString(TypeBinding type) {
      return String.valueOf(type.shortReadableName());
    }

    private String typeString(TypeReference type) {
      return type.toString();
    }
  }

  private static final char[][] UNSAFE_LONG_ANNOTATION_CHARS = CharOperation.splitOn(
      '.', UnsafeNativeLong.class.getName().toCharArray());

  /**
   * Checks an entire
   * {@link org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration}.
   * 
   */
  public static void check(CompilationUnitDeclaration cud) {
    JsniChecker checker = new JsniChecker(cud);
    checker.check();
  }

  private final CompilationUnitDeclaration cud;

  private JsniChecker(CompilationUnitDeclaration cud) {
    this.cud = cud;
  }

  private void check() {
    cud.traverse(new CheckingVisitor(), cud.scope);
  }
}
