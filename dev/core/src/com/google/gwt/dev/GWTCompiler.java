/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.dev.util.SelectionScriptGenerator;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerScriptStyle;
import com.google.gwt.dev.util.arg.ArgHandlerTreeLoggerFlag;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.dev.util.log.TreeLoggerWidget;
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
 * The main executable entry point for the GWT java to javascript compiler.
 */
public class GWTCompiler extends ToolBase {

  private class ArgHandlerModuleName extends ArgHandlerExtra {

    public boolean addExtraArg(String arg) {
      setModuleName(arg);
      return true;
    }

    public String getPurpose() {
      return "Specifies the name of the module to compile";
    }

    public String[] getTagArgs() {
      return new String[] {"module"};
    }

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

    private final Map cache = new HashMap();

    private Compilation compilation;

    public CompilationRebindOracle() {
      super(typeOracle, propOracle, rules, genDir, cacheManager);
    }

    /**
     * Overridden so that we can selectively record inputs and outputs to derive
     * the cache key for a compilation. Note that the cache gets invalidated if
     * the propOracle changes state.
     */
    public String rebind(TreeLogger logger, String in)
        throws UnableToCompleteException {
      String out = (String) cache.get(in);
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
        List genTypes = (List) generatedTypesByResultTypeName.get(out);
        if (genTypes != null) {
          for (Iterator iter = genTypes.iterator(); iter.hasNext();) {
            JClassType genType = (JClassType) iter.next();
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
        typeOracle, propOracle, rules, genDir, cacheManager) {

      /**
       * Record generated types.
       */
      protected void onGeneratedTypes(String result, JClassType[] genTypes) {
        List list = new ArrayList();
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

      Set answers = new HashSet();

      Property[] orderedProps = perms.getOrderedProperties();
      for (Iterator iter = perms.iterator(); iter.hasNext();) {
        String[] orderedPropValues = (String[]) iter.next();

        // Create a snapshot of the property values by setting their values
        // in the property oracle. Because my rebindOracle uses the shared
        // generator context (which in turns uses the propOracle), this
        // has the effect we're after. It isn't reentrant, though, so don't
        // expect to call this recursively.
        //
        propOracle.setPropertyValues(orderedProps, orderedPropValues);

        // Ask the rebind oracle.
        //
        logProperties(logger, orderedProps, orderedPropValues);
        String resultTypeName = rebindOracle.rebind(logger, requestTypeName);
        answers.add(resultTypeName);
      }
      return (String[]) Util.toArray(String.class, answers);
    }
  }

  private static final String EXT_CACHE_XML = ".cache.xml";

  public static void main(String[] args) {
    GWTCompiler compiler = new GWTCompiler();
    if (compiler.processArgs(args)) {
      if (compiler.run()) {
        // The only successful return path.
        //
        return;
      }
    }
    if (!compiler.getUseGuiLogger()) {
      System.exit(1);
    } else {
      // This thread returns and the process ends with the gui logger window
      // is closed. Calling System.exit() doesn't give you a chance to view it.
    }
  }

  private final CacheManager cacheManager;

  private Compilations compilations = new Compilations();

  private String[] declEntryPts;

  private File genDir;

  private Map generatedTypesByResultTypeName = new HashMap();

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
      public void setLogLevel(Type level) {
        logLevel = level;
      }
    });

    registerHandler(new ArgHandlerGenDir() {
      public void setDir(File dir) {
        genDir = dir;
      }
    });

    registerHandler(new ArgHandlerOutDir() {
      public void setDir(File dir) {
        outDir = dir;
      }
    });

    registerHandler(new ArgHandlerTreeLoggerFlag() {
      public boolean setFlag() {
        useGuiLogger = true;
        return true;
      }
    });

    registerHandler(new ArgHandlerModuleName());

