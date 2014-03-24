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
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.util.InstalledHelpInfo;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
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
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

/**
 * Resolves JSNI references to fields and methods and gives a informative errors if the references
 * cannot be resolved.
 * <p>
 *  * JSNI references consist of two parts @ClassDescriptor::memberDescriptor. Class descriptors
 * are source names and memberDescriptors are either a field name or a method name with a signature
 * specification. Signature specification a full signature or a wildcard (*).
 * <p>
 *  * The result of resolution will be a modified JSNI AST where all the resolved references will
 * carry the fully qualified class name and the full member reference, along with a mapping from
 * jsni references to JDT binding.
 * <p>
 * In addition in the following instances involving longs warning will be emitted to remind uses
 * that GWT longs are not JavaScript numbers:
 * <ul>
 * <li>JSNI methods with a parameter or return type of long or an array whose base type is long.
 * </li>
 * <li>Access from JSNI to a field whose type is long or an array whose base type is long.</li>
 * <li>Access from JSNI to a method with a parameter or return type of long or an array whose base
 * type is long.</li>
 * <li>JSNI references to anonymous classes.</li>
 * </ul>
 */
public class JsniReferenceResolver {

  /**
   * A call-back interface to resolve types.
   */
  public interface TypeResolver {
    /**
     * @param sourceOrBinaryName Either source or binary names are allowed in JSNI.
     */
    ReferenceBinding resolveType(String sourceOrBinaryName);
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
          new JsniReferenceResolverVisitor(meth, hasUnsafeLongsAnnotation).resolve(
              jsniMethod.function());
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

      if (meth.arguments == null) {
        return;
      }

      for (Argument arg : meth.arguments) {
        if (containsLong(arg.type, scope)) {
          longAccessError(arg, "Parameter '" + String.valueOf(arg.name)
              + "': type '" + typeString(arg.type)
              + "' is not safe to access in JSNI code");
        }
      }
    }

    private boolean containsLong(final TypeReference type, ClassScope scope) {
      return type != null
          && JsniReferenceResolver.this.containsLong(type.resolveType(scope));
    }

