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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.Compilation;
import com.google.gwt.dev.cfg.CompilationSchema;
import com.google.gwt.dev.cfg.Compilations;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.cfg.PropertyPermutations;
import com.google.gwt.dev.cfg.Rules;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.jdt.CacheManager;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jdt.StandardSourceOracle;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.jjs.JavaToJavaScriptCompiler;
import com.google.gwt.dev.shell.StandardRebindOracle;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.SelectionScriptGenerator;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerScriptStyle;
import com.google.gwt.dev.util.arg.ArgHandlerTreeLoggerFlag;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.DetachedTreeLoggerWindow;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.dev.util.xml.ReflectiveParser;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.ToolBase;
import com.google.gwt.util.tools.Utility;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The main executable entry point for the GWT Java to JavaScript compiler.
 */
public class GWTCompiler extends ToolBase {

  private class ArgHandlerModuleName extends ArgHandlerExtra {

    @Override
    public boolean addExtraArg(String arg) {
      setModuleName(arg);
      return true;
    }

    @Override
    public String getPurpose() {
      return "Specifies the name of the module to compile";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"module"};
    }

    @Override
    public boolean isRequired() {
      return true;
    }
  }

  /**
   * Used to smartly deal with rebind across the production of an entire
   * permutation, including cache checking and recording the inputs and outputs
   * into a {@link Compilation}.
   */
  private class CompilationRebindOracle extends StandardRebindOracle {

    private final Map<String, String> cache = new HashMap<String, String>();

    private Compilation compilation;

    public CompilationRebindOracle() {
      super(typeOracle, propOracle, rules, genDir, outDir, cacheManager);
    }

    /**
     * Overridden so that we can selectively record inputs and outputs to derive
     * the cache key for a compilation. Note that the cache gets invalidated if
     * the propOracle changes state.
     */
    @Override
    public String rebind(TreeLogger logger, String in)
        throws UnableToCompleteException {
      String out = cache.get(in);
      if (out == null) {
        // Actually do the work, then cache it.
        //
        out = super.rebind(logger, in);
        cache.put(in, out);
      } else {
        // Was cached.
        //
        String msg = "Rebind answer for '" + in + "' found in cache " + out;
        logger.log(TreeLogger.DEBUG, msg, null);
      }

      if (compilation != null && compilation.recordDecision(in, out)) {
        List<JClassType> genTypes = generatedTypesByResultTypeName.get(out);
        if (genTypes != null) {
          for (JClassType genType : genTypes) {
            String sourceHash = genType.getTypeHash();
            String genTypeName = genType.getQualifiedSourceName();
            compilation.recordGeneratedTypeHash(genTypeName, sourceHash);
          }
        }
      }

      return out;
    }

    public void recordInto(Compilation compilation) {
      this.compilation = compilation;
    }
  }

  private class DistillerRebindPermutationOracle implements
      RebindPermutationOracle {

    private final StandardRebindOracle rebindOracle = new StandardRebindOracle(
        typeOracle, propOracle, rules, genDir, outDir, cacheManager) {

      /**
       * Record generated types.
       */
      @Override
      protected void onGeneratedTypes(String result, JClassType[] genTypes) {
        List<JClassType> list = new ArrayList<JClassType>();
        Util.addAll(list, genTypes);
        Object existing = generatedTypesByResultTypeName.put(result, list);
        assert (existing == null) : "Internal error: redundant notification of generated types";
      }
    };

    public String[] getAllPossibleRebindAnswers(TreeLogger logger,
        String requestTypeName) throws UnableToCompleteException {

      String msg = "Computing all possible rebind results for '"
          + requestTypeName + "'";
      logger = logger.branch(TreeLogger.DEBUG, msg, null);

      Set<String> answers = new HashSet<String>();

      Property[] orderedProps = perms.getOrderedProperties();
      for (Iterator<String[]> iter = perms.iterator(); iter.hasNext();) {
        String[] orderedPropValues = iter.next();

        // Create a snapshot of the property values by setting their values
        // in the property oracle. Because my rebindOracle uses the shared
        // generator context (which in turns uses the propOracle), this
        // has the effect we're after. It isn't reentrant, though, so don't
        // expect to call this recursively.
        propOracle.setPropertyValues(orderedProps, orderedPropValues);

        // Ask the rebind oracle.
        logProperties(logger, orderedProps, orderedPropValues);
        String resultTypeName = rebindOracle.rebind(logger, requestTypeName);
        answers.add(resultTypeName);
      }
      return Util.toArray(String.class, answers);
    }
  }

  private static final String EXT_CACHE_XML = ".cache.xml";

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    GWTCompiler compiler = new GWTCompiler();
    if (compiler.processArgs(args)) {
      if (compiler.run()) {
        // Exit w/ success code.
        System.exit(0);
      }
    }
    // Exit w/ non-success code.
    System.exit(1);
  }

  private final CacheManager cacheManager;

  private Compilations compilations = new Compilations();

  private String[] declEntryPts;

  private File genDir;

  private Map<String, List<JClassType>> generatedTypesByResultTypeName = new HashMap<String, List<JClassType>>();

  private JavaToJavaScriptCompiler jjs;

  private Type logLevel;

  private ModuleDef module;

  private String moduleName;

  private boolean obfuscate;

  private File outDir;

  private PropertyPermutations perms;

  private boolean prettyNames;

  private Properties properties;

  private StaticPropertyOracle propOracle = new StaticPropertyOracle();

  private RebindPermutationOracle rebindPermOracle;

  private Rules rules;

  private StandardSourceOracle sourceOracle;

  private TypeOracle typeOracle;

  private boolean useGuiLogger;

  public GWTCompiler() {
    this(null);
  }

  public GWTCompiler(CacheManager cacheManager) {
    registerHandler(new ArgHandlerLogLevel() {
      @Override
      public void setLogLevel(Type level) {
        logLevel = level;
      }
    });

    registerHandler(new ArgHandlerGenDir() {
      @Override
      public void setDir(File dir) {
        genDir = dir;
      }
    });

    registerHandler(new ArgHandlerOutDir() {
      @Override
      public void setDir(File dir) {
        outDir = dir;
      }
    });

    registerHandler(new ArgHandlerTreeLoggerFlag() {
      @Override
      public boolean setFlag() {
        useGuiLogger = true;
        return true;
      }
    });

    registerHandler(new ArgHandlerModuleName());

    registerHandler(new ArgHandlerScriptStyle() {
      @Override
      public void setStyleDetailed() {
        GWTCompiler.this.setStyleDetailed();
      }

      @Override
      public void setStyleObfuscated() {
        GWTCompiler.this.setStyleObfuscated();
      }

      @Override
      public void setStylePretty() {
        GWTCompiler.this.setStylePretty();
      }
    });
    this.cacheManager = cacheManager;
  }

  public void distill(TreeLogger logger, ModuleDef moduleDef)
      throws UnableToCompleteException {
    this.module = moduleDef;

    // Set up all the initial state.
    checkModule(logger);

    // Tweak the output directory so that output lives under the module name.
    outDir = new File(outDir, module.getName());

    rules = module.getRules();
    typeOracle = module.getTypeOracle(logger);
    sourceOracle = new StandardSourceOracle(typeOracle);
    declEntryPts = module.getEntryPointTypeNames();
    rebindPermOracle = new DistillerRebindPermutationOracle();
    properties = module.getProperties();
    perms = new PropertyPermutations(properties);
    WebModeCompilerFrontEnd frontEnd = new WebModeCompilerFrontEnd(
        sourceOracle, rebindPermOracle);
    jjs = new JavaToJavaScriptCompiler(logger, frontEnd, declEntryPts,
        obfuscate, prettyNames);
    initCompilations(logger);

    // Compile for every permutation of properties.
    //
    SelectionScriptGenerator selGen = compilePermutations(logger);

    // Generate a selection script to pick the right permutation.
    //
    writeSelectionScripts(logger, selGen);

    // Copy all public files into the output directory.
    //
    copyPublicFiles(logger);

    logger.log(TreeLogger.INFO, "Compilation succeeded", null);
  }

  public File getGenDir() {
    return genDir;
  }

  public Type getLogLevel() {
    return logLevel;
  }

  public String getModuleName() {
    return moduleName;
  }

  public boolean getUseGuiLogger() {
    return useGuiLogger;
  }

  public void setGenDir(File dir) {
    genDir = dir;
  }

  public void setLogLevel(Type level) {
    this.logLevel = level;
  }

  public void setModuleName(String name) {
    moduleName = name;
  }

  public void setOutDir(File outDir) {
    this.outDir = outDir;
  }

  public void setStyleDetailed() {
    obfuscate = false;
    prettyNames = false;
  }

  public void setStyleObfuscated() {
    obfuscate = true;
  }

  public void setStylePretty() {
    obfuscate = false;
    prettyNames = true;
  }

  private void checkModule(TreeLogger logger) throws UnableToCompleteException {
    if (module.getEntryPointTypeNames().length == 0) {
      logger.log(TreeLogger.ERROR, "Module has no entry points defined", null);
      throw new UnableToCompleteException();
    }
  }

  private SelectionScriptGenerator compilePermutations(TreeLogger logger)
      throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.INFO, "Output will be written into "
        + outDir, null);
    Property[] orderedProps = perms.getOrderedProperties();
    SelectionScriptGenerator selGen = new SelectionScriptGenerator(module,
        orderedProps);
    int permNumber = 1;
    for (Iterator<String[]> iter = perms.iterator(); iter.hasNext(); ++permNumber) {

      String[] orderedPropValues = iter.next();
      String strongName = realizePermutation(logger, orderedProps,
          orderedPropValues, permNumber);
      selGen.recordSelection(orderedPropValues, strongName);
    }
    return selGen;
  }

  private void copyPublicFiles(TreeLogger logger)
      throws UnableToCompleteException {
    TreeLogger branch = null;
    boolean anyCopied = false;
    String[] files = module.getAllPublicFiles();
    for (int i = 0; i < files.length; ++i) {
      URL from = module.findPublicFile(files[i]);
      File to = new File(outDir, files[i]);
      boolean copied = Util.copy(logger, from, to);
      if (copied) {
        if (!anyCopied) {
          branch = logger.branch(TreeLogger.INFO,
              "Copying all files found on public path", null);
          if (!logger.isLoggable(TreeLogger.TRACE)) {
            branch = null;
          }
          anyCopied = true;
        }

        if (branch != null) {
          branch.log(TreeLogger.TRACE, to.getAbsolutePath(), null);
        }
      }
    }
  }

  private String getHtmlPrefix() {
    DefaultTextOutput out = new DefaultTextOutput(obfuscate);
    out.print("<html>");
    out.newlineOpt();

    // Setup the well-known variables.
    //
    out.print("<head><script>");
    out.newlineOpt();
    out.print("var $wnd = parent;");
    out.newlineOpt();
    out.print("var $doc = $wnd.document;");
    out.newlineOpt();
    out.print("var $moduleName, $moduleBase;");
    out.newlineOpt();
    out.print("</script></head>");
    out.newlineOpt();
    out.print("<body>");
    out.newlineOpt();

    // A nice message in case someone opens the file directly.
    //
    out.print("<font face='arial' size='-1'>This script is part of module</font>");
    out.newlineOpt();
    out.print("<code>");
    out.print(module.getName());
    out.print("</code>");
    out.newlineOpt();

    // Begin a script block inside the body. It's commented out so that the
    // browser won't mistake strings containing "<script>" for actual script.
    out.print("<script><!--");
    out.newline();
    return out.toString();
  }

  private String getHtmlSuffix() {
    DefaultTextOutput out = new DefaultTextOutput(obfuscate);
    String moduleFunction = module.getName().replace('.', '_');

    // Generate the call to tell the bootstrap code that we're ready to go.
    out.newlineOpt();
    out.print("if ($wnd." + moduleFunction + ") $wnd." + moduleFunction
        + ".onScriptLoad();");
    out.newline();
    out.print("--></script></body></html>");
    out.newlineOpt();

    return out.toString();
  }

  private String getJsPrefix() {
    DefaultTextOutput out = new DefaultTextOutput(obfuscate);

    out.print("(function(){");
    out.newlineOpt();

    // Setup the well-known variables.
    //
    out.print("var $wnd = window;");
    out.newlineOpt();
    out.print("var $doc = $wnd.document;");
    out.newlineOpt();
    out.print("var $moduleName, $moduleBase;");
    out.newlineOpt();

    return out.toString();
  }

  private String getJsSuffix() {
    DefaultTextOutput out = new DefaultTextOutput(obfuscate);
    String moduleFunction = module.getName().replace('.', '_');

    // Generate the call to tell the bootstrap code that we're ready to go.
    out.newlineOpt();
    out.print("if (" + moduleFunction + ") {");
    out.newlineOpt();
    out.print("  var __gwt_initHandlers = " + moduleFunction
        + ".__gwt_initHandlers;");
    out.print("  " + moduleFunction + ".onScriptLoad(gwtOnLoad);");
    out.newlineOpt();
    out.print("}");
    out.newlineOpt();
    out.print("})();");
    out.newlineOpt();

    return out.toString();
  }

  /**
   * This has to run after JJS exists which means that all rebind perms have
   * happened and thus the type oracle knows about everything.
   */
  private void initCompilations(TreeLogger logger)
      throws UnableToCompleteException {
    File[] cacheXmls = outDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(EXT_CACHE_XML);
      }
    });

    if (cacheXmls == null) {
      return;
    }

    long newestCup = jjs.getLastModifiedTimeOfNewestCompilationUnit();
    for (int i = 0; i < cacheXmls.length; i++) {
      File cacheXml = cacheXmls[i];
      String fn = cacheXml.getName();
      String strongName = fn.substring(0, fn.length() - EXT_CACHE_XML.length());

      // Make sure the cached script is not out of date.
      //
      long cacheXmlLastMod = cacheXml.lastModified();
      if (cacheXmlLastMod < newestCup) {
        // It is out of date; no need to even parse the XML.
        //
        String msg = "Compilation '" + fn
            + "' is out of date and will be removed";
        logger.log(TreeLogger.TRACE, msg, null);
        Util.deleteFilesStartingWith(outDir, strongName);
        continue;
      }

      // It is up-to-date, so we at least can load it.
      // We still need to verify that the source for generated types hasn't
      // changed.
      //
      TreeLogger branch = logger.branch(TreeLogger.DEBUG,
          "Loading cached compilation: " + cacheXml, null);
      Compilation c = new Compilation();
      c.setStrongName(strongName);
      CompilationSchema schema = new CompilationSchema(c);
      FileReader r = null;
      Throwable caught = null;
      try {
        r = new FileReader(cacheXml);
        ReflectiveParser.parse(logger, schema, r);
      } catch (FileNotFoundException e) {
        caught = e;
      } catch (UnableToCompleteException e) {
        caught = e;
      } finally {
        Utility.close(r);
      }

      if (caught != null) {
        String msg = "Unable to load the cached file";
        branch.log(TreeLogger.WARN, msg, caught);
        continue;
      }

      // Check that the hash code of the generated sources for this compilation
      // matches the current generated source for the same type.
      //
      boolean isBadCompilation = false;
      String[] genTypes = c.getGeneratedTypeNames();
      for (int j = 0; j < genTypes.length; j++) {
        String genTypeName = genTypes[j];
        String cachedHash = c.getTypeHash(genTypeName);
        JClassType genType = typeOracle.findType(genTypeName);
        if (genType == null) {
          // This cache entry refers to a type that no longer seems to exist.
          // Remove it.
          //
          String msg = "Compilation '" + fn + "' refers to generated type '"
              + genTypeName
              + "' which no longer exists; cache entry will be removed";
          branch.log(TreeLogger.TRACE, msg, null);
          Util.deleteFilesStartingWith(outDir, strongName);
          isBadCompilation = true;
          break;
        }

        String currentHash = genType.getTypeHash();

        if (!cachedHash.equals(currentHash)) {
          String msg = "Compilation '"
              + fn
              + "' was compiled with a different version of generated source for '"
              + genTypeName + "'; cache entry will be removed";
          branch.log(TreeLogger.TRACE, msg, null);
          Util.deleteFilesStartingWith(outDir, strongName);
          isBadCompilation = true;
          break;
        }
      }

      if (!isBadCompilation) {
        // Okay -- this compilation should be a cache candidate.
        compilations.add(c);
      }
    }
  }

  private void logProperties(TreeLogger logger, Property[] props,
      String[] values) {
    if (logger.isLoggable(TreeLogger.DEBUG)) {
      logger = logger.branch(TreeLogger.DEBUG, "Setting properties", null);
      for (int i = 0; i < props.length; i++) {
        String name = props[i].getName();
        String value = values[i];
        logger.log(TreeLogger.TRACE, name + " = " + value, null);
      }
    }
  }

  /**
   * Attempts to compile with a single permutation of properties. The result can
   * be one of the following:
   * <ul>
   * <li>There is an existing compilation having the same deferred binding
   * results (and thus would create identical output); compilation is skipped
   * <li>No existing compilation unit matches, so the compilation proceeds
   * </ul>
   */
  private String realizePermutation(TreeLogger logger, Property[] currentProps,
      String[] currentValues, int permNumber) throws UnableToCompleteException {
    String msg = "Analyzing permutation #" + permNumber;
    logger = logger.branch(TreeLogger.TRACE, msg, null);

    logProperties(logger, currentProps, currentValues);

    // Create a rebind oracle that will record decisions so that we can cache
    // them and avoid future computations.
    //
    CompilationRebindOracle rebindOracle = new CompilationRebindOracle();

    // Tell the property provider above about the current property values.
    // Note that the rebindOracle is actually sensitive to these values because
    // in its ctor is uses propOracle as its property oracle.
    //
    propOracle.setPropertyValues(currentProps, currentValues);

    // Check to see if we already have this compilation.
    // This will have the effect of filling the rebind oracle's cache.
    //
    String[] entryPts = module.getEntryPointTypeNames();
    Compilation cached = compilations.find(logger, rebindOracle, entryPts);
    if (cached != null) {
      msg = "Matches existing compilation " + cached.getStrongName();
      logger.log(TreeLogger.TRACE, msg, null);
      return cached.getStrongName();
    }

    // Now attach a compilation into which we can record the particular inputs
    // and outputs used by this compile process.
    //
    Compilation compilation = new Compilation();
    rebindOracle.recordInto(compilation);

    // Create JavaScript.
    //
    String js = jjs.compile(logger, rebindOracle);

    // Create a wrapper and an unambiguous name for the file.
    //
    String strongName = writeHtmlAndJsWithStrongName(logger, js);

    // Write out a cache control file that correlates to this script.
    //
    compilation.setStrongName(strongName);
    writeCacheFile(logger, compilation);

    // Add this compilation to the list of known compilations.
    //
    compilations.add(compilation);
    return compilation.getStrongName();
  }

  /**
   * Runs the compiler. If a gui-based TreeLogger is used, this method will not
   * return until its window is closed by the user.
   * 
   * @return success from the compiler, <code>true</code> if the compile
   *         completed without errors, <code>false</code> otherwise.
   */
  private boolean run() {
    // Set any platform specific system properties.
    BootStrapPlatform.setSystemProperties();

    if (useGuiLogger) {
      // Initialize a tree logger window.
      DetachedTreeLoggerWindow loggerWindow = DetachedTreeLoggerWindow.getInstance(
          "Build Output for " + moduleName, 800, 600, true);

      // Eager AWT initialization for OS X to ensure safe coexistence with SWT.
      BootStrapPlatform.maybeInitializeAWT();

      final AbstractTreeLogger logger = loggerWindow.getLogger();
      final boolean[] success = new boolean[1];

      // Compiler will be spawned onto a second thread, UI thread for tree
      // logger will remain on the main.
      Thread compilerThread = new Thread(new Runnable() {
        public void run() {
          success[0] = GWTCompiler.this.run(logger);
        }
      });

      compilerThread.setName("GWTCompiler Thread");
      compilerThread.start();
      loggerWindow.run();

      // Even if the tree logger window is closed, we wait for the compiler
      // to finish.
      waitForThreadToTerminate(compilerThread);

      return success[0];
    } else {
      return run(new PrintWriterTreeLogger());
    }
  }

  private boolean run(AbstractTreeLogger logger) {
    try {
      logger.setMaxDetail(logLevel);

      ModuleDef moduleDef = ModuleDefLoader.loadFromClassPath(logger,
          moduleName);
      distill(logger, moduleDef);
      return true;
    } catch (UnableToCompleteException e) {
      // We intentionally don't pass in the exception here since the real
      // cause has been logged.
      logger.log(TreeLogger.ERROR, "Build failed", null);
      return false;
    }
  }

  /**
   * Waits for a thread to terminate before it returns. This method is a
   * non-cancellable task, in that it will defer thread interruption until it is
   * done.
   * 
   * @param godot the thread that is being waited on.
   */
  private void waitForThreadToTerminate(final Thread godot) {
    // Goetz pattern for non-cancellable tasks.
    // http://www-128.ibm.com/developerworks/java/library/j-jtp05236.html
    boolean isInterrupted = false;
    try {
      while (true) {
        try {
          godot.join();
          return;
        } catch (InterruptedException e) {
          isInterrupted = true;
        }
      }
    } finally {
      if (isInterrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void writeCacheFile(TreeLogger logger, Compilation compilation)
      throws UnableToCompleteException {
    // Create and write the cache file.
    // The format matches the one read in consumeCacheEntry().
    //
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db;
    try {
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      logger.log(TreeLogger.ERROR, "Unable to construct cache entry XML", e);
      throw new UnableToCompleteException();
    }
    Document doc = db.newDocument();

    // <cache-entry ...>
    Element docElem = doc.createElement("cache-entry");
    doc.appendChild(docElem);

    // <generated-type-hash ...>
    String[] genTypeNames = compilation.getGeneratedTypeNames();
    for (int i = 0; i < genTypeNames.length; i++) {
      String genTypeName = genTypeNames[i];
      String hash = compilation.getTypeHash(genTypeName);
      Element childElem = doc.createElement("generated-type-hash");
      docElem.appendChild(childElem);
      childElem.setAttribute("class", genTypeName);
      childElem.setAttribute("hash", hash);
    }

    // deferred binding decisions
    String[] inputs = compilation.getRebindInputs();
    for (int i = 0, n = inputs.length; i < n; ++i) {
      String in = inputs[i];
      String out = compilation.getRebindOutput(in);

      // <rebind-decision ...>
      Element childElem = doc.createElement("rebind-decision");
      docElem.appendChild(childElem);
      childElem.setAttribute("in", in);
      childElem.setAttribute("out", out);
    }

    // Persist it.
    //
    String strongName = compilation.getStrongName();
    File cacheFile = new File(outDir, strongName + EXT_CACHE_XML);
    byte[] bytes = Util.toXmlUtf8(doc);
    Util.writeBytesToFile(logger, cacheFile, bytes);

    String msg = "Compilation metadata written to "
        + cacheFile.getAbsolutePath();
    logger.log(TreeLogger.TRACE, msg, null);
  }

  /**
   * Writes the script to 1) an HTML file containing the script and 2) a
   * JavaScript file containing the script. Contenst are encoded as UTF-8, and
   * the filenames will be an MD5 hash of the script contents.
   * 
   * @return the base part of the strong name, which can be used to create other
   *         files with different extensions
   */
  private String writeHtmlAndJsWithStrongName(TreeLogger logger, String js)
      throws UnableToCompleteException {
    try {
      byte[] scriptBytes = js.getBytes("UTF-8");
      String strongName = Util.computeStrongName(scriptBytes);
      {
        byte[] prefix = getHtmlPrefix().getBytes("UTF-8");
        byte[] suffix = getHtmlSuffix().getBytes("UTF-8");
        File outFile = new File(outDir, strongName + ".cache.html");
        Util.writeBytesToFile(logger, outFile, new byte[][] {
            prefix, scriptBytes, suffix});
        String msg = "Compilation written to " + outFile.getAbsolutePath();
        logger.log(TreeLogger.TRACE, msg, null);
      }
      {
        byte[] prefix = getJsPrefix().getBytes("UTF-8");
        byte[] suffix = getJsSuffix().getBytes("UTF-8");
        File outFile = new File(outDir, strongName + ".cache.js");
        Util.writeBytesToFile(logger, outFile, new byte[][] {
            prefix, scriptBytes, suffix});
        String msg = "Compilation written to " + outFile.getAbsolutePath();
        logger.log(TreeLogger.TRACE, msg, null);
      }
      return strongName;
    } catch (UnsupportedEncodingException e) {
      logger.log(TreeLogger.ERROR, "Unable to encode compiled script as UTF-8",
          e);
      throw new UnableToCompleteException();
    }
  }

  private void writeSelectionScripts(TreeLogger logger,
      SelectionScriptGenerator selGen) {
    {
      String html = selGen.generateSelectionScript(obfuscate, false);
      String fn = module.getName() + ".nocache.js";
      File selectionFile = new File(outDir, fn);
      Util.writeStringAsFile(selectionFile, html);
      String msg = "Compilation selection script written to "
          + selectionFile.getAbsolutePath();
      logger.log(TreeLogger.TRACE, msg, null);
    }
    {
      String html = selGen.generateSelectionScript(obfuscate, true);
      String fn = module.getName() + "-xs.nocache.js";
      File selectionFile = new File(outDir, fn);
      Util.writeStringAsFile(selectionFile, html);
      String msg = "Compilation selection script written to "
          + selectionFile.getAbsolutePath();
      logger.log(TreeLogger.TRACE, msg, null);
    }
  }
}
