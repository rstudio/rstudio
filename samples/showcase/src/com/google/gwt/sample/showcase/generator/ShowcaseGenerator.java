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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A generator that parses the example files used in the
 * {@link com.google.gwt.sample.showcase.client.Showcase} and generates data
 * files that contain the example source code and example style definitions.
 */
public class ShowcaseGenerator {
  /**
   * The names of the CSS files to parse.
   */
  private static final String[] CSS_FILES = {"GWT-default.css", "Showcase.css"};

  /**
   * The root of all files.
   */
  private static final String FILE_ROOT = "../../../samples/showcase/src/";

  /**
   * The destination folder for raw files.
   */
  private static final String DST_RAW = FILE_ROOT
      + "com/google/gwt/sample/showcase/public/raw/";

  /**
   * The destination folder for parsed source code.
   */
  private static final String DST_SOURCE = FILE_ROOT
      + "com/google/gwt/sample/showcase/public/source/";

  /**
   * The destination folder for parse style code.
   */
  private static final String DST_STYLE = FILE_ROOT
      + "com/google/gwt/sample/showcase/public/style/";

  /**
   * The root of content widget files.
   */
  private static final String SRC_CONTENT = FILE_ROOT
      + "com/google/gwt/sample/showcase/client/content/";

  /**
   * The path to the folder containing all CSS style sheets.
   */
  private static final String SRC_CSS = FILE_ROOT
      + "com/google/gwt/sample/showcase/public/";

  /**
   * The root of properties files.
   */
  private static final String SRC_PROP = FILE_ROOT
      + "com/google/gwt/sample/showcase/client/";

  /**
   * The tag used to denote required CSS files.
   */
  private static final String TAG_CSS = "@gwt.CSS";

  /**
   * The tag used to denote data to include in the source.
   */
  private static final String TAG_DATA = "@gwt.DATA";

  /**
   * The tag used to denote a file to include as raw code.
   */
  private static final String TAG_RAW = "@gwt.RAW";

  /**
   * The tag used to denote methods to include in the source.
   */
  private static final String TAG_SRC = "@gwt.SRC";

  /**
   * The contents of the CSS file.
   */
  private static String cssFileContents = null;

