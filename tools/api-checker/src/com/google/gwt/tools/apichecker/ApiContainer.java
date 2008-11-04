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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.JdtCompiler;
import com.google.gwt.dev.javac.TypeOracleMediator;
import com.google.gwt.dev.javac.impl.FileCompilationUnit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

/**
 * {@link ApiContainer} Encapsulates an API.
 * 
 */
public final class ApiContainer {

  private final Map<JClassType, Boolean> apiClassCache = new HashMap<JClassType, Boolean>();
  private final Map<String, ApiPackage> apiPackages = new HashMap<String, ApiPackage>();

  private final Set<String> excludedFiles = new HashSet<String>();
  private final Set<String> excludedPackages = new HashSet<String>();
  private final TreeLogger logger;

  private final String name;

  private int numFilesCount = 0;
  private Collection<File> sourceTrees = null;
  private final TypeOracle typeOracle;

  /**
   * A public constructor to create an {@code ApiContainer} from a config file.
   * 
   * @param fileName the config file
   * @param suffix The code looks for the values of the properties: dirRoot,
   *          name, sourceFiles, excludedFiles, ending in "suffix"
   * @param logger The logger for the code.
   * @throws IllegalArgumentException if one of the arguments is illegal
   * @throws UnableToCompleteException if there is a TypeOracle exception
   */
  public ApiContainer(String fileName, String suffix, TreeLogger logger)
      throws IllegalArgumentException, UnableToCompleteException {
    this.logger = logger;
    FileInputStream fis = null;
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    try {
      fis = new FileInputStream(fileName);
      Properties config = new Properties();
      config.load(fis);
      String apiName = config.getProperty("name" + suffix);
      String allSourceFiles = config.getProperty("sourceFiles" + suffix);
      String allExcludedFiles = config.getProperty("excludedFiles" + suffix);
      if (allExcludedFiles == null) {
        allExcludedFiles = "";
      }
      String allExcludedPackages = config.getProperty("excludedPackages");
      if (allExcludedPackages == null) {
        allExcludedPackages = "";
      }

      if (apiName == null || allSourceFiles == null) {
        throw new IllegalArgumentException(
            "in apiContainer constructor, either name (" + apiName
                + ") or sourceFiles (" + allSourceFiles + ") is null");
      }
      logger.log(TreeLogger.DEBUG, "read from config file " + fileName
          + ", name = " + apiName + ", allSourceFiles = " + allSourceFiles
          + ", allExcludedFiles = " + allExcludedFiles
          + ", allExcludedPackages = " + allExcludedPackages, null);

      String dirRoot = config.getProperty("dirRoot" + suffix);
      if (dirRoot == null) {
        dirRoot = "";
      }
      String sourceFilesArray[] = allSourceFiles.split(":");
      Collection<File> fileCollection = new Vector<File>();
      for (String tempStr : sourceFilesArray) {
        tempStr = tempStr.trim();
        checkFileExistence("source file: ", dirRoot + tempStr);
        fileCollection.add(new File(dirRoot + tempStr));
      }
      logger.log(TreeLogger.DEBUG, "fileCollection " + fileCollection, null);
      this.sourceTrees = fileCollection;

      String excludedFilesArray[] = allExcludedFiles.split(":");
      for (String excludedFile : excludedFilesArray) {
        checkFileExistence("excluded file: ", dirRoot + excludedFile);
      }
      generateCanonicalFileSet(excludedFilesArray, dirRoot, excludedFiles);

      String excludedPackagesArray[] = allExcludedPackages.split(":");
      excludedPackages.addAll(Arrays.asList(excludedPackagesArray));
      this.name = apiName;
      this.typeOracle = createTypeOracleFromSources();
      initializeApiPackages();
    } catch (MalformedURLException e1) {
      throw new IllegalArgumentException(e1);
    } catch (FileNotFoundException e2) {
      if (fis == null) {
        System.err.println("Have you specified the path of the config file correctly?");
      } else {
        System.err.println("Do you have a reference version of the API checked out?");
      }
      throw new IllegalArgumentException(e2);
    } catch (IOException e3) {
      throw new IllegalArgumentException(e3);
    } catch (NotFoundException e4) {
      logger.log(TreeLogger.ERROR, "logged a NotFoundException", e4);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Another public constructor. Used for programmatic invocation and testing.
   * 
   * @param name
   * @param logger
   * @param typeOracle
   */
  ApiContainer(String name, TreeLogger logger, TypeOracle typeOracle) {
    this.name = name;
    this.logger = logger;
    this.typeOracle = typeOracle;
    initializeApiPackages();
  }

  /**
   * Get all the API members as String.
   * 
   * @return the string value
   */
  public String getApiAsString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Api: " + name + "\n\n");
    List<ApiPackage> sortedApiPackages = new ArrayList<ApiPackage>(
        apiPackages.values());
    Collections.sort(sortedApiPackages);
    for (ApiPackage apiPackage : apiPackages.values()) {
      sb.append(apiPackage.getApiAsString());
    }
    return sb.toString();
  }

  ApiPackage getApiPackage(String packageName) {
    return apiPackages.get(packageName);
  }

  HashSet<String> getApiPackageNames() {
    return new HashSet<String>(apiPackages.keySet());
  }

  Set<ApiPackage> getApiPackagesBySet(Set<String> names) {
    Set<ApiPackage> ret = new HashSet<ApiPackage>();
    for (String packageName : names) {
      ret.add(apiPackages.get(packageName));
    }
    return ret;
  }

  TreeLogger getLogger() {
    return logger;
  }

  String getName() {
    return name;
  }
  
  boolean isApiClass(JClassType classType) {
    Boolean ret = apiClassCache.get(classType);
    if (ret != null) {
      return ret.booleanValue();
    }
    // to avoid infinite recursion for isApiClass("BAR", ..) when:
    // class FOO {
    // public class BAR extends FOO {
    // }
    // }
    apiClassCache.put(classType, Boolean.FALSE);
    boolean bool = computeIsApiClass(classType);
    if (bool) {
      apiClassCache.put(classType, Boolean.TRUE);
    } else {
      apiClassCache.put(classType, Boolean.FALSE);
    }
    // container.getLogger().log(TreeLogger.SPAM, "computed isApiClass for " +
    // classType + " as " + bool, null);
    return bool;
  }

  boolean isInstantiableApiClass(JClassType classType) {
    return !classType.isAbstract() && isApiClass(classType)
        && hasPublicOrProtectedConstructor(classType);
  }

  boolean isNotsubclassableApiClass(JClassType classType) {
    return isApiClass(classType) && !isSubclassable(classType);
  }

  boolean isSubclassableApiClass(JClassType classType) {
    return isApiClass(classType) && isSubclassable(classType);
  }

  private void addCompilationUnitsInPath(Set<CompilationUnit> units,
      File sourcePathEntry) throws NotFoundException, IOException,
      UnableToCompleteException {
    logger.log(TreeLogger.SPAM, "entering addCompilationUnitsInPath, file = "
        + sourcePathEntry, null);
    File[] files = sourcePathEntry.listFiles();
    if (files == null) {
      // No files found.
      return;
    }

    for (int i = 0; i < files.length; i++) {
      final File file = files[i];
      logger.log(TreeLogger.SPAM, "deciding the fate of file " + file, null);
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
          if (pkgName == null) {
            logger.log(TreeLogger.WARN, "Not adding file = "
                + file.toString() + ", because packageName = null", null);
          } else {
            logger.log(TreeLogger.DEBUG, "adding pkgName = " + pkgName
                + ", file = " + file.toString(), null);
          }
        }
        if (isValidPackage(pkgName, sourcePathEntry.toURL().toString())) {
          // Add if it's a source file and the package and fileNames are okay
          CompilationUnit unit = new FileCompilationUnit(file, pkgName);
          units.add(unit);
          numFilesCount++;
        } else {
          logger.log(TreeLogger.SPAM, " not adding file " + file.toURL(), null);
        }
      } else {
        // Recurse into subDirs
        addCompilationUnitsInPath(units, file);
      }
    }
  }

  private void checkFileExistence(String tag, String pathName)
      throws FileNotFoundException {
    File tempFile = new File(pathName);
    if (!tempFile.exists()) {
      throw new FileNotFoundException(tag + "file " + pathName + " not found");
    }
  }

  /**
   * Assumption: Clients may subclass an API class, but they will not add their
   * class to the package.
   * 
   * Notes: -- A class with only private constructors can be an API class.
   */
  private boolean computeIsApiClass(JClassType classType) {
    if (excludedPackages.contains(classType.getPackage().getName())) {
      return false;
    }

    // check for outer classes
    if (isPublicOuterClass(classType)) {
      return true;
    }
    // if classType is not a member type, return false
    if (!classType.isMemberType()) {
      return false;
    }
    JClassType enclosingType = classType.getEnclosingType();
    if (classType.isPublic()) {
      return isApiClass(enclosingType) || isAnySubtypeAnApiClass(enclosingType);
    }
    if (classType.isProtected()) {
      return isSubclassableApiClass(enclosingType)
          || isAnySubtypeASubclassableApiClass(enclosingType);
    }
    return false;
  }

  private TypeOracle createTypeOracleFromSources() throws NotFoundException,
      IOException, UnableToCompleteException {
    numFilesCount = 0;
    TypeOracleMediator mediator = new TypeOracleMediator();
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    for (File tempFile : sourceTrees) {
      addCompilationUnitsInPath(units, tempFile);
    }
    JdtCompiler.compile(units);
    mediator.refresh(logger, units);
    logger.log(TreeLogger.INFO, "API " + name
        + ", Finished with building typeOracle, added " + numFilesCount
        + " files", null);
    return mediator.getTypeOracle();
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
  private void generateCanonicalFileSet(String strArray[], String dirRoot,
      Set<String> result) throws IOException {
    if (strArray == null) {
      return;
    }
    for (String str : strArray) {
      str = str.trim();
      File tempFile = new File(dirRoot + str);
      str = tempFile.getCanonicalPath();
      result.add(str);
    }
  }

  private boolean hasPublicOrProtectedConstructor(JClassType classType) {
    JConstructor[] constructors = classType.getConstructors();
    for (JConstructor constructor : constructors) {
      if (constructor.isPublic() || constructor.isProtected()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Purge non API packages.
   */
  private void initializeApiPackages() {
    Set<JPackage> allPackages = new HashSet<JPackage>(
        Arrays.asList(typeOracle.getPackages()));
    Set<String> packagesNotAdded = new HashSet<String>();
    for (JPackage packageObject : allPackages) {
      if (isApiPackage(packageObject)) {
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
      logger.log(TreeLogger.INFO, "API " + name + " " + apiPackages.size()
          + " Api packages: " + apiPackages.keySet(), null);
    }
  }

  private boolean isAnySubtypeAnApiClass(JClassType classType) {
    JClassType subTypes[] = classType.getSubtypes();
    for (JClassType tempType : subTypes) {
      if (isApiClass(tempType)) {
        return true;
      }
    }
    return false;
  }

  private boolean isAnySubtypeASubclassableApiClass(JClassType classType) {
    JClassType subTypes[] = classType.getSubtypes();
    for (JClassType tempType : subTypes) {
      if (isSubclassableApiClass(tempType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * A package is an API package if it contains at least one API class. Refer
   * http://wiki.eclipse.org/index.php/Evolving_Java-based_APIs This definition
   * boils down to "a package is an API package iff it contains at least one API
   * class that is not enclosed in any other class."
   * 
   * @return return true if and only if the packageObject is an apiPackage
   */
  private boolean isApiPackage(JPackage packageObject) {
    JClassType classTypes[] = packageObject.getTypes();
    for (JClassType classType : classTypes) {
      if (isPublicOuterClass(classType)) {
        return true;
      }
    }
    return false;
  }

  private boolean isExcludedFile(String fileName) {
    String pattern = "file:";
    if (fileName.indexOf(pattern) == 0) {
      fileName = fileName.substring(pattern.length());
    }
    return excludedFiles.contains(fileName);
  }

  /**
   * @return returns true if classType is public AND an outer class
   */
  private boolean isPublicOuterClass(JClassType classType) {
    return classType.isPublic() && !classType.isMemberType();
  }

  private boolean isSubclassable(JClassType classType) {
    return !classType.isFinal() && hasPublicOrProtectedConstructor(classType);
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
