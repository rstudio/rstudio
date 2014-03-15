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
package com.google.gwt.dev.resource.impl;

import com.google.gwt.dev.resource.impl.DefaultFilters.FilterFileType;

import junit.framework.TestCase;

import org.apache.tools.ant.types.ZipScanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An implementation for DefaultExcludesFilterTest.
 *
 * Tests:
 *
 * 1. The filter conversion from ant to Java regex. Which cases can we handle,
 * and confirm that we perform correctly in all cases.
 *
 * 2. checking the 4 defaultFilters, one for each combination of defaultExcludes
 * and isJava.
 *
 * 3. Checking whether customFilter work correctly, both in presence and absence
 * of Ant.
 *
 * TODO (amitmanjhi): clean up this test code.
 */
public class DefaultFiltersTest extends TestCase {

  static class AdvancedPaths {
    String baseIncluded[] = {
        "Baz", "test/foo/Foo", "test/bar/Bar", "test/baz/Foo"};

    String baseExcludedTesting[] = {"foo/testing/Baz"};
    String baseExcludedExact[] = {"Foo", "Bar",};
    String baseExcludedDoubleStar[] = {"fooz/Baz", "barz/hello/Baz"};
    String baseExcluded[] = mergeArrays(baseExcludedTesting, baseExcludedExact,
        baseExcludedDoubleStar);

    void testAdvancedPath(ResourceFilterString expected,
        ResourceFilterString actual, String suffix) {
      for (String path : mergeArrays(baseIncluded, baseExcluded, getMiscPaths(
          "testing", false).toArray(EMPTY_ARRAY),
          getMiscPaths("a/bc/de", false).toArray(EMPTY_ARRAY))) {
        assertEquals(path + suffix, expected, actual);
      }
    }

    void testAdvancedPathAnt(ResourceFilterString expected,
        ResourceFilterString actual, String suffix) {
      for (String path : mergeArrays(baseIncluded, baseExcluded, getMiscPaths(
          "testing", true).toArray(EMPTY_ARRAY),
          getMiscPaths("a/bc/de", true).toArray(EMPTY_ARRAY))) {
        assertEquals(EXCLUDED_CHARS + path + EXCLUDED_CHARS + suffix,
            expected, actual);
        assertEquals(path + EXCLUDED_CHARS + suffix, expected, actual);
        assertEquals(path + suffix, expected, actual);
      }
      testAdvancedPath(expected, actual, suffix);
      new BasicPaths().testBasicPath(expected, actual, suffix);
    }

    void testAdvancedClassFilesPathAnt(ResourceFilterString expected,
        ResourceFilterString actual) {
      testAdvancedPathAnt(expected, actual, CLASS_FILE_SUFFIX);
    }

    void testAdvancedJavaPathAnt(ResourceFilterString expected,
        ResourceFilterString actual) {
      testAdvancedPathAnt(expected, actual, JAVA_FILE_SUFFIX);
    }

    void testAdvancedPath(ResourceFilterString expected,
        ResourceFilterString actual) {
      for (String path : mergeArrays(baseIncluded, baseExcluded, getMiscPaths(
          "testing", false).toArray(EMPTY_ARRAY),
          getMiscPaths("a/bc/de", false).toArray(EMPTY_ARRAY))) {
        assertEquals(path, expected, actual);
      }
    }

    void testAdvancedPathAnt(ResourceFilterString expected,
        ResourceFilterString actual) {
      for (String path : mergeArrays(baseIncluded, baseExcluded, getMiscPaths(
          "testing", true).toArray(EMPTY_ARRAY),
          getMiscPaths("a/bc/de", true).toArray(EMPTY_ARRAY))) {
        assertEquals(path, expected, actual);
        assertEquals(path + EXCLUDED_CHARS, expected, actual);
        assertEquals(EXCLUDED_CHARS + path + EXCLUDED_CHARS, expected, actual);
      }
      testAdvancedPath(expected, actual);
      new BasicPaths().testBasicPath(expected, actual);
    }
  }

