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
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.util.InstalledHelpInfo;
import com.google.gwt.dev.util.JsniRef;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
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

import java.util.Iterator;
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
        Set<String> errors, Map<String, Set<String>> warnings) {
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
      if (target.isDeprecated()) {
        add(warnings, "deprecation", "Referencing deprecated field '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'");
      }
    }

    private void checkMethodRef(ReferenceBinding clazz, JsniRef jsniRef,
        Set<String> errors, Map<String, Set<String>> warnings) {
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

      if (target.isDeprecated()) {
        add(warnings, "deprecation", "Referencing deprecated method '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'");
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
        Map<String, Set<String>> warnings = new LinkedHashMap<String, Set<String>>();

        if (jsniRef != null) {
          ReferenceBinding clazz = findClass(jsniRef);
          if (looksLikeAnonymousClass(jsniRef)
              || (clazz != null && clazz.isAnonymousType())) {
            add(warnings, "deprecation", "Referencing class '"
                + jsniRef.className()
                + ": JSNI references to anonymous classes are deprecated");

          } else if (clazz != null) {
            if (clazz.isDeprecated()) {
              add(warnings, "deprecation", "Referencing deprecated class '"
                  + jsniRef.className() + "'");
            }

            Set<String> refErrors = new LinkedHashSet<String>();
            if (jsniRef.isMethod()) {
              checkMethodRef(clazz, jsniRef, refErrors, warnings);
            } else {
              checkFieldRef(clazz, jsniRef, refErrors, warnings);
            }
            if (!refErrors.isEmpty()) {
              errors.put(jsniRefString, refErrors);
            }
          } else if (!jsniRef.className().equals("null")) {
            /*
             * TODO(scottb): re-enable this when we no longer get a bunch of
             * false failures. Currently we can't resolve top level types (like
             * boolean_Array_Rank_1_FieldSerializer), and we also don't resolve
             * array and primitive refs, like @Z[]::class.
             */
            // GWTProblem.recordInCud(ProblemSeverities.Warning, meth, cud,
            // "Referencing class '" + jsniRef.className()
            // + ": unable to resolve class, expect subsequent failures",
            // null);
          }
        }

        filterWarnings(meth, warnings);
        for (Set<String> set : warnings.values()) {
          for (String warning : set) {
            GWTProblem.recordProblem(meth, cud.compilationResult(), warning,
                null, ProblemSeverities.Warning);
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
     * Check whether the argument type is the <code>long</code> primitive type.
     * If the argument is <code>null</code>, returns <code>false</code>.
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

    /**
     * Given a MethodDeclaration and a map of warnings, filter those warning
     * messages that are keyed with a suppressed type.
     */
    private void filterWarnings(MethodDeclaration method,
        Map<String, Set<String>> warnings) {
      Annotation[] annotations = method.annotations;
      if (annotations == null) {
        return;
      }

      for (Annotation a : annotations) {
        if (SuppressWarnings.class.getName().equals(
            CharOperation.toString(((ReferenceBinding) a.resolvedType).compoundName))) {
          for (MemberValuePair pair : a.memberValuePairs()) {
            if (String.valueOf(pair.name).equals("value")) {
              String[] values;
              Expression valueExpr = pair.value;

              if (valueExpr instanceof StringLiteral) {
                // @SuppressWarnings("Foo")
                values = new String[] {((StringLiteral) valueExpr).constant.stringValue()};

              } else if (valueExpr instanceof ArrayInitializer) {
                // @SuppressWarnings({ "Foo", "Bar"})
                ArrayInitializer ai = (ArrayInitializer) valueExpr;
                values = new String[ai.expressions.length];
                for (int i = 0, j = values.length; i < j; i++) {
                  values[i] = ((StringLiteral) ai.expressions[i]).constant.stringValue();
                }
              } else {
                throw new InternalCompilerException(
                    "Unable to analyze SuppressWarnings annotation");
              }

              for (String value : values) {
                for (Iterator<String> it = warnings.keySet().iterator(); it.hasNext();) {
                  if (it.next().toLowerCase().equals(value.toLowerCase())) {
                    it.remove();
                  }
                }
              }
              return;
            }
          }
        }
      }
    }

    private ReferenceBinding findClass(JsniRef jsniRef) {
      char[][] compoundName = getCompoundName(jsniRef);
      TypeBinding binding = cud.scope.getType(compoundName, compoundName.length);

      /*
       * TODO(scottb): we cannot currently resolve top-level types; here's some
       * experimental code that will let us do this.
       */
      // ReferenceBinding binding = cud.scope.environment().askForType(
      // compoundName);
      // while (binding == null && compoundName.length > 1) {
      // int newLen = compoundName.length - 1;
      // char[][] next = new char[newLen][];
      // System.arraycopy(compoundName, 0, next, 0, newLen - 1);
      // next[newLen - 1] = CharOperation.concat(compoundName[newLen - 1],
      // compoundName[newLen], '$');
      // compoundName = next;
      // binding = cud.scope.environment().askForType(compoundName);
      // }
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
      GWTProblem.recordError(node, cud, message, new InstalledHelpInfo(
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

  /**
   * Adds an entry to a map of sets.
   */
  private static <K, V> void add(Map<K, Set<V>> map, K key, V value) {
    Set<V> set = map.get(key);
    if (set == null) {
      set = new LinkedHashSet<V>();
      map.put(key, set);
    }
    set.add(value);
  }

  private final CompilationUnitDeclaration cud;

  private JsniChecker(CompilationUnitDeclaration cud) {
    this.cud = cud;
  }

  private void check() {
    cud.traverse(new CheckingVisitor(), cud.scope);
  }
}
