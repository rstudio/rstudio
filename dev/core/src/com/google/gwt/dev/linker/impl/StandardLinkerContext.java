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
package com.google.gwt.dev.linker.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.cfg.Script;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.js.JsObfuscateNamer;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.JsPrettyNamer;
import com.google.gwt.dev.js.JsSourceGenerationVisitor;
import com.google.gwt.dev.js.JsStringInterner;
import com.google.gwt.dev.js.JsSymbolResolver;
import com.google.gwt.dev.js.JsUnusedFunctionRemover;
import com.google.gwt.dev.js.JsVerboseNamer;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.linker.CompilationResult;
import com.google.gwt.dev.linker.GeneratedResource;
import com.google.gwt.dev.linker.Linker;
import com.google.gwt.dev.linker.LinkerContext;
import com.google.gwt.dev.linker.ModuleScriptResource;
import com.google.gwt.dev.linker.ModuleStylesheetResource;
import com.google.gwt.dev.linker.PublicResource;
import com.google.gwt.dev.linker.SelectionProperty;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An implementation of {@link LinkerContext} that is initialized from a
 * {@link ModuleDef}.
 */
public class StandardLinkerContext implements LinkerContext {

  /**
   * Applies the {@link JsStringInterner} optimization to each top-level
   * function defined within a JsProgram.
   */
  private static class TopFunctionStringInterner extends JsModVisitor {

    public static boolean exec(JsProgram program) {
      TopFunctionStringInterner v = new TopFunctionStringInterner();
      v.accept(program);
      return v.didChange();
    }

    @Override
    public boolean visit(JsFunction x, JsContext<JsExpression> ctx) {
      didChange |= JsStringInterner.exec(x.getBody(), x.getScope());
      return false;
    }
  }

  /**
   * Orders CompilationResults by string comparison of their JavaScript.
   */
  public static final Comparator<CompilationResult> COMPILATION_RESULT_COMPARATOR = new Comparator<CompilationResult>() {
    public int compare(CompilationResult o1, CompilationResult o2) {
      return o1.getJavaScript().compareTo(o2.getJavaScript());
    }
  };

  /**
   * Orders GeneratedResources by string comparison of their partial paths.
   */
  public static final Comparator<GeneratedResource> GENERATED_RESOURCE_COMPARATOR = new Comparator<GeneratedResource>() {
    public int compare(GeneratedResource o1, GeneratedResource o2) {
      return o1.getPartialPath().compareTo(o2.getPartialPath());
    }
  };

  /**
   * Orders PublicResources by string comparison of their partial paths.
   */
  public static final Comparator<PublicResource> PUBLIC_RESOURCE_COMPARATOR = new Comparator<PublicResource>() {
    public int compare(PublicResource o1, PublicResource o2) {
      return o1.getPartialPath().compareTo(o2.getPartialPath());
    }
  };

  /**
   * Orders ModuleScriptResources by string comparison of their src attributes.
   */
  public static final Comparator<ModuleScriptResource> SCRIPT_RESOURCE_COMPARATOR = new Comparator<ModuleScriptResource>() {
    public int compare(ModuleScriptResource o1, ModuleScriptResource o2) {
      return o1.getSrc().compareTo(o2.getSrc());
    }
  };

