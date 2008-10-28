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
package com.google.gwt.i18n.rebind;

import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.i18n.client.Localizable;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract base functionality for <code>MessagesInterfaceCreator</code> and
 * <code>ConstantsInterfaceCreator</code>.
 */
public abstract class AbstractLocalizableInterfaceCreator {
  private static class RenameDuplicates extends ResourceKeyFormatter {
    private Set<String> methodNames = new HashSet<String>();

    @Override
    public String format(String key) {
      while (methodNames.contains(key)) {
        key += "_dup";
      }
      methodNames.add(key);
      return key;
    }
  }

  private static class ReplaceBadChars extends ResourceKeyFormatter {
    @Override
    public String format(String key) {
      StringBuilder buf = new StringBuilder();
      int keyLen = key == null ? 0 : key.length();
      for (int i = 0; i < keyLen; i = key.offsetByCodePoints(i, 1)) {
        int codePoint = key.codePointAt(i);
        if (i == 0 ? Character.isJavaIdentifierStart(codePoint)
            : Character.isJavaIdentifierPart(codePoint)) {
          buf.appendCodePoint(codePoint);
        } else {
          buf.append('_');
        }
      }
      if (buf.length() == 0) {
        buf.append('_');
      }
      return buf.toString();
    }
  }

  private abstract static class ResourceKeyFormatter {
    public abstract String format(String key);
  }

  /**
   * Index into this array using a nibble, 4 bits, to get the corresponding
   * hexadecimal character representation.
   */
  private static final char NIBBLE_TO_HEX_CHAR[] = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
      'E', 'F'};

  private static boolean needsUnicodeEscape(char ch) {
    if (ch == ' ') {
      return false;
    }
    switch (Character.getType(ch)) {
      case Character.COMBINING_SPACING_MARK:
      case Character.ENCLOSING_MARK:
      case Character.NON_SPACING_MARK:
      case Character.UNASSIGNED:
      case Character.PRIVATE_USE:
      case Character.SPACE_SEPARATOR:
      case Character.CONTROL:
      case Character.LINE_SEPARATOR:
      case Character.FORMAT:
      case Character.PARAGRAPH_SEPARATOR:
      case Character.SURROGATE:
        return true;

      default:
        break;
    }
    return false;
  }

  private static void unicodeEscape(char ch, StringBuilder buf) {
    buf.append('\\');
    buf.append('u');
    buf.append(NIBBLE_TO_HEX_CHAR[(ch >> 12) & 0x0F]);
    buf.append(NIBBLE_TO_HEX_CHAR[(ch >> 8) & 0x0F]);
    buf.append(NIBBLE_TO_HEX_CHAR[(ch >> 4) & 0x0F]);
    buf.append(NIBBLE_TO_HEX_CHAR[ch & 0x0F]);
  }

  /**
   * Composer for the current Constant.
   */
  protected SourceWriter composer;

  private List<ResourceKeyFormatter> formatters = new ArrayList<ResourceKeyFormatter>();

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
      Class<? extends Localizable> interfaceClass) throws IOException {
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
    genMethodDecl("String", defaultValue, key);
  }

  /**
   * Create method args based upon the default value.
   * 
   * @param defaultValue
   */
  protected abstract void genMethodArgs(String defaultValue);

  /**
   * Create an annotation to hold the default value.
   */
  protected abstract void genValueAnnotation(String defaultValue);

  /**
   * Returns the javaDocComment for the class.
   * 
   * @param path path of class
   * @return java doc comment
   */
  protected abstract String javaDocComment(String path);

  protected String makeJavaString(String value) {
    StringBuilder buf = new StringBuilder();
    buf.append('\"');
    for (int i = 0; i < value.length(); ++i) {
      char c = value.charAt(i);
      switch (c) {
        case '\r':
          buf.append("\\r");
          break;
        case '\n':
          buf.append("\\n");
          break;
        case '\"':
          buf.append("\\\"");
          break;
        default:
          if (needsUnicodeEscape(c)) {
            unicodeEscape(c, buf);
          } else {
            buf.append(c);
          }
          break;
      }
    }
    buf.append('\"');
    return buf.toString();
  }

  @SuppressWarnings("unchecked")
  // use of raw type from LocalizedProperties
  void generateFromPropertiesFile() throws IOException {
    InputStream propStream = new FileInputStream(resourceFile);
    LocalizedProperties p = new LocalizedProperties();
    p.load(propStream, Util.DEFAULT_ENCODING);
    addFormatters();
    // TODO: Look for a generic version of Tapestry's LocalizedProperties class
    Set<String> keySet = p.getPropertyMap().keySet();
    // sort keys for deterministic results
    String[] keys = keySet.toArray(new String[keySet.size()]);
    Arrays.sort(keys);
    if (keys.length == 0) {
      throw new IllegalStateException(
          "File '"
              + resourceFile
              + "' cannot be used to generate message classes, as it has no key/value pairs defined.");
    }
    for (String key : keys) {
      String value = p.getProperty(key);
      genSimpleMethodDecl(key, value);
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
    for (ResourceKeyFormatter formatter : formatters) {
      key = formatter.format(key);
    }
    return key;
  }

  private void genMethodDecl(String type, String defaultValue, String key) {
    composer.beginJavaDocComment();
    String escaped = makeJavaString(defaultValue);
    composer.println("Translated " + escaped + ".\n");
    composer.print("@return translated " + escaped);
    composer.endJavaDocComment();
    genValueAnnotation(defaultValue);
    composer.println("@Key(" + makeJavaString(key) + ")");
    String methodName = formatKey(key);
    composer.print(type + " " + methodName);
    composer.print("(");
    genMethodArgs(defaultValue);
    composer.print(");\n");
  }

  private void setup(String packageName, String className, File resourceBundle,
      File targetLocation, Class<? extends Localizable> interfaceClass)
      throws IOException {
    ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
        packageName, className);
    factory.makeInterface();
    // TODO(jat): need a way to add an @GeneratedFrom annotation.
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