  static class BasicPaths {
    String baseIncluded[] = {
        "foo", "/foo", "foo/bar", "/foo/bar", "/foo/bar", "/foo$/$", "/foo-_", "123FOO123", "cvs",
        "cvs/cvs/svn", ".foo_bar$", "foo/asvn"};
    String baseExcluded[] = {"foo/CVS/bar", "CVS/bar", "foo/.svn/bar", ".svn/bar", "foo/SCCS/bar",};
    String baseSuffixExcluded[] = {
        "foo/.cvsignore", "foo/CVS", "foo/.svn", "foo/SCCS",
        "foo/bar/vssver.scc", "/foo/bar/.DS_Store"};

    void testBasicPath(ResourceFilterString expected,
        ResourceFilterString actual, String suffix) {
      for (String str : mergeArrays(baseIncluded, baseExcluded,
          baseSuffixExcluded,
          getMiscPaths("testing", true).toArray(EMPTY_ARRAY), getMiscPaths(
              "a/bc/de", true).toArray(EMPTY_ARRAY))) {
        assertEquals(str, expected, actual);
        assertEquals(str + suffix, expected, actual);
        assertEquals(EXCLUDED_CHARS + str, expected, actual);
        assertEquals(EXCLUDED_CHARS + str + EXCLUDED_CHARS, expected, actual);
        assertEquals(EXCLUDED_CHARS + str + suffix, expected, actual);
        assertEquals(EXCLUDED_CHARS + str + EXCLUDED_CHARS + suffix,
            expected, actual);
      }
    }

    void testBasicPath(ResourceFilterString expected,
        ResourceFilterString actual) {
      for (String str : mergeArrays(baseIncluded, baseExcluded,
          baseSuffixExcluded,
          getMiscPaths("testing", true).toArray(EMPTY_ARRAY), getMiscPaths(
              "a/bc/de", true).toArray(EMPTY_ARRAY))) {
        assertEquals(str, expected, actual);
        assertEquals(EXCLUDED_CHARS + str, expected, actual);
        assertEquals(EXCLUDED_CHARS + str + EXCLUDED_CHARS, expected, actual);
      }
    }
  }

  /*
   * Sole purpose of this class is to get a useful debug message.
   */
  private static class ResourceFilterString {
    final ResourceFilter filter;
    final String stringRepresentation;

    ResourceFilterString(ResourceFilter filter, String stringRepresentation) {
      this.filter = filter;
      this.stringRepresentation = stringRepresentation;
    }
  }

  private static final String EMPTY_ARRAY[] = new String[0];
  private static final boolean DEFAULT_EXCLUDES = true;
  private static final boolean DEFAULT_INCLUDES = false;
  private static final String JAVA_FILE_SUFFIX = ".java";
  private static final String CLASS_FILE_SUFFIX = ".class";

  private static final String MERGED_PATTERNS[] = {
      "**/testing/**", "Foo", "Bar", "fooz/**", "barz/hello/**"};

  // careful that the pattern is still permitted by ant.
  private static final String EXCLUDED_CHARS = "#~%*";

  private static void assertEquals(String path, ResourceFilterString expected,
      ResourceFilterString actual) {
    boolean scanResult = expected.filter.allows(path);
    assertEquals("String to be matched = " + path + ", actual filter = "
        + actual.stringRepresentation + " should yield " + scanResult
        + ", expected Filter = " + expected.stringRepresentation, scanResult,
        actual.filter.allows(path));
  }

