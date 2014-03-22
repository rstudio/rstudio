/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.tools.cldr;

import com.google.gwt.codegen.server.AbortablePrintWriter;
import com.google.gwt.codegen.server.JavaSourceWriterBuilder;
import com.google.gwt.codegen.server.LoggingCodeGenContext;
import com.google.gwt.i18n.shared.GwtLocale;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Map;

/**
 * Base class for CLDR processors that generate GWT i18n resources.
 */
public abstract class Processor {

  /**
   * A CodeGenContext implementation that logs to j.u.logging and creates
   * output files using {@link Processor#createOutputFile(String, String)}.
   */
  protected class ProcessorCodeGenContext extends LoggingCodeGenContext {
    @Override
    public JavaSourceWriterBuilder addClass(String superPkg, String pkgName, String className) {
      String pkgPath = superPkg == null ? pkgName : superPkg + '/' + pkgName;
      if (pkgPath.length() > 0) {
        pkgPath = pkgPath.replace('.', '/') + '/';
      }
      String classPath = className.replace('.', '_');
      String fileName = pkgPath + classPath + ".java";
      try {
        PrintWriter pw = createOutputFile("", fileName);
        AbortablePrintWriter apw = new AbortablePrintWriter(pw);
        printHeader(apw);
        return new JavaSourceWriterBuilder(apw, pkgName, className);
      } catch (FileNotFoundException e) {
        error("Unable to create " + fileName, e);
        return null;
      } catch (IOException e) {
        error("Unable to create " + fileName, e);
        return null;
      }
    }
  }

  protected static final String I18N_PACKAGE_PATH = "user/src/com/google/gwt/i18n/";

  protected static <T> String join(String joiner, Iterable<T> objects) {
    StringBuilder buf = new StringBuilder();
    for (Object obj : objects) {
      if (buf.length() > 0) {
        buf.append(joiner);
      }
      buf.append(obj.toString());
    }
    return buf.toString();
  }

  protected static String localeSuffix(GwtLocale locale) {
    return (locale.isDefault() ? "" : "_") + locale.getAsString();
  }

  /**
   * @param value
   * @return value with all quotes escaped
   */
  protected static String quote(String value) {
    return value.replace("\"", "\\\"");
  }

  protected final Factory cldrFactory;

  protected final LocaleData localeData;

  protected final File outputDir;

  private boolean useOverride;

  /**
   * Initialize the shared portion of a Processor.
   * 
   * @param outputDir output directory for created files
   * @param cldrFactory CLDR factory used to create new CLDRFile instances
   * @param localeData LocaleData instance to collect data from CLDR files
   */
  protected Processor(File outputDir, Factory cldrFactory, LocaleData localeData) {
    this.outputDir = outputDir;
    this.cldrFactory = cldrFactory;
    this.localeData = localeData;
    useOverride = true;
  }

  /**
   * Execute this processor.
   * 
   * It will call loadData, cleanupData, writeOutputFiles, and then reset on its
   * localeData instance.
   * 
   * @throws IOException
   */
  public final void run() throws IOException {
    try {
      loadData();
      cleanupData();
      writeOutputFiles();
    } finally {
      localeData.reset();
    }
  }

  /**
   * Override hook for subclasses to implement any cleanup needed, such as
   * removing values which duplicate those from ancestors.
   */
  protected void cleanupData() {
    // do nothing by default
  }

  /**
   * Create an output file including any parent directories.
   * 
   * @param name name of file, which will be prefixed by
   *          user/src/com/google/gwt/i18n/client/impl/cldr
   * @param ext extension for file
   * @param locale locale name or null if not localized
   * @return a PrintWriter instance
   * @throws IOException
   */
  protected PrintWriter createFile(String name, String ext, String locale) throws IOException {
    if (locale == null || locale.length() == 0) {
      locale = "";
    } else {
      locale = "_" + locale;
    }
    return createOutputFile("client/impl/cldr/" + name + locale + "." + ext);
  }

  protected PrintWriter createOutputFile(String suffix) throws IOException, FileNotFoundException {
    return createOutputFile(I18N_PACKAGE_PATH, suffix);
  }

