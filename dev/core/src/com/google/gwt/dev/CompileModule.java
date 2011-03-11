/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.jjs.CorrelationFactory;
import com.google.gwt.dev.jjs.CorrelationFactory.DummyCorrelationFactory;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.InternalCompilerException.NodeInfo;
import com.google.gwt.dev.jjs.JJSOptionsImpl;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.BuildTypeMap;
import com.google.gwt.dev.jjs.impl.GenerateJavaAST;
import com.google.gwt.dev.jjs.impl.GwtAstBuilder;
import com.google.gwt.dev.jjs.impl.TypeLinker;
import com.google.gwt.dev.jjs.impl.TypeMap;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerModuleName;
import com.google.gwt.dev.util.arg.ArgHandlerOutDir;
import com.google.gwt.dev.util.arg.OptionOutDir;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles a GWT module.
 */
public class CompileModule {

  static class ArgProcessor extends ArgProcessorBase {
    public ArgProcessor(CompileModuleOptions options) {
      registerHandler(new ArgHandlerLogLevel(options));
      registerHandler(new ArgHandlerOutDir(options) {
        @Override
        public String[] getDefaultArgs() {
          return new String[]{getTag(), "bin"};
        }
      });
      registerHandler(new ArgHandlerModuleName(options));
    }

    @Override
    protected String getName() {
      return CompileModule.class.getName();
    }
  }

  interface CompileModuleOptions extends CompileTaskOptions, OptionOutDir {
  }

  static class CompileModuleOptionsImpl extends CompileTaskOptionsImpl implements
      CompileModuleOptions {

    private File outDir;

    public CompileModuleOptionsImpl() {
    }

    public CompileModuleOptionsImpl(CompileModuleOptions other) {
      copyFrom(other);
    }

    public void copyFrom(CompileModuleOptions other) {
      super.copyFrom(other);
      setOutDir(other.getOutDir());
    }

    public File getOutDir() {
      return outDir;
    }

    public void setOutDir(File outDir) {
      this.outDir = outDir;
    }
  }

  public static JProgram buildGenerateJavaAst(final TreeLogger logger, ModuleDef module,
      final CompilationState compilationState) throws UnableToCompleteException {
    final StandardGeneratorContext genCtx =
        new StandardGeneratorContext(compilationState, module, null, new ArtifactSet(), true);
    RebindPermutationOracle rpo = new RebindPermutationOracle() {
      public void clear() {
      }

      public String[] getAllPossibleRebindAnswers(TreeLogger logger, String sourceTypeName)
          throws UnableToCompleteException {
        return new String[0];
      }

      public CompilationState getCompilationState() {
        return compilationState;
      }

      public StandardGeneratorContext getGeneratorContext() {
        return genCtx;
      }
    };

    List<String> allRootTypes = new ArrayList<String>();
    for (CompilationUnit unit : compilationState.getCompilationUnits()) {
      allRootTypes.add(unit.getTypeName());
    }
    CompilationUnitDeclaration[] goldenCuds =
        WebModeCompilerFrontEnd.getCompilationUnitDeclarations(logger, allRootTypes
            .toArray(new String[allRootTypes.size()]), rpo, TypeLinker.NULL_TYPE_LINKER).compiledUnits;

    CorrelationFactory correlator = DummyCorrelationFactory.INSTANCE;
    JsProgram jsProgram = new JsProgram(correlator);
    JProgram jprogram = new JProgram(correlator);
    TypeMap typeMap = new TypeMap(jprogram);
    TypeDeclaration[] allTypeDeclarations = BuildTypeMap.exec(typeMap, goldenCuds, jsProgram);
    // BuildTypeMap can uncover syntactic JSNI errors; report & abort
    checkForErrors(logger, goldenCuds);

    // Compute all super type/sub type info
    jprogram.typeOracle.computeBeforeAST();

    // (2) Create our own Java AST from the JDT AST.
    GenerateJavaAST.exec(allTypeDeclarations, typeMap, jprogram, new JJSOptionsImpl());

    // GenerateJavaAST can uncover semantic JSNI errors; report & abort
    checkForErrors(logger, goldenCuds);
    return jprogram;
  }

  public static CompilationState buildGwtAst(TreeLogger logger, ModuleDef module)
      throws UnableToCompleteException {
    boolean gwtAstWasEnabled = GwtAstBuilder.ENABLED;
    try {
      GwtAstBuilder.ENABLED = true;
      long start = System.currentTimeMillis();
      final CompilationState compilationState = module.getCompilationState(logger);
      logger.log(TreeLogger.INFO, (System.currentTimeMillis() - start)
          + " time to get compilation state");
      return compilationState;
    } finally {
      GwtAstBuilder.ENABLED = gwtAstWasEnabled;
    }
  }

