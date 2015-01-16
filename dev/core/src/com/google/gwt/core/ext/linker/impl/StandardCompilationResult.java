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

import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.SoftPermutation;
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.core.ext.linker.SymbolData;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.util.DiskCache;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.Lists;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
    @Override
    public int compare(SortedMap<SelectionProperty, String> arg0,
        SortedMap<SelectionProperty, String> arg1) {
      int diff = arg0.size() - arg1.size();
      if (diff != 0) {
        return diff;
      }

      Iterator<String> i0 = arg0.values().iterator();
      Iterator<String> i1 = arg1.values().iterator();

      StringBuilder sb0 = new StringBuilder();
      StringBuilder sb1 = new StringBuilder();

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

  private static final DiskCache diskCache = DiskCache.INSTANCE;

  private final SortedSet<SortedMap<SelectionProperty, String>> propertyValues = new TreeSet<SortedMap<SelectionProperty, String>>(
      MAP_COMPARATOR);

  private List<SoftPermutation> softPermutations = Lists.create();

  private final StatementRanges[] applicationStatementRanges;

  private final String strongName;

  private final long symbolToken;

  private final int permutationId;

  private PermutationResult applicationPermutationResult;

  public StandardCompilationResult(PermutationResult applicationPermutationResult) {
    super(StandardLinkerContext.class);
    this.applicationPermutationResult = applicationPermutationResult;
    this.strongName = applicationPermutationResult.getJsStrongName();
    this.applicationStatementRanges = applicationPermutationResult.getStatementRanges();
    this.permutationId = applicationPermutationResult.getPermutation().getId();
    this.symbolToken =
        diskCache.writeByteArray(applicationPermutationResult.getSerializedSymbolMap());
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

  public void addSoftPermutation(Map<SelectionProperty, String> propertyMap) {
    softPermutations = Lists.add(softPermutations, new StandardSoftPermutation(
        softPermutations.size(), propertyMap));
  }

  @Override
  public String[] getJavaScript() {
    byte[][] applicationJs = applicationPermutationResult.getJs();
    int applicationFragmentCount = applicationJs.length;

    String[] jsStrings = new String[applicationFragmentCount];
    for (int fragmentIndex = 0; fragmentIndex < applicationFragmentCount; fragmentIndex++) {
      jsStrings[fragmentIndex] = Util.toString(applicationJs[fragmentIndex]);
    }
    return jsStrings;
  }

  @Override
  public int getPermutationId() {
    return permutationId;
  }

  @Override
  public SortedSet<SortedMap<SelectionProperty, String>> getPropertyMap() {
    return Collections.unmodifiableSortedSet(propertyValues);
  }

  @Override
  public SoftPermutation[] getSoftPermutations() {
    return softPermutations.toArray(new SoftPermutation[softPermutations.size()]);
  }

  @Override
  public StatementRanges[] getStatementRanges() {
    return applicationStatementRanges;
  }

  @Override
  public String getStrongName() {
    return strongName;
  }

  @Override
  public SymbolData[] getSymbolMap() {
    return diskCache.readObject(symbolToken, SymbolData[].class);
  }
}
