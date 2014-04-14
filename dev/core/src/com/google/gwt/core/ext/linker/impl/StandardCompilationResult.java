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
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private static final DiskCache diskCache = DiskCache.INSTANCE;

  private final SortedSet<SortedMap<SelectionProperty, String>> propertyValues = new TreeSet<SortedMap<SelectionProperty, String>>(
      MAP_COMPARATOR);

  private List<SoftPermutation> softPermutations = Lists.create();

  private final StatementRanges[] applicationStatementRanges;

  private final String strongName;

  private final long symbolToken;

  private final int permutationId;

  private Set<PermutationResult> libraryPermutationResults;

  private PermutationResult applicationPermutationResult;

  public StandardCompilationResult(PermutationResult permutationResult) {
    this(permutationResult, Sets.<PermutationResult>newHashSet());
  }

  public StandardCompilationResult(PermutationResult applicationPermutationResult,
      Set<PermutationResult> libraryPermutationResults) {
    super(StandardLinkerContext.class);
    this.applicationPermutationResult = applicationPermutationResult;
    this.libraryPermutationResults = libraryPermutationResults;
    this.strongName = computeStrongName();
    this.applicationStatementRanges = applicationPermutationResult.getStatementRanges();
    this.permutationId = applicationPermutationResult.getPermutation().getId();
    this.symbolToken =
        diskCache.writeByteArray(applicationPermutationResult.getSerializedSymbolMap());
  }

  private String computeStrongName() {
    // If there are no library permutations
    if (libraryPermutationResults.isEmpty()) {
      // then just reuse the precalculated root application permutation strong name.
      return applicationPermutationResult.getJsStrongName();
    }

    // Otherwise stick all the different strong names together
    StringBuffer strongNames = new StringBuffer();
    strongNames.append(applicationPermutationResult.getJsStrongName());
    for (PermutationResult libraryPermutationResult : libraryPermutationResults) {
      strongNames.append(libraryPermutationResult.getJsStrongName());
    }
    // And hash that.
    return Util.computeStrongName(strongNames.toString().getBytes());
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

    // If there are no libraries
    if (libraryPermutationResults.isEmpty()) {
      // then return just the application JavaScript.
      String[] jsStrings = new String[applicationFragmentCount];
      for (int fragmentIndex = 0; fragmentIndex < applicationFragmentCount; fragmentIndex++) {
        jsStrings[fragmentIndex] = Util.toString(applicationJs[fragmentIndex]);
      }
      return jsStrings;
    }

    // Otherwise if there are multiple libraries.
    assert applicationFragmentCount == 1 : "Libraries can only have one fragment.";

    StringBuffer jsBuffer = new StringBuffer();

    // Concatenate the libraries and application JavaScript.
    for (PermutationResult libraryPermutationResult : libraryPermutationResults) {
      byte[][] libraryJs = libraryPermutationResult.getJs();
      int libraryFragmentCount = libraryJs.length;

      assert libraryFragmentCount == 1 : "Libraries can only have one fragment.";

      jsBuffer.append(Util.toString(libraryJs[0]));
    }

    jsBuffer.append(Util.toString(applicationJs[0]));

    return new String[] {jsBuffer.toString()};
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
    byte[][] applicationJs = applicationPermutationResult.getJs();
    int applicationFragmentCount = applicationJs.length;

    // If there are no libraries
    if (libraryPermutationResults.isEmpty()) {
      // then return just the application statement ranges.
      return applicationStatementRanges;
    }

    // Otherwise if there are multiple libraries.
    assert applicationFragmentCount == 1 : "Libraries can only have one fragment.";

    // Concatenate the libraries and application JavaScript.
    List<StatementRanges> statementRangesList = new ArrayList<StatementRanges>();
    for (PermutationResult libraryPermutationResult : libraryPermutationResults) {
      StatementRanges[] libraryStatementRanges = libraryPermutationResult.getStatementRanges();
      int libraryFragmentCount = libraryStatementRanges.length;

      assert libraryFragmentCount == 1 : "Libraries can only have one fragment.";

      statementRangesList.add(libraryStatementRanges[0]);
    }

    statementRangesList.add(applicationStatementRanges[0]);
    // Some library might not have contained any source and thus have a null statementRange.
    statementRangesList.removeAll(Collections.singleton(null));

    return new StatementRanges[] {StandardStatementRanges.combine(statementRangesList)};
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
