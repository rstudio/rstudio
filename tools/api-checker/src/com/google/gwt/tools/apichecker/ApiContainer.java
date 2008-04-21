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

package com.google.gwt.tools.apichecker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jdt.CacheManager;
import com.google.gwt.dev.jdt.TypeOracleBuilder;
import com.google.gwt.dev.jdt.URLCompilationUnitProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

/**
 * Encapsulates an API.
 * 
 */
public class ApiContainer {
  private HashMap<String, ApiPackage> apiPackages = new HashMap<String, ApiPackage>();
  private HashMap<String, String> excludedFiles = null;
  private TreeLogger logger = null;
  private String name = null;
  private int numFilesCount = 0;
  private Collection<File> sourceTrees = null;
  private TypeOracle typeOracle = null;

  public ApiContainer(String fileName, String suffix, TreeLogger logger)
      throws IllegalArgumentException, MalformedURLException,
      FileNotFoundException, IOException, NotFoundException,
      UnableToCompleteException {
    this.logger = logger;
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    FileInputStream fis = new FileInputStream(fileName);
    Properties config = new Properties();
    config.load(fis);
    String apiName = config.getProperty("name" + suffix);
    String allSourceFiles = config.getProperty("sourceFiles" + suffix);
    String allExcludedFiles = config.getProperty("excludedFiles" + suffix);

    if (allExcludedFiles == null) {
      allExcludedFiles = "";
    }
    if (apiName == null || allSourceFiles == null) {
      throw new IllegalArgumentException(
          "in apiContainer constructor, either name (" + apiName
              + ") or sourceFiles (" + allSourceFiles + ") is null");
    }
    logger.log(TreeLogger.DEBUG, "read from config file " + fileName
        + ", name = " + apiName + ", allSourceFiles = " + allSourceFiles
        + ", allExcludedFiles = " + allExcludedFiles, null);

    String sourceFilesArray[] = allSourceFiles.split(":");
    Collection<File> fileCollection = new Vector<File>();
    for (String tempStr : sourceFilesArray) {
      tempStr = tempStr.trim();
      fileCollection.add(new File(tempStr));
    }
    this.sourceTrees = fileCollection;
    if (allExcludedFiles.equals("")) {
      this.excludedFiles = generateCanonicalHashmap(new String[0]);
    } else {
      String excludedFilesArray[] = allExcludedFiles.split(":");
      this.excludedFiles = generateCanonicalHashmap(excludedFilesArray);
    }
    this.name = apiName;
    createTypeOracleFromSources();
    initializeApiPackages();
  }

  // constructor is used while testing
  ApiContainer(String name, TreeLogger logger, TypeOracle typeOracle) {
    this.name = name;
    this.logger = logger;
    this.typeOracle = typeOracle;
    initializeApiPackages();
  }

  public ApiPackage getApiPackage(String packageName) {
    return apiPackages.get(packageName);
  }

  public HashSet<String> getApiPackageNames() {
    return new HashSet<String>(apiPackages.keySet());
  }

  public TreeLogger getLogger() {
    return logger;
  }
 
