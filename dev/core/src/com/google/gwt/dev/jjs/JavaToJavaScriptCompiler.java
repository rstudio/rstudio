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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.jjs.InternalCompilerException.NodeInfo;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.impl.ArrayNormalizer;
import com.google.gwt.dev.jjs.impl.AssertionNormalizer;
import com.google.gwt.dev.jjs.impl.AssertionRemover;
import com.google.gwt.dev.jjs.impl.BuildTypeMap;
import com.google.gwt.dev.jjs.impl.CastNormalizer;
import com.google.gwt.dev.jjs.impl.CatchBlockNormalizer;
import com.google.gwt.dev.jjs.impl.DeadCodeElimination;
import com.google.gwt.dev.jjs.impl.EqualityNormalizer;
import com.google.gwt.dev.jjs.impl.Finalizer;
import com.google.gwt.dev.jjs.impl.FixAssignmentToUnbox;
import com.google.gwt.dev.jjs.impl.GenerateJavaAST;
import com.google.gwt.dev.jjs.impl.GenerateJavaScriptAST;
import com.google.gwt.dev.jjs.impl.JavaScriptObjectNormalizer;
import com.google.gwt.dev.jjs.impl.JsoDevirtualizer;
import com.google.gwt.dev.jjs.impl.LongCastNormalizer;
import com.google.gwt.dev.jjs.impl.LongEmulationNormalizer;
import com.google.gwt.dev.jjs.impl.MakeCallsStatic;
import com.google.gwt.dev.jjs.impl.MethodCallTightener;
import com.google.gwt.dev.jjs.impl.MethodInliner;
import com.google.gwt.dev.jjs.impl.PostOptimizationCompoundAssignmentNormalizer;
import com.google.gwt.dev.jjs.impl.Pruner;
import com.google.gwt.dev.jjs.impl.ReplaceRebinds;
import com.google.gwt.dev.jjs.impl.TypeMap;
import com.google.gwt.dev.jjs.impl.TypeTightener;
import com.google.gwt.dev.js.JsIEBlockSizeVisitor;
import com.google.gwt.dev.js.JsInliner;
import com.google.gwt.dev.js.JsNormalizer;
import com.google.gwt.dev.js.JsObfuscateNamer;
import com.google.gwt.dev.js.JsPrettyNamer;
import com.google.gwt.dev.js.JsSourceGenerationVisitor;
import com.google.gwt.dev.js.JsStaticEval;
import com.google.gwt.dev.js.JsStringInterner;
import com.google.gwt.dev.js.JsSymbolResolver;
import com.google.gwt.dev.js.JsUnusedFunctionRemover;
import com.google.gwt.dev.js.JsVerboseNamer;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compiles the Java <code>JProgram</code> representation into its
 * corresponding JavaScript source.
 */
public class JavaToJavaScriptCompiler {

