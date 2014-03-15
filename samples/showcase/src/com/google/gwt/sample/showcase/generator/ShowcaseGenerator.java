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
package com.google.gwt.sample.showcase.generator;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.Showcase;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseRaw;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.sample.showcase.client.ShowcaseConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Generate the source code, css styles, and raw source used in the Showcase
 * examples.
 */
public class ShowcaseGenerator extends Generator {
  /**
   * The paths to the CSS style sheets used in Showcase. The paths are relative
   * to the root path of the {@link ClassLoader}. The variable $THEME will be
   * replaced by the current theme. The variable $RTL will be replaced by "_rtl"
   * for RTL locales.
   */
  private static final String[] SRC_CSS = {
      "com/google/gwt/user/theme/$THEME/public/gwt/$THEME/$THEME$RTL.css",
      "com/google/gwt/sample/showcase/client/Showcase.css"};

  /**
   * The class loader used to get resources.
   */
  private ClassLoader classLoader = null;

  /**
   * The generator context.
   */
  private GeneratorContext context = null;

  /**
   * The {@link TreeLogger} used to log messages.
   */
  private TreeLogger logger = null;

  /**
   * The set of raw files that have already been generated. Raw files can be
   * reused by different examples, but we only generate them once.
   */
  private Set<String> rawFiles = new HashSet<String>();

  @Override
  public String generate(
      TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    this.logger = logger;
    this.context = context;
    this.classLoader = Thread.currentThread().getContextClassLoader();

    // Only generate files on the first permutation
    if (!isFirstPass()) {
      return null;
    }

    // Get the Showcase ContentWidget subtypes to examine
    JClassType cwType = null;
    try {
      cwType = context.getTypeOracle().getType(ContentWidget.class.getName());
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "Cannot find ContentWidget class", e);
      throw new UnableToCompleteException();
    }
    JClassType[] types = cwType.getSubtypes();

    // Generate the source and raw source files
    for (JClassType type : types) {
      generateRawFiles(type);
      generateSourceFiles(type);
    }

    // Generate the CSS source files
    String[] themes = new String[]{Showcase.THEME};
    for (String theme : themes) {
      String styleDefsLTR = getStyleDefinitions(theme, false);
      String styleDefsRTL = getStyleDefinitions(theme, true);
      String outDirLTR = ShowcaseConstants.DST_SOURCE_STYLE + theme + "/";
      String outDirRTL = ShowcaseConstants.DST_SOURCE_STYLE + theme + "_rtl/";
      for (JClassType type : types) {
        generateStyleFiles(type, styleDefsLTR, outDirLTR);
        generateStyleFiles(type, styleDefsRTL, outDirRTL);
      }
    }

