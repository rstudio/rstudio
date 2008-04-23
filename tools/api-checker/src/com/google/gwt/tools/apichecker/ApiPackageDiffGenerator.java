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

import com.google.gwt.core.ext.typeinfo.NotFoundException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * encapsulates a class that produces the diff between the api of two packages.
 */
public class ApiPackageDiffGenerator {
  ApiDiffGenerator apiDiffGenerator = null;
  HashMap<String, ApiClassDiffGenerator> intersectingClasses = new HashMap<String, ApiClassDiffGenerator>();
  HashSet<String> missingClassNames = null;
  String name = null;
  ApiPackage newPackage = null;
  ApiPackage oldPackage = null;

  public ApiPackageDiffGenerator(String packageName,
      ApiDiffGenerator apiDiffGenerator) throws NotFoundException {
    this.apiDiffGenerator = apiDiffGenerator;
    name = packageName;
    newPackage = apiDiffGenerator.getNewApiContainer().getApiPackage(
        packageName);
    oldPackage = apiDiffGenerator.getOldApiContainer().getApiPackage(
        packageName);
    if (newPackage == null || oldPackage == null) {
      throw new NotFoundException("for package " + packageName
          + ", one of the package objects is null");
    }
  }

  public void cleanApiDiff() {
    Iterator<ApiClassDiffGenerator> tempIterator = intersectingClasses.values().iterator();
    while (tempIterator.hasNext()) {
      tempIterator.next().cleanApiDiff();
    }
  }

  public void computeApiDiff() throws NotFoundException {
    HashSet<String> newClassNames = newPackage.getApiClassNames();
    missingClassNames = oldPackage.getApiClassNames();
    HashSet<String> intersection = ApiDiffGenerator.extractCommonElements(
        newClassNames, missingClassNames);

    /* Inspect each of the classes in each of the packages in the intersection */
    Iterator<String> tempIterator = intersection.iterator();
    while (tempIterator.hasNext()) {
      String className = tempIterator.next();
      ApiClassDiffGenerator temp = new ApiClassDiffGenerator(className, this);
      intersectingClasses.put(className, temp);
      temp.computeApiDiff();
    }
  }

  public ApiClassDiffGenerator findApiClassDiffGenerator(String key) {
    return intersectingClasses.get(key);
  }

  public ApiDiffGenerator getApiDiffGenerator() {
    return apiDiffGenerator;
  }

  public ApiPackage getNewApiPackage() {
    return newPackage;
  }

  public ApiPackage getOldApiPackage() {
    return oldPackage;
  }

  public String printApiDiff() {
    int totalSize = missingClassNames.size() + intersectingClasses.size();
    if (totalSize == 0) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    Iterator<String> missingClassesIterator = missingClassNames.iterator();
    while (missingClassesIterator.hasNext()) {
      sb.append("\t\t" + missingClassesIterator.next()
          + ApiDiffGenerator.DELIMITER + ApiChange.Status.MISSING + "\n");
    }
    Iterator<ApiClassDiffGenerator> tempIterator = intersectingClasses.values().iterator();
    while (tempIterator.hasNext()) {
      sb.append(tempIterator.next().printApiDiff());
    }
    if (sb.length() == 0) {
      return "";
    }
    return "\tpackage " + name + "\n" + sb.toString() + "\n";
  }

}
