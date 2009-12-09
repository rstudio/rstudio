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
import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JEnumType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNonNullType;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JField.Disposition;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.js.JsAbstractSymbolResolver;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsProgram;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
   * isn't done until {@link GenerateJavaDom}.
   */
  private static class BuildDeclMapVisitor extends ASTVisitor {

    private String currentFileName;

    private int[] currentSeparatorPositions;

    private final JsProgram jsProgram;
    private JProgram program;
    private List<TypeDeclaration> typeDecls = new ArrayList<TypeDeclaration>();
    private final TypeMap typeMap;

    public BuildDeclMapVisitor(TypeMap typeMap, JsProgram jsProgram) {
      this.typeMap = typeMap;
      program = this.typeMap.getProgram();
      this.jsProgram = jsProgram;
    }

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
        SourceInfo info = makeSourceInfo(argument, enclosingBody.getMethod());
        LocalVariableBinding b = argument.binding;
        JType localType = (JType) typeMap.get(b.type);
        JLocal newLocal = program.createLocal(info, argument.name, localType,
            b.isFinal(), enclosingBody);
        typeMap.put(b, newLocal);
        return true;
      } catch (Throwable e) {
        throw translateException(argument, e);
      }
    }

    /**
     * Weird: we used to have JConstructor (and JConstructorCall) in our AST,
     * but we got rid of them completely and instead model them as instance
     * methods whose qualifier is a naked no-argument new operation. See
     * {@link GenerateJavaAST.JavaASTGenerationVisitor#processConstructor(ConstructorDeclaration)}
     * for details.
     */
    @Override
    public boolean visit(ConstructorDeclaration ctorDecl, ClassScope scope) {
      try {
        MethodBinding b = ctorDecl.binding;
        JClassType enclosingType = (JClassType) typeMap.get(scope.enclosingSourceType());
        String name = enclosingType.getShortName();
        SourceInfo info = makeSourceInfo(ctorDecl, enclosingType);
        JMethod newMethod = program.createMethod(info, name.toCharArray(),
            enclosingType, program.getNonNullType(enclosingType), false, false,
            true, b.isPrivate(), false);

        // Enums have hidden arguments for name and value
        if (enclosingType.isEnumOrSubclass() != null) {
          program.createParameter(info, "enum$name".toCharArray(),
              program.getTypeJavaLangString(), true, false, newMethod);
          program.createParameter(info, "enum$ordinal".toCharArray(),
              program.getTypePrimitiveInt(), true, false, newMethod);
        }

        // user args
        mapParameters(newMethod, ctorDecl);
        // original params are now frozen

        info.addCorrelation(program.getCorrelator().by(newMethod));

        int syntheticParamCount = 0;
        ReferenceBinding declaringClass = b.declaringClass;
        if (declaringClass.isNestedType() && !declaringClass.isStatic()) {
          // add synthetic args for outer this and locals
          NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClass;
          Set<String> alreadyNamedVariables = new HashSet<String>();
          if (nestedBinding.enclosingInstances != null) {
            for (int i = 0; i < nestedBinding.enclosingInstances.length; ++i) {
              SyntheticArgumentBinding arg = nestedBinding.enclosingInstances[i];
              String argName = String.valueOf(arg.name);
              if (alreadyNamedVariables.contains(argName)) {
                argName += "_" + i;
              }
              createParameter(arg, argName, newMethod);
              ++syntheticParamCount;
              alreadyNamedVariables.add(argName);
            }
          }

          if (nestedBinding.outerLocalVariables != null) {
            for (int i = 0; i < nestedBinding.outerLocalVariables.length; ++i) {
              SyntheticArgumentBinding arg = nestedBinding.outerLocalVariables[i];
              String argName = String.valueOf(arg.name);
              if (alreadyNamedVariables.contains(argName)) {
                argName += "_" + i;
              }
              createParameter(arg, argName, newMethod);
              ++syntheticParamCount;
              alreadyNamedVariables.add(argName);
            }
          }
        }

        typeMap.put(b, newMethod);

        // Now let's implicitly create a static function called 'new' that will
        // allow construction from JSNI methods
        if (!enclosingType.isAbstract()) {
          ReferenceBinding enclosingBinding = ctorDecl.binding.declaringClass.enclosingType();
          JReferenceType outerType = enclosingBinding == null ? null
              : (JReferenceType) typeMap.get(enclosingBinding);
          createSyntheticConstructor(newMethod,
              ctorDecl.binding.declaringClass.isStatic(), outerType);
        }

        return true;
      } catch (Throwable e) {
        throw translateException(ctorDecl, e);
      }
    }

    @Override
    public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
      try {
        FieldBinding b = fieldDeclaration.binding;
        JDeclaredType enclosingType = (JDeclaredType) typeMap.get(scope.enclosingSourceType());
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
        JType localType = (JType) typeMap.get(localDeclaration.type.resolvedType);
        JMethodBody enclosingBody = findEnclosingMethod(scope);
        SourceInfo info = makeSourceInfo(localDeclaration,
            enclosingBody.getMethod());
        JLocal newLocal = program.createLocal(info, localDeclaration.name,
            localType, b.isFinal(), enclosingBody);
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
        JDeclaredType enclosingType = (JDeclaredType) typeMap.get(scope.enclosingSourceType());
        SourceInfo info = makeSourceInfo(methodDeclaration, enclosingType);
        JMethod newMethod = processMethodBinding(b, enclosingType, info);
        mapParameters(newMethod, methodDeclaration);
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
    public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
      return process(localTypeDeclaration);
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

    private JField createEnumField(SourceInfo info, FieldBinding binding,
        JReferenceType enclosingType) {
      JType type = (JType) typeMap.get(binding.type);
      JField field = program.createEnumField(info, binding.name,
          (JEnumType) enclosingType, (JClassType) type, binding.original().id);
      info.addCorrelation(program.getCorrelator().by(field));
      typeMap.put(binding, field);
      return field;
    }

    private JField createField(SourceInfo info, FieldBinding binding,
        JDeclaredType enclosingType) {
      JType type = (JType) typeMap.get(binding.type);

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

      JField field = program.createField(info, binding.name, enclosingType,
          type, binding.isStatic(), disposition);
      typeMap.put(binding, field);
      info.addCorrelation(program.getCorrelator().by(field));
      return field;
    }

    private JField createField(SyntheticArgumentBinding binding,
        JDeclaredType enclosingType) {
      JType type = (JType) typeMap.get(binding.type);
      SourceInfo info = enclosingType.getSourceInfo().makeChild(
          BuildDeclMapVisitor.class, "Field " + String.valueOf(binding.name));
      JField field = program.createField(info, binding.name, enclosingType,
          type, false, Disposition.FINAL);
      info.addCorrelation(program.getCorrelator().by(field));
      if (binding.matchingField != null) {
        typeMap.put(binding.matchingField, field);
      }
      typeMap.put(binding, field);
      return field;
    }

    private JParameter createParameter(LocalVariableBinding binding,
        JMethod enclosingMethod) {
      JType type = (JType) typeMap.get(binding.type);
      SourceInfo info = makeSourceInfo(binding.declaration, enclosingMethod);
      JParameter param = program.createParameter(info, binding.name, type,
          binding.isFinal(), false, enclosingMethod);
      typeMap.put(binding, param);
      return param;
    }

    private JParameter createParameter(SyntheticArgumentBinding arg,
        String argName, JMethod enclosingMethod) {
      JType type = (JType) typeMap.get(arg.type);
      JParameter param = program.createParameter(
          enclosingMethod.getSourceInfo().makeChild(BuildTypeMap.class,
              "Parameter " + argName), argName.toCharArray(), type, true,
          false, enclosingMethod);
      return param;
    }

    /**
     * Create a method that invokes the specified constructor. This is done as
     * an aid to JSNI users to be able to invoke a Java constructor via a method
     * named ::new.
     * 
     * @param constructor the constructor to invoke
     * @param staticClass indicates if the class being constructed is static
     * @param enclosingType the type that encloses the type that is to be
     *          constructed. This may be <code>null</code> if the class is a
     *          top-level type.
     */
    private JMethod createSyntheticConstructor(JMethod constructor,
        boolean staticClass, JReferenceType enclosingType) {
      JClassType type = (JClassType) constructor.getEnclosingType();

      // Define the method
      JMethod synthetic = program.createMethod(type.getSourceInfo().makeChild(
          BuildDeclMapVisitor.class, "Synthetic constructor"),
          "new".toCharArray(), type, program.getNonNullType(type), false, true,
          true, false, false);

      // new Foo() : Create the instance
      JNewInstance newInstance = new JNewInstance(
          type.getSourceInfo().makeChild(BuildDeclMapVisitor.class,
              "new instance"), (JNonNullType) synthetic.getType());

      // (new Foo()).Foo() : Invoke the constructor method on the instance
      JMethodCall call = new JMethodCall(type.getSourceInfo().makeChild(
          BuildDeclMapVisitor.class, "constructor invocation"), newInstance,
          constructor);
      /*
       * If the type isn't static, make the first parameter a reference to the
       * instance of the enclosing class. It's the first instance to allow the
       * JSNI qualifier to be moved without affecting evaluation order.
       */
      JParameter enclosingInstance = null;
      if (!staticClass) {
        enclosingInstance = program.createParameter(
            synthetic.getSourceInfo().makeChild(BuildDeclMapVisitor.class,
                "outer instance"), "this$outer".toCharArray(), enclosingType,
            false, false, synthetic);
      }

      /*
       * In one pass, add the parameters to the synthetic constructor and
       * arguments to the method call.
       */
      for (Iterator<JParameter> i = constructor.getParams().iterator(); i.hasNext();) {
        JParameter param = i.next();
        /*
         * This supports x.new Inner() by passing the enclosing instance
         * implicitly as the last argument to the constructor.
         */
        if (enclosingInstance != null && !i.hasNext()) {
          call.addArg(new JParameterRef(synthetic.getSourceInfo().makeChild(
              BuildDeclMapVisitor.class, "enclosing instance"),
              enclosingInstance));
        } else {
          JParameter syntheticParam = program.createParameter(
              synthetic.getSourceInfo().makeChild(BuildDeclMapVisitor.class,
                  "Argument " + param.getName()),
              param.getName().toCharArray(), param.getType(), true, false,
              synthetic);
          call.addArg(new JParameterRef(
              syntheticParam.getSourceInfo().makeChild(
                  BuildDeclMapVisitor.class, "reference"), syntheticParam));
        }
      }

      // Lock the method.
      synthetic.freezeParamTypes();

      // return (new Foo()).Foo() : The only statement in the function
      JReturnStatement ret = new JReturnStatement(
          synthetic.getSourceInfo().makeChild(BuildDeclMapVisitor.class,
              "Return statement"), call);

      // Add the return statement to the method body
      JMethodBody body = (JMethodBody) synthetic.getBody();
      body.getBlock().addStmt(ret);

      // Done
      return synthetic;
    }

    private JMethodBody findEnclosingMethod(BlockScope scope) {
      JMethod method;
      MethodScope methodScope = scope.methodScope();
      if (methodScope.isInsideInitializer()) {
        JDeclaredType enclosingType = (JDeclaredType) typeMap.get(scope.classScope().referenceContext.binding);
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
      return (JMethodBody) method.getBody();
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

    private void mapParameters(JMethod method, AbstractMethodDeclaration x) {
      MethodBinding b = x.binding;
      int paramCount = (b.parameters != null ? b.parameters.length : 0);
      if (paramCount > 0) {
        for (int i = 0, n = x.arguments.length; i < n; ++i) {
          createParameter(x.arguments[i].binding, method);
        }
      }
      method.freezeParamTypes();
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
      SourceTypeBinding binding = typeDeclaration.binding;

      if (binding.constantPoolName() == null) {
        /*
         * Weird case: if JDT determines that this local class is totally
         * uninstantiable, it won't bother allocating a local name.
         */
        return false;
      }
      JDeclaredType type = (JDeclaredType) typeMap.get(binding);
      try {
        // Create an override for getClass().
        if (type instanceof JClassType
            && type != program.getTypeJavaLangObject()
            && type != program.getIndexedType("Array")) {
          JMethod getClassMethod = program.createMethod(
              type.getSourceInfo().makeChild(BuildDeclMapVisitor.class,
                  "Synthetic getClass()"), "getClass".toCharArray(), type,
              program.getTypeJavaLangClass(), false, false, false, false, false);
          assert (type.getMethods().get(2) == getClassMethod);
          getClassMethod.freezeParamTypes();
        }

        if (binding.isNestedType() && !binding.isStatic()) {
          // add synthetic fields for outer this and locals
          assert (type instanceof JClassType);
          NestedTypeBinding nestedBinding = (NestedTypeBinding) binding;
          if (nestedBinding.enclosingInstances != null) {
            for (int i = 0; i < nestedBinding.enclosingInstances.length; ++i) {
              SyntheticArgumentBinding arg = nestedBinding.enclosingInstances[i];
              if (arg.matchingField != null) {
                createField(arg, type);
              }
            }
          }

          if (nestedBinding.outerLocalVariables != null) {
            for (int i = 0; i < nestedBinding.outerLocalVariables.length; ++i) {
              SyntheticArgumentBinding arg = nestedBinding.outerLocalVariables[i];
              createField(arg, type);
            }
          }
        }

        ReferenceBinding superClassBinding = binding.superclass();
        if (superClassBinding != null) {
          // TODO: handle separately?
          assert (binding.superclass().isClass() || binding.superclass().isEnum());
          JClassType superClass = (JClassType) typeMap.get(superClassBinding);
          type.setSuperClass(superClass);
        }

        ReferenceBinding[] superInterfaces = binding.superInterfaces();
        for (int i = 0; i < superInterfaces.length; ++i) {
          ReferenceBinding superInterfaceBinding = superInterfaces[i];
          assert (superInterfaceBinding.isInterface());
          JInterfaceType superInterface = (JInterfaceType) typeMap.get(superInterfaceBinding);
          type.addImplements(superInterface);
        }

        if (type instanceof JEnumType) {
          processEnumType(binding, (JEnumType) type);
        }

        typeDecls.add(typeDeclaration);
        return true;
      } catch (OutOfMemoryError e) {
        // Always rethrow OOMs (might have no memory to load ICE class anyway).
        throw e;
      } catch (InternalCompilerException ice) {
        ice.addNode(type);
        throw ice;
      } catch (Throwable e) {
        throw new InternalCompilerException(type, "Error building type map", e);
      }
    }

    private void processEnumType(SourceTypeBinding binding, JEnumType type) {
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
            program.createParameter(newMethod.getSourceInfo().makeChild(
                BuildDeclMapVisitor.class, "name parameter"),
                "name".toCharArray(), program.getTypeJavaLangString(), true,
                false, newMethod);
          } else {
            assert false;
          }
          newMethod.freezeParamTypes();
        }
      }
    }

    private JMethod processMethodBinding(MethodBinding b,
        JDeclaredType enclosingType, SourceInfo info) {
      JType returnType = (JType) typeMap.get(b.returnType);
      JMethod newMethod = program.createMethod(info, b.selector, enclosingType,
          returnType, b.isAbstract(), b.isStatic(), b.isFinal(), b.isPrivate(),
          b.isNative());

      typeMap.put(b, newMethod);
      return newMethod;
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
      if (e instanceof OutOfMemoryError) {
        // Always rethrow OOMs (might have no memory to load ICE class anyway).
        throw (OutOfMemoryError) e;
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
      if (e instanceof OutOfMemoryError) {
        // Always rethrow OOMs (might have no memory to load ICE class anyway).
        throw (OutOfMemoryError) e;
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
  private static class BuildTypeMapVisitor extends ASTVisitor {

    private final JProgram program;

    private final TypeMap typeMap;

    public BuildTypeMapVisitor(TypeMap typeMap) {
      this.typeMap = typeMap;
      program = this.typeMap.getProgram();
    }

    @Override
    public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
      assert (TypeDeclaration.kind(localTypeDeclaration.modifiers) != TypeDeclaration.INTERFACE_DECL);
      return process(localTypeDeclaration);
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
        char[][] name = typeDeclaration.binding.compoundName;
        SourceTypeBinding binding = typeDeclaration.binding;
        if (binding instanceof LocalTypeBinding) {
          char[] localName = binding.constantPoolName();
          if (localName == null) {
            /*
             * Weird case: if JDT determines that this local class is totally
             * uninstantiable, it won't bother allocating a local name.
             */
            return false;
          }

          for (int i = 0, c = localName.length; i < c; ++i) {
            if (localName[i] == '/') {
              localName[i] = '.';
            }
          }
          name = new char[1][0];
          name[0] = localName;
        }

        SourceInfo info = makeSourceInfo(typeDeclaration);
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
            newType = program.createEnum(info, name);
          }
        } else {
          assert (false);
          return false;
        }
        info.addCorrelation(program.getCorrelator().by(newType));

        /**
         * We emulate static initializers and instance initializers as methods.
         * As in other cases, this gives us: simpler AST, easier to optimize,
         * more like output JavaScript. Clinit is always in slot 0, init (if it
         * exists) is always in slot 1.
         */
        JMethod clinit = program.createMethod(info.makeChild(
            BuildTypeMapVisitor.class, "Class initializer"),
            "$clinit".toCharArray(), newType, program.getTypeVoid(), false,
            true, true, true, false);
        clinit.freezeParamTypes();

        if (newType instanceof JClassType) {
          JMethod init = program.createMethod(info.makeChild(
              BuildTypeMapVisitor.class, "Instance initializer"),
              "$init".toCharArray(), newType, program.getTypeVoid(), false,
              false, true, true, false);
          init.freezeParamTypes();
        }

        typeMap.put(binding, newType);
        return true;
      } catch (Throwable e) {
        throw translateException(typeDeclaration, e);
      }
    }

    private InternalCompilerException translateException(
        TypeDeclaration typeDecl, Throwable e) {
      if (e instanceof OutOfMemoryError) {
        // Always rethrow OOMs (might have no memory to load ICE class anyway).
        throw (OutOfMemoryError) e;
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
        if (name != null
            && jsFunction.getParameters().contains(name.getStaticRef())) {
          x.resolve(name);
        }
      }
    }
  }

  public static TypeDeclaration[] exec(TypeMap typeMap,
      CompilationUnitDeclaration[] unitDecls, JsProgram jsProgram) {
    createPeersForTypes(unitDecls, typeMap);
    return createPeersForNonTypeDecls(unitDecls, typeMap, jsProgram);
  }

  private static TypeDeclaration[] createPeersForNonTypeDecls(
      CompilationUnitDeclaration[] unitDecls, TypeMap typeMap,
      JsProgram jsProgram) {
    // Traverse again to create our JNode peers for each method, field,
    // parameter, and local
    BuildDeclMapVisitor v2 = new BuildDeclMapVisitor(typeMap, jsProgram);
    for (int i = 0; i < unitDecls.length; ++i) {
      unitDecls[i].traverse(v2, unitDecls[i].scope);
    }
    return v2.getTypeDeclarataions();
  }

  private static void createPeersForTypes(
      CompilationUnitDeclaration[] unitDecls, TypeMap typeMap) {
    // Traverse once to create our JNode peers for each type
    BuildTypeMapVisitor v1 = new BuildTypeMapVisitor(typeMap);
    for (int i = 0; i < unitDecls.length; ++i) {
      unitDecls[i].traverse(v1, unitDecls[i].scope);
    }
  }
}