  private void addCompilationUnitsInPath(TypeOracleBuilder builder,
      File sourcePathEntry) throws NotFoundException, IOException,
      UnableToCompleteException {
    File[] files = sourcePathEntry.listFiles();
    if (files == null) {
      // No files found.
      return;
    }

    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      // Ignore files like .svn and .cvs
      if (file.getName().startsWith(".") || file.getName().equals("CVS")) {
        continue;
      }
      if (isExcludedFile(file.getCanonicalPath())) {
        // do not process the subtree
        logger.log(TreeLogger.DEBUG, "not traversing "
            + file.toURL().toString(), null);
        continue;
      }
      if (file.isFile()) {
        String pkgName = null;
        if (file.getName().endsWith("java")) {
          pkgName = extractPackageNameFromFile(file);
          logger.log(TreeLogger.DEBUG, "pkgName = " + pkgName + ", file = "
              + file.toString(), null);
        }
        if (isValidPackage(pkgName, sourcePathEntry.toURL().toString())) {
          // Add if it's a source file and the package and fileNames are okay
          URL location = file.toURL();
          CompilationUnitProvider cup = new URLCompilationUnitProvider(
              location, pkgName);
          logger.log(TreeLogger.DEBUG, "+ to CompilationUnit" + ", location="
              + location + ", pkgName=" + pkgName, null);
          builder.addCompilationUnit(cup);
          numFilesCount++;
        } else {
          logger.log(TreeLogger.SPAM, " not adding file " + file.toURL(), null);
        }
      } else {
        // Recurse into subDirs
        addCompilationUnitsInPath(builder, file);
      }
    }
  }

  private void createTypeOracleFromSources() throws NotFoundException,
      IOException, UnableToCompleteException {

    numFilesCount = 0;
    TypeOracleBuilder builder = new TypeOracleBuilder(new CacheManager(null,
        null, ApiCompatibilityChecker.DISABLE_CHECKS));
    for (Iterator<File> i = sourceTrees.iterator(); i.hasNext();) {
      addCompilationUnitsInPath(builder, i.next());
    }
    typeOracle = builder.build(logger);
    logger.log(TreeLogger.INFO, "API " + name
        + ", Finished with building typeOracle, added " + numFilesCount
        + " files", null);
  }

  private String extractPackageNameFromFile(File file) {
    if (!file.exists()) {
      return null;
    }
    String fileName = null;
    try {
      fileName = file.toURL().toString();
      FileReader fr = new FileReader(file);
      BufferedReader br = new BufferedReader(fr);
      String str = null;
      while ((str = br.readLine()) != null) {
        if (str.indexOf("package") != 0) {
          continue;
        }
        String parts[] = str.split("[\b\t\n\r ]+");
        if ((parts.length == 2) && parts[0].equals("package")) {
          return parts[1].substring(0, parts[1].length() - 1); // the ; char
        }
      }
      return null;
    } catch (Exception ex) {
      logger.log(TreeLogger.ERROR,
          "error in parsing and obtaining the packageName from file "
              + fileName + "error's message " + ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Convert a set into a HashMap for faster lookups.
   */
  private HashMap<String, String> generateCanonicalHashmap(String strArray[])
      throws IOException {
    HashMap<String, String> tempMap = new HashMap<String, String>();
    if (strArray == null) {
      return tempMap;
    }
    for (String str : strArray) {
      str = str.trim();
      File tempFile = new File(str);
      str = tempFile.getCanonicalPath();
      tempMap.put(str, str);
    }
    return tempMap;
  }

  /**
   * Purge non API packages.
   */
  private void initializeApiPackages() {
    HashSet<JPackage> allPackages = new HashSet<JPackage>(
        Arrays.asList(typeOracle.getPackages()));
    Iterator<JPackage> packagesIterator = allPackages.iterator();
    HashSet<String> packagesNotAdded = new HashSet<String>();
    while (packagesIterator.hasNext()) {
      JPackage packageObject = packagesIterator.next();
      if (ApiPackage.isApiPackage(packageObject)) {
        ApiPackage apiPackageObj = new ApiPackage(packageObject, this);
        apiPackages.put(apiPackageObj.getName(), apiPackageObj);
      } else {
        packagesNotAdded.add(packageObject.toString());
      }
    }
    if (packagesNotAdded.size() > 0) {
      logger.log(TreeLogger.DEBUG, "API " + name + ": not added "
          + packagesNotAdded.size() + " packages: " + packagesNotAdded, null);
    }
    if (apiPackages.size() > 0) {
      logger.log(TreeLogger.INFO, "API " + name + apiPackages.size()
          + " Api packages: " + apiPackages.keySet(), null);
    }
  }

  private boolean isExcludedFile(String fileName) {
    String pattern = "file:";
    if (fileName.indexOf(pattern) == 0) {
      fileName = fileName.substring(pattern.length());
    }
    return (excludedFiles.get(fileName) != null);
  }

  private boolean isValidPackage(String packageName, String filePath) {
    logger.log(TreeLogger.SPAM, "packageName = " + packageName
        + ", filePath = " + filePath, null);
    if (packageName == null) {
      return false;
    }
    int lastSlashPosition = filePath.lastIndexOf("/");
    if (lastSlashPosition == -1) {
      return false;
    }
    String dirPath = filePath.substring(0, lastSlashPosition);
    String packageNameAsPath = packageName.replace('.', '/');
    logger.log(TreeLogger.SPAM, "packageNameAsPath " + packageNameAsPath
        + ", dirPath = " + dirPath, null);
    return dirPath.endsWith(packageNameAsPath);
  }
}