  private static void findEntryPoints(TreeLogger logger,
      RebindOracle rebindOracle, String[] mainClassNames, JProgram program)
      throws UnableToCompleteException {
    JMethod bootStrapMethod = program.createMethod(null, "init".toCharArray(),
        null, program.getTypeVoid(), false, true, true, false, false);
    bootStrapMethod.freezeParamTypes();

    JMethodBody body = (JMethodBody) bootStrapMethod.getBody();
    for (int i = 0; i < mainClassNames.length; ++i) {
      String mainClassName = mainClassNames[i];
      JReferenceType referenceType = program.getFromTypeMap(mainClassName);

      if (referenceType == null) {
        logger.log(TreeLogger.ERROR,
            "Could not find module entry point class '" + mainClassName + "'",
            null);
        throw new UnableToCompleteException();
      }

      JExpression qualifier = null;
      JMethod mainMethod = findMainMethod(referenceType);
      if (mainMethod == null || !mainMethod.isStatic()) {
        // Couldn't find a static main method; must rebind the class
        String originalClassName = mainClassName;
        mainClassName = rebindOracle.rebind(logger, originalClassName);
        referenceType = program.getFromTypeMap(mainClassName);
        if (referenceType == null) {
          logger.log(TreeLogger.ERROR,
              "Could not find module entry point class '" + mainClassName
                  + "' after rebinding from '" + originalClassName + "'", null);
          throw new UnableToCompleteException();
        }

        if (!(referenceType instanceof JClassType)) {
          logger.log(TreeLogger.ERROR, "Module entry point class '"
              + mainClassName + "' must be a class", null);
          throw new UnableToCompleteException();
        }

        JClassType mainClass = (JClassType) referenceType;
        if (mainClass.isAbstract()) {
          logger.log(TreeLogger.ERROR, "Module entry point class '"
              + mainClassName + "' must not be abstract", null);
          throw new UnableToCompleteException();
        }

        mainMethod = findMainMethodRecurse(referenceType);
        if (mainMethod == null) {
          logger.log(TreeLogger.ERROR,
              "Could not find entry method 'onModuleLoad()' method in entry point class '"
                  + mainClassName + "'", null);
          throw new UnableToCompleteException();
        }

        if (mainMethod.isAbstract()) {
          logger.log(TreeLogger.ERROR,
              "Entry method 'onModuleLoad' in entry point class '"
                  + mainClassName + "' must not be abstract", null);
          throw new UnableToCompleteException();
        }

        if (!mainMethod.isStatic()) {
          // Find the appropriate (noArg) constructor
          JMethod noArgCtor = null;
          for (int j = 0; j < mainClass.methods.size(); ++j) {
            JMethod ctor = mainClass.methods.get(j);
            if (ctor.getName().equals(mainClass.getShortName())) {
              if (ctor.params.size() == 0) {
                noArgCtor = ctor;
              }
            }
          }
          if (noArgCtor == null) {
            logger.log(
                TreeLogger.ERROR,
                "No default (zero argument) constructor could be found in entry point class '"
                    + mainClassName
                    + "' to qualify a call to non-static entry method 'onModuleLoad'",
                null);
            throw new UnableToCompleteException();
          }

          // Construct a new instance of the class to qualify the non-static
          // call
          JNewInstance newInstance = new JNewInstance(program, null, mainClass);
          qualifier = new JMethodCall(program, null, newInstance, noArgCtor);
        }
      }

      // qualifier will be null if onModuleLoad is static
      JMethodCall onModuleLoadCall = new JMethodCall(program, null, qualifier,
          mainMethod);

      body.getStatements().add(makeStatsCalls(program, mainClassName));
      body.getStatements().add(onModuleLoadCall.makeStatement());
    }
    program.addEntryMethod(bootStrapMethod);
  }

  private static JMethod findMainMethod(JReferenceType referenceType) {
    for (int j = 0; j < referenceType.methods.size(); ++j) {
      JMethod method = referenceType.methods.get(j);
      if (method.getName().equals("onModuleLoad")) {
        if (method.params.size() == 0) {
          return method;
        }
      }
    }
    return null;
  }

