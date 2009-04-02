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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.javac.JavaSourceFile;
import com.google.gwt.dev.javac.JavaSourceOracle;

import junit.framework.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A simple {@link ResourceOracle} for testing.
 */
public class MockJavaSourceOracle implements JavaSourceOracle {

  private Map<String, JavaSourceFile> exportedMap = Collections.emptyMap();
  private Set<JavaSourceFile> exportedValues = Collections.emptySet();

  public MockJavaSourceOracle(JavaSourceFile... sourceFiles) {
    add(sourceFiles);
  }

  public void clear() {
  }

  public Set<String> getClassNames() {
    return exportedMap.keySet();
  }

  public Set<JavaSourceFile> getSourceFiles() {
    return exportedValues;
  }

  public Map<String, JavaSourceFile> getSourceMap() {
    return exportedMap;
  }

  void add(JavaSourceFile... sourceFiles) {
    Map<String, JavaSourceFile> newMap = new HashMap<String, JavaSourceFile>(
        exportedMap);
    for (JavaSourceFile sourceFile : sourceFiles) {
      String className = sourceFile.getTypeName();
      Assert.assertFalse(newMap.containsKey(className));
      newMap.put(className, sourceFile);
    }
    export(newMap);
  }

  void remove(String... classNames) {
    Map<String, JavaSourceFile> newMap = new HashMap<String, JavaSourceFile>(
        exportedMap);
    for (String className : classNames) {
      JavaSourceFile oldValue = newMap.remove(className);
      Assert.assertNotNull(oldValue);
    }
    export(newMap);
  }

  void replace(JavaSourceFile... sourceFiles) {
    Map<String, JavaSourceFile> newMap = new HashMap<String, JavaSourceFile>(
        exportedMap);
    for (JavaSourceFile sourceFile : sourceFiles) {
      String className = sourceFile.getTypeName();
      Assert.assertTrue(newMap.containsKey(className));
      newMap.put(className, sourceFile);
    }
    export(newMap);
  }

  private void export(Map<String, JavaSourceFile> newMap) {
    exportedMap = Collections.unmodifiableMap(newMap);
    // Make a new hash set for constant lookup.
    exportedValues = Collections.unmodifiableSet(new HashSet<JavaSourceFile>(
        exportedMap.values()));
  }

}