  /**
   * Orders SelectionProperties by string comparison of their names.
   */
  public static final Comparator<SelectionProperty> SELECTION_PROPERTY_COMPARATOR = new Comparator<SelectionProperty>() {
    public int compare(SelectionProperty o1, SelectionProperty o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  /**
   * Orders ModuleStyleResources by string comparison of their src attributes.
   */
  public static final Comparator<ModuleStylesheetResource> STYLE_RESOURCE_COMPARATOR = new Comparator<ModuleStylesheetResource>() {
    public int compare(ModuleStylesheetResource o1, ModuleStylesheetResource o2) {
      return o1.getSrc().compareTo(o2.getSrc());
    }
  };

  private final File compilationsDir;
  private final SortedSet<GeneratedResource> generatedResources;
  private final Map<String, GeneratedResource> generatedResourcesByName = new HashMap<String, GeneratedResource>();
  private final JJSOptions jjsOptions;

  /**
   * This determines where a call to {@link #commit(TreeLogger, OutputStream)}
   * will write to. It's intended to be updated by
   * {@link #invokeLinker(TreeLogger, String, Linker)} so that each Linker will
   * write into a different output directory.
   */
  private File linkerOutDir;
  private final String moduleFunctionName;
  private final String moduleName;

  /**
   * This is the root directory for the output of a particular module's
   * compilation.
   */
  private final File moduleOutDir;
  private final Set<String> openPaths = new HashSet<String>();
  private final Map<ByteArrayOutputStream, File> outs = new IdentityHashMap<ByteArrayOutputStream, File>();
  private final SortedSet<SelectionProperty> properties;
  private final Map<String, StandardSelectionProperty> propertiesByName = new HashMap<String, StandardSelectionProperty>();
  private final SortedSet<PublicResource> publicResources;
  private final Map<String, PublicResource> publicResourcesByName = new HashMap<String, PublicResource>();
  private final Map<String, StandardCompilationResult> resultsByStrongName = new HashMap<String, StandardCompilationResult>();
  private final SortedSet<ModuleScriptResource> scriptResources;
  private final SortedSet<ModuleStylesheetResource> stylesheetResources;

  public StandardLinkerContext(TreeLogger logger, ModuleDef module,
      File outDir, File generatorDir, JJSOptions jjsOptions) {
    logger = logger.branch(TreeLogger.DEBUG,
        "Constructing StandardLinkerContext", null);

    this.jjsOptions = jjsOptions;
    this.moduleFunctionName = module.getFunctionName();
    this.moduleName = module.getName();
    this.moduleOutDir = outDir;

    if (moduleOutDir != null) {
      compilationsDir = new File(moduleOutDir, ".gwt-compiler/compilations");
      Util.recursiveDelete(compilationsDir, true);
      compilationsDir.mkdirs();
      logger.log(TreeLogger.SPAM, "compilationsDir: "
          + compilationsDir.getPath(), null);
    } else {
      compilationsDir = null;
    }

    // Always return the properties in the same order as a convenience
    SortedSet<SelectionProperty> mutableProperties = new TreeSet<SelectionProperty>(
        SELECTION_PROPERTY_COMPARATOR);
    for (Property p : module.getProperties()) {
      // Create a new view
      StandardSelectionProperty newProp = new StandardSelectionProperty(p);
      mutableProperties.add(newProp);
      propertiesByName.put(newProp.getName(), newProp);
      logger.log(TreeLogger.SPAM, "Added property " + newProp, null);
    }
    properties = Collections.unmodifiableSortedSet(mutableProperties);

    SortedSet<ModuleScriptResource> scripts = new TreeSet<ModuleScriptResource>(
        SCRIPT_RESOURCE_COMPARATOR);
    for (Script script : module.getScripts()) {
      scripts.add(new StandardScriptResource(script.getSrc(),
          module.findPublicFile(script.getSrc())));
      logger.log(TreeLogger.SPAM, "Added script " + script.getSrc(), null);
    }
    scriptResources = Collections.unmodifiableSortedSet(scripts);

    SortedSet<ModuleStylesheetResource> styles = new TreeSet<ModuleStylesheetResource>(
        STYLE_RESOURCE_COMPARATOR);
    for (String style : module.getStyles()) {
      styles.add(new StandardStylesheetResource(style,
          module.findPublicFile(style)));
      logger.log(TreeLogger.SPAM, "Added style " + style, null);
    }
    stylesheetResources = Collections.unmodifiableSortedSet(styles);

    SortedSet<GeneratedResource> genResources = new TreeSet<GeneratedResource>(
        GENERATED_RESOURCE_COMPARATOR);
    if (generatorDir != null) {
      for (String path : Util.recursiveListPartialPaths(generatorDir, false)) {
        try {
          GeneratedResource resource = new StandardGeneratedResource(path,
              (new File(generatorDir, path)).toURL());
          generatedResourcesByName.put(path, resource);
          genResources.add(resource);
          logger.log(TreeLogger.SPAM, "Added generated resource " + resource,
              null);
        } catch (MalformedURLException e) {
          // This won't happen unless there's a bad partial path from Util
          logger.log(TreeLogger.ERROR,
              "Unable to convert generated resource to URL", e);
        }
      }
    }
    generatedResources = Collections.unmodifiableSortedSet(genResources);

    SortedSet<PublicResource> pubResources = new TreeSet<PublicResource>(
        PUBLIC_RESOURCE_COMPARATOR);
    for (String path : module.getAllPublicFiles()) {
      PublicResource resource = new StandardPublicResource(path,
          module.findPublicFile(path));
      publicResourcesByName.put(path, resource);
      pubResources.add(resource);
      logger.log(TreeLogger.SPAM, "Added public resource " + resource, null);
    }
    publicResources = Collections.unmodifiableSortedSet(pubResources);
  }

  public void commit(TreeLogger logger, OutputStream toCommit)
      throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.DEBUG,
        "Attempting to commit OutputStream", null);

    if (!outs.containsKey(toCommit)) {
      logger.log(TreeLogger.ERROR,
          "OutputStream was foreign to this LinkerContext", null);
      throw new UnableToCompleteException();
    }

    File f = outs.get(toCommit);
    if (f == null) {
      logger.log(TreeLogger.ERROR,
          "The OutputStream has already been committed", null);
      throw new UnableToCompleteException();
    }

    /*
     * Record that we will no longer accept this OutputStream as opposed to
     * removing it, which would erroneously indicate that it was a foreign
     * OutputStream.
     */
    ByteArrayOutputStream original = (ByteArrayOutputStream) toCommit;
    outs.put(original, null);

    try {
      Util.writeBytesToFile(logger, f, original.toByteArray());
    } finally {
      // Dump the byte buffer;
      original.reset();
    }
    logger.log(TreeLogger.DEBUG, "Successfully committed " + f.getPath(), null);
  }

