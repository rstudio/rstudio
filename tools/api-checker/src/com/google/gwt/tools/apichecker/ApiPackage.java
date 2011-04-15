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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An immutable class that encapsulates an API package.
 */
final class ApiPackage implements Comparable<ApiPackage>, ApiElement {

  private Map<String, ApiClass> apiClasses = new HashMap<String, ApiClass>();
  private final ApiContainer apiContainer;
  private final TreeLogger logger;
  private final String name;
  private final JPackage packageObject;

  ApiPackage(JPackage obj, ApiContainer container) {
    packageObject = obj;
    this.apiContainer = container;
    logger = container.getLogger();
    name = obj.getName();
    initialize();
  }

  public int compareTo(ApiPackage other) {
    return this.getName().compareTo(other.getName());
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ApiPackage)) {
      return false;
    }
    return this.getName().equals(((ApiPackage) o).getName());
  }

  public String getRelativeSignature() {
    return name;
  }

  @Override
  public int hashCode() {
    return this.getName().hashCode();
  }

  @Override
  public String toString() {
    return name;
  }

  List<JClassType> getAllClasses() {
    List<JClassType> allClasses =
        new ArrayList<JClassType>(Arrays.asList(packageObject.getTypes()));
    logger.log(TreeLogger.SPAM, "API " + packageObject + " has " + allClasses.size()
        + " outer classes", null);
    int index = 0;
    while (index < allClasses.size()) {
      JClassType classObject = allClasses.get(index);
      allClasses.addAll(Arrays.asList(classObject.getNestedTypes()));
      index++;
    }
    logger.log(TreeLogger.SPAM, "API " + packageObject + " has " + allClasses.size()
        + " total classes", null);
    return allClasses;
  }

  String getApiAsString() {
    StringBuffer sb = new StringBuffer();
    sb.append(name + ", size = " + apiClasses.size() + "\n");
    ArrayList<ApiClass> apiClassesList = new ArrayList<ApiClass>(apiClasses.values());
    Collections.sort(apiClassesList);
    for (ApiClass apiClass : apiClassesList) {
      sb.append(apiClass.getApiAsString());
    }
    return sb.toString();
  }

  ApiClass getApiClass(String className) {
    return apiClasses.get(className);
  }

  Set<ApiClass> getApiClassesBySet(Set<String> classNames) {
    Set<ApiClass> set = new HashSet<ApiClass>();
    for (String className : classNames) {
      set.add(getApiClass(className));
    }
    return set;
  }

  Set<String> getApiClassNames() {
    return new HashSet<String>(apiClasses.keySet());
  }

  ApiContainer getApiContainer() {
    return apiContainer;
  }

  String getName() {
    return name;
  }

  private void initialize() {
    ArrayList<String> notAddedClassNames = new ArrayList<String>();
    for (JClassType classType : getAllClasses()) {
      if (apiContainer.isApiClass(classType)) {
        ApiClass apiClass = new ApiClass(classType, this);
        apiClasses.put(classType.getQualifiedSourceName(), apiClass);
      } else {
        notAddedClassNames.add(classType.toString());
      }
    }
    if (notAddedClassNames.size() > 0) {
      logger.log(TreeLogger.SPAM, "API " + apiContainer.getName() + ", package: " + name
          + ", not adding " + notAddedClassNames.size() + " nonApi classes: " + notAddedClassNames,
          null);
    }
  }

}
