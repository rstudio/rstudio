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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.SymbolData;
import com.google.gwt.dev.PermutationResult;
import com.google.gwt.dev.util.FileBackedObject;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The standard implementation of {@link CompilationResult}.
 */
public class StandardCompilationResult extends CompilationResult {

  private static final class MapComparator implements
      Comparator<SortedMap<SelectionProperty, String>>, Serializable {
    public int compare(SortedMap<SelectionProperty, String> arg0,
        SortedMap<SelectionProperty, String> arg1) {
      int diff = arg0.size() - arg1.size();
      if (diff != 0) {
        return diff;
      }

      Iterator<String> i0 = arg0.values().iterator();
      Iterator<String> i1 = arg1.values().iterator();

      StringBuffer sb0 = new StringBuffer();
      StringBuffer sb1 = new StringBuffer();

      while (i0.hasNext()) {
        assert i1.hasNext();
        sb0.append(i0.next());
        sb1.append(i1.next());
      }
      assert !i1.hasNext();

      return sb0.toString().compareTo(sb1.toString());
    }
  }

  /**
   * Smaller maps come before larger maps, then we compare the concatenation of
   * every value.
   */
  public static final Comparator<SortedMap<SelectionProperty, String>> MAP_COMPARATOR = new MapComparator();

  private transient SoftReference<String[]> js;

  private final FileBackedObject<PermutationResult> resultFile;

  private transient SoftReference<SortedMap<SymbolData, String>> symbolMap;

  private final SortedSet<SortedMap<SelectionProperty, String>> propertyValues = new TreeSet<SortedMap<SelectionProperty, String>>(
      MAP_COMPARATOR);
  private final String strongName;

  public StandardCompilationResult(String[] js, String strongName,
      FileBackedObject<PermutationResult> resultFile) {
    super(StandardLinkerContext.class);
    this.js = new SoftReference<String[]>(js);
    this.strongName = strongName;
    this.resultFile = resultFile;
  }

  /**
   * Record a particular permutation of SelectionProperty values that resulted
   * in the compilation.
   */
  public void addSelectionPermutation(Map<SelectionProperty, String> values) {
    SortedMap<SelectionProperty, String> map = new TreeMap<SelectionProperty, String>(
        StandardLinkerContext.SELECTION_PROPERTY_COMPARATOR);
    map.putAll(values);
    propertyValues.add(Collections.unmodifiableSortedMap(map));
  }

  @Override
  public String[] getJavaScript() {
    String[] toReturn = null;
    if (js != null) {
      toReturn = js.get();
    }

    if (toReturn == null) {
      PermutationResult result = loadPermutationResult();
      toReturn = result.getJs();
      js = new SoftReference<String[]>(toReturn);
    }

    return toReturn;
  }

  @Override
  public SortedSet<SortedMap<SelectionProperty, String>> getPropertyMap() {
    return Collections.unmodifiableSortedSet(propertyValues);
  }

  @Override
  public String getStrongName() {
    return strongName;
  }

  @Override
  public SortedMap<SymbolData, String> getSymbolMap() {
    SortedMap<SymbolData, String> toReturn = null;
    if (symbolMap != null) {
      toReturn = symbolMap.get();
    }

    if (toReturn == null) {
      PermutationResult result = loadPermutationResult();
      toReturn = result.getSymbolMap();
      symbolMap = new SoftReference<SortedMap<SymbolData, String>>(toReturn);
    }

    return toReturn;
  }

  private PermutationResult loadPermutationResult() {
    try {
      return resultFile.newInstance(TreeLogger.NULL);
    } catch (UnableToCompleteException e) {
      throw new RuntimeException(
          "Unexpectedly unable to read PermutationResult");
    }
  }
}