  public StandardCompilationResult getCompilation(TreeLogger logger, String js)
      throws UnableToCompleteException {

    byte[] bytes = Util.getBytes(js);
    StandardCompilationResult result = resultsByStrongName.get(Util.computeStrongName(bytes));
    if (result == null) {
      result = new StandardCompilationResult(logger, js, compilationsDir);
      resultsByStrongName.put(result.getStrongName(), result);
    }
    return result;
  }

  public SortedSet<CompilationResult> getCompilations() {
    SortedSet<CompilationResult> toReturn = new TreeSet<CompilationResult>(
        COMPILATION_RESULT_COMPARATOR);
    toReturn.addAll(resultsByStrongName.values());
    return Collections.unmodifiableSortedSet(toReturn);
  }

  public SortedSet<GeneratedResource> getGeneratedResources() {
    return generatedResources;
  }

  public String getModuleFunctionName() {
    return moduleFunctionName;
  }

  public String getModuleName() {
    return moduleName;
  }

  public SortedSet<ModuleScriptResource> getModuleScripts() {
    return scriptResources;
  }

  public SortedSet<ModuleStylesheetResource> getModuleStylesheets() {
    return stylesheetResources;
  }

  public SortedSet<SelectionProperty> getProperties() {
    return properties;
  }

  public StandardSelectionProperty getProperty(String name) {
    return propertiesByName.get(name);
  }

  public SortedSet<PublicResource> getPublicResources() {
    return publicResources;
  }