  protected PrintWriter createOutputFile(String prefix, String suffix) throws IOException,
      FileNotFoundException {
    PrintWriter pw;
    File f = new File(outputDir, prefix + suffix);
    File parent = f.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }
    f.createNewFile();
    pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),
        "UTF-8")), false);
    return pw;
  }

  protected void generateIntMethod(PrintWriter pw, String category, GwtLocale locale, String key,
      String method) {
    String value = localeData.getEntry(category, locale, key);
    if (value != null) {
      pw.println();
      if (useOverride) {
        pw.println("  @Override");
      }
      pw.println("  public int " + method + "() {");
      pw.println("    return " + value + ";");
      pw.println("  }");
    }
  }

  protected void generateStringMethod(PrintWriter pw, String category, GwtLocale locale,
      String key, String method) {
    String value = localeData.getEntry(category, locale, key);
    generateStringValue(pw, method, value);
  }

  protected void generateStringValue(PrintWriter pw, String method, String value) {
    if (value != null) {
      pw.println();
      if (useOverride) {
        pw.println("  @Override");
      }
      pw.println("  public String " + method + "() {");
      pw.println("    return \"" + quote(value) + "\";");
      pw.println("  }");
    }
  }

  /**
   * @return true if generated methods should use @Override.
   */
  protected boolean getOverrides() {
    return useOverride;
  }

  /**
   * Load data needed by this processor.
   * 
   * @throws IOException
   */
  protected abstract void loadData() throws IOException;

  protected void printHeader(PrintWriter pw) {
    printJavaHeader(pw);
  }

  protected void printJavaHeader(PrintWriter pw) {
    int year = Calendar.getInstance().get(Calendar.YEAR);
    pw.println("/*");
    pw.println(" * Copyright " + year + " Google Inc.");
    pw.println(" * ");
    pw.println(" * Licensed under the Apache License, Version 2.0 (the "
        + "\"License\"); you may not");
    pw.println(" * use this file except in compliance with the License. You "
        + "may obtain a copy of");
    pw.println(" * the License at");
    pw.println(" * ");
    pw.println(" * http://www.apache.org/licenses/LICENSE-2.0");
    pw.println(" * ");
    pw.println(" * Unless required by applicable law or agreed to in writing, " + "software");
    pw.println(" * distributed under the License is distributed on an \"AS "
        + "IS\" BASIS, WITHOUT");
    pw.println(" * WARRANTIES OR CONDITIONS OF ANY KIND, either express or " + "implied. See the");
    pw.println(" * License for the specific language governing permissions and "
        + "limitations under");
    pw.println(" * the License.");
    pw.println(" */");
  }

  protected void printPropertiesHeader(PrintWriter pw) {
    int year = Calendar.getInstance().get(Calendar.YEAR);
    pw.println("# Copyright " + year + " Google Inc.");
    pw.println("# ");
    pw.println("# Licensed under the Apache License, Version 2.0 (the "
        + "\"License\"); you may not");
    pw.println("# use this file except in compliance with the License. You "
        + "may obtain a copy of");
    pw.println("# the License at");
    pw.println("# ");
    pw.println("# http://www.apache.org/licenses/LICENSE-2.0");
    pw.println("# ");
    pw.println("# Unless required by applicable law or agreed to in writing, " + "software");
    pw.println("# distributed under the License is distributed on an \"AS "
        + "IS\" BASIS, WITHOUT");
    pw.println("# WARRANTIES OR CONDITIONS OF ANY KIND, either express or " + "implied. See the");
    pw.println("# License for the specific language governing permissions and "
        + "limitations under");
    pw.println("# the License.");
  }

  protected void printVersion(PrintWriter pw, GwtLocale locale, String prefix) {
    pw.println(prefix + "DO NOT EDIT - GENERATED FROM CLDR DATA:");
    pw.println(prefix + " cldrVersion=" + CLDRFile.GEN_VERSION);
    Map<String, String> map = localeData.getEntries("version", locale);
    for (Map.Entry<String, String> entry : map.entrySet()) {
      pw.println(prefix + " " + entry.getKey() + "=" + entry.getValue());
    }
    pw.println();
  }

  /**
   * Set whether method definitions should use @Override.
   * 
   * @param useOverride
   */
  protected void setOverrides(boolean useOverride) {
    this.useOverride = useOverride;
  }

  /**
   * Write output files produced by this processor.
   * 
   * @throws IOException
   */
  protected abstract void writeOutputFiles() throws IOException;
}
