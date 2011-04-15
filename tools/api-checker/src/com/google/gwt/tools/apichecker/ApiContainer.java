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
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompilationUnitBuilder;
import com.google.gwt.dev.javac.JdtCompiler;
import com.google.gwt.dev.javac.TypeOracleMediatorFromSource;
import com.google.gwt.dev.resource.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link ApiContainer} Encapsulates an API.
 * 
 */
public final class ApiContainer {

  private final Map<JClassType, Boolean> apiClassCache = new HashMap<JClassType, Boolean>();
  private final Map<String, ApiPackage> apiPackages = new HashMap<String, ApiPackage>();

  private final Set<String> excludedPackages;
  private final TreeLogger logger;
  private final String name;

  private final TypeOracle typeOracle;

  /**
   * A public constructor used for programmatic invocation and testing.
   * 
   * @param name Api name
   * @param resources a set of Resources
   * @param excludedPackages a set of excludedPackages
   * @param logger TreeLogger for logging messages
   * @throws IllegalArgumentException if one of the arguments is illegal
   * @throws UnableToCompleteException if there is a TypeOracle exception
   */
  ApiContainer(String name, Set<Resource> resources, Set<String> excludedPackages, TreeLogger logger)
      throws UnableToCompleteException {
    this.name = name;
    this.logger = logger;
    logger.log(TreeLogger.INFO, "name = " + name + ", builders.size = " + resources.size(), null);
    this.typeOracle = createTypeOracle(resources);
    this.excludedPackages = excludedPackages;
    initializeApiPackages();
  }

  /**
   * Get all the API members as String.
   * 
   * @return the string value
   */
  public String getApiAsString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Api: " + name + ", size = " + apiPackages.size() + "\n\n");
    List<ApiPackage> sortedApiPackages = new ArrayList<ApiPackage>(apiPackages.values());
    Collections.sort(sortedApiPackages);
    for (ApiPackage apiPackage : sortedApiPackages) {
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

  private TypeOracle createTypeOracle(Set<Resource> resources) throws UnableToCompleteException {
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    for (Resource resource : resources) {
      CompilationUnitBuilder builder = CompilationUnitBuilder.create(resource);
      builders.add(builder);
    }
    List<CompilationUnit> units = JdtCompiler.compile(builders);
    boolean anyError = false;
    TreeLogger branch = logger.branch(TreeLogger.TRACE, "Checking for compile errors");
    for (CompilationUnit unit : units) {
      CompilationProblemReporter.reportErrors(branch, unit, false);
      anyError |= unit.isError();
    }
    if (anyError) {
      logger.log(TreeLogger.ERROR, "Unable to build typeOracle for " + getName());
      throw new UnableToCompleteException();
    }

    TypeOracleMediatorFromSource mediator = new TypeOracleMediatorFromSource();
    mediator.addNewUnits(logger, units);
    logger.log(TreeLogger.INFO, "API " + name + ", Finished with building typeOracle, added "
        + units.size() + " files", null);
    return mediator.getTypeOracle();
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
    Set<JPackage> allPackages = new HashSet<JPackage>(Arrays.asList(typeOracle.getPackages()));
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
      logger.log(TreeLogger.DEBUG, "API " + name + ": not added " + packagesNotAdded.size()
          + " packages: " + packagesNotAdded, null);
    }
    if (apiPackages.size() > 0) {
      logger.log(TreeLogger.INFO, "API " + name + " " + apiPackages.size() + " Api packages: "
          + apiPackages.keySet(), null);
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
    if (excludedPackages.contains(packageObject.getName())) {
      return false;
    }
    JClassType classTypes[] = packageObject.getTypes();
    for (JClassType classType : classTypes) {
      if (isPublicOuterClass(classType)) {
        return true;
      }
    }
    return false;
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

}
