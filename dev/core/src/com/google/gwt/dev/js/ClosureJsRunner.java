/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.codesplitter.FragmentPartitioningResult;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsProgramFragment;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.base.Throwables;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.io.ByteStreams;
import com.google.gwt.thirdparty.javascript.jscomp.CheckLevel;
import com.google.gwt.thirdparty.javascript.jscomp.Compiler;
import com.google.gwt.thirdparty.javascript.jscomp.CompilerInput;
import com.google.gwt.thirdparty.javascript.jscomp.CompilerOptions;
import com.google.gwt.thirdparty.javascript.jscomp.CompilerOptions.Reach;
import com.google.gwt.thirdparty.javascript.jscomp.DiagnosticGroups;
import com.google.gwt.thirdparty.javascript.jscomp.JSError;
import com.google.gwt.thirdparty.javascript.jscomp.JSModule;
import com.google.gwt.thirdparty.javascript.jscomp.JSSourceFile;
import com.google.gwt.thirdparty.javascript.jscomp.PropertyRenamingPolicy;
import com.google.gwt.thirdparty.javascript.jscomp.Result;
import com.google.gwt.thirdparty.javascript.jscomp.SourceAst;
import com.google.gwt.thirdparty.javascript.jscomp.VariableMap;
import com.google.gwt.thirdparty.javascript.jscomp.VariableRenamingPolicy;
import com.google.gwt.thirdparty.javascript.jscomp.WarningLevel;
import com.google.gwt.thirdparty.javascript.rhino.InputId;
import com.google.gwt.thirdparty.javascript.rhino.Node;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A class that represents an single invocation of the Closure Compiler.
 */
public class ClosureJsRunner {
  // The externs expected in externs.zip, in sorted order.
  private static final List<String> DEFAULT_EXTERNS_NAMES = ImmutableList.of(
      // JS externs
      "es3.js",
      "es5.js",

      // Event APIs
      "w3c_event.js", "w3c_event3.js", "w3c_device_sensor_event.js",
      "gecko_event.js",
      "ie_event.js",
      "webkit_event.js",

      // DOM apis
      "w3c_dom1.js", "w3c_dom2.js", "w3c_dom3.js",
      "gecko_dom.js",
      "ie_dom.js",
      "webkit_dom.js",

      // CSS apis
      "w3c_css.js",
      "gecko_css.js",
      "ie_css.js",
      "webkit_css.js",

      // Top-level namespaces
      "chrome.js", "google.js",

      "deprecated.js", "fileapi.js", "flash.js", "gears_symbols.js", "gears_types.js",
      "gecko_xml.js", "html5.js", "ie_vml.js", "iphone.js", "webstorage.js", "w3c_anim_timing.js",
      "w3c_css3d.js", "w3c_elementtraversal.js", "w3c_geolocation.js", "w3c_indexeddb.js",
      "w3c_navigation_timing.js", "w3c_range.js", "w3c_selectors.js", "w3c_xml.js", "window.js",
      "webkit_notifications.js", "webgl.js");

  /**
   * @return a mutable list
   * @throws IOException
   */
  public static List<JSSourceFile> getDefaultExterns() throws IOException {
    Class<ClosureJsRunner> clazz = ClosureJsRunner.class;
    InputStream input = clazz.getResourceAsStream("/com/google/javascript/jscomp/externs.zip");
    if (input == null) {
      /*
       * HACK - the open source version of the closure compiler maps the resource into a different
       * location.
       */
      input = clazz.getResourceAsStream("/externs.zip");
    }
    ZipInputStream zip = new ZipInputStream(input);
    Map<String, JSSourceFile> externsMap = Maps.newHashMap();
    for (ZipEntry entry = null; (entry = zip.getNextEntry()) != null;) {
      BufferedInputStream entryStream =
          new BufferedInputStream(ByteStreams.limit(zip, entry.getSize()));
      externsMap.put(entry.getName(), JSSourceFile.fromInputStream(
      // Give the files an odd prefix, so that they do not conflict
      // with the user's files.
          "externs.zip//" + entry.getName(), entryStream));
    }

    Preconditions.checkState(externsMap.keySet().equals(Sets.newHashSet(DEFAULT_EXTERNS_NAMES)),
        "Externs zip must match our hard-coded list of externs.");

    // Order matters, so the resources must be added to the result list
    // in the expected order.
    List<JSSourceFile> externs = Lists.newArrayList();
    for (String key : DEFAULT_EXTERNS_NAMES) {
      externs.add(externsMap.get(key));
    }

    return externs;
  }

