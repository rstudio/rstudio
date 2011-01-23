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

import com.google.gwt.dev.javac.JsniCollector;
import com.google.gwt.dev.jdt.AbstractCompiler.CompilationResults;
import com.google.gwt.dev.jdt.SafeASTVisitor;
import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JEnumType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JField.Disposition;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.js.JsAbstractSymbolResolver;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.NestedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticArgumentBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.util.Util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a Builder for {@link TypeMap}. The whole point of this pass is to
 * create raw unfinished, unlinked AST nodes for types, methods, fields, and
 * parameters, and to map the original JDT nodes to these AST nodes. That way
 * when GenerateJavaDom runs, it just uses the TypeMap output from this Builder
 * to retrieve whatever referenceable nodes it needs without worrying about
 * whether they need to be created. Building our AST from JDT starts here.
 */
public class BuildTypeMap {

  /**
   * Creates JNodes for every method, field, initializer, parameter, and local
   * and memorizes the mapping from the JDT Binding to the corresponding JNode
   * for each thing created. Note that this pass also 1) sets up the super
   * type(s) for any member or local types created in BuildTypeMapVisitor (see
   * the comments there about why it had to be deferred). 2) adds each
   * user-defined type to a flat list. 3) Creates JNodes for all methods and
   * variables and memorizes the mapping from the JDT Binding to the
   * corresponding JNode for each created method and variable. 4) Maps all
   * synthetic arguments and fields for nested and local classes. 5) Slurps in
   * JSNI code for native methods as an opaque string.
   *
   * Note that methods and fields are not added to their classes here, that
   * isn't done until {@link GenerateJavaAST}.
   */
  private class BuildDeclMapVisitor extends SafeASTVisitor {

    private String currentFileName;
    private int[] currentSeparatorPositions;
    private List<TypeDeclaration> typeDecls = new ArrayList<TypeDeclaration>();

    public TypeDeclaration[] getTypeDeclarataions() {
      return typeDecls.toArray(new TypeDeclaration[typeDecls.size()]);
    }

    @Override
    public boolean visit(AnnotationMethodDeclaration methodDeclaration,
        ClassScope scope) {
      return visit((MethodDeclaration) methodDeclaration, scope);
    }

    @Override
    public boolean visit(Argument argument, BlockScope scope) {
      try {
        if (scope == scope.methodScope()) {
          return true;
        }

        JMethodBody enclosingBody = findEnclosingMethod(scope);
        if (enclosingBody == null) {
          // Happens in the case of external types
          return true;
        }
        SourceInfo info = makeSourceInfo(argument, enclosingBody.getMethod());
        LocalVariableBinding b = argument.binding;
        JType localType = getType(b.type);
        JLocal newLocal = JProgram.createLocal(info, String.valueOf(argument.name),
            localType, b.isFinal(), enclosingBody);
        typeMap.put(b, newLocal);
        return true;
      } catch (Throwable e) {
        throw translateException(argument, e);
      }
    }

    @Override
    public boolean visit(ConstructorDeclaration ctorDecl, ClassScope scope) {
      try {
        MethodBinding b = ctorDecl.binding;
        JClassType enclosingType = (JClassType) getType(
            scope.enclosingSourceType());
        SourceInfo info = makeSourceInfo(ctorDecl, enclosingType);
        processConstructor(b, ctorDecl, info);
        return true;
      } catch (Throwable e) {
        throw translateException(ctorDecl, e);
      }
    }

    @Override
    public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
      try {
        FieldBinding b = fieldDeclaration.binding;
        JDeclaredType enclosingType = (JDeclaredType) getType(
            scope.enclosingSourceType());
        SourceInfo info = makeSourceInfo(fieldDeclaration, enclosingType);
        Expression initialization = fieldDeclaration.initialization;
        if (initialization != null
            && initialization instanceof AllocationExpression
            && ((AllocationExpression) initialization).enumConstant != null) {
          createEnumField(info, b, enclosingType);
        } else {
          createField(info, b, enclosingType);
        }
        return true;
      } catch (Throwable e) {
        throw translateException(fieldDeclaration, e);
      }
    }

