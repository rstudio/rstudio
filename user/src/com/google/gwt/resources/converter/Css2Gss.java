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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.resources.css.GenerateCssAst;
import com.google.gwt.resources.css.ast.CssStylesheet;
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
import java.util.Map;

/**
 * Converter from Css to Gss.
 */
public class Css2Gss {
  private final URL cssFile;
  private final TreeLogger treeLogger;
  private final boolean lenient;

  private PrintWriter printWriter;
  private Map<String, String> defNameMapping;

  public Css2Gss(String filePath) throws MalformedURLException {
    this(new File(filePath).toURI().toURL(), false);
  }

  public Css2Gss(URL fileUrl, TreeLogger treeLogger) {
    this(fileUrl, treeLogger, false);
  }

  public Css2Gss(URL fileUrl, TreeLogger treeLogger, boolean lenient) {
    cssFile = fileUrl;
    this.treeLogger = treeLogger;
    this.lenient = lenient;
  }

  public Css2Gss(URL resource, boolean lenient) {
    cssFile = resource;
    printWriter = new PrintWriter(System.err);
    this.treeLogger = new PrintWriterTreeLogger(printWriter);
    this.lenient = lenient;
  }

  public String toGss() throws UnableToCompleteException {
      try {
        CssStylesheet sheet = GenerateCssAst.exec(treeLogger, cssFile);

        DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(lenient, treeLogger);
        defCollectorVisitor.accept(sheet);
        defNameMapping = defCollectorVisitor.getDefMapping();

        new UndefinedConstantVisitor(new HashSet<String>(defNameMapping.values()),
            lenient, treeLogger).accept(sheet);

        new ElseNodeCreator().accept(sheet);

        new AlternateAnnotationCreatorVisitor().accept(sheet);

        GssGenerationVisitor gssGenerationVisitor = new GssGenerationVisitor(
            new DefaultTextOutput(false), defNameMapping, lenient, treeLogger);
        gssGenerationVisitor.accept(sheet);

        return gssGenerationVisitor.getContent();
      } finally {
        if (printWriter != null) {
          printWriter.flush();
        }
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
        System.out.println(convertFile(options.resource));
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
        String gss = convertFile(cssFile);
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

  private static String convertFile(File resource) throws MalformedURLException,
      UnableToCompleteException {
    return new Css2Gss(resource.toURI().toURL(), false).toGss();
  }

  private static void printUsage() {
    System.err.println("Usage :");
    System.err.println("java " + Css2Gss.class.getName() + " [Options] [file or directory]");
    System.err.println("Options:");
    System.err.println("  -r -> Recursively convert all css files on the given directory"
        + "(leaves .css files in place)");
  }

  private static class Options {

    boolean recurse;
    boolean singleFile;
    File resource;

    static Options parseOrQuit(String[] args) {

      Options options = new Options();

      if (args.length == 1) {
        options.recurse = false;
        options.resource = new File(args[0]);
        verifyResourceExists(options.resource);
        options.singleFile = !options.resource.isDirectory();
        return options;
      }

      if (args.length != 2) {
        printUsage();
        System.exit(-1);
      }

      if (!args[0].trim().equals("-r")) {
        printUsage();
        System.exit(-1);
      }

      String fileName = args[1];
      options.recurse = true;
      options.resource = new File(fileName);
      verifyResourceExists(options.resource);
      if (!options.resource.isDirectory()) {
        System.err.println("When using -r second parameter needs to be a directory");
        System.exit(-1);
      }
      return options;
    }

    private static void verifyResourceExists(File f) {
      if (!f.exists()) {
        System.err.println("File or Directory does not exists: " + f.getAbsolutePath());
        System.exit(-1);
      }
    }
  }
}