  /**
   * The instance of the Closure Compiler used for the compile.
   */
  private Compiler compiler = null;

  /**
   * The set of external properties discovered in the provided AST.
   */
  private Set<String> externalProps = Sets.newHashSet();

  /**
   * The set of external global variables discovered in the provided AST.
   */
  private Set<String> externalVars = Sets.newHashSet();

  /**
   * The set of internal global variables discovered in the provided AST.
   */
  private Set<String> globalVars = Sets.newHashSet();

  /**
   * Whether AST validation should be performed on the generated
   * Closure Compiler AST.
   */
  private final boolean validate = true;

  /**
   * A map of GWT fragment numbers to Closure module indexes.
   */
  private int[] closureModuleSequenceMap;

  /**
   * The number of non-exclusive fragments that are part of the load sequence
   * (including the main and leftovers).
   */
  private int loadModulesCount;

  public ClosureJsRunner() {
  }

  public void compile(JProgram jprogram, JsProgram program, String[] js,
      JsOutputOption jsOutputOption) {
    CompilerOptions options;
    try {
      options = getClosureCompilerOptions(jsOutputOption);
    } catch (ParseException e) {
      throw new RuntimeException("Error setting closure compiler options", e);
    }
    // Turn off Closure Compiler logging
    Logger.getLogger("com.google.gwt.thirdparty.javascript.jscomp").setLevel(Level.OFF);

    // Create a fresh compiler instance.
    compiler = new Compiler();

    // Translate the ASTs and build the modules
    computeFragmentMap(jprogram, program);
    List<JSModule> modules = createClosureModules(program);

    // Build the externs based on what we discovered building the modules.
    List<JSSourceFile> externs = getClosureCompilerExterns();

    Result result = compiler.compileModules(externs, modules, options);
    if (result.success) {
      int fragments = program.getFragmentCount();
      for (int i = 0; i < fragments; i++) {
        int module = mapFragmentIndexToModuleIndex(i);
        js[i] = compiler.toSource(modules.get(module));
      }
    } else {
      for (JSError error : result.errors) {
        System.err.println("error optimizing:" + error.toString());
        throw new RuntimeException(error.description);
      }
    }
  }

