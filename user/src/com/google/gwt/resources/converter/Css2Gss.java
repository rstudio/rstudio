/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.resources.converter;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.resources.css.GenerateCssAst;
import com.google.gwt.resources.css.ast.CssStylesheet;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.base.Splitter;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.io.Files;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Converter from Css to Gss.
 */
public class Css2Gss {
  private final URL cssFile;
  private final TreeLogger treeLogger;
  private final boolean lenient;

  private PrintWriter printWriter;
  private Map<String, String> defNameMapping;
  private Predicate<String> simpleBooleanConditionPredicate;
  private final Set<URL> scopeFiles;

  public Css2Gss(String filePath) throws MalformedURLException {
    this(new File(filePath).toURI().toURL(), false);
  }

  public Css2Gss(URL resource, boolean lenient) {
    this(resource, lenient, Predicates.<String>alwaysFalse(), new HashSet<URL>());
  }

  public Css2Gss(URL resource, boolean lenient,
      Predicate<String> simpleBooleanConditionPredicate, Set<URL> scopeFiles) {
    cssFile = resource;
    printWriter = new PrintWriter(System.err);
    PrintWriterTreeLogger printWriterTreeLogger = new PrintWriterTreeLogger(printWriter);
    printWriterTreeLogger.setMaxDetail(Type.WARN);
    this.treeLogger = printWriterTreeLogger;
    this.lenient = lenient;
    this.simpleBooleanConditionPredicate = simpleBooleanConditionPredicate;
    this.scopeFiles = scopeFiles;
  }

  public Css2Gss(URL fileUrl, TreeLogger treeLogger, boolean lenient,
      Predicate<String> simpleBooleanConditionPredicate, Set<URL> scopeFiles) {
    cssFile = fileUrl;
    this.treeLogger = treeLogger;
    this.lenient = lenient;
    this.simpleBooleanConditionPredicate = simpleBooleanConditionPredicate;
    this.scopeFiles = scopeFiles;
  }

  public Css2Gss(URL url, TreeLogger logger, boolean lenientConversion,
      Predicate<String> simpleBooleanConditionPredicate) {
    this(url, logger, lenientConversion, simpleBooleanConditionPredicate, new HashSet<URL>());
  }

  public String toGss() throws UnableToCompleteException {
      try {
        CssStylesheet sheet = GenerateCssAst.exec(treeLogger, cssFile);

        DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(lenient, treeLogger);
        defCollectorVisitor.accept(sheet);
        defNameMapping = defCollectorVisitor.getDefMapping();

        addScopeDefs(scopeFiles, defNameMapping);

        new UndefinedConstantVisitor(new HashSet<String>(defNameMapping.values()),
            lenient, treeLogger).accept(sheet);

        new ElseNodeCreator().accept(sheet);

        new AlternateAnnotationCreatorVisitor().accept(sheet);

        GssGenerationVisitor gssGenerationVisitor = new GssGenerationVisitor(
            new DefaultTextOutput(false), defNameMapping, lenient, treeLogger,
            simpleBooleanConditionPredicate);
        gssGenerationVisitor.accept(sheet);

        return gssGenerationVisitor.getContent();
      } finally {
        if (printWriter != null) {
          printWriter.flush();
        }
      }
  }

  private void addScopeDefs(Set<URL> scopeFiles, Map<String, String> defNameMapping)
      throws UnableToCompleteException {
    for (URL fileName : scopeFiles) {
      CssStylesheet sheet = GenerateCssAst.exec(treeLogger, fileName);
      DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(lenient, treeLogger);
      defCollectorVisitor.accept(sheet);
      defNameMapping.putAll(defCollectorVisitor.getDefMapping());
    }
  }

  /**
   * GSS allows only uppercase letters and numbers for a name of the constant. The constants
   * need to be renamed in order to be compatible with GSS. This method returns a mapping
   * between the old name and the new name compatible with GSS.
   */
  public Map<String, String> getDefNameMapping() {
    return defNameMapping;
  }

  public static void main(String... args) {

    Options options = Options.parseOrQuit(args);

    if (options.singleFile) {
      try {
        System.out.println(convertFile(options.resource, options.simpleBooleanConditions,
            options.scopeFiles));
        System.exit(0);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(-1);
      }
    }

    Collection<File> filesToConvert =
        FileUtils.listFiles(options.resource, new String[] {"css"}, options.recurse);

    for (File cssFile : filesToConvert) {
      try {
        if (doesCorrespondingGssFileExists(cssFile)) {
          System.out.println(
              "GSS file already exists - will not convert, file: " + cssFile.getAbsolutePath());
          continue;
        }
        String gss = convertFile(cssFile, options.simpleBooleanConditions,
            options.scopeFiles);
        writeGss(gss, cssFile);
        System.out.println("Converted " + cssFile.getAbsolutePath());
      } catch (Exception e) {
        System.err.println("Failed to convert " + cssFile.getAbsolutePath());
        e.printStackTrace();
      }
    }
  }

  private static boolean doesCorrespondingGssFileExists(File cssFile) {
    File gssFile = getCorrespondingGssFile(cssFile);
    return gssFile.exists();
  }

  private static File getCorrespondingGssFile(File cssFile) {
    String name = cssFile.getName();
    assert name.endsWith(".css");

    name = name.substring(0, name.length() - ".css".length()) + ".gss";
    return new File(cssFile.getParentFile(), name);
  }

  private static void writeGss(String gss, File cssFile) throws IOException {
    File gssFile = getCorrespondingGssFile(cssFile);
    Files.asCharSink(gssFile, Charsets.UTF_8).write(gss);
  }

