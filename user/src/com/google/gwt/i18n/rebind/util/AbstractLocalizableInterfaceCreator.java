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
package com.google.gwt.i18n.rebind.util;

import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.i18n.tools.I18NSync;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import org.apache.tapestry.util.text.LocalizedProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Abstract base functionality for <code>MessagesInterfaceCreator</code> and
 * <code>ConstantsInterfaceCreator</code>.
 */
public abstract class AbstractLocalizableInterfaceCreator {
  private static class RenameDuplicates extends ResourceKeyFormatter {
    private Set methodNames = new HashSet();

    public String format(String key) {
      if (methodNames.contains(key)) {
        key += "_dup";
        return format(key);
      } else {
        methodNames.add(key);
        return key;
      }
    }
  }

  private static class ReplaceBadChars extends ResourceKeyFormatter {
    public String format(String key) {
      return DEFAULT_CHARS.matcher(key).replaceAll("_");
    }
  }

  private abstract static class ResourceKeyFormatter {
    public abstract String format(String key);
  }

  private static Pattern DEFAULT_CHARS = Pattern.compile("[.-]");

  /**
   * Composer for the current Constant.
   */
  protected SourceWriter composer;

  private List formatters = new ArrayList();

  private File resourceFile;

  private File sourceFile;

  private PrintWriter writer;

  /**
   * Creates a new constants creator.
   * 
   * @param className constant class to create
   * @param packageName package to create it in
   * @param resourceBundle resource bundle with value
   * @param targetLocation
   * @throws IOException
   */
  public AbstractLocalizableInterfaceCreator(String className,
      String packageName, File resourceBundle, File targetLocation,
      Class interfaceClass) throws IOException {
    setup(packageName, className, resourceBundle, targetLocation,
        interfaceClass);
  }

  /**
   * Generate class.
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  public void generate() throws FileNotFoundException, IOException {
    try {
      try {
        // right now, only property files are legal
        generateFromPropertiesFile();
      } finally {
        writer.close();
      }
    } catch (IOException e) {
      sourceFile.delete();
      throw e;
    } catch (RuntimeException e) {
      sourceFile.delete();
      throw e;
    }
  }

  /**
   * Create a String method declaration from a Dictionary/value pair.
   * 
   * @param key Dictionary
   * @param defaultValue default value
   */
  public void genSimpleMethodDecl(String key, String defaultValue) {
    genMethodDecl("String", defaultValue, key, key);
  }

  /**
   * Create method args based upon the default value.
   * 
   * @param defaultValue
   */
  protected abstract void genMethodArgs(String defaultValue);

  /**
   * Returns the javaDocComment for the class.
   * 
   * @param path path of class
   * @return java doc comment
   */
  protected abstract String javaDocComment(String path);

  void generateFromPropertiesFile() throws IOException {
    InputStream propStream = new FileInputStream(resourceFile);
    LocalizedProperties p = new LocalizedProperties();
    p.load(propStream, Util.DEFAULT_ENCODING);
    addFormatters();
    Iterator elements = p.getPropertyMap().entrySet().iterator();
    if (elements.hasNext() == false) {
      throw new IllegalStateException(
          "File '"
              + resourceFile
              + "' cannot be used to generate message classes, as it has no key/value pairs defined.");
    }
    while (elements.hasNext()) {
      Entry s = (Entry) elements.next();
      genSimpleMethodDecl((String) s.getKey(), (String) s.getValue());
    }
    composer.commit(new PrintWriterTreeLogger());
  }

  private void addFormatters() {
    // For now, we completely control property key formatters.
    formatters.add(new ReplaceBadChars());

    // Rename Duplicates must always come last.
    formatters.add(new RenameDuplicates());
  }

  private String formatKey(String key) {
    for (int i = 0; i < formatters.size(); i++) {
      ResourceKeyFormatter formatter = (ResourceKeyFormatter) formatters.get(i);
      key = formatter.format(key);
    }
    if (Util.isValidJavaIdent(key) == false) {
      System.err.println("Warning: " + key
          + " is not a legitimate method name. " + sourceFile
          + " will have compiler errors");
    }
    return key;
  }

  private void genMethodDecl(String type, String defaultValue, String key,
      String typeArg) {
    composer.beginJavaDocComment();
    composer.println("Translated \"" + defaultValue + "\".\n");
    composer.println("@return translated \"" + defaultValue + "\"");
    composer.print(I18NSync.ID + typeArg);
    composer.endJavaDocComment();
    key = formatKey(key);
    composer.print(type + " " + key);
    composer.print("(");
    genMethodArgs(defaultValue);
    composer.print(");\n");
  }

  private void setup(String packageName, String className, File resourceBundle,
      File targetLocation, Class interfaceClass) throws IOException {
    ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
        packageName, className);
    factory.makeInterface();
    factory.setJavaDocCommentForClass(javaDocComment(resourceBundle.getCanonicalPath().replace(
        File.separatorChar, '/')));
    factory.addImplementedInterface(interfaceClass.getName());
    FileOutputStream file = new FileOutputStream(targetLocation);
    Writer underlying = new OutputStreamWriter(file, Util.DEFAULT_ENCODING);
    writer = new PrintWriter(underlying);
    composer = factory.createSourceWriter(writer);
    resourceFile = resourceBundle;
    sourceFile = targetLocation;
  }
}
