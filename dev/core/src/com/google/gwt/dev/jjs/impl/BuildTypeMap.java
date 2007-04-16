/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.js.JsniMethod;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.JsParserException.SourceDetail;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatements;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.env.IGenericType;
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
import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
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

    private static SourceInfo makeSourceInfo(
        AbstractMethodDeclaration methodDecl) {
      CompilationResult compResult = methodDecl.compilationResult;
      int[] indexes = compResult.lineSeparatorPositions;
      String fileName = String.valueOf(compResult.fileName);
      int startLine = ProblemHandler.searchLineNumber(indexes,
          methodDecl.sourceStart);
      return new SourceInfo(methodDecl.sourceStart, methodDecl.bodyEnd,
          startLine, fileName);
    }

    private static InternalCompilerException translateException(
        AbstractMethodDeclaration amd, Throwable e) {
      InternalCompilerException ice;
      if (e instanceof InternalCompilerException) {
        ice = (InternalCompilerException) e;
      } else {
        ice = new InternalCompilerException("Error building type map", e);
      }
      ice.addNode(amd.getClass().getName(), amd.toString(), makeSourceInfo(amd));
      return ice;
    }

    private String currentFileName;
    private int[] currentSeparatorPositions;
    private final JsParser jsParser = new JsParser();
    private final JsProgram jsProgram;
    private JProgram program;
    private ArrayList/* <TypeDeclaration> */typeDecls = new ArrayList/* <TypeDeclaration> */();

    private final TypeMap typeMap;

    public BuildDeclMapVisitor(TypeMap typeMap, JsProgram jsProgram) {
      this.typeMap = typeMap;
      program = this.typeMap.getProgram();
      this.jsProgram = jsProgram;
    }

    public TypeDeclaration[] getTypeDeclarataions() {
      return (TypeDeclaration[]) typeDecls.toArray(new TypeDeclaration[typeDecls.size()]);
    }

    public boolean visit(Argument argument, BlockScope scope) {
      try {
        if (scope == scope.methodScope()) {
          return true;
        }

        SourceInfo info = makeSourceInfo(argument);
        LocalVariableBinding b = argument.binding;
        JType localType = (JType) typeMap.get(b.type);
        JMethod enclosingMethod = findEnclosingMethod(scope);
        JLocal newLocal = program.createLocal(info, argument.name, localType,
            b.isFinal(), enclosingMethod);
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
    public boolean visit(ConstructorDeclaration ctorDecl, ClassScope scope) {
      try {
        MethodBinding b = ctorDecl.binding;
        JClassType enclosingType = (JClassType) typeMap.get(scope.enclosingSourceType());
        String name = enclosingType.getShortName();
        SourceInfo info = makeSourceInfo(ctorDecl);
        JMethod newMethod = program.createMethod(info, name.toCharArray(),
            enclosingType, enclosingType, false, false, true, b.isPrivate(),
            false);
        mapThrownExceptions(newMethod, ctorDecl);

        // user args
        mapParameters(newMethod, ctorDecl);
        // original params are now frozen

        int syntheticParamCount = 0;
        ReferenceBinding declaringClass = b.declaringClass;
        if (declaringClass.isNestedType() && !declaringClass.isStatic()) {
          // add synthetic args for outer this and locals
          NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClass;
          Set alreadyNamedVariables = new HashSet();
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
        return true;
      } catch (Throwable e) {
        throw translateException(ctorDecl, e);
      }
    }

    public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
      try {
        FieldBinding b = fieldDeclaration.binding;
        SourceInfo info = makeSourceInfo(fieldDeclaration);
        JReferenceType enclosingType = (JReferenceType) typeMap.get(scope.enclosingSourceType());
        createField(info, b, enclosingType,
            fieldDeclaration.initialization != null);
        return true;
      } catch (Throwable e) {
        throw translateException(fieldDeclaration, e);
      }
    }

    public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
      try {
        LocalVariableBinding b = localDeclaration.binding;
        JType localType = (JType) typeMap.get(localDeclaration.type.resolvedType);
        JMethod enclosingMethod = findEnclosingMethod(scope);
        SourceInfo info = makeSourceInfo(localDeclaration);
        JLocal newLocal = program.createLocal(info, localDeclaration.name,
            localType, b.isFinal(), enclosingMethod);
        typeMap.put(b, newLocal);
        return true;
      } catch (Throwable e) {
        throw translateException(localDeclaration, e);
      }
    }

    public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
      try {
        MethodBinding b = methodDeclaration.binding;
        SourceInfo info = makeSourceInfo(methodDeclaration);
        JType returnType = (JType) typeMap.get(methodDeclaration.returnType.resolvedType);
        JReferenceType enclosingType = (JReferenceType) typeMap.get(scope.enclosingSourceType());
        JMethod newMethod = program.createMethod(info,
            methodDeclaration.selector, enclosingType, returnType,
            b.isAbstract(), b.isStatic(), b.isFinal(), b.isPrivate(),
            b.isNative());

        mapThrownExceptions(newMethod, methodDeclaration);
        mapParameters(newMethod, methodDeclaration);
        typeMap.put(b, newMethod);

        if (newMethod.isNative()) {
          // Handle JSNI block
          char[] source = methodDeclaration.compilationResult().getCompilationUnit().getContents();
          String jsniCode = String.valueOf(source, methodDeclaration.bodyStart,
              methodDeclaration.bodyEnd - methodDeclaration.bodyStart + 1);
          int startPos = jsniCode.indexOf("/*-{");
          int endPos = jsniCode.lastIndexOf("}-*/");
          if (startPos < 0 && endPos < 0) {
            GenerateJavaAST.reportJsniError(
                info,
                methodDeclaration,
                "Native methods require a JavaScript implementation enclosed with /*-{ and }-*/");
            return true;
          }
          if (startPos < 0) {
            GenerateJavaAST.reportJsniError(info, methodDeclaration,
                "Unable to find start of native block; begin your JavaScript block with: /*-{");
            return true;
          }
          if (endPos < 0) {
            GenerateJavaAST.reportJsniError(
                info,
                methodDeclaration,
                "Unable to find end of native block; terminate your JavaScript block with: }-*/");
            return true;
          }

          startPos += 3; // move up to open brace
          endPos += 1; // move past close brace

          jsniCode = jsniCode.substring(startPos, endPos);

          // Here we parse it as an anonymous function, but we will give it a
          // name later when we generate the JavaScript during code generation.
          //
          String syntheticFnHeader = "function (";
          boolean first = true;
          for (int i = 0; i < newMethod.params.size(); ++i) {
            JParameter param = (JParameter) newMethod.params.get(i);
            if (first) {
              first = false;
            } else {
              syntheticFnHeader += ',';
            }
            syntheticFnHeader += param.getName();
          }
          syntheticFnHeader += ')';
          StringReader sr = new StringReader(syntheticFnHeader + '\n'
              + jsniCode);
          try {
            // start at -1 to avoid counting our synthetic header
            // TODO: get the character position start correct
            JsStatements result = jsParser.parse(jsProgram.getScope(), sr, -1);
            JsExprStmt jsExprStmt = (JsExprStmt) result.get(0);
            JsFunction jsFunction = (JsFunction) jsExprStmt.getExpression();
            ((JsniMethod) newMethod).setFunc(jsFunction);
          } catch (IOException e) {
            throw new InternalCompilerException(
                "Internal error parsing JSNI in method '" + newMethod
                    + "' in type '" + enclosingType.getName() + "'", e);
          } catch (JsParserException e) {
            /*
             * count the number of characters to the problem (from the start of
             * the JSNI code)
             */
            SourceDetail detail = e.getSourceDetail();
            int line = detail.getLine();
            char[] chars = jsniCode.toCharArray();
            int i = 0, n = chars.length;
            while (line > 0) {
              // CHECKSTYLE_OFF
              switch (chars[i]) {
                case '\r':
                  // if skip an extra character if this is a CR/LF
                  if (i + 1 < n && chars[i + 1] == '\n') {
                    ++i;
                  }
                  // intentional fall-through
                case '\n':
                  --line;
                  // intentional fall-through
                default:
                  ++i;
              }
              // CHECKSTYLE_ON
            }

            // TODO: check this
            // Map into the original source stream;
            i += startPos + detail.getLineOffset();
            info = new SourceInfo(i, i,
                info.getStartLine() + detail.getLine(), info.getFileName());
            GenerateJavaAST.reportJsniError(info, methodDeclaration,
                e.getMessage());
          }
        }

        return true;
      } catch (Throwable e) {
        throw translateException(methodDeclaration, e);
      }
    }

    public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
      return process(localTypeDeclaration);
    }

    public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
      return process(memberTypeDeclaration);
    }

    public boolean visit(TypeDeclaration typeDeclaration,
        CompilationUnitScope scope) {
      return process(typeDeclaration);
    }

    private JField createField(SourceInfo info, FieldBinding binding,
        JReferenceType enclosingType, boolean hasInitializer) {
      JType type = (JType) typeMap.get(binding.type);
      JField field = program.createField(info, binding.name, enclosingType,
          type, binding.isStatic(), binding.isFinal(), hasInitializer);
      typeMap.put(binding, field);
      return field;
    }

    private JField createField(SyntheticArgumentBinding binding,
        JReferenceType enclosingType) {
      JType type = (JType) typeMap.get(binding.type);
      JField field = program.createField(null, binding.name, enclosingType,
          type, false, true, true);
      if (binding.matchingField != null) {
        typeMap.put(binding.matchingField, field);
      }
      typeMap.put(binding, field);
      return field;
    }

    private JParameter createParameter(LocalVariableBinding binding,
        JMethod enclosingMethod) {
      JType type = (JType) typeMap.get(binding.type);
      SourceInfo info = makeSourceInfo(binding.declaration);
      JParameter param = program.createParameter(info, binding.name, type,
          binding.isFinal(), enclosingMethod);
      typeMap.put(binding, param);
      return param;
    }

    private JParameter createParameter(SyntheticArgumentBinding arg,
        String argName, JMethod enclosingMethod) {
      JType type = (JType) typeMap.get(arg.type);
      JParameter param = program.createParameter(null, argName.toCharArray(),
          type, true, enclosingMethod);
      return param;
    }

    private JMethod findEnclosingMethod(BlockScope scope) {
      MethodScope methodScope = scope.methodScope();
      if (methodScope.isInsideInitializer()) {
        JReferenceType enclosingType = (JReferenceType) typeMap.get(scope.classScope().referenceContext.binding);
        if (methodScope.isStatic) {
          // clinit
          return (JMethod) enclosingType.methods.get(0);
        } else {
          // init
          assert (enclosingType instanceof JClassType);
          return (JMethod) enclosingType.methods.get(1);
        }
      }

      AbstractMethodDeclaration referenceMethod = methodScope.referenceMethod();
      return (JMethod) typeMap.get(referenceMethod.binding);
    }

    private SourceInfo makeSourceInfo(Statement stmt) {
      int startLine = ProblemHandler.searchLineNumber(
          currentSeparatorPositions, stmt.sourceStart);
      return new SourceInfo(stmt.sourceStart, stmt.sourceEnd, startLine,
          currentFileName);
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

    private void mapThrownExceptions(JMethod method, AbstractMethodDeclaration x) {
      MethodBinding b = x.binding;
      if (b.thrownExceptions != null) {
        for (int i = 0; i < b.thrownExceptions.length; ++i) {
          ReferenceBinding refBinding = b.thrownExceptions[i];
          JClassType thrownException = (JClassType) typeMap.get(refBinding);
          method.thrownExceptions.add(thrownException);
        }
      }
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
      JReferenceType type = (JReferenceType) typeMap.get(binding);
      try {
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
          assert (binding.superclass().isClass());
          JClassType superClass = (JClassType) typeMap.get(superClassBinding);
          type.extnds = superClass;
        }

        ReferenceBinding[] superInterfaces = binding.superInterfaces();
        for (int i = 0; i < superInterfaces.length; ++i) {
          ReferenceBinding superInterfaceBinding = superInterfaces[i];
          assert (superInterfaceBinding.isInterface());
          JInterfaceType superInterface = (JInterfaceType) typeMap.get(superInterfaceBinding);
          type.implments.add(superInterface);
        }
        typeDecls.add(typeDeclaration);
        return true;
      } catch (InternalCompilerException ice) {
        ice.addNode(type);
        throw ice;
      } catch (Throwable e) {
        throw new InternalCompilerException(type, "Error building type map", e);
      }
    }

    private InternalCompilerException translateException(Statement stmt,
        Throwable e) {
      InternalCompilerException ice;
      if (e instanceof InternalCompilerException) {
        ice = (InternalCompilerException) e;
      } else {
        ice = new InternalCompilerException("Error building type map", e);
      }
      ice.addNode(stmt.getClass().getName(), stmt.toString(),
          makeSourceInfo(stmt));
      return ice;
    }
  }

  /**
   * Creates JNodes for every type and memorizes the mapping from the JDT
   * Binding to the corresponding JNode for each created type. Note that since
   * there could be forward references, it is not possible to set up super types;
   * it must be done is a subsequent pass.
   */
  private static class BuildTypeMapVisitor extends ASTVisitor {

    private static SourceInfo makeSourceInfo(TypeDeclaration typeDecl) {
      CompilationResult compResult = typeDecl.compilationResult;
      int[] indexes = compResult.lineSeparatorPositions;
      String fileName = String.valueOf(compResult.fileName);
      int startLine = ProblemHandler.searchLineNumber(indexes,
          typeDecl.sourceStart);
      return new SourceInfo(typeDecl.sourceStart, typeDecl.bodyEnd, startLine,
          fileName);
    }

    private static InternalCompilerException translateException(
        TypeDeclaration typeDecl, Throwable e) {
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

    private final JProgram program;
    private final TypeMap typeMap;

    public BuildTypeMapVisitor(TypeMap typeMap) {
      this.typeMap = typeMap;
      program = this.typeMap.getProgram();
    }

    public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
      assert (localTypeDeclaration.kind() != IGenericType.INTERFACE_DECL);
      return process(localTypeDeclaration);
    }

    public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
      return process(memberTypeDeclaration);
    }

    public boolean visit(TypeDeclaration typeDeclaration,
        CompilationUnitScope scope) {
      return process(typeDeclaration);
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
        JReferenceType newType;
        if (binding.isClass()) {
          newType = program.createClass(info, name, binding.isAbstract(),
              binding.isFinal());
        } else if (binding.isInterface()) {
          newType = program.createInterface(info, name);
        } else {
          assert (false);
          return false;
        }

        /**
         * We emulate static initializers and instance initializers as methods.
         * As in other cases, this gives us: simpler AST, easier to optimize,
         * more like output JavaScript. Clinit is always in slot 0, init (if it
         * exists) is always in slot 1.
         */
        JMethod clinit = program.createMethod(null, "$clinit".toCharArray(),
            newType, program.getTypeVoid(), false, true, true, true, false);
        clinit.freezeParamTypes();

        if (newType instanceof JClassType) {
          JMethod init = program.createMethod(null, "$init".toCharArray(),
              newType, program.getTypeVoid(), false, false, true, true, false);
          init.freezeParamTypes();
        }

        typeMap.put(binding, newType);
        return true;
      } catch (Throwable e) {
        throw translateException(typeDeclaration, e);
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
