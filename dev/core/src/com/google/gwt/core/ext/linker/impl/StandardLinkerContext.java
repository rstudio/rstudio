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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.PublicResource;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.cfg.Script;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.jjs.SourceInfo;
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
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.OutputFileSet;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An implementation of {@link LinkerContext} that is initialized from a
 * {@link ModuleDef}.
 */
public class StandardLinkerContext extends Linker implements LinkerContext {

  /**
   * Applies the {@link JsStringInterner} optimization to each top-level
   * function defined within a JsProgram.
   */
  private static class TopFunctionStringInterner extends JsModVisitor {

    public static boolean exec(JsProgram program) {
      TopFunctionStringInterner v = new TopFunctionStringInterner(program);
      v.accept(program);
      return v.didChange();
    }

    private final JsProgram program;

    public TopFunctionStringInterner(JsProgram program) {
      this.program = program;
    }

    @Override
    public boolean visit(JsFunction x, JsContext<JsExpression> ctx) {
      didChange |= JsStringInterner.exec(program, x.getBody(), x.getScope());
      return false;
    }
  }

  static final Comparator<ConfigurationProperty> CONFIGURATION_PROPERTY_COMPARATOR = new Comparator<ConfigurationProperty>() {
    public int compare(ConfigurationProperty o1, ConfigurationProperty o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  static final Comparator<SelectionProperty> SELECTION_PROPERTY_COMPARATOR = new Comparator<SelectionProperty>() {
    public int compare(SelectionProperty o1, SelectionProperty o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  private final ArtifactSet artifacts = new ArtifactSet();

  private final SortedSet<ConfigurationProperty> configurationProperties;
  private final JJSOptions jjsOptions;
  private final Map<Class<? extends Linker>, String> linkerShortNames = new HashMap<Class<? extends Linker>, String>();

  private final String moduleFunctionName;
  private final long moduleLastModified;
  private final String moduleName;

  private final Map<String, StandardSelectionProperty> propertiesByName = new HashMap<String, StandardSelectionProperty>();
  private final Map<String, StandardCompilationResult> resultsByStrongName = new HashMap<String, StandardCompilationResult>();
  private final SortedSet<SelectionProperty> selectionProperties;

  private final Linker[] linkers;

  public StandardLinkerContext(TreeLogger logger, ModuleDef module,
      JJSOptions jjsOptions) throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.DEBUG,
        "Constructing StandardLinkerContext", null);

    this.jjsOptions = jjsOptions;
    this.moduleFunctionName = module.getFunctionName();
    this.moduleName = module.getName();
    this.moduleLastModified = module.lastModified();

    // Sort the linkers into the order they should actually run.
    List<Class<? extends Linker>> sortedLinkers = new ArrayList<Class<? extends Linker>>();

    // Get all the pre-linkers first.
    for (Class<? extends Linker> linkerClass : module.getActiveLinkers()) {
      Order order = linkerClass.getAnnotation(LinkerOrder.class).value();
      assert (order != null);
      if (order == Order.PRE) {
        sortedLinkers.add(linkerClass);
      }
    }

    // Get the primary linker.
    Class<? extends Linker> primary = module.getActivePrimaryLinker();
    if (primary == null) {
      logger.log(logger.ERROR, "Primary linker is null.  Does your module " +
          "inherit from com.google.gwt.core.Core or com.google.gwt.user.User?");
    } else {
      sortedLinkers.add(module.getActivePrimaryLinker());
    }

    // Get all the post-linkers IN REVERSE ORDER.
    {
      List<Class<? extends Linker>> postLinkerClasses = new ArrayList<Class<? extends Linker>>();
      for (Class<? extends Linker> linkerClass : module.getActiveLinkers()) {
        Order order = linkerClass.getAnnotation(LinkerOrder.class).value();
        assert (order != null);
        if (order == Order.POST) {
          postLinkerClasses.add(linkerClass);
        }
      }
      Collections.reverse(postLinkerClasses);
      sortedLinkers.addAll(postLinkerClasses);
    }

    linkers = new Linker[sortedLinkers.size()];
    int i = 0;
    for (Class<? extends Linker> linkerClass : sortedLinkers) {
      try {
        linkers[i++] = linkerClass.newInstance();
      } catch (InstantiationException e) {
        logger.log(TreeLogger.ERROR, "Unable to create Linker", e);
        throw new UnableToCompleteException();
      } catch (IllegalAccessException e) {
        logger.log(TreeLogger.ERROR, "Unable to create Linker", e);
        throw new UnableToCompleteException();
      }
    }

    for (Map.Entry<String, Class<? extends Linker>> entry : module.getLinkers().entrySet()) {
      linkerShortNames.put(entry.getValue(), entry.getKey());
    }

    /*
     * This will make all private PublicResources and GeneratedResources appear
     * in the root of the module auxiliary directory.
     */
    linkerShortNames.put(this.getClass(), "");

    // Break ModuleDef properties out into LinkerContext interfaces
    {
      SortedSet<ConfigurationProperty> mutableConfigurationProperties = new TreeSet<ConfigurationProperty>(
          CONFIGURATION_PROPERTY_COMPARATOR);
      SortedSet<SelectionProperty> mutableSelectionProperties = new TreeSet<SelectionProperty>(
          SELECTION_PROPERTY_COMPARATOR);
      for (Property p : module.getProperties()) {
        // Create a new view
        if (p instanceof com.google.gwt.dev.cfg.ConfigurationProperty) {
          StandardConfigurationProperty newProp = new StandardConfigurationProperty(
              (com.google.gwt.dev.cfg.ConfigurationProperty) p);
          mutableConfigurationProperties.add(newProp);
          logger.log(TreeLogger.SPAM,
              "Added configuration property " + newProp, null);
        } else if (p instanceof BindingProperty) {
          StandardSelectionProperty newProp = new StandardSelectionProperty(
              (BindingProperty) p);
          mutableSelectionProperties.add(newProp);
          propertiesByName.put(newProp.getName(), newProp);
          logger.log(TreeLogger.SPAM, "Added selection property " + newProp,
              null);
        } else {
          logger.log(TreeLogger.ERROR, "Unknown property type "
              + p.getClass().getName());
        }
      }
      selectionProperties = Collections.unmodifiableSortedSet(mutableSelectionProperties);
      configurationProperties = Collections.unmodifiableSortedSet(mutableConfigurationProperties);
    }

    /*
     * Add static resources in the specified module as artifacts.
     */
    for (String path : module.getAllPublicFiles()) {
      String partialPath = path.replace(File.separatorChar, '/');
      PublicResource resource = new StandardPublicResource(partialPath,
          module.findPublicFile(path));
      artifacts.add(resource);
      logger.log(TreeLogger.SPAM, "Added public resource " + resource, null);
    }

    recordStaticReferences(logger, module);
  }

  /**
   * Adds or replaces Artifacts in the ArtifactSet that will be passed into the
   * Linkers invoked.
   */
  public void addOrReplaceArtifacts(ArtifactSet artifacts) {
    this.artifacts.removeAll(artifacts);
    this.artifacts.addAll(artifacts);
  }

  /**
   * Returns the ArtifactSet that will passed into the invoke Linkers.
   */
  public ArtifactSet getArtifacts() {
    return artifacts;
  }

  /**
   * Gets or creates a CompilationResult for the given JavaScript program.
   */
  public StandardCompilationResult getCompilation(
      PermutationResult permutationResult) {
    byte[][] js = permutationResult.getJs();
    String strongName = Util.computeStrongName(js);
    StandardCompilationResult result = resultsByStrongName.get(strongName);
    if (result == null) {
      result = new StandardCompilationResult(strongName, js,
          permutationResult.getSerializedSymbolMap(),
          permutationResult.getStatementRanges(),
          permutationResult.getPermutationId());
      resultsByStrongName.put(result.getStrongName(), result);
      artifacts.add(result);
    }
    artifacts.addAll(permutationResult.getArtifacts());
    return result;
  }

  public SortedSet<ConfigurationProperty> getConfigurationProperties() {
    return configurationProperties;
  }

  @Override
  public String getDescription() {
    return "Root Linker";
  }

  public String getModuleFunctionName() {
    return moduleFunctionName;
  }

  public long getModuleLastModified() {
    return moduleLastModified;
  }

  public String getModuleName() {
    return moduleName;
  }

  public SortedSet<SelectionProperty> getProperties() {
    return selectionProperties;
  }

  public StandardSelectionProperty getProperty(String name) {
    return propertiesByName.get(name);
  }

  /**
   * Run the linker stack.
   */
  public ArtifactSet invokeLink(TreeLogger logger)
      throws UnableToCompleteException {
    ArtifactSet workingArtifacts = new ArtifactSet(artifacts);

    for (Linker linker : linkers) {
      TreeLogger linkerLogger = logger.branch(TreeLogger.TRACE,
          "Invoking Linker " + linker.getDescription(), null);
      workingArtifacts.freeze();
      try {
        workingArtifacts = linker.link(linkerLogger, this, workingArtifacts);
      } catch (Throwable e) {
        linkerLogger.log(TreeLogger.ERROR, "Failed to link", e);
        throw new UnableToCompleteException();
      }
    }
    return workingArtifacts;
  }

  public ArtifactSet invokeRelink(TreeLogger logger,
      ArtifactSet newlyGeneratedArtifacts) throws UnableToCompleteException {
    ArtifactSet workingArtifacts = new ArtifactSet(newlyGeneratedArtifacts);

    for (Linker linker : linkers) {
      TreeLogger linkerLogger = logger.branch(TreeLogger.TRACE,
          "Invoking relink on Linker " + linker.getDescription(), null);
      workingArtifacts.freeze();
      try {
        workingArtifacts = linker.relink(linkerLogger, this, workingArtifacts);
      } catch (Throwable e) {
        linkerLogger.log(TreeLogger.ERROR, "Failed to relink", e);
        throw new UnableToCompleteException();
      }
    }
    return workingArtifacts;
  }

  public boolean isOutputCompact() {
    return jjsOptions.getOutput().shouldMinimize();
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    throw new UnsupportedOperationException();
  }

  public String optimizeJavaScript(TreeLogger logger, String program)
      throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.DEBUG, "Attempting to optimize JS", null);
    Reader r = new StringReader(program);
    JsProgram jsProgram = new JsProgram();
    JsScope topScope = jsProgram.getScope();
    JsName funcName = topScope.declareName(getModuleFunctionName());
    funcName.setObfuscatable(false);

    try {
      SourceInfo sourceInfo = jsProgram.createSourceInfoSynthetic(
          StandardLinkerContext.class, "Linker-derived JS");
      JsParser.parseInto(sourceInfo, topScope, jsProgram.getGlobalBlock(), r);
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

  /**
   * Emit EmittedArtifacts artifacts onto <code>out</code>. Does not close
   * <code>out</code>.
   * 
   * @param logger where to log progress
   * @param artifacts the artifacts to emit
   * @param emitPrivates whether to emit the private artifacts only, vs. the
   *          public artifacts only
   * @param out where to emit the artifact contents
   */
  public void produceOutput(TreeLogger logger, ArtifactSet artifacts,
      boolean emitPrivates, OutputFileSet out) throws UnableToCompleteException {
    String publicness = emitPrivates ? "private" : "public";
    logger = logger.branch(TreeLogger.TRACE, "Linking " + publicness
        + " artifacts into " + out.getPathDescription(), null);

    for (EmittedArtifact artifact : artifacts.find(EmittedArtifact.class)) {
      TreeLogger artifactLogger = logger.branch(TreeLogger.DEBUG,
          "Emitting resource " + artifact.getPartialPath(), null);

      if (artifact.isPrivate() != emitPrivates) {
        continue;
      }

      String partialPath = artifact.getPartialPath();
      if (artifact.isPrivate()) {
        partialPath = getExtraPathForLinker(artifact.getLinker(), partialPath);
        if (partialPath.startsWith("/")) {
          partialPath = partialPath.substring(1);
        }
      }
      try {
        OutputStream artifactStream = out.openForWrite(partialPath,
            artifact.getLastModified());
        artifact.writeTo(artifactLogger, artifactStream);
        artifactStream.close();
      } catch (IOException e) {
        artifactLogger.log(TreeLogger.ERROR,
            "Fatal error emitting this artifact", e);
      }
    }
  }

  /**
   * Creates a linker-specific subdirectory in the module's auxiliary output
   * directory.
   */
  private String getExtraPathForLinker(Class<? extends Linker> linkerType,
      String partialPath) {
    assert linkerShortNames.containsKey(linkerType) : linkerType.getName()
        + " unknown";
    return linkerShortNames.get(linkerType) + '/' + partialPath;
  }

  /**
   * Record script references and CSS references that are listed in the module
   * file.
   */
  private void recordStaticReferences(TreeLogger logger, ModuleDef module) {
    {
      int index = 0;
      for (Script script : module.getScripts()) {
        String url = script.getSrc();
        artifacts.add(new StandardScriptReference(url, index++));
        logger.log(TreeLogger.SPAM, "Added script " + url, null);
      }
    }

    {
      int index = 0;
      for (String style : module.getStyles()) {
        artifacts.add(new StandardStylesheetReference(style, index++));
        logger.log(TreeLogger.SPAM, "Added style " + style, null);
      }
    }
  }
}