  private static List<String> getMiscPaths(String middleString,
      boolean endInSlash) {
    List<String> testPaths = new ArrayList<String>();
    testPaths.addAll(Arrays.asList(new String[] {
        "Foo", "Bar", "foo/xyz", "afoo/xyz-_$", "b/foo/x",
        "foo/BarTestabc.java", "foo/xyz/BarTestabc.java", "a/b/testing/c/d",
        "a/testing/b/c/FooBazBarTest.java", "a/testing/b/Foo/BazBar.java",
        "a/testing/b/Foo$-_$Bar.class", "a/testing/b/Foo$/$.class"}));

    String pathPrefixes[] = {"", "/", "foo/", "/foo/", "bar/foo/", "/bar/foo/"};
    List<String> pathSuffixes = new ArrayList<String>();
    if (endInSlash) {
      // special handling because currently we don't handle paths that end in /
      pathSuffixes.addAll(Arrays.asList(new String[] {
          "", "/", "/foo", "/foo/", "/foo/bar", "/foo/bar/"}));
    } else {
      pathSuffixes.addAll(Arrays.asList(new String[] {"", "/foo", "/foo/bar",}));
    }
    for (String pathPrefix : pathPrefixes) {
      for (String pathSuffix : pathSuffixes) {
        testPaths.add(pathPrefix + middleString + pathSuffix);
      }
    }
    return testPaths;
  }

  private static String[] mergeArrays(String[]... baseArrays) {
    int count = 0;
    for (String arrayElement[] : baseArrays) {
      count += arrayElement.length;
    }
    String retArray[] = new String[count];
    count = 0;
    for (String arrayElement[] : baseArrays) {
      for (String element : arrayElement) {
        retArray[count++] = element;
      }
    }
    return retArray;
  }