  private static JMethod findMainMethodRecurse(JReferenceType referenceType) {
    for (JReferenceType it = referenceType; it != null; it = it.extnds) {
      JMethod result = findMainMethod(it);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Create a variable assignment to invoke a call to the statistics collector.
   * 
   * <pre>
   * Stats.isStatsAvailable() &&
   *   Stats.onModuleStart("mainClassName");
   * </pre>
   */
  private static JStatement makeStatsCalls(JProgram program,
      String mainClassName) {
    JMethod isStatsAvailableMethod = program.getIndexedMethod("Stats.isStatsAvailable");
    JMethod onModuleStartMethod = program.getIndexedMethod("Stats.onModuleStart");

    JMethodCall availableCall = new JMethodCall(program, null, null,
        isStatsAvailableMethod);
    JMethodCall onModuleStartCall = new JMethodCall(program, null, null,
        onModuleStartMethod);
    onModuleStartCall.getArgs().add(program.getLiteralString(mainClassName));

    JBinaryOperation amp = new JBinaryOperation(program, null,
        program.getTypePrimitiveBoolean(), JBinaryOperator.AND, availableCall,
        onModuleStartCall);

    return amp.makeStatement();
  }

  private final String[] declEntryPoints;
  private final CompilationUnitDeclaration[] goldenCuds;
  private final JJSOptions options;
  private final Set<IProblem> problemSet = new HashSet<IProblem>();

  public JavaToJavaScriptCompiler(TreeLogger logger,
      WebModeCompilerFrontEnd compiler, String[] declEntryPts)
      throws UnableToCompleteException {
    this(logger, compiler, declEntryPts, new JJSOptions());
  }

  public JavaToJavaScriptCompiler(TreeLogger logger,
      WebModeCompilerFrontEnd compiler, String[] declEntryPts,
      JJSOptions compilerOptions) throws UnableToCompleteException {

    if (declEntryPts.length == 0) {
      throw new IllegalArgumentException("entry point(s) required");
    }

    this.options = new JJSOptions(compilerOptions);

    // Remember these for subsequent compiles.
    //
    this.declEntryPoints = declEntryPts;

    if (!options.isValidateOnly()) {
      // Find all the possible rebound entry points.
      RebindPermutationOracle rpo = compiler.getRebindPermutationOracle();
      Set<String> allEntryPoints = new TreeSet<String>();
      for (String element : declEntryPts) {
        String[] all = rpo.getAllPossibleRebindAnswers(logger, element);
        Util.addAll(allEntryPoints, all);
      }
      allEntryPoints.addAll(JProgram.CODEGEN_TYPES_SET);
      allEntryPoints.addAll(JProgram.INDEX_TYPES_SET);
      allEntryPoints.add("java.lang.Object");
      allEntryPoints.add("java.lang.String");
      allEntryPoints.add("java.lang.Iterable");
      declEntryPts = allEntryPoints.toArray(new String[0]);
    }

    // Compile the source and get the compiler so we can get the parse tree
    //
    goldenCuds = compiler.getCompilationUnitDeclarations(logger, declEntryPts);

    // Check for compilation problems. We don't log here because any problems
    // found here will have already been logged by AbstractCompiler.
    //
    checkForErrors(logger, false);
  }

  /**
   * Creates finished JavaScript source code from the specified Java compilation
   * units.
   */
  public String compile(TreeLogger logger, RebindOracle rebindOracle)
      throws UnableToCompleteException {

    try {

      JProgram jprogram = new JProgram(logger, rebindOracle);
      JsProgram jsProgram = new JsProgram();

      if (JProgram.isTracingEnabled()) {
        System.out.println("------------------------------------------------------------");
        System.out.println("|                     (new permuation)                     |");
        System.out.println("------------------------------------------------------------");
      }

      {
        /*
         * (1) Build a flattened map of TypeDeclarations => JType. The resulting
         * map contains entries for all reference types. BuildTypeMap also
         * parses all JSNI.
         */
        TypeMap typeMap = new TypeMap(jprogram);
        TypeDeclaration[] allTypeDeclarations = BuildTypeMap.exec(typeMap,
            goldenCuds, jsProgram);

        // BuildTypeMap can uncover syntactic JSNI errors; report & abort
        checkForErrors(logger, true);

        // Compute all super type/sub type info
        jprogram.typeOracle.computeBeforeAST();

        // (2) Create our own Java AST from the JDT AST.
        GenerateJavaAST.exec(allTypeDeclarations, typeMap, jprogram, jsProgram,
            options.isEnableAssertions());

        // GenerateJavaAST can uncover semantic JSNI errors; report & abort
        checkForErrors(logger, true);

        // Enable GC.
        typeMap = null;
        allTypeDeclarations = null;
      }

      // (3) Perform Java AST normalizations.

      FixAssignmentToUnbox.exec(jprogram);

      /*
       * TODO: If we defer this until later, we could maybe use the results of
       * the assertions to enable more optimizations.
       */
      if (options.isEnableAssertions()) {
        // Turn into assertion checking calls.
        AssertionNormalizer.exec(jprogram);
      } else {
        // Remove all assert statements.
        AssertionRemover.exec(jprogram);
      }

      // Resolve all rebinds through GWT.create().
      ReplaceRebinds.exec(jprogram);

      if (options.isValidateOnly()) {
        // That's it, we're done.
        return null;
      }

      // Also rebind all non-static entry points.
      findEntryPoints(logger, rebindOracle, declEntryPoints, jprogram);

      // Replace references to JSO subtypes with JSO itself.
      JavaScriptObjectNormalizer.exec(jprogram);

      // (4) Optimize the normalized Java AST
      boolean didChange;
      do {
        // Recompute clinits each time, they can become empty.
        jprogram.typeOracle.recomputeClinits();

        didChange = false;
        // Remove unreferenced types, fields, methods, [params, locals]
        didChange = Pruner.exec(jprogram, true) || didChange;
        // finalize locals, params, fields, methods, classes
        didChange = Finalizer.exec(jprogram) || didChange;
        // rewrite non-polymorphic calls as static calls; update all call sites
        didChange = MakeCallsStatic.exec(jprogram) || didChange;

        // type flow tightening
        // - fields, locals based on assignment
        // - params based on assignment and call sites
        // - method bodies based on return statements
        // - polymorphic methods based on return types of all implementors
        // - optimize casts and instance of
        didChange = TypeTightener.exec(jprogram) || didChange;

        // tighten method call bindings
        didChange = MethodCallTightener.exec(jprogram) || didChange;

        // dead code removal??
        didChange = DeadCodeElimination.exec(jprogram) || didChange;

        if (options.isAggressivelyOptimize()) {
          // inlining
          didChange = MethodInliner.exec(jprogram) || didChange;
        }
        // prove that any types that have been culled from the main tree are
        // unreferenced due to type tightening?
      } while (didChange);

      // (5) "Normalize" the high-level Java tree into a lower-level tree more
      // suited for JavaScript code generation. Don't go reordering these
      // willy-nilly because there are some subtle interdependencies.
      LongCastNormalizer.exec(jprogram);
      JsoDevirtualizer.exec(jprogram);
      CatchBlockNormalizer.exec(jprogram);
      PostOptimizationCompoundAssignmentNormalizer.exec(jprogram);
      LongEmulationNormalizer.exec(jprogram);
      CastNormalizer.exec(jprogram);
      ArrayNormalizer.exec(jprogram);
      EqualityNormalizer.exec(jprogram);

      // (6) Perform further post-normalization optimizations
      // Prune everything
      Pruner.exec(jprogram, false);

      // (7) Generate a JavaScript code DOM from the Java type declarations
      jprogram.typeOracle.recomputeClinits();
      GenerateJavaScriptAST.exec(jprogram, jsProgram, options.getOutput());

      // (8) Normalize the JS AST.
      // Fix invalid constructs created during JS AST gen.
      JsNormalizer.exec(jsProgram);
      // Resolve all unresolved JsNameRefs.
      JsSymbolResolver.exec(jsProgram);

      // (9) Optimize the JS AST.
      if (options.isAggressivelyOptimize()) {
        do {
          didChange = false;
          // Remove unused functions, possible
          didChange = JsStaticEval.exec(jsProgram) || didChange;
          // Inline JavaScript function invocations
          didChange = options.isAggressivelyOptimize()
              && JsInliner.exec(jsProgram) || didChange;
          // Remove unused functions, possible
          didChange = JsUnusedFunctionRemover.exec(jsProgram) || didChange;
        } while (didChange);
      }

      // (10) Obfuscate
      switch (options.getOutput()) {
        case OBFUSCATED:
          JsStringInterner.exec(jsProgram);
          JsObfuscateNamer.exec(jsProgram);
          break;
        case PRETTY:
          // We don't intern strings in pretty mode to improve readability
          JsPrettyNamer.exec(jsProgram);
          break;
        case DETAILED:
          JsStringInterner.exec(jsProgram);
          JsVerboseNamer.exec(jsProgram);
          break;
        default:
          throw new InternalCompilerException("Unknown output mode");
      }

      // (11) Perform any post-obfuscation normalizations.

      // Work around an IE7 bug,
      // http://code.google.com/p/google-web-toolkit/issues/detail?id=1440
      JsIEBlockSizeVisitor.exec(jsProgram);

      // (12) Generate the final output text.
      DefaultTextOutput out = new DefaultTextOutput(
          options.getOutput().shouldMinimize());
      JsSourceGenerationVisitor v = new JsSourceGenerationVisitor(out);
      v.accept(jsProgram);
      return out.toString();
    } catch (UnableToCompleteException e) {
      // just rethrow
      throw e;
    } catch (InternalCompilerException e) {
      TreeLogger topBranch = logger.branch(TreeLogger.ERROR,
          "An internal compiler exception occurred", e);
      List<NodeInfo> nodeTrace = e.getNodeTrace();
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
      throw new UnableToCompleteException();
    } catch (Throwable e) {
      logger.log(TreeLogger.ERROR, "Unexpected internal compiler error", e);
      throw new UnableToCompleteException();
    }
  }

  private void checkForErrors(final TreeLogger logger, boolean itemizeErrors)
      throws UnableToCompleteException {
    boolean compilationFailed = false;
    if (goldenCuds.length == 0) {
      compilationFailed = true;
    }
    for (CompilationUnitDeclaration cud : goldenCuds) {
      CompilationResult result = cud.compilationResult();
      if (result.hasErrors()) {
        compilationFailed = true;
        // Early out if we don't need to itemize.
        if (!itemizeErrors) {
          break;
        }
        TreeLogger branch = logger.branch(TreeLogger.ERROR, "Errors in "
            + String.valueOf(result.getFileName()), null);
        IProblem[] errors = result.getErrors();
        for (IProblem problem : errors) {
          if (problemSet.contains(problem)) {
            continue;
          }

          problemSet.add(problem);

          // Strip the initial code from each error.
          //
          String msg = problem.toString();
          msg = msg.substring(msg.indexOf(' '));

          // Append 'file (line): msg' to the error message.
          //
          int line = problem.getSourceLineNumber();
          StringBuffer msgBuf = new StringBuffer();
          msgBuf.append("Line ");
          msgBuf.append(line);
          msgBuf.append(": ");
          msgBuf.append(msg);
          branch.log(TreeLogger.ERROR, msgBuf.toString(), null);
        }
      }
    }
    if (compilationFailed) {
      logger.log(TreeLogger.ERROR, "Cannot proceed due to previous errors",
          null);
      throw new UnableToCompleteException();
    }
  }
}