    return null;
  }

  /**
   * Set the full contents of a resource in the public directory.
   *
   * @param partialPath the path to the file relative to the public directory
   * @param contents the file contents
   */
  private void createPublicResource(String partialPath, String contents)
      throws UnableToCompleteException {
    try {
      OutputStream outStream = context.tryCreateResource(logger, partialPath);
      if (outStream == null) {
        String message = "Attempting to generate duplicate public resource: "
            + partialPath
            + ".\nAll generated source files must have unique names.";
        logger.log(TreeLogger.ERROR, message);
        throw new UnableToCompleteException();
      }
      outStream.write(contents.getBytes());
      context.commitResource(logger, outStream);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Error writing file: " + partialPath, e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Generate the raw files used by a {@link ContentWidget}.
   *
   * @param type the {@link ContentWidget} subclass
   */
  private void generateRawFiles(JClassType type)
      throws UnableToCompleteException {
    // Look for annotation
    if (!type.isAnnotationPresent(ShowcaseRaw.class)) {
      return;
    }

    // Get the package info
    String pkgName = type.getPackage().getName();
    String pkgPath = pkgName.replace('.', '/') + "/";

    // Generate each raw source file
    String[] filenames = type.getAnnotation(ShowcaseRaw.class).value();
    for (String filename : filenames) {
      // Check if the file has already been generated.
      String path = pkgName + filename;
      if (rawFiles.contains(path)) {
        continue;
      }
      rawFiles.add(path);

      // Get the file contents
      String fileContents = getResourceContents(pkgPath + filename);

      // Make the source pretty
      fileContents = fileContents.replace("<", "&lt;");
      fileContents = fileContents.replace(">", "&gt;");
      fileContents = fileContents.replace("* \n   */\n", "*/\n");
      fileContents = "<pre>" + fileContents + "</pre>";

      // Save the raw source in the public directory
      String dstPath = ShowcaseConstants.DST_SOURCE_RAW + filename + ".html";
      createPublicResource(dstPath, fileContents);
    }
  }

  /**
   * Generate the formatted source code for a {@link ContentWidget}.
   *
   * @param type the {@link ContentWidget} subclass
   */
  private void generateSourceFiles(JClassType type)
      throws UnableToCompleteException {
    // Get the file contents
    String filename = type.getQualifiedSourceName().replace('.', '/') + ".java";
    String fileContents = getResourceContents(filename);

    // Get each data code block
    String formattedSource = "";
    String dataTag = "@" + ShowcaseData.class.getSimpleName();
    String sourceTag = "@" + ShowcaseSource.class.getSimpleName();
    int dataTagIndex = fileContents.indexOf(dataTag);
    int srcTagIndex = fileContents.indexOf(sourceTag);
    while (dataTagIndex >= 0 || srcTagIndex >= 0) {
      if (dataTagIndex >= 0
          && (dataTagIndex < srcTagIndex || srcTagIndex < 0)) {
        // Get the boundaries of a DATA tag
        int beginIndex = fileContents.lastIndexOf("  /*", dataTagIndex);
        int beginTagIndex = fileContents.lastIndexOf("\n", dataTagIndex) + 1;
        int endTagIndex = fileContents.indexOf("\n", dataTagIndex) + 1;
        int endIndex = fileContents.indexOf(";", beginIndex) + 1;

        // Add to the formatted source
        String srcData = fileContents.substring(beginIndex, beginTagIndex)
            + fileContents.substring(endTagIndex, endIndex);
        formattedSource += srcData + "\n\n";

        // Get next tag
        dataTagIndex = fileContents.indexOf(dataTag, endIndex + 1);
      } else {
        // Get the boundaries of a SRC tag
        int beginIndex = fileContents.lastIndexOf("/*", srcTagIndex) - 2;
        int beginTagIndex = fileContents.lastIndexOf("\n", srcTagIndex) + 1;
        int endTagIndex = fileContents.indexOf("\n", srcTagIndex) + 1;
        int endIndex = fileContents.indexOf("\n  }", beginIndex) + 4;

        // Add to the formatted source
        String srcCode = fileContents.substring(beginIndex, beginTagIndex)
            + fileContents.substring(endTagIndex, endIndex);
        formattedSource += srcCode + "\n\n";

        // Get the next tag
        srcTagIndex = fileContents.indexOf(sourceTag, endIndex + 1);
      }
    }

    // Make the source pretty
    formattedSource = formattedSource.replace("<", "&lt;");
    formattedSource = formattedSource.replace(">", "&gt;");
    formattedSource = formattedSource.replace("* \n   */\n", "*/\n");
    formattedSource = "<pre>" + formattedSource + "</pre>";

    // Save the source code to a file
    String dstPath = ShowcaseConstants.DST_SOURCE_EXAMPLE
        + type.getSimpleSourceName() + ".html";
    createPublicResource(dstPath, formattedSource);
  }

  /**
   * Generate the styles used by a {@link ContentWidget}.
   *
   * @param type the {@link ContentWidget} subclass
   * @param styleDefs the concatenated style definitions
   * @param outDir the output directory
   */
  private void generateStyleFiles(
      JClassType type, String styleDefs, String outDir)
      throws UnableToCompleteException {
    // Look for annotation
    if (!type.isAnnotationPresent(ShowcaseStyle.class)) {
      return;
    }

    // Generate a style file for each theme/RTL mode pair
    String[] prefixes = type.getAnnotation(ShowcaseStyle.class).value();
    Map<String, String> matched = new LinkedHashMap<String, String>();
    for (String prefix : prefixes) {
      // Get the start location of the style code in the source file
      boolean foundStyle = false;
      int start = styleDefs.indexOf("\n" + prefix);
      while (start >= 0) {
        // Get the cssContents string name pattern
        int end = styleDefs.indexOf("{", start);
        String matchedName = styleDefs.substring(start, end).trim();

        // Get the style code
        end = styleDefs.indexOf("}", start) + 1;
        String styleDef = "<pre>" + styleDefs.substring(start, end) + "</pre>";
        matched.put(matchedName, styleDef);

        // Goto the next match
        foundStyle = true;
        start = styleDefs.indexOf("\n" + prefix, end);
      }

      // No style exists
      if (!foundStyle) {
        matched.put(prefix, "<pre>" + prefix + " {\n}</pre>");
      }
    }

    // Combine all of the styles into one formatted string
    String formattedStyle = "";
    for (String styleDef : matched.values()) {
      formattedStyle += styleDef;
    }

    // Save the raw source in the public directory
    String dstPath = outDir + type.getSimpleSourceName() + ".html";
    createPublicResource(dstPath, formattedStyle);
  }

  /**
   * Get the full contents of a resource.
   *
   * @param path the path to the resource
   * @return the contents of the resource
   */
  private String getResourceContents(String path)
      throws UnableToCompleteException {
    InputStream in = classLoader.getResourceAsStream(path);
    if (in == null) {
      logger.log(TreeLogger.ERROR, "Resource not found: " + path);
      throw new UnableToCompleteException();
    }

    StringBuffer fileContentsBuf = new StringBuffer();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(in));
      String temp;
      while ((temp = br.readLine()) != null) {
        fileContentsBuf.append(temp).append('\n');
      }
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Cannot read resource", e);
      throw new UnableToCompleteException();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
        }
      }
    }

    // Return the file contents as a string
    return fileContentsBuf.toString();
  }

  /**
   * Load the contents of all CSS files used in the Showcase for a given
   * theme/RTL mode, concatenated into one string.
   *
   * @param theme the style theme
   * @param isRTL true if RTL mode
   * @return the concatenated styles
   */
  private String getStyleDefinitions(String theme, boolean isRTL)
      throws UnableToCompleteException {
    String cssContents = "";
    for (String path : SRC_CSS) {
      path = path.replace("$THEME", theme);
      if (isRTL) {
        path = path.replace("$RTL", "_rtl");
      } else {
        path = path.replace("$RTL", "");
      }
      cssContents += getResourceContents(path) + "\n\n";
    }
    return cssContents;
  }

  /**
   * Ensure that we only generate files once by creating a placeholder file,
   * then looking for it on subsequent generates.
   *
   * @return true if this is the first pass, false if not
   */
  private boolean isFirstPass() {
    String placeholder = ShowcaseConstants.DST_SOURCE + "generated";
    try {
      OutputStream outStream = context.tryCreateResource(logger, placeholder);
      if (outStream == null) {
        return false;
      } else {
        context.commitResource(logger, outStream);
      }
    } catch (UnableToCompleteException e) {
      logger.log(TreeLogger.ERROR, "Unable to generate", e);
      return false;
    }
    return true;
  }
}