    @Override
    public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
      try {
        LocalVariableBinding b = localDeclaration.binding;
        TypeBinding resolvedType = localDeclaration.type.resolvedType;
        JType localType;
        if (resolvedType.constantPoolName() != null) {
          localType = getType(resolvedType);
        } else {
          // Special case, a statically unreachable local type.
          localType = JNullType.INSTANCE;
        }
        JMethodBody enclosingBody = findEnclosingMethod(scope);
        if (enclosingBody == null) {
          // Happens in the case of external types
          return true;
        }
        SourceInfo info = makeSourceInfo(localDeclaration, enclosingBody.getMethod());
        JLocal newLocal = JProgram.createLocal(info,
            String.valueOf(localDeclaration.name), localType, b.isFinal(),
            enclosingBody);
        typeMap.put(b, newLocal);
        return true;
      } catch (Throwable e) {
        throw translateException(localDeclaration, e);
      }
    }

    @Override
    public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
      try {
        MethodBinding b = methodDeclaration.binding;
        JDeclaredType enclosingType = (JDeclaredType) getType(
            scope.enclosingSourceType());
        SourceInfo info = makeSourceInfo(methodDeclaration, enclosingType);
        JMethod newMethod = processMethodBinding(b, enclosingType, info);
        SourceInfo methodInfo = makeSourceInfo(methodDeclaration, newMethod);
        mapParameters(newMethod, methodDeclaration, methodInfo);
        info.addCorrelation(program.getCorrelator().by(newMethod));

        if (newMethod.isNative()) {
          processNativeMethod(methodDeclaration, info, enclosingType, newMethod);
        }

        return true;
      } catch (Throwable e) {
        throw translateException(methodDeclaration, e);
      }
    }

    @Override
    public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
      return process(memberTypeDeclaration);
    }

    @Override
    public boolean visit(TypeDeclaration typeDeclaration,
        CompilationUnitScope scope) {
      return process(typeDeclaration);
    }

    @Override
    public boolean visitValid(TypeDeclaration localTypeDeclaration,
        BlockScope scope) {
      return process(localTypeDeclaration);
    }

    private JField createEnumField(SourceInfo info, FieldBinding binding,
        JReferenceType enclosingType) {
      JType type = getType(binding.type);
      JField field = program.createEnumField(info,
          String.valueOf(binding.name), (JEnumType) enclosingType,
          (JClassType) type, binding.original().id);
      info.addCorrelation(program.getCorrelator().by(field));
      typeMap.put(binding, field);
      return field;
    }

    private JMethodBody findEnclosingMethod(BlockScope scope) {
      JMethod method;
      MethodScope methodScope = scope.methodScope();
      if (methodScope.isInsideInitializer()) {
        JDeclaredType enclosingType = (JDeclaredType) getType(
            scope.classScope().referenceContext.binding);
        if (methodScope.isStatic) {
          // clinit
          method = enclosingType.getMethods().get(0);
        } else {
          // init
          assert (enclosingType instanceof JClassType);
          method = enclosingType.getMethods().get(1);
        }
      } else {
        AbstractMethodDeclaration referenceMethod = methodScope.referenceMethod();
        method = (JMethod) typeMap.get(referenceMethod.binding);
      }
      assert !method.isNative() && !method.isAbstract();
      return (JMethodBody) (method.getEnclosingType().isExternal() ? null :
          method.getBody());
    }

    private SourceInfo makeSourceInfo(AbstractMethodDeclaration methodDecl,
        HasSourceInfo enclosing) {
      CompilationResult compResult = methodDecl.compilationResult;
      int[] indexes = compResult.lineSeparatorPositions;
      String fileName = String.valueOf(compResult.fileName);
      int startLine = Util.getLineNumber(methodDecl.sourceStart, indexes, 0,
          indexes.length - 1);
      SourceInfo toReturn = program.createSourceInfo(methodDecl.sourceStart,
          methodDecl.bodyEnd, startLine, fileName);

      // The SourceInfo will inherit Correlations from its enclosing object
      if (enclosing != null) {
        toReturn.copyMissingCorrelationsFrom(enclosing.getSourceInfo());
      }

      return toReturn;
    }

    private SourceInfo makeSourceInfo(Statement stmt, HasSourceInfo enclosing) {
      int startLine = Util.getLineNumber(stmt.sourceStart,
          currentSeparatorPositions, 0, currentSeparatorPositions.length - 1);
      SourceInfo toReturn = program.createSourceInfo(stmt.sourceStart,
          stmt.sourceEnd, startLine, currentFileName);

      // The SourceInfo will inherit Correlations from its enclosing object
      if (enclosing != null) {
        toReturn.copyMissingCorrelationsFrom(enclosing.getSourceInfo());
      }

      return toReturn;
    }

    /**
     * Add synthetic fields, setup super types. You'll notice that we DON'T have
     * any concept of "inner" or "outer" types in our AST. Truth is, we found it
     * easier to simply model everything as flat classes and emulate the nesting
     * behavior (and final local access on local classes). It's much closer to
     * how we'll eventually be generating JavaScript code (code generation is
     * more straightforward), it makes for fewer kinds of things to worry about
     * when optimizing (more aggressive optimizations), and it's how Java
     * actually implements the stuff under the hood anyway.
     */
    private boolean process(TypeDeclaration typeDeclaration) {
      CompilationResult compResult = typeDeclaration.compilationResult;
      currentSeparatorPositions = compResult.lineSeparatorPositions;
      currentFileName = String.valueOf(compResult.fileName);

      if (BuildTypeMap.this.process(typeDeclaration.binding)) {
        if (!linker.isExternalType(dotify(typeDeclaration.binding.compoundName))) {
          typeDecls.add(typeDeclaration);
        }
        return true;
      }

      return false;
    }

    private void processNativeMethod(MethodDeclaration methodDeclaration,
        SourceInfo info, JDeclaredType enclosingType, JMethod newMethod) {
      // TODO: use existing parsed JSNI functions from CompilationState.
      // Handle JSNI block
      char[] source = methodDeclaration.compilationResult().getCompilationUnit().getContents();
      String unitSource = String.valueOf(source);
      JsFunction jsFunction = JsniCollector.parseJsniFunction(
          methodDeclaration, unitSource, enclosingType.getName(),
          info.getFileName(), jsProgram);
      if (jsFunction != null) {
        jsFunction.setFromJava(true);
        ((JsniMethodBody) newMethod.getBody()).setFunc(jsFunction);
        // Ensure that we've resolved the parameter and local references within
        // the JSNI method for later pruning.
        JsParameterResolver localResolver = new JsParameterResolver(jsFunction);
        localResolver.accept(jsFunction);
      }
    }

    private InternalCompilerException translateException(
        AbstractMethodDeclaration amd, Throwable e) {
      if (e instanceof VirtualMachineError) {
        // Always rethrow VM errors (an attempt to wrap may fail).
        throw (VirtualMachineError) e;
      }
      InternalCompilerException ice;
      if (e instanceof InternalCompilerException) {
        ice = (InternalCompilerException) e;
      } else {
        ice = new InternalCompilerException("Error building type map", e);
      }
      ice.addNode(amd.getClass().getName(), amd.toString(), makeSourceInfo(amd,
          null));
      return ice;
    }

    private InternalCompilerException translateException(Statement stmt,
        Throwable e) {
      if (e instanceof VirtualMachineError) {
        // Always rethrow VM errors (an attempt to wrap may fail).
        throw (VirtualMachineError) e;
      }
      InternalCompilerException ice;
      if (e instanceof InternalCompilerException) {
        ice = (InternalCompilerException) e;
      } else {
        ice = new InternalCompilerException("Error building type map", e);
      }
      ice.addNode(stmt.getClass().getName(), stmt.toString(), makeSourceInfo(
          stmt, null));
      return ice;
    }
  }

  /**
   * Creates JNodes for every type and memorizes the mapping from the JDT
   * Binding to the corresponding JNode for each created type. Note that since
   * there could be forward references, it is not possible to set up super
   * types; it must be done is a subsequent pass.
   */
  private class BuildTypeMapVisitor extends SafeASTVisitor {

    @Override
    public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
      return process(memberTypeDeclaration);
    }

    @Override
    public boolean visit(TypeDeclaration typeDeclaration,
        CompilationUnitScope scope) {
      return process(typeDeclaration);
    }

    @Override
    public boolean visitValid(TypeDeclaration localTypeDeclaration,
        BlockScope scope) {
      assert (TypeDeclaration.kind(localTypeDeclaration.modifiers) != TypeDeclaration.INTERFACE_DECL);
      return process(localTypeDeclaration);
    }

    private SourceInfo makeSourceInfo(TypeDeclaration typeDecl) {
      CompilationResult compResult = typeDecl.compilationResult;
      int[] indexes = compResult.lineSeparatorPositions;
      String fileName = String.valueOf(compResult.fileName);
      int startLine = Util.getLineNumber(typeDecl.sourceStart, indexes, 0,
          indexes.length - 1);
      return program.createSourceInfo(typeDecl.sourceStart, typeDecl.bodyEnd,
          startLine, fileName);
    }

    private boolean process(TypeDeclaration typeDeclaration) {
      try {
        SourceTypeBinding binding = typeDeclaration.binding;
        String name = dotify(binding.compoundName);
        if (binding instanceof LocalTypeBinding) {
          char[] localName = binding.constantPoolName();
          name = new String(localName).replace('/', '.');
        }

        SourceInfo info = makeSourceInfo(typeDeclaration);
        typeMap.put(binding, createType(name, info, binding));
        return true;
      } catch (Throwable e) {
        throw translateException(typeDeclaration, e);
      }
    }

    private InternalCompilerException translateException(
        TypeDeclaration typeDecl, Throwable e) {
      if (e instanceof VirtualMachineError) {
        // Always rethrow VM errors (an attempt to wrap may fail).
        throw (VirtualMachineError) e;
      }
      InternalCompilerException ice;
      if (e instanceof InternalCompilerException) {
        ice = (InternalCompilerException) e;
      } else {
        ice = new InternalCompilerException("Error building type map", e);
      }
      ice.addNode(typeDecl.getClass().getName(), typeDecl.toString(),
          makeSourceInfo(typeDecl));
      return ice;
    }
  }

  /**
   * Resolves the scope of JS identifiers solely within the scope of a method.
   */
  private static class JsParameterResolver extends JsAbstractSymbolResolver {
    private final JsFunction jsFunction;

    public JsParameterResolver(JsFunction jsFunction) {
      this.jsFunction = jsFunction;
    }

    @Override
    public void resolve(JsNameRef x) {
      // Only resolve unqualified names
      if (x.getQualifier() == null) {
        JsName name = getScope().findExistingName(x.getIdent());

        // Ensure that we're resolving a name from the function's parameters
        JsNode node = name == null ? null : name.getStaticRef();
        if (node instanceof JsParameter) {
          JsParameter param = (JsParameter) node;
          if (jsFunction.getParameters().contains(param)) {
            x.resolve(name);
          }
        }
      }
    }
  }

  private interface ExternalTypeTask {

    void process(String klass, BinaryTypeBinding binding);
  }

  private class ExternalTypeCreator implements ExternalTypeTask {

    public void process(String klass, BinaryTypeBinding binding) {
      if (program.getFromTypeMap(klass) == null) {
        // NB(tobyr) There are a few cases where certain compiler intrinsic
        // types are only needed if the program references them
        // (e.g. boxed numeric types). If we don't have the binding for those
        // types, we can safely ignore it.
        createExternalType(klass, binding);
      }
    }
  }

  private class ExternalTypeResolver implements ExternalTypeTask {

    public void process(String klass, BinaryTypeBinding binding) {
      if (binding != null) {
        JDeclaredType type = program.getFromTypeMap(klass);
        resolve(type, binding);
      }
    }
  }

  // TODO: Remove this overload altogether at some point.

  public static TypeDeclaration[] exec(TypeMap typeMap,
      CompilationUnitDeclaration[] unitDecls, JsProgram jsProgram) {
    CompilationResults results = new CompilationResults(unitDecls,
        new HashMap<String, BinaryTypeBinding>(0));
    return exec(typeMap, results, jsProgram, TypeLinker.NULL_TYPE_LINKER);
  }

  public static TypeDeclaration[] exec(TypeMap typeMap,
      CompilationResults results, JsProgram jsProgram, TypeLinker linker) {
    BuildTypeMap btm = new BuildTypeMap(typeMap, jsProgram, linker,
        results.compiledUnits);
    Event buildTypeMapEvent = SpeedTracerLogger.start(CompilerEventType.BUILD_TYPE_MAP_FOR_AST);
    btm.createPeersForUnits();
    btm.resolveExternalTypes(results.binaryBindings);
    TypeDeclaration[] result = btm.createPeersForNonTypeDecls();
    buildTypeMapEvent.end();
    return result;
  }

  static String dotify(char[][] name) {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < name.length; ++i) {
      if (i > 0) {
        result.append('.');
      }

      result.append(name[i]);
    }
    return result.toString();
  }

  private final JsProgram jsProgram;
  private final TypeLinker linker;
  private final JProgram program;
  private final TypeMap typeMap;
  private final CompilationUnitDeclaration[] unitDecls;

  private BuildTypeMap(TypeMap typeMap, JsProgram jsProgram, TypeLinker linker,
      CompilationUnitDeclaration[] unitDecls) {
    this.typeMap = typeMap;
    this.program = typeMap.getProgram();
    this.jsProgram = jsProgram;
    this.linker = linker;
    this.unitDecls = unitDecls;
  }

  private void addThrownExceptions(MethodBinding methodBinding,
      JMethod method) {
    for (ReferenceBinding thrownBinding : methodBinding.thrownExceptions) {
      JClassType type = (JClassType) getType(thrownBinding.erasure());
      method.addThrownException(type);
    }
  }

  private JDeclaredType createExternalType(String name,
      ReferenceBinding binding) {
    SourceInfo sourceInfo = makeBinarySourceInfo(binding, program);
    JDeclaredType type = createType(name, sourceInfo, binding);
    typeMap.put(binding, type);
    return type;
  }

  private JField createField(SourceInfo info, FieldBinding binding,
      JDeclaredType enclosingType) {
    JType type = getType(binding.type);

    boolean isCompileTimeConstant = binding.isStatic() && (binding.isFinal())
        && (binding.constant() != Constant.NotAConstant)
        && (binding.type.isBaseType());
    assert (type instanceof JPrimitiveType || !isCompileTimeConstant);

    assert (!binding.isFinal() || !binding.isVolatile());
    Disposition disposition;
    if (isCompileTimeConstant) {
      disposition = Disposition.COMPILE_TIME_CONSTANT;
    } else if (binding.isFinal()) {
      disposition = Disposition.FINAL;
    } else if (binding.isVolatile()) {
      disposition = Disposition.VOLATILE;
    } else {
      disposition = Disposition.NONE;
    }

    JField field = program.createField(info, String.valueOf(binding.name),
        enclosingType, type, binding.isStatic(), disposition);
    typeMap.put(binding, field);
    info.addCorrelation(program.getCorrelator().by(field));
    return field;
  }

  private JField createField(SyntheticArgumentBinding binding,
      JDeclaredType enclosingType, Disposition disposition) {
    JType type = getType(binding.type);
    SourceInfo info = enclosingType.getSourceInfo().makeChild(
        BuildDeclMapVisitor.class, "Field " + String.valueOf(binding.name));
    JField field = program.createField(info, String.valueOf(binding.name),
        enclosingType, type, false, disposition);
    info.addCorrelation(program.getCorrelator().by(field));
    if (binding.matchingField != null) {
      typeMap.put(binding.matchingField, field);
    }
    typeMap.put(binding, field);
    return field;
  }

  private void createMethod(MethodBinding binding, SourceInfo info) {
    JDeclaredType enclosingType = (JDeclaredType) getType(
        binding.declaringClass);
    JMethod newMethod = processMethodBinding(binding, enclosingType, info);
    mapParameters(newMethod, binding, info);
  }

  private JParameter createParameter(TypeBinding paramType,
      JMethod enclosingMethod, SourceInfo info, int argPosition) {
    JType type = getType(paramType);
    // TODO(tobyr) Get the actual param name if it's present in debug info
    // or otherwise.
    String syntheticParamName = "arg" + argPosition;
    JParameter param = JProgram.createParameter(info, syntheticParamName, type,
        true, false, enclosingMethod);
    // Don't need to put the parameter in the TypeMap as it won't be looked
    // up for binary types.
    return param;
  }

  private JParameter createParameter(LocalVariableBinding binding,
      JMethod enclosingMethod, SourceInfo info) {
    JType type = getType(binding.type);
    JParameter param = JProgram.createParameter(info,
        String.valueOf(binding.name), type, binding.isFinal(), false,
        enclosingMethod);
    typeMap.put(binding, param);
    return param;
  }

  private JParameter createParameter(SyntheticArgumentBinding arg,
      String argName, JMethod enclosingMethod) {
    JType type = getType(arg.type);
    JParameter param = JProgram.createParameter(
        enclosingMethod.getSourceInfo().makeChild(BuildTypeMap.class,
            "Parameter " + argName), argName, type, true, false,
        enclosingMethod);
    return param;
  }

  private TypeDeclaration[] createPeersForNonTypeDecls() {
    // Traverse to create our JNode peers for each method, field,
    // parameter, and local
    BuildDeclMapVisitor v = new BuildDeclMapVisitor();
    for (CompilationUnitDeclaration unitDecl : unitDecls) {
      unitDecl.traverse(v, unitDecl.scope);
    }
    return v.getTypeDeclarataions();
  }

  private void createPeersForUnits() {
    // Traverse to create our JNode peers for each type
    BuildTypeMapVisitor v = new BuildTypeMapVisitor();
    for (CompilationUnitDeclaration unitDecl : unitDecls) {
      unitDecl.traverse(v, unitDecl.scope);
    }
  }

  private JDeclaredType createType(String name, SourceInfo info,
      ReferenceBinding binding) {

    JDeclaredType newType;
    if (binding.isClass()) {
      newType = program.createClass(info, name, binding.isAbstract(),
          binding.isFinal());
    } else if (binding.isInterface() || binding.isAnnotationType()) {
      newType = program.createInterface(info, name);
    } else if (binding.isEnum()) {
      if (binding.isAnonymousType()) {
        // Don't model an enum subclass as a JEnumType.
        newType = program.createClass(info, name, false, true);
      } else {
        newType = program.createEnum(info, name, binding.isAbstract());
      }
    } else {
      throw new InternalCompilerException(
          "ReferenceBinding is not a class, interface, or enum.");
    }

    info.addCorrelation(program.getCorrelator().by(newType));

    /**
     * We emulate static initializers and instance initializers as methods.
     * As in other cases, this gives us: simpler AST, easier to optimize,
     * more like output JavaScript. Clinit is always in slot 0, init (if it
     * exists) is always in slot 1.
     */
    JMethod clinit = program.createMethod(
        info.makeChild(BuildTypeMapVisitor.class, "Class initializer"),
        "$clinit", newType, program.getTypeVoid(), false, true, true, true,
        false);
    clinit.freezeParamTypes();
    clinit.setSynthetic();

    if (newType instanceof JClassType) {
      JMethod init = program.createMethod(
          info.makeChild(BuildTypeMapVisitor.class, "Instance initializer"),
          "$init", newType, program.getTypeVoid(), false, false, true, true,
          false);
      init.freezeParamTypes();
      init.setSynthetic();
    }

    newType.setExternal(linker.isExternalType(newType.getName()));
    return newType;
  }

  private void forEachExternalType(Map<String, BinaryTypeBinding> bindings,
      ExternalTypeTask task) {
    for (Map.Entry<String, BinaryTypeBinding> entry : bindings.entrySet()) {
      String klass = entry.getKey();
      if (linker.isExternalType(klass)) {
        BinaryTypeBinding binding = bindings.get(klass);
        if (binding != null) {
          task.process(klass, binding);
        }
      }
    }
  }

  private JType getType(TypeBinding binding) {
    JType type = (JType) typeMap.tryGet(binding);

    if (type != null) {
      return type;
    }

    if (binding instanceof ArrayBinding) {
      binding = ((ArrayBinding) binding).leafComponentType;
    }

    if (!(binding instanceof ReferenceBinding)) {
      throw new InternalCompilerException(
          "Expected a ReferenceBinding but received a " + binding.getClass());
    }

    ReferenceBinding refBinding = (ReferenceBinding) binding;
    String name = dotify(refBinding.compoundName);
    if (!linker.isExternalType(name)) {
      // typeMap.get() will fail with an appropriate exception
      return (JType) typeMap.get(binding);
    }

    type = createExternalType(name, refBinding);
    resolve((JDeclaredType) type, refBinding);
    return type;
  }

  private SourceInfo makeBinarySourceInfo(ReferenceBinding typeBinding,
      JProgram program) {
    char[] chars = typeBinding.getFileName();
    String fileName = chars == null ? "" : String.valueOf(chars);
    return program.createSourceInfo(-1, fileName);
  }

  private void mapParameters(JMethod method, AbstractMethodDeclaration x,
      SourceInfo info) {
    MethodBinding b = x.binding;
    int paramCount = (b.parameters != null ? b.parameters.length : 0);
    if (paramCount > 0) {
      for (int i = 0, n = x.arguments.length; i < n; ++i) {
        createParameter(x.arguments[i].binding, method, info);
      }
    }
    method.freezeParamTypes();
  }

  private void mapParameters(JMethod method, MethodBinding binding,
      SourceInfo info) {
    int paramCount = binding.parameters != null ? binding.parameters.length : 0;
    if (paramCount > 0) {
      int counter = 0;
      for (TypeBinding argType : binding.parameters) {
        createParameter(argType, method, info, counter++);
      }
    }
    method.freezeParamTypes();
  }

  private boolean process(ReferenceBinding binding) {
    JDeclaredType type = (JDeclaredType) getType(binding);

    try {
      // Create an override for getClass().
      if (type instanceof JClassType
          && type != program.getTypeJavaLangObject()) {

        JMethod getClassMethod = program.createMethod(
            type.getSourceInfo().makeChild(BuildDeclMapVisitor.class,
                "Synthetic getClass()"), "getClass", type,
            program.getTypeJavaLangClass(), false, false, false, false, false);
        assert (type.getMethods().get(2) == getClassMethod);
        getClassMethod.freezeParamTypes();
        getClassMethod.setSynthetic();
      }

      if (binding.isNestedType() && !binding.isStatic()
          && !(binding instanceof BinaryTypeBinding)) {
        // TODO(tobyr) Do something here for binary types?

        // add synthetic fields for outer this and locals
        assert (type instanceof JClassType);
        NestedTypeBinding nestedBinding = (NestedTypeBinding) binding;
        if (nestedBinding.enclosingInstances != null) {
          for (int i = 0; i < nestedBinding.enclosingInstances.length; ++i) {
            SyntheticArgumentBinding arg = nestedBinding.enclosingInstances[i];
            if (arg.matchingField != null) {
              createField(arg, type, Disposition.THIS_REF);
            }
          }
        }

        if (nestedBinding.outerLocalVariables != null) {
          for (int i = 0; i < nestedBinding.outerLocalVariables.length; ++i) {
            SyntheticArgumentBinding arg = nestedBinding.outerLocalVariables[i];
            // See InnerClassTest.testOuterThisFromSuperCall().
            boolean isReallyThisRef = false;
            if (arg.actualOuterLocalVariable instanceof SyntheticArgumentBinding) {
              SyntheticArgumentBinding outer = (SyntheticArgumentBinding) arg.actualOuterLocalVariable;
              if (outer.matchingField != null) {
                JField field = (JField) typeMap.get(outer.matchingField);
                if (field.isThisRef()) {
                  isReallyThisRef = true;
                }
              }
            }
            createField(arg, type, isReallyThisRef ? Disposition.THIS_REF
                : Disposition.FINAL);
          }
        }
      }

      ReferenceBinding superClassBinding = binding.superclass();
      if (type instanceof JClassType && superClassBinding != null) {
        // TODO: handle separately?
        assert (binding.superclass().isClass()
            || binding.superclass().isEnum());
        JClassType superClass = (JClassType) getType(superClassBinding);
        ((JClassType) type).setSuperClass(superClass);
      }

      ReferenceBinding[] superInterfaces = binding.superInterfaces();
      for (ReferenceBinding superInterfaceBinding : superInterfaces) {
        assert (superInterfaceBinding.isInterface());
        JInterfaceType superInterface = (JInterfaceType) getType(
            superInterfaceBinding);
        type.addImplements(superInterface);
      }

      ReferenceBinding enclosingBinding = binding.enclosingType();
      if (enclosingBinding != null) {
        type.setEnclosingType((JDeclaredType) getType(enclosingBinding));
      }

      if (type instanceof JEnumType) {
        processEnumType(binding, (JEnumType) type);
      }

      return true;
    } catch (VirtualMachineError e) {
      // Always rethrow VM errors (an attempt to wrap may fail).
      throw e;
    } catch (InternalCompilerException ice) {
      ice.addNode(type);
      throw ice;
    } catch (Throwable e) {
      throw new InternalCompilerException(type, "Error building type map", e);
    }
  }

  private JConstructor processConstructor(MethodBinding b,
      ConstructorDeclaration decl, SourceInfo info) {
    JClassType enclosingType = (JClassType) getType(b.declaringClass);
    JConstructor newCtor = program.createConstructor(info, enclosingType);

    // Enums have hidden arguments for name and value
    if (enclosingType.isEnumOrSubclass() != null) {
      JProgram.createParameter(info, "enum$name",
          program.getTypeJavaLangString(), true, false, newCtor);
      JProgram.createParameter(info, "enum$ordinal",
          program.getTypePrimitiveInt(), true, false, newCtor);
    }

    ReferenceBinding declaringClass = b.declaringClass;
    Set<String> alreadyNamedVariables = new HashSet<String>();
    if (declaringClass.isNestedType() && !declaringClass.isStatic()) {
      // TODO(tobyr) Do we have to do the equivalent for binary types here
      // or will this just fall out correctly?

      // add synthetic args for outer this
      NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClass;
      if (nestedBinding.enclosingInstances != null) {
        for (int i = 0; i < nestedBinding.enclosingInstances.length; ++i) {
          SyntheticArgumentBinding arg = nestedBinding.enclosingInstances[i];
          String argName = String.valueOf(arg.name);
          if (alreadyNamedVariables.contains(argName)) {
            argName += "_" + i;
          }
          createParameter(arg, argName, newCtor);
          alreadyNamedVariables.add(argName);
        }
      }
    }

    // user args
    if (decl == null) {
      mapParameters(newCtor, b, info);
    } else {
      mapParameters(newCtor, decl, info);
    }
    // original params are now frozen

    addThrownExceptions(b, newCtor);

    info.addCorrelation(program.getCorrelator().by(newCtor));

    if (declaringClass.isNestedType() && !declaringClass.isStatic()) {
      // add synthetic args for locals
      NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClass;
      // add synthetic args for outer this and locals
      if (nestedBinding.outerLocalVariables != null) {
        for (int i = 0; i < nestedBinding.outerLocalVariables.length; ++i) {
          SyntheticArgumentBinding arg = nestedBinding.outerLocalVariables[i];
          String argName = String.valueOf(arg.name);
          if (alreadyNamedVariables.contains(argName)) {
            argName += "_" + i;
          }
          createParameter(arg, argName, newCtor);
          alreadyNamedVariables.add(argName);
        }
      }
    }

    if (enclosingType.isExternal()) {
      newCtor.setBody(null);
    }

    typeMap.put(b, newCtor);
    return newCtor;
  }

  private void processEnumType(ReferenceBinding binding, JEnumType type) {
    // Visit the synthetic values() and valueOf() methods.
    for (MethodBinding methodBinding : binding.methods()) {
      if (methodBinding instanceof SyntheticMethodBinding) {
        JMethod newMethod = processMethodBinding(methodBinding, type,
            type.getSourceInfo());
        TypeBinding[] parameters = methodBinding.parameters;
        if (parameters.length == 0) {
          assert newMethod.getName().equals("values");
        } else if (parameters.length == 1) {
          assert newMethod.getName().equals("valueOf");
          assert typeMap.get(parameters[0]) == program.getTypeJavaLangString();
          JProgram.createParameter(
              newMethod.getSourceInfo().makeChild(BuildDeclMapVisitor.class,
                  "name parameter"), "name", program.getTypeJavaLangString(),
              true, false, newMethod);
        } else {
          assert false;
        }
        newMethod.freezeParamTypes();
      }
    }
  }

  private void processExternalMethod(MethodBinding binding, JDeclaredType type) {
    if (binding.isPrivate() || (type.getName().startsWith("java.")
        && !binding.isPublic() && !binding.isProtected())) {
      return;
    }
    if (binding.isConstructor()) {
      processConstructor(binding, null, type.getSourceInfo());
    } else {
      createMethod(binding, type.getSourceInfo());
    }
  }

  private JMethod processMethodBinding(MethodBinding b,
      JDeclaredType enclosingType, SourceInfo info) {
    JType returnType = getType(b.returnType);
    JMethod newMethod = program.createMethod(info, String.valueOf(b.selector),
        enclosingType, returnType, b.isAbstract(), b.isStatic(), b.isFinal(),
        b.isPrivate(), b.isNative());
    addThrownExceptions(b, newMethod);
    if (b.isSynthetic()) {
      newMethod.setSynthetic();
    }

    if (enclosingType.isExternal()) {
      newMethod.setBody(null);
    }
    typeMap.put(b, newMethod);
    return newMethod;
  }

  private void resolve(JDeclaredType type, ReferenceBinding binding) {
    process(binding);

    for (FieldBinding fieldBinding : binding.fields()) {
      if (fieldBinding.isPrivate() || (type.getName().startsWith("java.")
          && !fieldBinding.isPublic() && !fieldBinding.isProtected())) {
        continue;
      }

      createField(type.getSourceInfo(), fieldBinding, type);
    }

    for (MethodBinding methodBinding : binding.methods()) {
      processExternalMethod(methodBinding, type);
    }

    if (binding instanceof BinaryTypeBinding) {
      // Unlike SourceTypeBindings, we have to explicitly ask for bridge methods
      // for BinaryTypeBindings.
      try {
        // TODO(tobyr) Fix so we don't have to use reflection.
        Method m = BinaryTypeBinding.class.getDeclaredMethod("bridgeMethods");
        MethodBinding[] bridgeMethods = (MethodBinding[]) m.invoke(binding);

        for (MethodBinding methodBinding : bridgeMethods) {
          processExternalMethod(methodBinding, type);
        }
      } catch (Exception e) {
        throw new InternalCompilerException("Unexpected failure", e);
      }
    }
  }

  /**
   * Creates and resolves all external types.
   */
  private void resolveExternalTypes(Map<String, BinaryTypeBinding> bindings) {
    forEachExternalType(bindings, new ExternalTypeCreator());
    forEachExternalType(bindings, new ExternalTypeResolver());
  }
}
