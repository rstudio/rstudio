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
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.InstalledHelpInfo;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.collect.Sets;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
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
import org.eclipse.jdt.internal.compiler.lookup.NestedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticArgumentBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.UnresolvedReferenceBinding;

import java.util.Locale;
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

  /**
   * A call-back interface to resolve types.
   */
  public interface TypeResolver {
    ReferenceBinding resolveType(String typeName);
  }

  private class JsniDeclChecker extends ASTVisitor implements
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
    private final Set<String> suppressWarnings;

    public JsniRefChecker(MethodDeclaration method,
        boolean hasUnsafeLongsAnnotation) {
      this.method = method;
      this.hasUnsafeLongsAnnotation = hasUnsafeLongsAnnotation;
      this.suppressWarnings = getSuppressedWarnings(method);
    }

    public void check(JsFunction function) {
      this.accept(function);
    }

    @Override
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
      this.errorInfo = x.getSourceInfo();
      String ident = x.getIdent();
      if (ident.charAt(0) == '@') {
        JsniRef jsniRef = JsniRef.parse(ident);
        if (jsniRef == null) {
          emitError("Malformed JSNI identifier '" + ident + "'");
        } else {
          checkRef(jsniRef);
        }
      }
      this.errorInfo = null;
    }

    private void checkFieldRef(ReferenceBinding clazz, JsniRef jsniRef) {
      assert jsniRef.isField();
      if ("class".equals(jsniRef.memberName())) {
        return;
      }
      FieldBinding target = getField(clazz, jsniRef);
      if (target == null) {
        emitWarning("jsni", "Referencing field '" + jsniRef.className() + "."
            + jsniRef.memberName()
            + "': unable to resolve field, expect subsequent failures");
        return;
      }
      if (target.isDeprecated()) {
        emitWarning("deprecation", "Referencing deprecated field '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'");
      }

      if (hasUnsafeLongsAnnotation) {
        return;
      }
      if (containsLong(target.type)) {
        emitError("Referencing field '" + jsniRef.className() + "."
            + jsniRef.memberName() + "': type '" + typeString(target.type)
            + "' is not safe to access in JSNI code");
      }
    }

    private void checkMethodRef(ReferenceBinding clazz, JsniRef jsniRef) {
      assert jsniRef.isMethod();
      MethodBinding target = getMethod(clazz, jsniRef);
      if (target == null) {
        emitWarning("jsni", "Referencing method '" + jsniRef.className() + "."
            + jsniRef.memberSignature()
            + "': unable to resolve method, expect subsequent failures");
        return;
      }
      if (target.isDeprecated()) {
        emitWarning("deprecation", "Referencing deprecated method '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'");
      }

      if (hasUnsafeLongsAnnotation) {
        return;
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
    }

    private void checkRef(JsniRef jsniRef) {
      String className = jsniRef.className();
      if ("null".equals(className)) {
        return;
      }

      boolean isArray = false;
      while (className.endsWith("[]")) {
        isArray = true;
        className = className.substring(0, className.length() - 2);
      }

      /*
       * TODO(bobv): OMG WTF LOL. Okay, but seriously, the LHS of a JSNI ref for
       * a primitive type should be the keyword, e.g. "int.class".
       */
      ReferenceBinding clazz = findClass(className);
      boolean isPrimitive = (clazz == null) && className.length() == 1
          && "ZBCDFIJSV".indexOf(className.charAt(0)) >= 0;

      if (isArray || isPrimitive) {
        if (!jsniRef.isField() || !jsniRef.memberName().equals("class")) {
          emitError("Referencing member '" + jsniRef.className() + "."
              + jsniRef.memberName()
              + "': 'class' is the only legal reference for "
              + (isArray ? "array" : "primitive") + " types");
          return;
        }
      }

      if (isPrimitive) {
        return;
      }

      // TODO(bobv): uncomment this.
      // ReferenceBinding clazz = findClass(className);
      if (looksLikeAnonymousClass(jsniRef)
          || (clazz != null && clazz.isAnonymousType())) {
        emitError("Referencing class '" + className
            + ": JSNI references to anonymous classes are illegal");
      } else if (clazz != null) {
        if (clazz.isDeprecated()) {
          emitWarning("deprecation", "Referencing deprecated class '"
              + className + "'");
        }

        if (jsniRef.isMethod()) {
          checkMethodRef(clazz, jsniRef);
        } else {
          checkFieldRef(clazz, jsniRef);
        }
      } else {
        emitWarning("jsni", "Referencing class '" + className
            + "': unable to resolve class, expect subsequent failures");
      }
    }

    private void emitError(String msg) {
      JsniCollector.reportJsniError(errorInfo, method, msg);
    }

    private void emitWarning(String category, String msg) {
      if (suppressWarnings.contains(category)
          || suppressWarnings.contains("all")) {
        return;
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
        while (clazz != null) {
          for (MethodBinding findMethod : clazz.getMethods(methodName.toCharArray())) {
            if (paramTypesMatch(findMethod, jsniRef)) {
              return findMethod;
            }
          }
          clazz = clazz.superclass();
        }
      }
      return null;
    }

    private Set<String> getSuppressedWarnings(MethodDeclaration method) {
      Annotation[] annotations = method.annotations;
      if (annotations == null) {
        return Sets.create();
      }

      for (Annotation a : annotations) {
        if (SuppressWarnings.class.getName().equals(
            CharOperation.toString(((ReferenceBinding) a.resolvedType).compoundName))) {
          for (MemberValuePair pair : a.memberValuePairs()) {
            if (String.valueOf(pair.name).equals("value")) {
              Expression valueExpr = pair.value;
              if (valueExpr instanceof StringLiteral) {
                // @SuppressWarnings("Foo")
                return Sets.create(((StringLiteral) valueExpr).constant.stringValue().toLowerCase(Locale.ENGLISH));
              } else if (valueExpr instanceof ArrayInitializer) {
                // @SuppressWarnings({ "Foo", "Bar"})
                ArrayInitializer ai = (ArrayInitializer) valueExpr;
                String[] values = new String[ai.expressions.length];
                for (int i = 0, j = values.length; i < j; i++) {
                  values[i] = ((StringLiteral) ai.expressions[i]).constant.stringValue().toLowerCase(Locale.ENGLISH);
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
      return Sets.create();
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
      Map<AbstractMethodDeclaration, JsniMethod> jsniMethods,
      TypeResolver typeResolver) {
    new JsniChecker(cud, typeResolver, jsniMethods).check();
  }

  private final CompilationUnitDeclaration cud;
  private final Map<AbstractMethodDeclaration, JsniMethod> jsniMethods;
  private final TypeResolver typeResolver;

  private JsniChecker(CompilationUnitDeclaration cud,
      TypeResolver typeResolver,
      Map<AbstractMethodDeclaration, JsniMethod> jsniMethods) {
    this.cud = cud;
    this.typeResolver = typeResolver;
    this.jsniMethods = jsniMethods;
  }

  private void check() {
    // First check the declarations.
    cud.traverse(new JsniDeclChecker(), cud.scope);
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
