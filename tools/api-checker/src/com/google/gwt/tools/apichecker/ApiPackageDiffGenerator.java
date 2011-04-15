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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * encapsulates a class that produces the diff between the api of two packages.
 */
final class ApiPackageDiffGenerator implements Comparable<ApiPackageDiffGenerator> {
  private final ApiDiffGenerator apiDiffGenerator;
  private Map<String, ApiClassDiffGenerator> intersectingClasses =
      new HashMap<String, ApiClassDiffGenerator>();
  private Set<String> missingClassNames = null;
  private final String name;
  private final ApiPackage newPackage;
  private final ApiPackage oldPackage;

  ApiPackageDiffGenerator(String packageName, ApiDiffGenerator apiDiffGenerator)
      throws NotFoundException {
    this.apiDiffGenerator = apiDiffGenerator;
    name = packageName;
    newPackage = apiDiffGenerator.getNewApiContainer().getApiPackage(packageName);
    oldPackage = apiDiffGenerator.getOldApiContainer().getApiPackage(packageName);
    if (newPackage == null || oldPackage == null) {
      throw new NotFoundException("for package " + packageName
          + ", one of the package objects is null");
    }
  }

  public int compareTo(ApiPackageDiffGenerator other) {
    return this.getName().compareTo(other.getName());
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ApiPackageDiffGenerator)) {
      return false;
    }
    return this.getName().equals(((ApiPackageDiffGenerator) o).getName());
  }

  @Override
  public int hashCode() {
    return this.getName().hashCode();
  }

  void computeApiDiff() throws NotFoundException {
    Set<String> newClassNames = newPackage.getApiClassNames();
    missingClassNames = oldPackage.getApiClassNames();
    Set<String> intersection =
        ApiDiffGenerator.removeIntersection(newClassNames, missingClassNames);

    /* Inspect each of the classes in each of the packages in the intersection */
    for (String className : intersection) {
      ApiClassDiffGenerator tempClassDiffGenerator = new ApiClassDiffGenerator(className, this);
      intersectingClasses.put(className, tempClassDiffGenerator);
      tempClassDiffGenerator.computeApiDiff();
    }
  }

  ApiClassDiffGenerator findApiClassDiffGenerator(String key) {
    return intersectingClasses.get(key);
  }

  Collection<ApiChange> getApiDiff() {
    Collection<ApiChange> collection = new ArrayList<ApiChange>();
    Collection<ApiClass> missingClasses = oldPackage.getApiClassesBySet(missingClassNames);
    for (ApiClass missingClass : missingClasses) {
      collection.add(new ApiChange(missingClass, ApiChange.Status.MISSING));
    }
    List<ApiClassDiffGenerator> intersectingClassesList =
        new ArrayList<ApiClassDiffGenerator>(intersectingClasses.values());
    Collections.sort(intersectingClassesList);
    for (ApiClassDiffGenerator intersectingClass : intersectingClasses.values()) {
      collection.addAll(intersectingClass.getApiDiff());
    }
    return collection;
  }

  ApiDiffGenerator getApiDiffGenerator() {
    return apiDiffGenerator;
  }

  String getName() {
    return name;
  }

  ApiPackage getNewApiPackage() {
    return newPackage;
  }

  ApiPackage getOldApiPackage() {
    return oldPackage;
  }

}