  /**
   * Update all properties files so that the names of in the translated files
   * have the same case as the names in the default properties file. This is
   * required do to a problem in translation that converts all names to lower
   * case.
   */
  public static void generatePropertiesFiles() {
    // Get a full list of properties from the default file
    List<String> propNames = new ArrayList<String>();
    String filename = SRC_PROP + "ShowcaseConstants.properties";
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(filename));
      String temp;
      while ((temp = br.readLine()) != null) {
        int end = temp.indexOf('=');
        if (end > 0) {
          String propName = temp.substring(0, end - 1);
          propNames.add(propName.trim());
        }
      }
    } catch (FileNotFoundException e) {
      System.out.println("Cannot find file: " + filename);
    } catch (IOException e) {
      System.out.println("Cannot read file: " + filename);
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
        }
      }
    }

    // Get all properties files
    File defaultFile = new File(filename);
    File root = new File(SRC_PROP);
    File[] files = root.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (!files[i].equals(defaultFile) && isFileType(files[i], "properties")) {
        String fileContents = getFileContents(files[i]);
        for (String propName : propNames) {
          fileContents = fileContents.replaceAll("(?i)" + propName, propName);
        }
        setFileContents(files[i], fileContents);
      }
    }
  }

  /**
   * Generate all raw files and put them into a safe directory.
   */
  public static void generateRawFiles() {
    // Remove all existing raw files
    clearFolder(DST_RAW);

    // Generate new raw files
    generateRawFiles(new File(SRC_CONTENT));
  }

  /**
   * Generate all source files and put them into a safe directory.
   */
  public static void generateSourceFiles() {
    // Remove all existing source files
    clearFolder(DST_SOURCE);

    // Generate new source files
    generateSourceFiles(new File(SRC_CONTENT));
  }

  public static void generateStyleFiles() {
    // Remove all existing style files
    clearFolder(DST_STYLE);

    // Combine the contents of all CSS files into one string
    cssFileContents = "";
    for (int i = 0; i < CSS_FILES.length; i++) {
      File cssFile = new File(SRC_CSS + CSS_FILES[i]);
      cssFileContents += getFileContents(cssFile) + "\n\n";
    }

    // Generate new style files
    generateStyleFiles(new File(SRC_CONTENT));
  }

  public static void main(String[] args) {
    // Generate required source files
    System.out.print("Generating source files...");
    generateSourceFiles();
    System.out.println("done");

    // Generate required source files
    System.out.print("Generating raw files...");
    generateRawFiles();
    System.out.println("done");

    // Generate required style files
    System.out.print("Generating style files...");
    generateStyleFiles();
    System.out.println("done");

    // Generate required properties files
    System.out.print("Generating properties files...");
    generatePropertiesFiles();
    System.out.println("done");
  }

  /**
   * Clear the contents of a folder.
   * 
   * @param path the path to the folder
   */
  private static void clearFolder(String path) {
    File root = new File(path);
    File[] files = root.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (files[i].isFile()) {
        files[i].delete();
      }
    }
  }

  /**
   * Recursively iterate the files in a directory and generate the source code
   * for each.
   */
  private static void generateRawFiles(File root) {
    if (root.isDirectory()) {
      // Recurse the directory
      File[] subFiles = root.listFiles();
      for (int i = 0; i < subFiles.length; i++) {
        if (!subFiles[i].getName().equals(".svn")) {
          generateRawFiles(subFiles[i]);
        }
      }
    } else if (root.isFile()) {
      String fileContents = getFileContents(root);
      int rawTagIndex = fileContents.indexOf(TAG_RAW);
      if (fileContents == "" || rawTagIndex < 0) {
        return;
      }

      // Remove line with gwt.RAW
      int startLine = fileContents.lastIndexOf("\n", rawTagIndex);
      int endLine = fileContents.indexOf("\n", rawTagIndex);
      fileContents = fileContents.substring(0, startLine + 1)
          + fileContents.substring(endLine + 1);

      // Make the source pretty
      fileContents = fileContents.replace("<", "&lt;");
      fileContents = fileContents.replace(">", "&gt;");
      fileContents = fileContents.replace("* \n   */\n", "*/\n");
      fileContents = "<pre>" + fileContents + "</pre>";

      // Save the source code to a file
      String saveFile = root.getPath().substring(FILE_ROOT.length()) + ".html";
      saveFile = saveFile.replace('/', '.');
      saveFile = DST_RAW + saveFile;
      setFileContents(saveFile, fileContents);
    }
  }

  /**
   * Recursively iterate the files in a directory and generate the source code
   * for each.
   */
  private static void generateSourceFiles(File root) {
    if (root.isDirectory()) {
      // Recurse the directory
      File[] subFiles = root.listFiles();
      for (int i = 0; i < subFiles.length; i++) {
        generateSourceFiles(subFiles[i]);
      }
    } else if (root.isFile()) {
      // Get the file contents
      if (!isFileType(root, "java")) {
        return;
      }
      String fileContents = getFileContents(root);
      if (fileContents == "" || fileContents.indexOf(TAG_RAW) >= 0) {
        return;
      }

      // Get each data code block
      String formattedSource = "";
      int dataTagIndex = fileContents.indexOf(TAG_DATA);
      int srcTagIndex = fileContents.indexOf(TAG_SRC);
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
          dataTagIndex = fileContents.indexOf(TAG_DATA, endIndex + 1);
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
          srcTagIndex = fileContents.indexOf(TAG_SRC, endIndex + 1);
        }
      }

      // Make the source pretty
      formattedSource = formattedSource.replace("<", "&lt;");
      formattedSource = formattedSource.replace(">", "&gt;");
      formattedSource = formattedSource.replace("* \n   */\n", "*/\n");
      formattedSource = "<pre>" + formattedSource + "</pre>";

      // Save the source code to a file
      String saveFile = root.getPath().substring(FILE_ROOT.length());
      saveFile = saveFile.replaceAll(".java", ".html");
      saveFile = saveFile.replace('/', '.');
      saveFile = DST_SOURCE + saveFile;
      setFileContents(saveFile, formattedSource);
    }
  }

  /**
   * Recursively iterate the files in a directory and generate the style code
   * for each.
   */
  private static void generateStyleFiles(File root) {
    if (root.isDirectory()) {
      // Recurse the directory
      File[] subFiles = root.listFiles();
      for (int i = 0; i < subFiles.length; i++) {
        generateStyleFiles(subFiles[i]);
      }
    } else if (root.isFile()) {
      // Get the file contents
      if (!isFileType(root, "java")) {
        return;
      }
      String fileContents = getFileContents(root);
      if (fileContents == "") {
        return;
      }

      // Get the class names from the file
      List<String> styleNames = new ArrayList<String>();
      int tagIndex = fileContents.indexOf(TAG_CSS);
      while (tagIndex >= 0) {
        // Get the style name
        int beginIndex = fileContents.indexOf(" ", tagIndex);
        int endIndex = fileContents.indexOf("\n", tagIndex);
        String styleName = fileContents.substring(beginIndex, endIndex).trim();
        styleNames.add(styleName);

        // Get next tag
        tagIndex = fileContents.indexOf(TAG_CSS, tagIndex + 1);
      }

      // Iterate through the style names
      Map<String, String> styleDefs = new LinkedHashMap<String, String>();
      for (String styleName : styleNames) {
        // Get the start location of the style code in the source file
        boolean foundStyle = false;
        int start = cssFileContents.indexOf("\n" + styleName);
        while (start >= 0) {
          // Get the matched string name pattern
          int end = cssFileContents.indexOf("{", start);
          String matchedName = cssFileContents.substring(start, end).trim();

          // Get the style code
          end = cssFileContents.indexOf("}", start) + 1;
          String styleDef = "<pre>" + cssFileContents.substring(start, end)
              + "</pre>";
          styleDefs.put(matchedName, styleDef);

          // Goto the next match
          foundStyle = true;
          start = cssFileContents.indexOf("\n" + styleName, end);
        }

        // No style exists
        if (!foundStyle) {
          styleDefs.put(styleName, "<pre>" + styleName + " {\n}</pre>");
        }
      }

      // Combine all of the styles
      String formattedStyle = "";
      for (String styleDef : styleDefs.values()) {
        formattedStyle += styleDef;
      }

      // Save the source code to a file
      String saveFile = root.getPath().substring(FILE_ROOT.length());
      saveFile = saveFile.replaceAll(".java", ".html");
      saveFile = saveFile.replace('/', '.');
      saveFile = DST_STYLE + saveFile;
      setFileContents(saveFile, formattedStyle);
    }
  }

  /**
   * Get the full contents of a file.
   * 
   * @param file the file
   * @return the contents of the file
   */
  private static String getFileContents(File file) {
    String filename = file.getPath();
    StringBuffer fileContentsBuf = new StringBuffer();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(filename));
      String temp;
      while ((temp = br.readLine()) != null) {
        fileContentsBuf.append(temp).append('\n');
      }
    } catch (FileNotFoundException e) {
      System.out.println("Cannot find file: " + filename);
    } catch (IOException e) {
      System.out.println("Cannot read file: " + filename);
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
   * Verifies that the {@link File} is of the correct type.
   * 
   * @param file the {@link File} to check
   * @param filetype the file type, without the .
   * @return true if file type is correct
   */
  private static boolean isFileType(File file, String filetype) {
    String ext = "." + filetype;
    String path = file.getPath();
    return path.substring(path.length() - ext.length()).equals(ext);
  }

  /**
   * Set the full contents of a file.
   * 
   * @param file the {@link File}
   * @param contents the file contents
   */
  private static void setFileContents(File file, String contents) {
    setFileContents(file.getPath(), contents);
  }

  /**
   * Set the full contents of a file.
   * 
   * @param filename the filename
   * @param contents the file contents
   */
  private static void setFileContents(String filename, String contents) {
    BufferedWriter out = null;
    try {
      FileWriter fstream = new FileWriter(filename);
      out = new BufferedWriter(fstream);
      out.write(contents);
    } catch (IOException e) {
      System.out.println("Cannot save file: " + filename);
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
        }
      }
    }
  }
}