  /**
   * Run a linker in an isolated out directory.
   */
  public void invokeLinker(TreeLogger logger, String target, Linker linker)
      throws UnableToCompleteException {
    try {
      // Assign the directory the Linker will work in.
      linkerOutDir = new File(moduleOutDir, target);
      if (!moduleOutDir.equals(linkerOutDir.getParentFile())) {
        // This should never actually happen, since the target must be
        // a valid Java identifier
        logger.log(TreeLogger.ERROR,
            "Trying to create linker dir in wrong place", null);
        throw new UnableToCompleteException();
      }

      // We nuke the contents of the directory
      Util.recursiveDelete(linkerOutDir, true);
      linkerOutDir.mkdirs();

      if (!linkerOutDir.canWrite()) {
        logger.log(TreeLogger.ERROR, "Unable create linker dir"
            + linkerOutDir.getPath(), null);
        throw new UnableToCompleteException();
      }

      logger = logger.branch(TreeLogger.INFO, "Linking compilation with "
          + linker.getDescription() + " Linker into " + linkerOutDir.getPath(),
          null);
      linker.link(logger, this);
    } finally {
      reset();
    }
  }

  public String optimizeJavaScript(TreeLogger logger, String program)
      throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.DEBUG, "Attempting to optimize JS", null);
    JsParser parser = new JsParser();
    Reader r = new StringReader(program);
    JsProgram jsProgram = new JsProgram();
    JsScope topScope = jsProgram.getScope();
    JsName funcName = topScope.declareName(getModuleFunctionName());
    funcName.setObfuscatable(false);

    try {
      parser.parseInto(topScope, jsProgram.getGlobalBlock(), r, 1);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to parse JavaScript", e);
      throw new UnableToCompleteException();
    } catch (JsParserException e) {
      logger.log(TreeLogger.ERROR, "Unable to parse JavaScript", e);
      throw new UnableToCompleteException();
    }

    JsSymbolResolver.exec(jsProgram);
    JsUnusedFunctionRemover.exec(jsProgram);

    switch (jjsOptions.getOutput()) {
      case OBFUSCATED:
        /*
         * We can't apply the regular JsStringInterner to the JsProgram that
         * we've just created. In the normal case, the JsStringInterner adds an
         * additional statement to the program's global JsBlock, however we
         * don't know exactly what the form and structure of our JsProgram are,
         * so we'll limit the scope of the modifications to each top-level
         * function within the program.
         */
        TopFunctionStringInterner.exec(jsProgram);
        JsObfuscateNamer.exec(jsProgram);
        break;
      case PRETTY:
        // We don't intern strings in pretty mode to improve readability
        JsPrettyNamer.exec(jsProgram);
        break;
      case DETAILED:
        // As above with OBFUSCATED
        TopFunctionStringInterner.exec(jsProgram);
        JsVerboseNamer.exec(jsProgram);
        break;
      default:
        throw new InternalCompilerException("Unknown output mode");
    }

    DefaultTextOutput out = new DefaultTextOutput(
        jjsOptions.getOutput().shouldMinimize());
    JsSourceGenerationVisitor v = new JsSourceGenerationVisitor(out);
    v.accept(jsProgram);
    return out.toString();
  }

  public OutputStream tryCreateArtifact(TreeLogger logger, String partialPath) {
    File f = new File(linkerOutDir, partialPath);
    if (f.exists() || openPaths.contains(partialPath)) {
      logger.branch(TreeLogger.DEBUG, "Refusing to create artifact "
          + partialPath + " because it already exists or is already open.",
          null);
      return null;
    }

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    outs.put(out, f);
    openPaths.add(partialPath);
    return out;
  }

  public GeneratedResource tryGetGeneratedResource(TreeLogger logger,
      String name) {
    return generatedResourcesByName.get(name);
  }

  public PublicResource tryGetPublicResource(TreeLogger logger, String name) {
    return publicResourcesByName.get(name);
  }

  /**
   * Reset the context.
   */
  private void reset() {
    linkerOutDir = null;
    openPaths.clear();
    outs.clear();
  }
}
