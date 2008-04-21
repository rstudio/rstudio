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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JPackage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Encapsulates an API package.
 */
public class ApiPackage {
  /**
   * A package is an API package if it contains at least one API class. Refer
   * http://wiki.eclipse.org/index.php/Evolving_Java-based_APIs This definition
   * boils down to "a package is an API package iff it contains at least one API
   * class that is not enclosed in any other class."
   * 
   * @return return true if and only if the packageObject is an apiPackage
   */
  public static boolean isApiPackage(JPackage packageObject) {
    JClassType classTypes[] = packageObject.getTypes();
    for (JClassType classType : classTypes) {
      if (ApiClass.isPublicOuterClass(classType)) {
        return true;
      }
    }
    return false;
  }

  private HashMap<String, ApiClass> apiClasses = new HashMap<String, ApiClass>();
  private ApiContainer container = null;

  private TreeLogger logger = null;
  private String name = null;

  private JPackage packageObject = null;

  public ApiPackage(JPackage obj, ApiContainer container) {
    packageObject = obj;
    this.container = container;
    if (logger == null) {
      logger = container.getLogger();
    }
    name = obj.getName();
    initialize();
  }

  public ArrayList<JClassType> getAllClasses() {
    ArrayList<JClassType> allClasses = new ArrayList<JClassType>(
        Arrays.asList(packageObject.getTypes()));
    logger.log(TreeLogger.SPAM, "API " + packageObject + " has "
        + allClasses.size() + " outer classes", null);
    int index = 0;
    while (index < allClasses.size()) {
      JClassType classObject = allClasses.get(index);
      allClasses.addAll(Arrays.asList(classObject.getNestedTypes()));
      index++;
    }
    logger.log(TreeLogger.SPAM, "API " + packageObject + " has "
        + allClasses.size() + " total classes", null);
    return allClasses;
  }

  public ApiClass getApiClass(String className) {
    return apiClasses.get(className);
  }

  public HashSet<String> getApiClassNames() {
    return new HashSet<String>(apiClasses.keySet());
  }

  public ApiContainer getApiContainer() {
    return container;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  private void initialize() {
    Iterator<JClassType> allClassesIterator = getAllClasses().iterator();
    ArrayList<String> notAddedClassNames = new ArrayList<String>();
    while (allClassesIterator.hasNext()) {
      JClassType classType = allClassesIterator.next();
      if (ApiClass.isApiClass(classType)) {
        ApiClass apiClass = new ApiClass(classType, this);
        apiClasses.put(classType.getQualifiedSourceName(), apiClass);
      } else {
        notAddedClassNames.add(classType.toString());
      }
    }
    if (notAddedClassNames.size() > 0) {
      logger.log(TreeLogger.SPAM, "API " + name + ", package: " + name
          + ", not adding " + notAddedClassNames.size() + " nonApi classes: "
          + notAddedClassNames, null);
    }
  }

}