  protected List<JSSourceFile> getDefaultExternsList() {
    List<JSSourceFile> defaultExterns;
    try {
      defaultExterns = getDefaultExterns();
      return defaultExterns;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private void computeFragmentMap(JProgram jprogram, JsProgram jsProgram) {
    int fragments = jsProgram.getFragmentCount();
    List<Integer> initSeq = jprogram.getInitialFragmentIdSequence();
    FragmentPartitioningResult partitionResult = jprogram.getFragmentPartitioningResult();

    //
    // The fragments are expected in a specific order:
    // init, split-1, split-2, ...,
    // where the leftovers are dependent on the init module
    // and the split modules are dependent on the leftovers
    //
    // However, Closure Compiler modules must be in dependency order
    //

    assert closureModuleSequenceMap == null;
    closureModuleSequenceMap = new int[fragments];
    for (int i = 0; i < fragments; i++) {
      closureModuleSequenceMap[i] = -1;
    }

    int module = 0;
    // The initial fragments is always first.
    closureModuleSequenceMap[0] = module++;

    // Then come the specified load order sequence
    for (int i = 0; i < initSeq.size(); i++) {
      int initSeqNum = initSeq.get(i);
      if (partitionResult != null) {
        initSeqNum = partitionResult.getFragmentForRunAsync(initSeqNum);
      }
      closureModuleSequenceMap[initSeqNum] = module++;
    }

    // Then the leftovers fragments:
    if (fragments > 1) {
      int leftoverIndex = fragments - 1;
      if (partitionResult != null) {
        leftoverIndex = partitionResult.getLeftoverFragmentId();
      }
      closureModuleSequenceMap[leftoverIndex] = module++;
    }

    // Finally, the exclusive fragments.
    // The order of the remaining fragments doesn't matter.
    for (int i = 0; i < fragments; i++) {
      if (closureModuleSequenceMap[i] == -1) {
        closureModuleSequenceMap[i] = module++;
      }
    }
    loadModulesCount = 1 + initSeq.size() + 1; // main + init sequence + leftovers
  }

  private CompilerInput createClosureJsAst(JsProgram program, JsProgramFragment fragment,
      String source) {
    String inputName = source;
    InputId inputId = new InputId(inputName);
    ClosureJsAstTranslator translator = new ClosureJsAstTranslator(validate, program);
    Node root = translator.translate(fragment, inputId, source);
    globalVars.addAll(translator.getGlobalVariableNames());
    externalProps.addAll(translator.getExternalPropertyReferences());
    externalVars.addAll(translator.getExternalVariableReferences());
    SourceAst sourceAst = new ClosureJsAst(inputId, root);
    CompilerInput input = new CompilerInput(sourceAst, false);
    return input;
  }

  private JSModule createClosureModule(JsProgram program, JsProgramFragment fragment, String source) {
    JSModule module = new JSModule(source);
    module.add(createClosureJsAst(program, fragment, source));
    return module;
  }

  private List<JSModule> createClosureModules(JsProgram program) {
    int fragments = program.getFragmentCount();
    JSModule[] modules = new JSModule[fragments];

    for (int i = 0; i < fragments; i++) {
      modules[mapFragmentIndexToModuleIndex(i)] =
          createClosureModule(program, program.getFragment(i), "module" + i);
    }
    if (fragments > 1) {
      //
      // The fragments are expected in a specific order:
      // init, split-1, split-2, ...,
      // where the leftovers are dependent on the init module
      // and the split modules are dependent on the leftovers
      for (int i = 1; i < loadModulesCount; i++) {
        modules[i].addDependency(modules[i - 1]);
      }

      JSModule leftovers = modules[loadModulesCount - 1];
      for (int i = loadModulesCount; i < modules.length; i++) {
        Preconditions.checkNotNull(modules[i], "Module: ", i);
        modules[i].addDependency(leftovers);
      }
    }
    modules[0].add(JSSourceFile.fromCode("hack", "window['gwtOnLoad'] = gwtOnLoad;\n"));

    return Arrays.asList(modules);
  }

  private List<JSSourceFile> getClosureCompilerExterns() {
    List<JSSourceFile> externs = getDefaultExternsList();
    externs.add(JSSourceFile.fromCode("gwt_externs",

    "var gwtOnLoad;\n"
        + "var $entry;\n"
        + "    var $gwt_version;\n"
        + "    var $wnd;\n"
        + "    var $doc;\n"
        + "    var $moduleName\n"
        + "    var $moduleBase;\n"
        + "    var $permProps\n"
        + "    var $strongName;\n"
        + "    var $stats;\n"
        + "    var $sessionId;\n"
        + "    window.prototype.__gwtStatsEvent;\n"
        + "    window.prototype.__gwtStatsSessionId;\n"
        + "    window.prototype.moduleName;\n"
        + "    window.prototype.sessionId;\n"
        + "    window.prototype.subSystem;\n"
        + "    window.prototype.evtGroup;\n"
        + "    window.prototype.millis;\n"
        + "    window.prototype.type;\n"
        + "    window.prototype.$h;\n"
        + "\n"));

    // Generate externs
    String generatedExterns = "var gwt_externs;\n";
    for (String prop : this.externalProps) {
      generatedExterns += "gwt_externs." + prop + ";\n";
    }

    for (String var : this.externalVars) {
      generatedExterns += "var " + var + ";\n";
    }

    externs.add(JSSourceFile.fromCode("gwt_generated_externs", generatedExterns));

    return externs;
  }

  private CompilerOptions getClosureCompilerOptions(JsOutputOption jsOutputOption)
      throws ParseException {
    CompilerOptions options = new CompilerOptions();
    WarningLevel.QUIET.setOptionsForWarningLevel(options);

    // Basically, use CompilationLevel.ADVANCED_OPTIMIZATIONS:

    // Build an identity map of variable names to prevent GWT names from
    // being renamed while allowing new global variables to be renamed.
    HashMap<String, String> varNames = new HashMap<String, String>();
    for (String var : globalVars) {
      varNames.put(var, var);
    }
    options.setInputVariableMapSerialized(VariableMap.fromMap(varNames).toBytes());
    if (jsOutputOption == JsOutputOption.OBFUSCATED) {
      options.setRenamingPolicy(VariableRenamingPolicy.ALL, PropertyRenamingPolicy.OFF);
      options.prettyPrint = false;
      // This can help debug renaming policy changes.
      // options.generatePseudoNames = true;
    } else {
      options.setRenamingPolicy(VariableRenamingPolicy.OFF, PropertyRenamingPolicy.OFF);
      options.prettyPrint = true;
    }

    // All the safe optimizations.
    options.closurePass = true;
    options.foldConstants = true;
    options.coalesceVariableNames = true;
    options.deadAssignmentElimination = true;
    options.extractPrototypeMemberDeclarations = true;
    options.collapseVariableDeclarations = true;
    options.convertToDottedProperties = true;
    options.rewriteFunctionExpressions = true;
    options.labelRenaming = true;
    options.removeDeadCode = true;
    options.optimizeArgumentsArray = true;
    options.setCollapseObjectLiterals(true);
    options.setShadowVariables(true);

    // All the advance optimizations.
    options.reserveRawExports = true;
    options.removeUnusedPrototypeProperties = true;
    options.collapseAnonymousFunctions = true;
    options.smartNameRemoval = true; // ?
    options.inlineConstantVars = true;
    options.setInlineFunctions(Reach.ALL);
    options.inlineGetters = true;
    options.setInlineVariables(Reach.ALL);
    options.flowSensitiveInlineVariables = true;
    options.computeFunctionSideEffects = true;
    // Remove unused vars also removes unused functions.
    options.setRemoveUnusedVariable(Reach.ALL);
    options.optimizeParameters = true;
    options.optimizeReturns = true;
    options.optimizeCalls = true;

    // Maybe turn these off as well
    options.collapseProperties = true; // ?
    options.crossModuleCodeMotion = true; // ?
    options.crossModuleMethodMotion = true; // ?
    options.devirtualizePrototypeMethods = true; // ?

    // Advanced optimization, disabled
    options.setRemoveClosureAsserts(false);
    options.aliasKeywords = false;
    options.removeUnusedPrototypePropertiesInExterns = false;
    options.checkGlobalThisLevel = CheckLevel.OFF;
    options.rewriteFunctionExpressions = false; // Performance hit

    // Kindly tell the user that they have JsDocs that we don't understand.
    options.setWarningLevel(DiagnosticGroups.NON_STANDARD_JSDOC, CheckLevel.OFF);

    return options;
  }

  private int mapFragmentIndexToModuleIndex(int index) {
    assert closureModuleSequenceMap.length > index;
    return closureModuleSequenceMap[index];
  }
}