    private String typeString(TypeReference type) {
      return type.toString();
    }
  }

  private class JsniReferenceResolverVisitor extends JsModVisitor {

    private final boolean hasUnsafeLongsAnnotation;
    private final MethodDeclaration method;

    public JsniReferenceResolverVisitor(MethodDeclaration method,
        boolean hasUnsafeLongsAnnotation) {
      this.method = method;
      this.hasUnsafeLongsAnnotation = hasUnsafeLongsAnnotation;
    }

    public void resolve(JsFunction function) {
      this.accept(function);
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      String ident = x.getIdent();
      if (ident.charAt(0) != '@') {
        // Not a jsni reference.
        return;
      }

      JsniRef jsniRef = JsniRef.parse(ident);
      if (jsniRef == null) {
        emitError("Malformed JSNI identifier '" + ident + "'", x.getSourceInfo());
        return;
      }

      resolveClassReference(jsniRef);

      Binding binding = resolveReference(x.getSourceInfo(), jsniRef, x.getQualifier() != null,
          ctx.isLvalue());

      assert !x.isResolved();
      if (!ident.equals(jsniRef.getResolvedReference())) {
        // Replace by the resolved reference (consisting of the fully qualified classname and the
        // method description including actual signature) so that dispatch everywhere is consistent
        // with the one resolved here.
        ident = jsniRef.getResolvedReference();
        JsNameRef newRef = new JsNameRef(x.getSourceInfo(), ident);
        newRef.setQualifier(x.getQualifier());
        ctx.replaceMe(newRef);
      }
      if (binding != null) {
        jsniRefs.put(ident, binding);
      }
    }

    private void resolveClassReference(JsniRef jsniRef) {
      // Precedence rules as of JLS 6.4.1.
      // 1. Enclosing type.
      // 2. Visible type in same compilation unit.
      // 3. Named import.
      // 4. Same package.
      // 5. Import on demand.

      String originalName = jsniRef.className();
      String importedClassName = originalName;
      if (importedClassName.contains(".")) {
        // Only retain up the first dot to support innerclasses. E.g. import c.g.A and reference
        // @A.I::f.
        importedClassName = importedClassName.substring(0,importedClassName.indexOf("."));
      }

      // 1 & 2. Check to see if this name refers to the enclosing class or is directly accessible
      // from it.
      ReferenceBinding declaringClass = method.binding.declaringClass;
      while (declaringClass != null) {
        String declaringClassName = JdtUtil.getSourceName(declaringClass);
        if (declaringClassName.equals(importedClassName) ||
            declaringClassName.endsWith("." + importedClassName)) {
          // Referring to declaring class name using unqualified name.
          jsniRef.setResolvedClassName(declaringClassName +
              originalName.substring(importedClassName.length()));
          return;
        }
        String fullClassName = declaringClassName + "." + originalName;
        if (typeResolver.resolveType(fullClassName) != null) {
          jsniRef.setResolvedClassName(fullClassName);
          return;
        }
        declaringClass = declaringClass.enclosingTypeAt(1);
      }

      // 3. Check to see if this name is one of the named imports.
      for (ImportReference importReference : cudImports) {
        String nameFromImport = JdtUtil.asDottedString(importReference.getImportName());
        if (!importReference.isStatic()  && importReference.trailingStarPosition == 0 &&
           nameFromImport.endsWith("." + importedClassName)) {
          jsniRef.setResolvedClassName(
              nameFromImport + originalName.substring(importedClassName.length()));
          return;
        }
      }

      // 4. Check to see if this name is resolvable from the current package.
      String currentPackageClassName =
          String.valueOf(method.binding.declaringClass.qualifiedPackageName());
      currentPackageClassName += (currentPackageClassName.isEmpty() ? "" : ".") +  originalName;

      if (typeResolver.resolveType(currentPackageClassName) != null) {
        jsniRef.setResolvedClassName(currentPackageClassName);
        return;
      }

      // 5. Check to see if this name is resolvable as an import on demand.
      for (ImportReference importReference : cudImports) {
        if (importReference.isStatic() || importReference.trailingStarPosition == 0) {
          continue;
        }
        String fullClassName = JdtUtil.asDottedString(importReference.getImportName())
            + "." + originalName;
        if (typeResolver.resolveType(fullClassName) != null) {
          jsniRef.setResolvedClassName(fullClassName);
          return;
        }
      }
      // Otherwise leave it as it is.
      // TODO(rluble): Maybe we should leave it null here.
      jsniRef.setResolvedClassName(jsniRef.className());
    }

    private FieldBinding checkFieldRef(SourceInfo errorInfo, ReferenceBinding clazz,
        JsniRef jsniRef, boolean hasQualifier, boolean isLvalue) {
      assert jsniRef.isField();
      FieldBinding target = getField(clazz, jsniRef);
      if (target == null) {
        emitError("Referencing field '" + jsniRef.className() + "."
            + jsniRef.memberName() + "': unable to resolve field", errorInfo);
        return null;
      }
      if (target.isDeprecated()) {
        emitWarning("deprecation",
            "Referencing deprecated field '" + jsniRef.className() + "."
                + jsniRef.memberName() + "'", errorInfo);
      }
      if (isLvalue && target.constant() != Constant.NotAConstant) {
        emitError("Illegal assignment to compile-time constant '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'", errorInfo);
      }
      if (target.isStatic() && hasQualifier) {
        emitError("Unnecessary qualifier on static field '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'", errorInfo);
      } else if (!target.isStatic() && !hasQualifier) {
        emitError("Missing qualifier on instance field '" + jsniRef.className()
            + "." + jsniRef.memberName() + "'", errorInfo);
      }

      if (hasUnsafeLongsAnnotation) {
        return target;
      }
      if (containsLong(target.type)) {
        emitError("Referencing field '" + jsniRef.className() + "."
            + jsniRef.memberName() + "': type '" + typeString(target.type)
            + "' is not safe to access in JSNI code", errorInfo);
      }
      return target;
    }

    private MethodBinding checkMethodRef(SourceInfo errorInfo, ReferenceBinding clazz,
        JsniRef jsniRef, boolean hasQualifier, boolean isLvalue) {
      assert jsniRef.isMethod();
      List<MethodBinding> targets = getMatchingMethods(clazz, jsniRef);
      if (targets.size() > 1) {
          emitError("Referencing method '" + jsniRef.className() + "."
            + jsniRef.memberSignature() + "': ambiguous wildcard match", errorInfo);
        return null;
      } else if (targets.isEmpty()) {
        emitError("Referencing method '" + jsniRef.className() + "."
            + jsniRef.memberSignature() + "': unable to resolve method", errorInfo);
        return null;
      }
      MethodBinding target = targets.get(0);
      if (target.isDeprecated()) {
        emitWarning("deprecation",
            "Referencing deprecated method '" + jsniRef.className() + "."
                + jsniRef.memberName() + "'", errorInfo);
      }
      if (isLvalue) {
        emitError("Illegal assignment to method '" + jsniRef.className() + "."
            + jsniRef.memberName() + "'", errorInfo);
      }
      boolean needsQualifer = !target.isStatic() && !target.isConstructor();
      if (!needsQualifer && hasQualifier) {
        emitError("Unnecessary qualifier on static method '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'", errorInfo);
      } else if (needsQualifer && !hasQualifier) {
        emitError("Missing qualifier on instance method '"
            + jsniRef.className() + "." + jsniRef.memberName() + "'", errorInfo);
      }
      if (!target.isStatic() && JSORestrictionsChecker.isJso(clazz)) {
        emitError("Referencing method '" + jsniRef.className() + "."
            + jsniRef.memberSignature()
            + "': references to instance methods in overlay types are illegal", errorInfo);
      }
      if (checkerState.isJsoInterface(clazz)) {
        String implementor = checkerState.getJsoImplementor(clazz);
        emitError("Referencing interface method '" + jsniRef.className() + "."
            + jsniRef.memberSignature() + "': implemented by '" + implementor
            + "'; references to instance methods in overlay types are illegal"
            + "; use a stronger type or a Java trampoline method", errorInfo);
      }

      if (hasUnsafeLongsAnnotation) {
        return target;
      }
      if (containsLong(target.returnType)) {
        emitError("Referencing method '" + jsniRef.className() + "."
            + jsniRef.memberName() + "': return type '"
            + typeString(target.returnType)
            + "' is not safe to access in JSNI code", errorInfo);
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
                + "' may not be passed out of JSNI code", errorInfo);
          }
        }
      }
      return target;
    }

    private Binding resolveReference(SourceInfo errorInfo, JsniRef jsniRef, boolean hasQualifier,
        boolean isLvalue) {
      String className = jsniRef.getResolvedClassName();
      if ("null".equals(className)) {
        // Do not emit errors for null.nullField or null.nullMethod.
        // TODO(rluble): Why should these ever reach resolveReference()?
        if (jsniRef.isField() && !"nullField".equals(jsniRef.memberName())) {
          emitError("Referencing field '" + jsniRef.className() + "."
              + jsniRef.memberName()
              + "': 'nullField' is the only legal field reference for 'null'", errorInfo);
        } else if (jsniRef.isMethod() && !"nullMethod()".equals(jsniRef.memberSignature())) {
          emitError("Referencing method '" + jsniRef.className() + "."
              + jsniRef.memberSignature()
              + "': 'nullMethod()' is the only legal method for 'null'", errorInfo);
        }
        jsniRef.setResolvedMemberWithSignature(jsniRef.memberSignature());
        return null;
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

      if ((binding == null && looksLikeAnonymousClass(jsniRef))
          || (binding != null && binding.isAnonymousType())) {
        emitError("Referencing class '" + className
            + "': JSNI references to anonymous classes are illegal", errorInfo);
        return null;
      } else if (binding == null) {
        emitError("Referencing class '" + className
            + "': unable to resolve class", errorInfo);
        return null;
      }

      if (clazz != null && clazz.isDeprecated()) {
        emitWarning("deprecation", "Referencing deprecated class '" + className
            + "'", errorInfo);
      }

      if (jsniRef.isField() && "class".equals(jsniRef.memberName())) {
        if (isLvalue) {
          emitError("Illegal assignment to class literal '"
              + jsniRef.className() + ".class'", errorInfo);
          return null;
        }
        // Reference to the class itself.
        jsniRef.setResolvedClassName(JdtUtil.getSourceName(binding));
        jsniRef.setResolvedMemberWithSignature(jsniRef.memberSignature());
        if (jsniRef.isArray()) {
          ArrayBinding arrayBinding =
              method.scope.createArrayType(binding, jsniRef.getDimensions());
          return arrayBinding;
        } else {
          return binding;
        }
      }

      if (jsniRef.isArray() || isPrimitive) {
        emitError("Referencing member '" + jsniRef.fullClassName() + "."
            + jsniRef.memberName()
            + "': 'class' is the only legal reference for "
            + (jsniRef.isArray() ? "array" : "primitive") + " types", errorInfo);
        return null;
      }

      assert clazz != null;
      if (jsniRef.isMethod()) {
        MethodBinding methodBinding =
            checkMethodRef(errorInfo, clazz, jsniRef, hasQualifier, isLvalue);
        // Reference to the class where the method is defined itself.
        resolveJsniRef(jsniRef, methodBinding);
        return methodBinding;
      } else {
        FieldBinding fieldBinding =
            checkFieldRef(errorInfo, clazz, jsniRef, hasQualifier, isLvalue);
        // Reference to the class where the method is defined itself.
        resolveJsniRef(jsniRef, fieldBinding);
        return fieldBinding;
      }
    }

    private void emitError(String msg, SourceInfo errorInfo) {
      JsniCollector.reportJsniError(errorInfo, method, msg);
    }

    private void emitWarning(String category, String msg, SourceInfo errorInfo) {
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

    private MethodBinding getMatchingConstructor(ReferenceBinding clazz, JsniRef jsniRef) {
      for (MethodBinding constructorBinding : clazz.getMethods(INIT_CTOR_CHARS)) {
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
        if (constructorBinding.parameters != null) {
          for (TypeBinding binding : constructorBinding.parameters) {
            methodSig.append(binding.signature());
          }
        }
        if (methodSig.toString().equals(jsniRef.paramTypesString())) {
          return constructorBinding;
        }
      }
      return null;
    }

    /**
     * Returns true if {@code method}, is a method in {@code fromClass} (to support current
     * private method access} or is a public method of a superClass.
     * the sublcass.<p>
     *
     * Unlike regular Java, protected and package-private access to a supertype is not allowed.
     */
    private boolean isMethodVisibleToJsniRef(ReferenceBinding fromClass, MethodBinding method) {
      return fromClass == method.declaringClass || method.isPublic();
    }

    private List<MethodBinding> getMatchingMethods(ReferenceBinding clazz, JsniRef jsniRef) {
      assert jsniRef.isMethod();
      List<MethodBinding> foundMethods = Lists.newArrayList();
      String methodName = jsniRef.memberName();
      if ("new".equals(methodName)) {
        MethodBinding constructorBinding = getMatchingConstructor(clazz, jsniRef);
        if (constructorBinding != null) {
          foundMethods.add(constructorBinding);
        }
      } else {
        Queue<ReferenceBinding> work = Lists.newLinkedList();
        work.add(clazz);
        while (!work.isEmpty()) {
          ReferenceBinding currentClass = work.remove();
          NEXT_METHOD:
          for (MethodBinding findMethod : currentClass.getMethods(methodName.toCharArray())) {
            if (!isMethodVisibleToJsniRef(clazz, findMethod)) {
              continue;
            }
            if (!paramTypesMatch(findMethod, jsniRef)) {
              continue;
            }
            for (MethodBinding alreadyFound : foundMethods) {
              // Only collect methods with different signatures (same signatures are overloads
              // hence they are ok.
              if (paramTypesMatch(alreadyFound, findMethod)) {
                break NEXT_METHOD;
              }
            }
            foundMethods.add(findMethod);
          }
          ReferenceBinding[] superInterfaces = currentClass.superInterfaces();
          if (superInterfaces != null) {
            work.addAll(Arrays.asList(superInterfaces));
          }
          ReferenceBinding superclass = currentClass.superclass();
          if (superclass != null) {
            work.add(superclass);
          }
        }
      }
      return foundMethods;
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

    private boolean paramTypesMatch(MethodBinding thisMethod, MethodBinding thatMethod) {
      int thisParameterCount = thisMethod.parameters == null ? 0 : thisMethod.parameters.length;
      int thatParameterCount = thatMethod.parameters == null ? 0 : thatMethod.parameters.length;

      if (thisParameterCount != thatParameterCount) {
        return false;
      }

      for (int i = 0; i < thisParameterCount; i++) {
        TypeBinding thisBinding = thisMethod.parameters[i];
        TypeBinding thatBinding = thatMethod.parameters[i];
        if (!new String(thisBinding.signature()).equals(new String(thatBinding.signature()))) {
          return false;
        }
      }
      return true;
    }

    private String typeString(TypeBinding type) {
      return String.valueOf(type.shortReadableName());
    }
  }

  private static final char[] INIT_CTOR_CHARS = "<init>".toCharArray();

  private static final char[][] UNSAFE_LONG_ANNOTATION_CHARS = CharOperation.splitOn(
      '.', UnsafeNativeLong.class.getName().toCharArray());

  /**
   * Resolve JSNI references in an entire
   * {@link org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration}.
   *
   */
  public static void resolve(CompilationUnitDeclaration cud,
      List<ImportReference> cudOriginalImports,
      CheckerState checkerState,
      Map<MethodDeclaration, JsniMethod> jsniMethods,
      Map<String, Binding> jsniRefs, TypeResolver typeResolver) {
    new JsniReferenceResolver(cud, cudOriginalImports, checkerState, typeResolver, jsniMethods, jsniRefs).resolve();
  }

  Set<String> getSuppressedWarnings(Annotation[] annotations) {
    if (annotations == null) {
      return ImmutableSet.of();
    }

    for (Annotation a : annotations) {
      if (!SuppressWarnings.class.getName().equals(
          CharOperation.toString(((ReferenceBinding) a.resolvedType).compoundName))) {
        continue;
      }
      for (MemberValuePair pair : a.memberValuePairs()) {
        if (!String.valueOf(pair.name).equals("value")) {
          continue;
        }
        Expression valueExpr = pair.value;
        if (valueExpr instanceof StringLiteral) {
          // @SuppressWarnings("Foo")
          return ImmutableSet.of(((StringLiteral) valueExpr).constant.stringValue().toLowerCase(
              Locale.ENGLISH));
        } else if (valueExpr instanceof ArrayInitializer) {
          // @SuppressWarnings({ "Foo", "Bar"})
          ArrayInitializer ai = (ArrayInitializer) valueExpr;
          ImmutableSet.Builder valuesSetBuilder = ImmutableSet.builder();
          for (int i = 0, j = ai.expressions.length; i < j; i++) {
            if ((ai.expressions[i]) instanceof StringLiteral) {
              StringLiteral expression = (StringLiteral) ai.expressions[i];
              valuesSetBuilder.add(expression.constant.stringValue().toLowerCase(Locale.ENGLISH));
            } else {
              suppressionAnnotationWarning(a,
                  "Unable to analyze SuppressWarnings annotation, " +
                      ai.expressions[i].toString() + " not a string constant.");
            }
          }
          return valuesSetBuilder.build();
        } else {
          suppressionAnnotationWarning(a, "Unable to analyze SuppressWarnings annotation, " +
              valueExpr.toString() + " not a string constant.");
        }
      }
    }
    return ImmutableSet.of();
  }

  private final CheckerState checkerState;
  private final CompilationUnitDeclaration cud;
  private final List<ImportReference> cudImports;
  private final Map<MethodDeclaration, JsniMethod> jsniMethods;
  private final Map<String, Binding> jsniRefs;
  private final Stack<Set<String>> suppressWarningsStack = new Stack<Set<String>>();
  private final TypeResolver typeResolver;

  private JsniReferenceResolver(CompilationUnitDeclaration cud, List<ImportReference> cudImports,
      CheckerState checkerState, TypeResolver typeResolver,
      Map<MethodDeclaration, JsniMethod> jsniMethods,
      Map<String, Binding> jsniRefs) {
    this.checkerState = checkerState;
    this.cud = cud;
    this.cudImports = cudImports;
    this.typeResolver = typeResolver;
    this.jsniMethods = jsniMethods;
    this.jsniRefs = jsniRefs;
  }

  private void resolve() {
    // First resolve the declarations.
    cud.traverse(new JsniDeclChecker(), cud.scope);
  }

  /**
   * Check whether the argument type is the <code>long</code> primitive type. If
   * the argument is <code>null</code>, returns <code>false</code>.
   */
  private boolean containsLong(TypeBinding type) {
    if (!(type instanceof BaseTypeBinding)) {
      return false;
    }

    BaseTypeBinding btb = (BaseTypeBinding) type;
    if (btb.id == TypeIds.T_long) {
        return true;
    }

    return false;
  }

  private boolean hasUnsafeLongsAnnotation(MethodDeclaration meth,
      ClassScope scope) {
    if (meth.annotations == null) {
      return false;
    }

    for (Annotation annot : meth.annotations) {
      if (isUnsafeLongAnnotation(annot, scope)) {
        return true;
      }
    }
    return false;
  }

  private boolean isUnsafeLongAnnotation(Annotation annot, ClassScope scope) {
    if (annot.type == null) {
      return false;
    }

    TypeBinding resolved = annot.type.resolveType(scope);
    if (resolved == null || !(resolved instanceof ReferenceBinding)) {
      return false;
    }

    ReferenceBinding rb = (ReferenceBinding) resolved;
    if (CharOperation.equals(rb.compoundName, UNSAFE_LONG_ANNOTATION_CHARS)) {
      return true;
    }
    return false;
  }

  private void longAccessError(ASTNode node, String message) {
    GWTProblem.recordError(node, cud, message, new InstalledHelpInfo(
        "longJsniRestriction.html"));
  }

  private void suppressionAnnotationWarning(ASTNode node, String message) {
    GWTProblem.recordProblem(node, cud.compilationResult(), message, null,
        ProblemSeverities.Warning);
  }

  private static void resolveJsniRef(JsniRef jsniRef, FieldBinding fieldBinding) {
    if (fieldBinding  == null) {
      return;
    }
    jsniRef.setResolvedClassName(JdtUtil.getSourceName(fieldBinding.declaringClass));
    jsniRef.setResolvedMemberWithSignature(new String(fieldBinding.name));
  }

  private static void resolveJsniRef(JsniRef jsniRef, MethodBinding methodBinding) {
    if (methodBinding  == null) {
      return;
    }
    ReferenceBinding declaringClassBinding = methodBinding.declaringClass;
    jsniRef.setResolvedClassName(JdtUtil.getSourceName(declaringClassBinding));
    StringBuilder methodNameWithSignature = new StringBuilder();
    String selector = String.valueOf(methodBinding.selector);
    List<TypeBinding> parameterTypeBindings = Lists.newArrayList();

    if (selector.equals("<init>")) {
      // It is a constructor.
      // (1) use the JSNI selector instead of <init>.
      selector = "new";
      // (2) add the implicit constructor parameters types for non static inner classes.
      if (JdtUtil.isInnerClass(declaringClassBinding)) {
        NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClassBinding;
        if (nestedBinding.enclosingInstances != null) {
          for (SyntheticArgumentBinding argumentBinding : nestedBinding.enclosingInstances) {
            parameterTypeBindings.add(argumentBinding.type);
          }
        }
      }
    }

    parameterTypeBindings.addAll(Arrays.asList(methodBinding.parameters));
    methodNameWithSignature.append(selector);
    methodNameWithSignature.append("(");
    for (TypeBinding parameterTypeBinding : parameterTypeBindings) {
      methodNameWithSignature.append(parameterTypeBinding.signature());
    }
    methodNameWithSignature.append(")");
    jsniRef.setResolvedMemberWithSignature(methodNameWithSignature.toString());
  }
}
