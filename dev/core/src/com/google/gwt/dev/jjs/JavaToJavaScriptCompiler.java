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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.dev.jdt.ICompilationUnitAdapter;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.jjs.InternalCompilerException.NodeInfo;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.impl.ArrayNormalizer;
import com.google.gwt.dev.jjs.impl.AssertionRemover;
import com.google.gwt.dev.jjs.impl.BuildTypeMap;
import com.google.gwt.dev.jjs.impl.CastNormalizer;
import com.google.gwt.dev.jjs.impl.CatchBlockNormalizer;
import com.google.gwt.dev.jjs.impl.CompoundAssignmentNormalizer;
import com.google.gwt.dev.jjs.impl.DeadCodeElimination;
import com.google.gwt.dev.jjs.impl.GenerateJavaAST;
import com.google.gwt.dev.jjs.impl.GenerateJavaScriptAST;
import com.google.gwt.dev.jjs.impl.JavaScriptObjectCaster;
import com.google.gwt.dev.jjs.impl.MakeCallsStatic;
import com.google.gwt.dev.jjs.impl.MethodAndClassFinalizer;
import com.google.gwt.dev.jjs.impl.MethodCallTightener;
import com.google.gwt.dev.jjs.impl.MethodInliner;
import com.google.gwt.dev.jjs.impl.Pruner;
import com.google.gwt.dev.jjs.impl.ReplaceRebinds;
import com.google.gwt.dev.jjs.impl.TypeMap;
import com.google.gwt.dev.jjs.impl.TypeTightener;
import com.google.gwt.dev.js.JsNormalizer;
import com.google.gwt.dev.js.JsObfuscateNamer;
import com.google.gwt.dev.js.JsPrettyNamer;
import com.google.gwt.dev.js.JsSourceGenerationVisitor;
import com.google.gwt.dev.js.JsSymbolResolver;
import com.google.gwt.dev.js.JsVerboseNamer;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
            JMethod ctor = (JMethod) mainClass.methods.get(j);
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
      JMethodBody body = (JMethodBody) bootStrapMethod.getBody();
      body.getStatements().add(onModuleLoadCall.makeStatement());
    }
    program.addEntryMethod(bootStrapMethod);
  }

  private static JMethod findMainMethod(JReferenceType referenceType) {
    for (int j = 0; j < referenceType.methods.size(); ++j) {
      JMethod method = (JMethod) referenceType.methods.get(j);
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

  private final String[] declEntryPoints;
  private final CompilationUnitDeclaration[] goldenCuds;
  private long lastModified;
  private final boolean obfuscate;
  private final boolean prettyNames;
  private final Set/* <IProblem> */problemSet = new HashSet/* <IProblem> */();

  public JavaToJavaScriptCompiler(final TreeLogger logger,
      final WebModeCompilerFrontEnd compiler, final String[] declEntryPts)
      throws UnableToCompleteException {
    this(logger, compiler, declEntryPts, true, false);
  }

  public JavaToJavaScriptCompiler(final TreeLogger logger,
      final WebModeCompilerFrontEnd compiler, final String[] declEntryPts,
      boolean obfuscate, boolean prettyNames) throws UnableToCompleteException {

    if (declEntryPts.length == 0) {
      throw new IllegalArgumentException("entry point(s) required");
    }

    // Remember these for subsequent compiles.
    //
    this.declEntryPoints = declEntryPts;

    // Should we obfuscate or, if not, use pretty names?
    //
    this.obfuscate = obfuscate;
    this.prettyNames = prettyNames;

    // Find all the possible rebound entry points.
    //
    RebindPermutationOracle rpo = compiler.getRebindPermutationOracle();
    Set allEntryPoints = new HashSet();
    for (int i = 0; i < declEntryPts.length; i++) {
      String[] all = rpo.getAllPossibleRebindAnswers(logger, declEntryPts[i]);
      Util.addAll(allEntryPoints, all);
    }
    String[] entryPts = Util.toStringArray(allEntryPoints);

    // Add intrinsics needed for code generation.
    //
    int k = entryPts.length;
    String[] seedTypeNames = new String[k + 6];
    System.arraycopy(entryPts, 0, seedTypeNames, 0, k);
    seedTypeNames[k++] = "com.google.gwt.lang.Array";
    seedTypeNames[k++] = "com.google.gwt.lang.Cast";
    seedTypeNames[k++] = "com.google.gwt.lang.Exceptions";
    seedTypeNames[k++] = "java.lang.Object";
    seedTypeNames[k++] = "java.lang.String";
    seedTypeNames[k++] = "java.lang.Iterable";

    // Compile the source and get the compiler so we can get the parse tree
    //
    goldenCuds = compiler.getCompilationUnitDeclarations(logger, seedTypeNames);

    // Check for compilation problems. We don't log here because any problems
    // found here will have already been logged by AbstractCompiler.
    //
    checkForErrors(logger, false);

    // Find the newest of all these.
    //
    lastModified = 0;
    CompilationUnitProvider newestCup = null;
    for (int i = 0; i < goldenCuds.length; i++) {
      CompilationUnitDeclaration cud = goldenCuds[i];
      ICompilationUnitAdapter icua = (ICompilationUnitAdapter) cud.compilationResult.compilationUnit;
      CompilationUnitProvider cup = icua.getCompilationUnitProvider();
      long cupLastModified = cup.getLastModified();
      if (cupLastModified > lastModified) {
        newestCup = cup;
        lastModified = cupLastModified;
      }
    }
    if (newestCup != null) {
      String loc = newestCup.getLocation();
      String msg = "Newest compilation unit is '" + loc + "'";
      logger.log(TreeLogger.DEBUG, msg, null);
    }
  }

  /**
   * Creates finished JavaScript source code from the specified Java compilation
   * units.
   */
  public String compile(TreeLogger logger, RebindOracle rebindOracle)
      throws UnableToCompleteException {

    try {

      // (1) Build a flattened map of TypeDeclarations => JType.
      //

      // Note that all reference types (even nested and local ones) are in the
      // resulting type map. BuildTypeMap also parses all JSNI.
      //
      JProgram jprogram = new JProgram(logger, rebindOracle);
      TypeMap typeMap = new TypeMap(jprogram);
      JsProgram jsProgram = new JsProgram();
      TypeDeclaration[] allTypeDeclarations = BuildTypeMap.exec(typeMap,
          goldenCuds, jsProgram);

      // BuildTypeMap can uncover syntactic JSNI errors; report & abort
      // 
      checkForErrors(logger, true);

      // Compute all super type/sub type info
      jprogram.typeOracle.computeBeforeAST();

      // (3) Create a normalized Java AST using our own notation.
      //

      // Create the tree from JDT
      GenerateJavaAST.exec(allTypeDeclarations, typeMap, jprogram);

      // GenerateJavaAST can uncover semantic JSNI errors; report & abort
      // 
      checkForErrors(logger, true);

      // TODO: figure out how to have a debug mode.
      boolean isDebugEnabled = false;
      if (!isDebugEnabled) {
        // Remove all assert statements.
        AssertionRemover.exec(jprogram);
      }

      // Compute which classes have clinits
      jprogram.typeOracle.computeAfterAST();

      // Fix up GWT.create() into new operations
      ReplaceRebinds.exec(jprogram);

      // Rebind each entry point.
      //
      findEntryPoints(logger, rebindOracle, declEntryPoints, jprogram);

      // (4) Optimize the normalized Java AST
      boolean didChange;
      do {
        didChange = false;
        // Remove unreferenced types, fields, methods, [params, locals]
        didChange = Pruner.exec(jprogram, true) || didChange;
        // finalize locals, params, fields, methods, classes
        didChange = MethodAndClassFinalizer.exec(jprogram) || didChange;
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

        // inlining
        didChange = MethodInliner.exec(jprogram) || didChange;

        if (didChange) {
          // recompute clinits; some may now be empty
          jprogram.typeOracle.recomputeClinits();
        }

        // prove that any types that have been culled from the main tree are
        // unreferenced due to type tightening?
      } while (didChange);

      // (5) "Normalize" the high-level Java tree into a lower-level tree more
      // suited for JavaScript code generation. Don't go reordering these
      // willy-nilly because there are some subtle interdependencies.
      if (isDebugEnabled) {
        // AssertionNormalizer.exec(jprogram);
      }
      CatchBlockNormalizer.exec(jprogram);
      CompoundAssignmentNormalizer.exec(jprogram);
      JavaScriptObjectCaster.exec(jprogram);
      CastNormalizer.exec(jprogram);
      ArrayNormalizer.exec(jprogram);

      // (6) Perform further post-normalization optimizations
      // Prune everything
      Pruner.exec(jprogram, false);

      // (7) Generate a JavaScript code DOM from the Java type declarations
      GenerateJavaScriptAST.exec(jprogram, jsProgram, obfuscate, prettyNames);

      // (8) Fix invalid constructs created during JS AST gen
      JsNormalizer.exec(jsProgram);

      // (9) Resolve all unresolved JsNameRefs
      JsSymbolResolver.exec(jsProgram);

      // (10) Obfuscate
      if (obfuscate) {
        JsObfuscateNamer.exec(jsProgram);
      } else if (prettyNames) {
        JsPrettyNamer.exec(jsProgram);
      } else {
        JsVerboseNamer.exec(jsProgram);
      }

      DefaultTextOutput out = new DefaultTextOutput(obfuscate);
      JsSourceGenerationVisitor v = new JsSourceGenerationVisitor(out);
      v.accept(jsProgram);
      return out.toString();
    } catch (UnableToCompleteException e) {
      // just rethrow
      throw e;
    } catch (InternalCompilerException e) {
      TreeLogger topBranch = logger.branch(TreeLogger.ERROR,
          "An internal compiler exception occurred", e);
      List nodeTrace = e.getNodeTrace();
      for (Iterator it = nodeTrace.iterator(); it.hasNext();) {
        NodeInfo nodeInfo = (NodeInfo) it.next();
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

  public long getLastModifiedTimeOfNewestCompilationUnit() {
    return lastModified;
  }

  private void checkForErrors(final TreeLogger logger, boolean itemizeErrors)
      throws UnableToCompleteException {
    boolean compilationFailed = false;
    if (goldenCuds.length == 0) {
      compilationFailed = true;
    }
    for (int iCud = 0; iCud < goldenCuds.length; iCud++) {
      CompilationUnitDeclaration cud = goldenCuds[iCud];
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
        for (int i = 0; i < errors.length; i++) {
          IProblem problem = errors[i];
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