    registerHandler(new ArgHandlerScriptStyle() {
      public void setStyleDetailed() {
        GWTCompiler.this.setStyleDetailed();
      }

      public void setStyleObfuscated() {
        GWTCompiler.this.setStyleObfuscated();
      }

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
    //
    checkModule(logger);

    // Tweak the output directory so that output lives under the module name.
    //
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
    writeSelectionScript(logger, selGen);

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
    for (Iterator iter = perms.iterator(); iter.hasNext(); ++permNumber) {
      String[] orderedPropValues = (String[]) iter.next();
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
    StringBuffer sb = new StringBuffer();

    sb.append("<html>\n");

    // Setup the well-known variables.
    //
    sb.append("<head><script>\n");
    sb.append("var $wnd = parent;\n");
    sb.append("var $doc = $wnd.document;\n");
    sb.append("var $moduleName = \"" + moduleName + "\";\n");
    sb.append("</script></head>\n");

    // Set up the body to call the onload handler.
    //
    sb.append("<body>\n");

    // A nice message in case someone opens the file directly.
    //
    sb.append("<font face='arial' size='-1'>This script is part of module</font>\n");
    sb.append("<code>");
    sb.append(module.getName());
    sb.append("</code>\n");

    // Begin a script block inside the body. It's commented out so that the
    // browser won't mistake strings containing "<script>" for actual script.
    sb.append("<script><!--\n");

    String s = sb.toString();
    return s;
  }

  private String getHtmlSuffix() {
    StringBuffer sb = new StringBuffer();
    String moduleFunction = module.getName().replace('.', '_');

    // Generate the call to tell the bootstrap code that we're ready to go.
    sb.append("\n");
    sb.append("if ($wnd." + moduleFunction + ") $wnd." + moduleFunction
        + ".onScriptLoad();\n");
    sb.append("--></script></body></html>\n");

    String s = sb.toString();
    return s;
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
              + "' which no longers exists; cache entry will be removed";
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
    // Note that the rebindOracle is actually senstive to these values because
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
    String strongName = writeHtmlWithStrongName(logger, js);

    // Write out a cache control file that correlates to this script.
    //
    compilation.setStrongName(strongName);
    writeCacheFile(logger, compilation);

    // Add this compilation to the list of known compilations.
    //
    compilations.add(compilation);
    return compilation.getStrongName();
  }

  private boolean run() {
    AbstractTreeLogger logger;
    if (useGuiLogger) {
      logger = TreeLoggerWidget.getAsDetachedWindow("Build Output for "
          + moduleName, 800, 600, true);
    } else {
      logger = new PrintWriterTreeLogger();
    }

    try {
      logger.setMaxDetail(logLevel);

      ModuleDef moduleDef = ModuleDefLoader.loadFromClassPath(logger,
          moduleName);
      distill(logger, moduleDef);
      return true;
    } catch (UnableToCompleteException e) {
      // We intentionally don't pass in the exception here since the real
      // cause has been logged.
      //
      logger.log(TreeLogger.ERROR, "Build failed", null);
      return false;
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
   * Writes the script to a file enclosed in an html wrapper, encoded as UTF-8,
   * and whose filename is an MD5 hash of the script contents.
   * 
   * @return the base part of the strong name, which can be used to create other
   *         files with different extensions
   */
  private String writeHtmlWithStrongName(TreeLogger logger, String js)
      throws UnableToCompleteException {
    try {
      byte[] prefix = getHtmlPrefix().getBytes("UTF-8");
      byte[] scriptBytes = js.getBytes("UTF-8");
      byte[] suffix = getHtmlSuffix().getBytes("UTF-8");
      String strongName = Util.computeStrongName(scriptBytes);
      File outFile = new File(outDir, strongName + ".cache.html");
      Util.writeBytesToFile(logger, outFile, new byte[][] {
          prefix, scriptBytes, suffix});

      String msg = "Compilation written to " + outFile.getAbsolutePath();
      logger.log(TreeLogger.TRACE, msg, null);

      return strongName;
    } catch (UnsupportedEncodingException e) {
      logger.log(TreeLogger.ERROR, "Unable to encode compiled script as UTF-8",
          e);
      throw new UnableToCompleteException();
    }
  }

  private void writeSelectionScript(TreeLogger logger,
      SelectionScriptGenerator selGen) {
    String html = selGen.generateSelectionScript(obfuscate);
    String fn = module.getName() + ".nocache.js";
    File selectionFile = new File(outDir, fn);
    Util.writeStringAsFile(selectionFile, html);
    String msg = "Compilation selection script written to "
        + selectionFile.getAbsolutePath();
    logger.log(TreeLogger.TRACE, msg, null);
  }
}
