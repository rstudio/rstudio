/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.ext.soyc.coderef;

import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * It contains all packages and they reference to classes {@link ClassDescriptor}.
 *
 */
public class PackageDescriptor {

  public static final String DEFAULT_PKG = "<default>";

  /**
   * Creates a package descriptor from a code model (mapping between class names and class
   * descriptors).
   */
  public static PackageDescriptor from(Map<String, ClassDescriptor> codeModel) {
    PackageDescriptor topPackage = new PackageDescriptor(DEFAULT_PKG, "");
    for (ClassDescriptor classDescriptor : codeModel.values()) {
      PackageDescriptor packageDescriptor = topPackage;
      if (!classDescriptor.getPackageName().equals(topPackage.longName)) {
        for (String packageName : classDescriptor.getPackageName().split("\\.")) {
          packageDescriptor = packageDescriptor.createOrGetChildPackage(packageName);
        }
      }
      packageDescriptor.addClass(classDescriptor);
    }
    topPackage.collapseEmptyPackages();
    return topPackage;
  }

  private ArrayList<PackageDescriptor> packages = Lists.newArrayList();
  private ArrayList<ClassDescriptor> classes = Lists.newArrayList();
  private String name = DEFAULT_PKG;
  private String longName = "";

  public PackageDescriptor(String packageName, String longPackageName) {
    name = packageName;
    longName = longPackageName;
  }

  /**
   * Condenses packages with single subpackages and no classes, eg. com {google {gwt {..}}}  ->
   * com.google.gwt {..}
   */
  private void collapseEmptyPackages() {
    if (packages.size() == 1 && classes.size() == 0) {
      PackageDescriptor childPackage = packages.get(0);
      childPackage.collapseEmptyPackages();
      name = (name.equals(DEFAULT_PKG) ? "" : name + ".")  + childPackage.name;
      longName = childPackage.longName;
      packages = childPackage.packages;
      classes = childPackage.classes;
      return;
    }
    for (PackageDescriptor pkg : packages) {
      pkg.collapseEmptyPackages();
    }
  }

  private PackageDescriptor createOrGetChildPackage(String packageName) {
    // get child if any
    for (PackageDescriptor childPackage : packages) {
      if (childPackage.getName().equals(packageName)) {
        return childPackage;
      }
    }
    // create child
    PackageDescriptor childPackage = new PackageDescriptor(packageName,
        longName.length() > 0 ? longName + "." + packageName : packageName);
    this.addPackage(childPackage);
    return childPackage;
  }

  /**
   * Returns all classes in this package and its subpackages in a form of mapping between qualified
   * class name and class descriptor.
   */
  public Map<String, ClassDescriptor> getAllClassesByName() {
    Map<String, ClassDescriptor> map = Maps.newTreeMap();
    this.populateClassesByName(map);
    return map;
  }

  private void populateClassesByName(Map<String, ClassDescriptor> classesByName) {
    for (ClassDescriptor classDescriptor : this.getClasses()) {
      classesByName.put(classDescriptor.getFullName(), classDescriptor);
    }
    for (PackageDescriptor subPackage : this.getPackages()) {
      subPackage.populateClassesByName(classesByName);
    }
  }

  public void addClass(ClassDescriptor cls) {
    this.classes.add(cls);
  }

  public void addPackage(PackageDescriptor pkg) {
    this.packages.add(pkg);
  }

  /**
   * Returns the list of classes in this packages without including subpackages.
   */
  public Collection<ClassDescriptor> getClasses() {
    return classes;
  }

  public String getName() {
    return name;
  }

  /**
   * Returns the list of subpackages.
   */
  public Collection<PackageDescriptor> getPackages() {
    return packages;
  }
}