  public static void main(String[] args) {
    Memory.initialize();
    if (System.getProperty("gwt.jjs.dumpAst") != null) {
      System.out.println("Will dump AST to: " + System.getProperty("gwt.jjs.dumpAst"));
    }

    SpeedTracerLogger.init();

    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final CompileModuleOptions options = new CompileModuleOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          // TODO: updates?
          return new CompileModule(options).run(logger);
        }
      };
      if (CompileTaskRunner.runWithAppropriateLogger(options, task)) {
        // Exit w/ success code.
        System.exit(0);
      }
    }
    // Exit w/ non-success code.
    System.exit(1);
  }

  static UnableToCompleteException logAndTranslateException(TreeLogger logger, Throwable e) {
    if (e instanceof UnableToCompleteException) {
      // just rethrow
      return (UnableToCompleteException) e;
    } else if (e instanceof InternalCompilerException) {
      TreeLogger topBranch =
          logger.branch(TreeLogger.ERROR, "An internal compiler exception occurred", e);
      List<NodeInfo> nodeTrace = ((InternalCompilerException) e).getNodeTrace();
      for (NodeInfo nodeInfo : nodeTrace) {
        SourceInfo info = nodeInfo.getSourceInfo();
        String msg;
        if (info != null) {
          String fileName = info.getFileName();
          fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
          fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
          msg = "at " + fileName + "(" + info.getStartLine() + "): ";
        } else {
          msg = "<no source info>: ";
        }

        String description = nodeInfo.getDescription();
        if (description != null) {
          msg += description;
        } else {
          msg += "<no description available>";
        }
        TreeLogger nodeBranch = topBranch.branch(TreeLogger.ERROR, msg, null);
        String className = nodeInfo.getClassName();
        if (className != null) {
          nodeBranch.log(TreeLogger.INFO, className, null);
        }
      }
      return new UnableToCompleteException();
    } else if (e instanceof VirtualMachineError) {
      // Always rethrow VM errors (an attempt to wrap may fail).
      throw (VirtualMachineError) e;
    } else {
      logger.log(TreeLogger.ERROR, "Unexpected internal compiler error", e);
      return new UnableToCompleteException();
    }
  }

  private static void checkForErrors(TreeLogger logger, CompilationUnitDeclaration[] goldenCuds)
      throws UnableToCompleteException {
    for (CompilationUnitDeclaration cud : goldenCuds) {
      CompilationResult result = cud.compilationResult();
      if (result.hasErrors()) {
        logger.log(TreeLogger.ERROR, "Aborting on '" + String.valueOf(cud.getFileName()) + "'");
        throw new UnableToCompleteException();
      }
    }
  }

  private final CompileModuleOptionsImpl options;

  public CompileModule(CompileModuleOptions options) {
    this.options = new CompileModuleOptionsImpl(options);
  }

  public boolean run(final TreeLogger logger) {
    try {
      ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, options.getModuleNames().get(0));
      final CompilationState compilationState = buildGwtAst(logger, module);

      long start = System.currentTimeMillis();
      Map<String, JDeclaredType> compStateTypes = new HashMap<String, JDeclaredType>();
      for (CompilationUnit unit : compilationState.getCompilationUnits()) {
        for (JDeclaredType type : unit.getTypes()) {
          compStateTypes.put(type.getName(), type);
        }
      }
      logger.log(TreeLogger.INFO, (System.currentTimeMillis() - start) + " time to get all types");

      start = System.currentTimeMillis();
      JProgram jprogram = buildGenerateJavaAst(logger, module, compilationState);
      logger.log(TreeLogger.INFO, (System.currentTimeMillis() - start) + " time to build old AST");

      for (JDeclaredType genJavaAstType : jprogram.getDeclaredTypes()) {
        String typeName = genJavaAstType.getName();
        if ("com.google.gwt.core.client.JavaScriptObject".equals(typeName)) {
          // Known mismatch; genJavaAst version implements all JSO interfaces.
          continue;
        }
        JDeclaredType compStateType = compStateTypes.get(typeName);
        if (compStateType == null) {
          System.out.println("No matching prebuilt type for '" + typeName + "'");
        } else {
          String oldSource = genJavaAstType.toSource();
          String newSource = compStateType.toSource();
          if (!oldSource.equals(newSource)) {
            System.out.println("Mismatched output for '" + typeName + "'");
            System.out.println("GenerateJavaAST:");
            System.out.println(oldSource);
            System.out.println("GwtAstBuilder:");
            System.out.println(newSource);
          }
        }
      }

      return !compilationState.hasErrors();
    } catch (Throwable e) {
      logAndTranslateException(logger, e);
      return false;
    }
  }
}
