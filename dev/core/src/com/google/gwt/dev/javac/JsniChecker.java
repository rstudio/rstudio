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
import com.google.gwt.dev.javac.JSORestrictionsChecker.CheckerState;
import com.google.gwt.dev.jdt.SafeASTVisitor;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.InstalledHelpInfo;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.collect.Sets;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.NestedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticArgumentBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.UnresolvedReferenceBinding;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

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

  /**
   * A call-back interface to resolve types.
   */
  public interface TypeResolver {
    ReferenceBinding resolveType(String typeName);
  }

  private class JsniDeclChecker extends SafeASTVisitor implements
      ClassFileConstants {
    @Override
    public void endVisit(MethodDeclaration meth, ClassScope scope) {
      if (meth.isNative()) {
        boolean hasUnsafeLongsAnnotation = hasUnsafeLongsAnnotation(meth, scope);
        if (!hasUnsafeLongsAnnotation) {
          checkDecl(meth, scope);
        }
        JsniMethod jsniMethod = jsniMethods.get(meth);
        if (jsniMethod != null) {
          new JsniRefChecker(meth, hasUnsafeLongsAnnotation).check(jsniMethod.function());
        }
      }
      suppressWarningsStack.pop();
    }

    @Override
    public void endVisit(TypeDeclaration typeDeclaration, ClassScope scope) {
      suppressWarningsStack.pop();
    }

    @Override
    public void endVisit(TypeDeclaration typeDeclaration,
        CompilationUnitScope scope) {
      suppressWarningsStack.pop();
    }

    @Override
    public void endVisitValid(TypeDeclaration typeDeclaration, BlockScope scope) {
      suppressWarningsStack.pop();
    }

    @Override
    public boolean visit(MethodDeclaration meth, ClassScope scope) {
      suppressWarningsStack.push(getSuppressedWarnings(meth.annotations));
      return true;
    }

    @Override
    public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
      suppressWarningsStack.push(getSuppressedWarnings(typeDeclaration.annotations));
      return true;
    }

    @Override
    public boolean visit(TypeDeclaration typeDeclaration,
        CompilationUnitScope scope) {
      suppressWarningsStack.push(getSuppressedWarnings(typeDeclaration.annotations));
      return true;
    }

    @Override
    public boolean visitValid(TypeDeclaration typeDeclaration, BlockScope scope) {
      suppressWarningsStack.push(getSuppressedWarnings(typeDeclaration.annotations));
      return true;
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

    private boolean containsLong(final TypeReference type, ClassScope scope) {
      return type != null
          && JsniChecker.this.containsLong(type.resolveType(scope));
    }

    private String typeString(TypeReference type) {
      return type.toString();
    }
  }

  private class JsniRefChecker extends JsVisitor {

    private transient SourceInfo errorInfo;
    private final boolean hasUnsafeLongsAnnotation;
    private final MethodDeclaration method;

    public JsniRefChecker(MethodDeclaration method,
        boolean hasUnsafeLongsAnnotation) {
      this.method = method;
      this.hasUnsafeLongsAnnotation = hasUnsafeLongsAnnotation;
    }

    public void check(JsFunction function) {
      this.accept(function);
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      this.errorInfo = x.getSourceInfo();
      String ident = x.getIdent();
      if (ident.charAt(0) == '@') {
        JsniRef jsniRef = JsniRef.parse(ident);
        if (jsniRef == null) {
          emitError("Malformed JSNI identifier '" + ident + "'");
        } else {
          Binding binding = checkRef(jsniRef, x.getQualifier() != null,
              ctx.isLvalue());
          if (binding != null) {
            jsniRefs.put(ident, binding);
          }
        }
      }
      this.errorInfo = null;
    }

    private FieldBinding checkFieldRef(ReferenceBinding clazz, JsniRef jsniRef,
        boolean hasQualifier, boolean isLvalue) {
      assert jsniRef.isField();
      FieldBinding target = getField(clazz, jsniRef);
      if (target == null) {
        emitError("Referencing field '" + jsniRef.className() + "."
            + jsniRef.memberName() + "': unable to resolve field");
        return null;
      }
      if (target.isDeprecated()) {
        emitWarning("deprecation",
            "Referencing deprecated field '" + jsniRef.className() + "."
                + jsniRef.memberName() + "'");
      }
      if (isLvalue && target.constant() != Constant.NotAConstant) {
        emitError("Illegal assignment to compile-time constant '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'");
      }
      if (target.isStatic() && hasQualifier) {
        emitError("Unnecessary qualifier on static field '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'");
      } else if (!target.isStatic() && !hasQualifier) {
        emitError("Missing qualifier on instance field '" + jsniRef.className()
            + "." + jsniRef.memberName() + "'");
      }

      if (hasUnsafeLongsAnnotation) {
        return target;
      }
      if (containsLong(target.type)) {
        emitError("Referencing field '" + jsniRef.className() + "."
            + jsniRef.memberName() + "': type '" + typeString(target.type)
            + "' is not safe to access in JSNI code");
      }
      return target;
    }

    private MethodBinding checkMethodRef(ReferenceBinding clazz,
        JsniRef jsniRef, boolean hasQualifier, boolean isLvalue) {
      assert jsniRef.isMethod();
      MethodBinding target = getMethod(clazz, jsniRef);
      if (target == null) {
        emitError("Referencing method '" + jsniRef.className() + "."
            + jsniRef.memberSignature() + "': unable to resolve method");
        return null;
      }
      if (target.isDeprecated()) {
        emitWarning("deprecation",
            "Referencing deprecated method '" + jsniRef.className() + "."
                + jsniRef.memberName() + "'");
      }
      if (isLvalue) {
        emitError("Illegal assignment to method '" + jsniRef.className() + "."
            + jsniRef.memberName() + "'");
      }
      boolean needsQualifer = !target.isStatic() && !target.isConstructor();
      if (!needsQualifer && hasQualifier) {
        emitError("Unnecessary qualifier on static method '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'");
      } else if (needsQualifer && !hasQualifier) {
        emitError("Missing qualifier on instance method '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'");
      }
      if (!target.isStatic() && JSORestrictionsChecker.isJso(clazz)) {
        emitError("Referencing method '" + jsniRef.className() + "."
            + jsniRef.memberSignature()
            + "': references to instance methods in overlay types are illegal");
      }
      if (checkerState.isJsoInterface(clazz)) {
        String implementor = checkerState.getJsoImplementor(clazz);
        emitError("Referencing interface method '" + jsniRef.className() + "."
            + jsniRef.memberSignature() + "': implemented by '" + implementor
            + "'; references to instance methods in overlay types are illegal"
            + "; use a stronger type or a Java trampoline method");
      }

      if (hasUnsafeLongsAnnotation) {
        return target;
      }
      if (containsLong(target.returnType)) {
        emitError("Referencing method '" + jsniRef.className() + "."
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
            emitError("Parameter " + i + " of method '" + jsniRef.className()
                + "." + jsniRef.memberName() + "': type '"
                + typeString(paramType)
                + "' may not be passed out of JSNI code");
          }
        }
      }
      return target;
    }

    private Binding checkRef(JsniRef jsniRef, boolean hasQualifier,
        boolean isLvalue) {
      String className = jsniRef.className();
      if ("null".equals(className)) {
        if (jsniRef.isField()) {
          if (!"nullField".equals(jsniRef.memberName())) {
            emitError("Referencing field '" + jsniRef.className() + "."
                + jsniRef.memberName()
                + "': 'nullField' is the only legal field reference for 'null'");
          }
        } else {
          if (!"nullMethod()".equals(jsniRef.memberSignature())) {
            emitError("Referencing method '" + jsniRef.className() + "."
                + jsniRef.memberSignature()
                + "': 'nullMethod()' is the only legal method for 'null'");
          }
          return null;
        }
        return null;
      }

      boolean isArray = false;
      int dims = 0;
      while (className.endsWith("[]")) {
        ++dims;
        isArray = true;
        className = className.substring(0, className.length() - 2);
      }

      boolean isPrimitive;
      ReferenceBinding clazz;
      TypeBinding binding = method.scope.getBaseType(className.toCharArray());
      if (binding != null) {
        isPrimitive = true;
        clazz = null;
      } else {
        isPrimitive = false;
        binding = clazz = findClass(className);
      }

      // TODO(deprecation): remove this support eventually.
      if (binding == null && className.length() == 1
          && "ZBCDFIJSV".indexOf(className.charAt(0)) >= 0) {
        isPrimitive = true;
        binding = getTypeBinding(className.charAt(0));
        assert binding != null;
        JsniCollector.reportJsniWarning(
            errorInfo,
            method,
            "Referencing primitive type '" + className
                + "': this is deprecated, use '"
                + String.valueOf(binding.sourceName()) + "' instead");
      }

      if ((binding == null && looksLikeAnonymousClass(jsniRef))
          || (binding != null && binding.isAnonymousType())) {
        emitError("Referencing class '" + className
            + "': JSNI references to anonymous classes are illegal");
        return null;
      } else if (binding == null) {
        emitError("Referencing class '" + className
            + "': unable to resolve class");
        return null;
      }

      if (clazz != null && clazz.isDeprecated()) {
        emitWarning("deprecation", "Referencing deprecated class '" + className
            + "'");
      }

      if (jsniRef.isField() && "class".equals(jsniRef.memberName())) {
        if (isLvalue) {
          emitError("Illegal assignment to class literal '"
              + jsniRef.className() + ".class'");
          return null;
        }
        // Reference to the class itself.
        if (isArray) {
          return method.scope.createArrayType(binding, dims);
        } else {
          return binding;
        }
      }

      if (isArray || isPrimitive) {
        emitError("Referencing member '" + jsniRef.className() + "."
            + jsniRef.memberName()
            + "': 'class' is the only legal reference for "
            + (isArray ? "array" : "primitive") + " types");
        return null;
      }

      assert clazz != null;
      if (jsniRef.isMethod()) {
        return checkMethodRef(clazz, jsniRef, hasQualifier, isLvalue);
      } else {
        return checkFieldRef(clazz, jsniRef, hasQualifier, isLvalue);
      }
    }

    private void emitError(String msg) {
      JsniCollector.reportJsniError(errorInfo, method, msg);
    }

    private void emitWarning(String category, String msg) {
      for (Set<String> suppressWarnings : suppressWarningsStack) {
        if (suppressWarnings.contains(category)
            || suppressWarnings.contains("all")) {
          return;
        }
      }
      JsniCollector.reportJsniWarning(errorInfo, method, msg);
    }

    private ReferenceBinding findClass(String className) {
      ReferenceBinding binding = typeResolver.resolveType(className);
      assert !(binding instanceof ProblemReferenceBinding);
      assert !(binding instanceof UnresolvedReferenceBinding);
      return binding;
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
      String methodName = jsniRef.memberName();
      if ("new".equals(methodName)) {
        for (MethodBinding findMethod : clazz.getMethods(INIT_CTOR_CHARS)) {
          StringBuilder methodSig = new StringBuilder();
          if (clazz instanceof NestedTypeBinding) {
            // Match synthetic args for enclosing instances.
            NestedTypeBinding nestedBinding = (NestedTypeBinding) clazz;
            if (nestedBinding.enclosingInstances != null) {
              for (int i = 0; i < nestedBinding.enclosingInstances.length; ++i) {
                SyntheticArgumentBinding arg = nestedBinding.enclosingInstances[i];
                methodSig.append(arg.type.signature());
              }
            }
          }
          if (findMethod.parameters != null) {
            for (TypeBinding binding : findMethod.parameters) {
              methodSig.append(binding.signature());
            }
          }
          if (methodSig.toString().equals(jsniRef.paramTypesString())) {
            return findMethod;
          }
        }
      } else {
        Queue<ReferenceBinding> work = new LinkedList<ReferenceBinding>();
        work.add(clazz);
        while (!work.isEmpty()) {
          clazz = work.remove();
          for (MethodBinding findMethod : clazz.getMethods(methodName.toCharArray())) {
            if (paramTypesMatch(findMethod, jsniRef)) {
              return findMethod;
            }
          }
          ReferenceBinding[] superInterfaces = clazz.superInterfaces();
          if (superInterfaces != null) {
            work.addAll(Arrays.asList(superInterfaces));
          }
          ReferenceBinding superclass = clazz.superclass();
          if (superclass != null) {
            work.add(superclass);
          }
        }
      }
      return null;
    }

    @Deprecated
    private TypeBinding getTypeBinding(char c) {
      switch (c) {
        case 'I':
          return TypeBinding.INT;
        case 'Z':
          return TypeBinding.BOOLEAN;
        case 'V':
          return TypeBinding.VOID;
        case 'C':
          return TypeBinding.CHAR;
        case 'D':
          return TypeBinding.DOUBLE;
        case 'B':
          return TypeBinding.BYTE;
        case 'F':
          return TypeBinding.FLOAT;
        case 'J':
          return TypeBinding.LONG;
        case 'S':
          return TypeBinding.SHORT;
        default:
          return null;
      }
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
      if (jsniRef.matchesAnyOverload()) {
        return true;
      }
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
  }

  private static final char[] INIT_CTOR_CHARS = "<init>".toCharArray();

  private static final char[][] UNSAFE_LONG_ANNOTATION_CHARS = CharOperation.splitOn(
      '.', UnsafeNativeLong.class.getName().toCharArray());

  /**
   * Checks an entire
   * {@link org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration}.
   * 
   */
  public static void check(CompilationUnitDeclaration cud,
      CheckerState checkerState,
      Map<MethodDeclaration, JsniMethod> jsniMethods,
      Map<String, Binding> jsniRefs, TypeResolver typeResolver) {
    new JsniChecker(cud, checkerState, typeResolver, jsniMethods, jsniRefs).check();
  }

  static Set<String> getSuppressedWarnings(Annotation[] annotations) {
    if (annotations != null) {
      for (Annotation a : annotations) {
        if (SuppressWarnings.class.getName().equals(
            CharOperation.toString(((ReferenceBinding) a.resolvedType).compoundName))) {
          for (MemberValuePair pair : a.memberValuePairs()) {
            if (String.valueOf(pair.name).equals("value")) {
              Expression valueExpr = pair.value;
              if (valueExpr instanceof StringLiteral) {
                // @SuppressWarnings("Foo")
                return Sets.create(((StringLiteral) valueExpr).constant.stringValue().toLowerCase(
                    Locale.ENGLISH));
              } else if (valueExpr instanceof ArrayInitializer) {
                // @SuppressWarnings({ "Foo", "Bar"})
                ArrayInitializer ai = (ArrayInitializer) valueExpr;
                String[] values = new String[ai.expressions.length];
                for (int i = 0, j = values.length; i < j; i++) {
                  values[i] = ((StringLiteral) ai.expressions[i]).constant.stringValue().toLowerCase(
                      Locale.ENGLISH);
                }
                return Sets.create(values);
              } else {
                throw new InternalCompilerException(
                    "Unable to analyze SuppressWarnings annotation");
              }
            }
          }
        }
      }
    }
    return Sets.create();
  }

  private final CheckerState checkerState;
  private final CompilationUnitDeclaration cud;
  private final Map<MethodDeclaration, JsniMethod> jsniMethods;
  private final Map<String, Binding> jsniRefs;
  private final Stack<Set<String>> suppressWarningsStack = new Stack<Set<String>>();
  private final TypeResolver typeResolver;

  private JsniChecker(CompilationUnitDeclaration cud,
      CheckerState checkerState, TypeResolver typeResolver,
      Map<MethodDeclaration, JsniMethod> jsniMethods,
      Map<String, Binding> jsniRefs) {
    this.checkerState = checkerState;
    this.cud = cud;
    this.typeResolver = typeResolver;
    this.jsniMethods = jsniMethods;
    this.jsniRefs = jsniRefs;
  }

  private void check() {
    // First check the declarations.
    cud.traverse(new JsniDeclChecker(), cud.scope);
  }

  /**
   * Check whether the argument type is the <code>long</code> primitive type. If
   * the argument is <code>null</code>, returns <code>false</code>.
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
}