  private static String convertFile(File resource, Set<String> simpleBooleanConditions,
      Set<URL> scopeFiles) throws MalformedURLException, UnableToCompleteException {
    Predicate<String> simpleConditionPredicate;

    if (simpleBooleanConditions != null) {
      simpleConditionPredicate = Predicates.in(simpleBooleanConditions);
    } else {
      simpleConditionPredicate = Predicates.alwaysFalse();
    }

    return new Css2Gss(resource.toURI().toURL(), false, simpleConditionPredicate,
        scopeFiles).toGss();
  }

  private static void printUsage() {
    System.err.println("Usage :");
    System.err.println("java " + Css2Gss.class.getName() + " [Options] [file or directory]");
    System.err.println("Options:");
    System.err.println(" -r -> Recursively convert all css files on the given directory"
        + "(leaves .css files in place)");
    System.err.println(" -condition list_of_condition -> Specify a comma-separated list of " +
        "variables that are used in conditionals and that will be mapped to configuration " +
        "properties. The converter will not use the is() function when it will convert these " +
        "conditions");
    System.err.println(" -scope list_of_files -> Specify a comma-separated list of "
        + "css files to be used in this conversion to determine all defined variables");
  }

  static class Options {
    static final Map<String, ArgumentConsumer> argumentConsumers;

    static {
      argumentConsumers = new LinkedHashMap<String, ArgumentConsumer>();
      argumentConsumers.put("-r", new ArgumentConsumer() {
        @Override
        public boolean consume(Options option, String nextArg) {
          option.recurse = true;
          return false;
        }
      });

      argumentConsumers.put("-condition", new ArgumentConsumer() {
        @Override
        public boolean consume(Options option, String nextArg) {
          if (nextArg == null) {
            quitEarly("-condition option must be followed by a comma separated list of conditions");
          }

          option.simpleBooleanConditions = FluentIterable.from(Splitter.on(',').split(nextArg))
              .toSet();

          return true;
        }
      });

      argumentConsumers.put("-basedir", new ArgumentConsumer() {
        @Override
        public boolean consume(Options option, String nextArg) {
          nextArg += nextArg.endsWith(File.separator) ? "" : File.separator;
          option.baseDir = new File(nextArg);

          if (!option.baseDir.exists() || !option.baseDir.isDirectory()) {
            quitEarly("Basedir is does not exist");
          }

          return true;
        }
      });

      argumentConsumers.put("-scope", new ArgumentConsumer() {
          @Override
        public boolean consume(Options option, String nextArg) {
          option.scope = nextArg;
          return true;
        }
      });
    }

    boolean recurse;
    boolean singleFile;
    ImmutableSet<URL> scopeFiles = ImmutableSet.of();
    File resource;
    Set<String> simpleBooleanConditions;
    File baseDir;

    private String scope;

    private static Options parseOrQuit(String[] args) {
      if (!validateArgs(args)) {
        quitEarly(null);
      }

      Options options = new Options();

      int index = 0;

      // consume options
      while (index < args.length - 1) {
        String arg = args[index++];
        String nextArg = index < args.length - 1 ? args[index] : null;

        ArgumentConsumer consumer = argumentConsumers.get(arg);

        if (consumer == null) {
          quitEarly("Unknown argument: " + arg);
        }

        boolean skipNextArg = consumer.consume(options, nextArg);

        if (skipNextArg) {
          index++;
        }
      }

      if (index == args.length) {
        quitEarly("Missing file or directly as last parameter");
      }

      if (options.scope != null) {
        ImmutableSet<String> scopeFileSet =
            FluentIterable.from(Splitter.on(',').split(options.scope)).toSet();
        HashSet<URL> set = new HashSet<URL>();
        for (String scopeFile : scopeFileSet) {
          File file = null;
          if (options.baseDir != null && !scopeFile.startsWith(File.separator)) {
            file = new File(options.baseDir, scopeFile).getAbsoluteFile();
          } else {
            file = new File(scopeFile).getAbsoluteFile();
          }
          if (!file.exists() && !file.isFile()) {
            quitEarly("The scope file '" + file.getAbsolutePath() + "' does not exist");
          }
          try {
            set.add(file.toURI().toURL());
          } catch (MalformedURLException e) {
            quitEarly("Can not create url for scope file: '" + scopeFile + "'");
          }
        }
        options.scopeFiles = ImmutableSet.copyOf(set);
      }

      // last argument is always the file or directory path
      if (options.baseDir != null && !args[index].startsWith(File.separator)) {
        options.resource = new File(options.baseDir, args[index]).getAbsoluteFile();
      } else {
        options.resource = new File(args[index]).getAbsoluteFile();
      }

      options.singleFile = !options.resource.isDirectory();

      // validate options
      if (!options.resource.exists()) {
        quitEarly("File or Directory does not exists: " + options.resource.getAbsolutePath());
      }

      if (options.recurse && !options.resource.isDirectory()) {
        quitEarly("When using -r the last parameter needs to be a directory");
      }

      return options;
    }

    private static void quitEarly(String errorMsg) {
      if (errorMsg != null) {
        System.err.println("Error: " + errorMsg);
      }

      printUsage();
      System.exit(-1);
    }

    private static boolean validateArgs(String[] args) {
      return args.length > 0 && args.length < 9;
    }
  }

  private interface ArgumentConsumer {
    /**
     *Returns true if the next argument has been consumed.
     */
    boolean consume(Options option, String nextArg);
  }
}