  public void testEmptyFilters() {
    BasicPaths basicPaths = new BasicPaths();
    FilterFileType fileType;

    // first arg: ant filter, second arg: our custom filter
    fileType = FilterFileType.RESOURCE_FILES;
    basicPaths.testBasicPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        EMPTY_ARRAY, DEFAULT_EXCLUDES, fileType, "antDefaultFilter"),
        new ResourceFilterString(fileType.getDefaultFilter(),
            "defaultFilter"));
    basicPaths.testBasicPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        EMPTY_ARRAY, DEFAULT_INCLUDES, fileType, "antDefaultIncludesFilter"),
        new ResourceFilterString(fileType.getFileTypeFilter(),
            "defaultIncludesFilter"));

    fileType = FilterFileType.JAVA_FILES;
    basicPaths.testBasicPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        EMPTY_ARRAY, DEFAULT_EXCLUDES, fileType, "antDefaultJavaFilter"),
        new ResourceFilterString(fileType.getDefaultFilter(),
            "defaultJavaFilter"), fileType.getSuffix());
    basicPaths.testBasicPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        EMPTY_ARRAY, DEFAULT_INCLUDES, fileType, "antJustJavaFilter"),
        new ResourceFilterString(fileType.getFileTypeFilter(),
            "justJavaFilter"), fileType.getSuffix());
    
    fileType = FilterFileType.CLASS_FILES;
    basicPaths.testBasicPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.CLASS_FILES, "antDefaultClassFilesFilter"),
        new ResourceFilterString(fileType.getDefaultFilter(),
            "defaultClassFilesFilter"), fileType.getSuffix());
    basicPaths.testBasicPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        EMPTY_ARRAY, DEFAULT_INCLUDES, FilterFileType.CLASS_FILES, "antJustClassFilesFilter"),
        new ResourceFilterString(fileType.getFileTypeFilter(),
            "justClassFilesFilter"), fileType.getSuffix());    
  }

  /**
   * (a) test that filters are correctly converted to non-null and null
   * patterns. (b) test that filters match the same String.
   */
  public void testFilterConversion() {
    List<String> nullFilters = Arrays.asList(new String[] {
        "***/testing/**", "**/{/**", "**/}/**", "**/+/**",});
    List<String> okayFilters = new ArrayList<String>();
    okayFilters.addAll(Arrays.asList(new String[] {
        "**/#/**", "**/~/**", "Foo", "Bar", "foo/**", "foo/*Test*java",
        "**/testing/**", "**/testing/**/Foo*Bar*.java",
        "**/testing/**/Foo$*r.class",}));
    String doubleStarPrefixes[] = {"", "/", "**/", "/**/", "foo**/", "/foo**/"};
    String doubleStarSuffixes[] = {"", "/", "/**", "/**/", "/**foo", "/**foo/"};
    String middleString = "testing";
    for (String doubleStarPrefix : doubleStarPrefixes) {
      for (String doubleStarSuffix : doubleStarSuffixes) {
        okayFilters.add(doubleStarPrefix + middleString + doubleStarSuffix);
      }
    }

    List<String> testPaths = getMiscPaths("testing", false);
    DefaultFilters filters = new DefaultFilters();
    for (String filter : nullFilters) {
      assertNull(filter + " conversion should be null",
          filters.getPatternFromAntPattern(filter));
    }

    for (String filter : okayFilters) {
      String pattern = filters.getPatternFromAntPattern(filter);
      assertNotNull(filter + " conversion should be non-null", pattern);

      ResourceFilterString antFilterString = getAntFilter(
          new String[] {filter}, EMPTY_ARRAY, EMPTY_ARRAY, DEFAULT_EXCLUDES,
          FilterFileType.RESOURCE_FILES, "ant_" + filter);
      ResourceFilterString customFilterString = new ResourceFilterString(
          filters.customFilterWithCatchAll(new String[] {filter}, EMPTY_ARRAY,
              EMPTY_ARRAY, true, null, FilterFileType.RESOURCE_FILES), "custom_" + pattern);
      for (String path : testPaths) {
        assertEquals(path, antFilterString, customFilterString);
      }
    }
  }

  public void testFilterParts() {
    AdvancedPaths advancedPaths = new AdvancedPaths();
    ResourceFilter filter = null;

    // everything except those starting with '/' should be included
    filter = new DefaultFilters().getIncludesFilterPart(EMPTY_ARRAY);
    advancedPaths.testAdvancedPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        EMPTY_ARRAY, DEFAULT_INCLUDES, FilterFileType.RESOURCE_FILES, "antDefaultFilter"),
        new ResourceFilterString(filter, "defaultFilter"));

    // everything should be excluded
    filter = new DefaultFilters().getExcludesFilterPart(EMPTY_ARRAY);
    advancedPaths.testAdvancedPath(getAntFilter(new String[] {"a/1/2/3"},
        new String[] {"**", "/**"}, EMPTY_ARRAY, DEFAULT_INCLUDES, FilterFileType.RESOURCE_FILES,
        "antDefaultFilter"), new ResourceFilterString(filter, "defaultFilter"));

    filter = new DefaultFilters().getIncludesFilterPart(MERGED_PATTERNS);
    advancedPaths.testAdvancedPath(getAntFilter(MERGED_PATTERNS, EMPTY_ARRAY,
        EMPTY_ARRAY, DEFAULT_INCLUDES, FilterFileType.RESOURCE_FILES, "antMergedPatterns"),
        new ResourceFilterString(filter, "customMergedPatterns"));
  }

  // no ant, catchAll filter is null
  public void testNonEmptyFilters() {
    AdvancedPaths advancedPaths = new AdvancedPaths();

    ResourceFilter filter = null;
    // pass empty includeArray. Matches everything that is not excluded.
    filter = getFilterWithoutCatchAll(EMPTY_ARRAY, MERGED_PATTERNS, EMPTY_ARRAY,
        FilterFileType.RESOURCE_FILES);
    advancedPaths.testAdvancedPath(getAntFilter(EMPTY_ARRAY, MERGED_PATTERNS,
        EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.RESOURCE_FILES, "ant_emptyArray_mergedPatterns"),
        new ResourceFilterString(filter, "custom_emptyArray_mergedPatterns"));

    // pass empty excludeArray. Matches everything that is included.
    filter = getFilterWithoutCatchAll(MERGED_PATTERNS, EMPTY_ARRAY, EMPTY_ARRAY,
        FilterFileType.RESOURCE_FILES);
    advancedPaths.testAdvancedPath(getAntFilter(MERGED_PATTERNS, EMPTY_ARRAY,
        EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.RESOURCE_FILES, "ant_mergedPatterns_emptyArray"),
        new ResourceFilterString(filter, "custom_mergedPatterns_emptyArray"));

    // pass non-empty include and exclude array. Matches nothing
    filter = getFilterWithoutCatchAll(MERGED_PATTERNS, MERGED_PATTERNS,
        EMPTY_ARRAY, FilterFileType.RESOURCE_FILES);
    advancedPaths.testAdvancedPath(
        getAntFilter(MERGED_PATTERNS, MERGED_PATTERNS, EMPTY_ARRAY,
            DEFAULT_EXCLUDES, FilterFileType.RESOURCE_FILES, "ant_mergedPatterns_mergedPatterns"),
        new ResourceFilterString(filter, "custom_mergedPatterns_mergedPatterns"));
  }

  // finish, catchAll filter is not-null
  public void testNonEmptyFiltersAnt() {
    AdvancedPaths advancedPaths = new AdvancedPaths();

    ResourceFilter filter = null;
    // pass empty includeArray. Matches everything that is not excluded.
    filter = getFilterWithCatchAll(EMPTY_ARRAY, MERGED_PATTERNS, EMPTY_ARRAY,
        FilterFileType.RESOURCE_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(EMPTY_ARRAY, MERGED_PATTERNS,
        EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.RESOURCE_FILES,
        "ant_emptyIncludeArray_mergedPatterns"),
        new ResourceFilterString(filter, "custom_emptyArray_mergedPatterns"));

    // pass empty excludeArray. Matches everything that is included.
    filter = getFilterWithCatchAll(MERGED_PATTERNS, EMPTY_ARRAY, EMPTY_ARRAY,
        FilterFileType.RESOURCE_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(MERGED_PATTERNS, EMPTY_ARRAY,
        EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.RESOURCE_FILES,
        "ant_emptyExcludeArray_mergedPatterns"),
        new ResourceFilterString(filter, "custom_emptyArray_mergedPatterns"));

    // pass non-empty include and exclude array. Matches nothing
    filter = getFilterWithCatchAll(MERGED_PATTERNS, MERGED_PATTERNS, EMPTY_ARRAY,
        FilterFileType.RESOURCE_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(MERGED_PATTERNS,
        MERGED_PATTERNS, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.RESOURCE_FILES,
        "ant_mergedPatterns_mergedPatterns"), new ResourceFilterString(filter,
        "custom_mergedPatterns_mergedPatterns"));
  }

  // no ant, catchAll filter is null
  public void testNonEmptyJavaFilters() {
    AdvancedPaths advancedPaths = new AdvancedPaths();
    String newMergedPatterns[] = getMergedPatterns(JAVA_FILE_SUFFIX);
    ResourceFilter filter = null;
    
    // pass empty includeArray. Means catch all
    filter = getFilterWithoutCatchAll(EMPTY_ARRAY, newMergedPatterns,
        EMPTY_ARRAY, FilterFileType.JAVA_FILES);
    advancedPaths.testAdvancedPath(getAntFilter(EMPTY_ARRAY,
        newMergedPatterns, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.JAVA_FILES,
        "ant_emptyArray_newMergedPatterns"), new ResourceFilterString(filter,
        "custom_emptyArray_newMergedPatterns"), JAVA_FILE_SUFFIX);

    // pass empty excludeArray. Means catch only the pattern
    filter = getFilterWithoutCatchAll(newMergedPatterns, EMPTY_ARRAY,
        EMPTY_ARRAY, FilterFileType.JAVA_FILES);
    advancedPaths.testAdvancedPath(getAntFilter(newMergedPatterns,
        EMPTY_ARRAY, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.JAVA_FILES,
        "ant_newMergedPatterns_emptyArray"), new ResourceFilterString(filter,
        "custom_newMergedPatterns_emptyArray"), JAVA_FILE_SUFFIX);

    // pass non-empty include and exclude array.
    filter = getFilterWithoutCatchAll(newMergedPatterns, newMergedPatterns,
        EMPTY_ARRAY, FilterFileType.JAVA_FILES);
    advancedPaths.testAdvancedPath(getAntFilter(newMergedPatterns,
        newMergedPatterns, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.JAVA_FILES,
        "ant_newMergedPatterns_newMergedPatterns"), new ResourceFilterString(
        filter, "custom_newMergedPatterns_newMergedPatterns"), JAVA_FILE_SUFFIX);
  }

  public void testNonEmptyJavaFiltersAnt() {
    AdvancedPaths advancedPaths = new AdvancedPaths();
    String newMergedPatterns[] = getMergedPatterns(JAVA_FILE_SUFFIX);
    ResourceFilter filter = null;
    
    // pass empty includeArray. Means catch all
    filter = getFilterWithCatchAll(EMPTY_ARRAY, newMergedPatterns, EMPTY_ARRAY,
        FilterFileType.JAVA_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(EMPTY_ARRAY,
        newMergedPatterns, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.JAVA_FILES,
        "ant_emptyArray_newMergedPatterns"), new ResourceFilterString(filter,
        "custom_emptyArray_newMergedPatterns"), JAVA_FILE_SUFFIX);

    // pass empty excludeArray. Means catch only the pattern
    filter = getFilterWithCatchAll(newMergedPatterns, EMPTY_ARRAY, EMPTY_ARRAY,
        FilterFileType.JAVA_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(newMergedPatterns,
        EMPTY_ARRAY, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.JAVA_FILES,
        "ant_newMergedPatterns_emptyArray"), new ResourceFilterString(filter,
        "custom_newMergedPatterns_emptyArray"), JAVA_FILE_SUFFIX);

    // pass non-empty include and exclude array.
    filter = getFilterWithCatchAll(newMergedPatterns, newMergedPatterns,
        EMPTY_ARRAY, FilterFileType.JAVA_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(newMergedPatterns,
        newMergedPatterns, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.JAVA_FILES,
        "ant_newMergedPatterns_newMergedPatterns"), new ResourceFilterString(
        filter, "custom_newMergedPatterns_newMergedPatterns"), JAVA_FILE_SUFFIX);
  }
  
  public void testNonEmptyJavaSkipFiltersAnt() {
    AdvancedPaths advancedPaths = new AdvancedPaths();
    String newMergedPatterns[] = getMergedPatterns(JAVA_FILE_SUFFIX);
    ResourceFilter filter = null;

    // pass empty includeArray. Means catch all, skipping newMergedPatterns
    filter = getFilterWithCatchAll(EMPTY_ARRAY, EMPTY_ARRAY, newMergedPatterns,
        FilterFileType.JAVA_FILES);
    advancedPaths.testAdvancedJavaPathAnt(getAntFilter(EMPTY_ARRAY,
        EMPTY_ARRAY, newMergedPatterns, DEFAULT_EXCLUDES, FilterFileType.JAVA_FILES,
        "ant_emptyArray_newMergedPatterns"), new ResourceFilterString(filter,
        "custom_emptyArray_newMergedPatterns"));

    // pass non-empty include and skip array.
    filter = getFilterWithCatchAll(newMergedPatterns, EMPTY_ARRAY,
        newMergedPatterns, FilterFileType.JAVA_FILES);
    advancedPaths.testAdvancedJavaPathAnt(getAntFilter(newMergedPatterns,
        EMPTY_ARRAY, newMergedPatterns, DEFAULT_EXCLUDES, FilterFileType.JAVA_FILES,
        "ant_newMergedPatterns_newMergedPatterns"), new ResourceFilterString(
        filter, "custom_newMergedPatterns_newMergedPatterns"));

    // in a single filter, skip and exclude are equivalent
    filter = getFilterWithCatchAll(newMergedPatterns, newMergedPatterns,
        EMPTY_ARRAY, FilterFileType.JAVA_FILES);
    advancedPaths.testAdvancedJavaPathAnt(getAntFilter(newMergedPatterns,
        EMPTY_ARRAY, newMergedPatterns, DEFAULT_EXCLUDES, FilterFileType.JAVA_FILES,
        "ant_newMergedPatterns_newMergedPatterns"), new ResourceFilterString(
        filter, "custom_newMergedPatterns_newMergedPatterns"));
  }
  
  // no ant, catchAll filter is null
  public void testNonEmptyClassFileFilters() {
    AdvancedPaths advancedPaths = new AdvancedPaths();

    String newMergedPatterns[] = getMergedPatterns(CLASS_FILE_SUFFIX);
    ResourceFilter filter = null;
    // pass empty includeArray. Means catch all
    filter = getFilterWithoutCatchAll(EMPTY_ARRAY, newMergedPatterns,
        EMPTY_ARRAY, FilterFileType.CLASS_FILES);
    advancedPaths.testAdvancedPath(getAntFilter(EMPTY_ARRAY,
        newMergedPatterns, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.CLASS_FILES,
        "ant_emptyArray_newMergedPatterns"), new ResourceFilterString(filter,
        "custom_emptyArray_newMergedPatterns"), CLASS_FILE_SUFFIX);

    // pass empty excludeArray. Means catch only the pattern
    filter = getFilterWithoutCatchAll(newMergedPatterns, EMPTY_ARRAY,
        EMPTY_ARRAY, FilterFileType.CLASS_FILES);
    advancedPaths.testAdvancedPath(getAntFilter(newMergedPatterns,
        EMPTY_ARRAY, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.CLASS_FILES,
        "ant_newMergedPatterns_emptyArray"), new ResourceFilterString(filter,
        "custom_newMergedPatterns_emptyArray"), CLASS_FILE_SUFFIX);

    // pass non-empty include and exclude array.
    filter = getFilterWithoutCatchAll(newMergedPatterns, newMergedPatterns,
        EMPTY_ARRAY, FilterFileType.CLASS_FILES);
    advancedPaths.testAdvancedPath(getAntFilter(newMergedPatterns,
        newMergedPatterns, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.CLASS_FILES,
        "ant_newMergedPatterns_newMergedPatterns"), new ResourceFilterString(
        filter, "custom_newMergedPatterns_newMergedPatterns"), CLASS_FILE_SUFFIX);
  }

  public void testNonEmptyClassFileFiltersAnt() {
    AdvancedPaths advancedPaths = new AdvancedPaths();
    String newMergedPatterns[] = getMergedPatterns(CLASS_FILE_SUFFIX);
    ResourceFilter filter = null;
    
    // pass empty includeArray. Means catch all
    filter = getFilterWithCatchAll(EMPTY_ARRAY, newMergedPatterns, EMPTY_ARRAY,
        FilterFileType.CLASS_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(EMPTY_ARRAY,
        newMergedPatterns, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.CLASS_FILES,
        "ant_emptyArray_newMergedPatterns"), new ResourceFilterString(filter,
        "custom_emptyArray_newMergedPatterns"), CLASS_FILE_SUFFIX);

    // pass empty excludeArray. Means catch only the pattern
    filter = getFilterWithCatchAll(newMergedPatterns, EMPTY_ARRAY, EMPTY_ARRAY,
        FilterFileType.CLASS_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(newMergedPatterns,
        EMPTY_ARRAY, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.CLASS_FILES,
        "ant_newMergedPatterns_emptyArray"), new ResourceFilterString(filter,
        "custom_newMergedPatterns_emptyArray"), CLASS_FILE_SUFFIX);

    // pass non-empty include and exclude array.
    filter = getFilterWithCatchAll(newMergedPatterns, newMergedPatterns,
        EMPTY_ARRAY, FilterFileType.CLASS_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(newMergedPatterns,
        newMergedPatterns, EMPTY_ARRAY, DEFAULT_EXCLUDES, FilterFileType.CLASS_FILES,
        "ant_newMergedPatterns_newMergedPatterns"), new ResourceFilterString(
        filter, "custom_newMergedPatterns_newMergedPatterns"), CLASS_FILE_SUFFIX);
  }
  
  private String[] getMergedPatterns(String suffix) {
    String newMergedPatterns[] = new String[MERGED_PATTERNS.length];
    for (int i = 0; i < MERGED_PATTERNS.length; i++) {
      if (MERGED_PATTERNS[i].endsWith("*")) {
        newMergedPatterns[i] = MERGED_PATTERNS[i];
      } else {
        newMergedPatterns[i] = MERGED_PATTERNS[i] + suffix;
      }
    }
    return newMergedPatterns;
  }
  
  public void testNonEmptyClassFileSkipFiltersAnt() {
    AdvancedPaths advancedPaths = new AdvancedPaths();
    String newMergedPatterns[] = getMergedPatterns(CLASS_FILE_SUFFIX);
    ResourceFilter filter = null;

    // pass empty includeArray. Means catch all, skipping newMergedPatterns
    filter = getFilterWithCatchAll(EMPTY_ARRAY, EMPTY_ARRAY, newMergedPatterns,
        FilterFileType.CLASS_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(EMPTY_ARRAY,
        EMPTY_ARRAY, newMergedPatterns, DEFAULT_EXCLUDES, FilterFileType.CLASS_FILES,
        "ant_emptyArray_newMergedPatterns"), new ResourceFilterString(filter,
        "custom_emptyArray_newMergedPatterns"), CLASS_FILE_SUFFIX);

    // pass non-empty include and skip array.
    filter = getFilterWithCatchAll(newMergedPatterns, EMPTY_ARRAY,
        newMergedPatterns, FilterFileType.CLASS_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(newMergedPatterns,
        EMPTY_ARRAY, newMergedPatterns, DEFAULT_EXCLUDES, FilterFileType.CLASS_FILES,
        "ant_newMergedPatterns_newMergedPatterns"), new ResourceFilterString(
        filter, "custom_newMergedPatterns_newMergedPatterns"), CLASS_FILE_SUFFIX);

    // in a single filter, skip and exclude are equivalent
    filter = getFilterWithCatchAll(newMergedPatterns, newMergedPatterns,
        EMPTY_ARRAY, FilterFileType.CLASS_FILES);
    advancedPaths.testAdvancedPathAnt(getAntFilter(newMergedPatterns,
        EMPTY_ARRAY, newMergedPatterns, DEFAULT_EXCLUDES, FilterFileType.CLASS_FILES,
        "ant_newMergedPatterns_newMergedPatterns"), new ResourceFilterString(
        filter, "custom_newMergedPatterns_newMergedPatterns"), CLASS_FILE_SUFFIX);
  }
  
  private ResourceFilterString getAntFilter(String includes[],
      String excludes[], String skips[], boolean defaultExcludes,
      final FilterFileType filterFileType, String tag) {
    final ZipScanner scanner = DefaultFilters.getScanner(includes, excludes,
        skips, defaultExcludes, true);
    return new ResourceFilterString(new ResourceFilter() {
      @Override
      public boolean allows(String path) {
        return fileTypeMatches(filterFileType, path) && scanner.match(path);
      }
    }, tag != null ? tag : "includes: " + Arrays.toString(includes)
        + ", excludes: " + Arrays.toString(excludes));
  }

  private ResourceFilter getFilterWithCatchAll(String includesList[],
      String excludesList[], String skipList[], FilterFileType filterFileType) {
    switch (filterFileType) {
      case JAVA_FILES:
        return new DefaultFilters().customJavaFilter(includesList, excludesList,
            skipList, true, true);
      case CLASS_FILES:
        return new DefaultFilters().customClassFilesFilter(includesList, excludesList,
            skipList, true, true);
      case RESOURCE_FILES:
      default:
        return new DefaultFilters().customResourceFilter(includesList,
            excludesList, skipList, true, true);
    }
  }

  // caseSensitive and excludeDefaults are set
  private ResourceFilter getFilterWithoutCatchAll(String includesList[],
      String excludesList[], String skipList[],  FilterFileType filterFileType) {
    return new DefaultFilters().customFilterWithCatchAll(includesList,
        excludesList, skipList, true, null, filterFileType);
  }
  
  private boolean fileTypeMatches(FilterFileType filterFileType, String path) {
    switch (filterFileType) {
      case JAVA_FILES:
        return path.endsWith(".java");
      case CLASS_FILES:
        return path.endsWith(".class");
    }
    return true;
  }
}
