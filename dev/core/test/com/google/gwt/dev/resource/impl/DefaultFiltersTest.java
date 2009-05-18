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

    void testAdvancedJavaPath(ResourceFilterString expected,
        ResourceFilterString actual) {
      for (String path : mergeArrays(baseIncluded, baseExcluded, getMiscPaths(
          "testing", false).toArray(EMPTY_ARRAY),
          getMiscPaths("a/bc/de", false).toArray(EMPTY_ARRAY))) {
        assertEquals(path + ".java", expected, actual);
      }
    }

    void testAdvancedJavaPathAnt(ResourceFilterString expected,
        ResourceFilterString actual) {
      for (String path : mergeArrays(baseIncluded, baseExcluded, getMiscPaths(
          "testing", true).toArray(EMPTY_ARRAY),
          getMiscPaths("a/bc/de", true).toArray(EMPTY_ARRAY))) {
        assertEquals(excludedChars + path + excludedChars + javaSuffix,
            expected, actual);
        assertEquals(path + excludedChars + javaSuffix, expected, actual);
        assertEquals(path + javaSuffix, expected, actual);
      }
      testAdvancedJavaPath(expected, actual);
      new BasicPaths().testBasicJavaPath(expected, actual);
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
        assertEquals(path + excludedChars, expected, actual);
        assertEquals(excludedChars + path + excludedChars, expected, actual);
      }
      testAdvancedPath(expected, actual);
      new BasicPaths().testBasicPath(expected, actual);
    }
  }

  static class BasicPaths {
    String baseIncluded[] = {
        "foo", "/foo", "foo/bar", "/foo/bar", "/foo/bar", "/foo$/$", "/foo-_",
        "123FOO123", "cvs", "cvs/cvs/svn", ".foo_bar$", "foo/asvn"};
    String baseExcluded[] = {"foo/CVS/bar", "foo/.svn/bar", "foo/SCCS/bar",};
    String baseSuffixExcluded[] = {
        "foo/.cvsignore", "foo/CVS", "foo/.svn", "foo/SCCS",
        "foo/bar/vssver.scc", "/foo/bar/.DS_Store"};

    void testBasicJavaPath(ResourceFilterString expected,
        ResourceFilterString actual) {
      for (String str : mergeArrays(baseIncluded, baseExcluded,
          baseSuffixExcluded,
          getMiscPaths("testing", true).toArray(EMPTY_ARRAY), getMiscPaths(
              "a/bc/de", true).toArray(EMPTY_ARRAY))) {
        assertEquals(str, expected, actual);
        assertEquals(str + javaSuffix, expected, actual);
        assertEquals(excludedChars + str, expected, actual);
        assertEquals(excludedChars + str + excludedChars, expected, actual);
        assertEquals(excludedChars + str + javaSuffix, expected, actual);
        assertEquals(excludedChars + str + excludedChars + javaSuffix,
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
        assertEquals(excludedChars + str, expected, actual);
        assertEquals(excludedChars + str + excludedChars, expected, actual);
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
  private static final boolean NOT_JAVA = false;
  private static final boolean YES_JAVA = true;

  private static final String mergedPatterns[] = {
      "**/testing/**", "Foo", "Bar", "fooz/**", "barz/hello/**"};

  // careful that the pattern is still permitted by ant.
  private static final String excludedChars = "#~%*";

  private static final String javaSuffix = ".java";

  private static void assertEquals(String path, ResourceFilterString expected,
      ResourceFilterString actual) {
    boolean scanResult = expected.filter.allows(path);
    assertEquals("String to be matched = " + path + ", actual filter = "
        + actual.stringRepresentation + " should yied " + scanResult
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
    DefaultFilters defaultFilters = new DefaultFilters();

    // first arg: ant filter, second arg: our custom filter
    basicPaths.testBasicPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        DEFAULT_EXCLUDES, NOT_JAVA, "antDefaultFilter"),
        new ResourceFilterString(defaultFilters.defaultResourceFilter,
            "defaultFilter"));
    basicPaths.testBasicPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        DEFAULT_INCLUDES, NOT_JAVA, "antDefaultIncludesFilter"),
        new ResourceFilterString(defaultFilters.justResourceFilter,
            "defaultIncludesFilter"));

    basicPaths.testBasicJavaPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        DEFAULT_EXCLUDES, YES_JAVA, "antDefaultJavaFilter"),
        new ResourceFilterString(defaultFilters.defaultJavaFilter,
            "defaultJavaFilter"));
    basicPaths.testBasicJavaPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        DEFAULT_INCLUDES, YES_JAVA, "antJustJavaFilter"),
        new ResourceFilterString(defaultFilters.justJavaFilter,
            "justJavaFilter"));
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
          new String[] {filter}, EMPTY_ARRAY, DEFAULT_EXCLUDES, NOT_JAVA,
          "ant_" + filter);
      ResourceFilterString customFilterString = new ResourceFilterString(
          filters.customFilterWithCatchAll(new String[] {filter}, EMPTY_ARRAY,
              true, null, NOT_JAVA), "custom_" + pattern);
      for (String path : testPaths) {
        assertEquals(path, antFilterString, customFilterString);
      }
    }
  }

  public void testFilterParts() {
    AdvancedPaths advancedPaths = new AdvancedPaths();
    ResourceFilter filter = null;

    // everything except those starting with '/' should be included
    filter = new DefaultFilters().getFilterPart(EMPTY_ARRAY, true);
    advancedPaths.testAdvancedPath(getAntFilter(EMPTY_ARRAY, EMPTY_ARRAY,
        DEFAULT_INCLUDES, NOT_JAVA, "antDefaultFilter"),
        new ResourceFilterString(filter, "defaultFilter"));

    // everything should be excluded
    filter = new DefaultFilters().getFilterPart(EMPTY_ARRAY, false);
    advancedPaths.testAdvancedPath(getAntFilter(new String[] {"a/1/2/3"},
        new String[] {"**", "/**"}, DEFAULT_INCLUDES, NOT_JAVA,
        "antDefaultFilter"), new ResourceFilterString(filter, "defaultFilter"));

    filter = new DefaultFilters().getFilterPart(mergedPatterns, true);
    advancedPaths.testAdvancedPath(getAntFilter(mergedPatterns, EMPTY_ARRAY,
        DEFAULT_INCLUDES, NOT_JAVA, "antMergedPatterns"),
        new ResourceFilterString(filter, "customMergedPatterns"));
  }

  // no ant, catchAll filter is null
  public void testNonEmptyFilters() {
    AdvancedPaths advancedPaths = new AdvancedPaths();

    ResourceFilter filter = null;
    // pass empty includeArray. Matches everything that is not excluded.
    filter = getFilterWithoutCatchAll(EMPTY_ARRAY, mergedPatterns, NOT_JAVA);
    advancedPaths.testAdvancedPath(getAntFilter(EMPTY_ARRAY, mergedPatterns,
        DEFAULT_EXCLUDES, NOT_JAVA, "ant_emptyArray_mergedPatterns"),
        new ResourceFilterString(filter, "custom_emptyArray_mergedPatterns"));

    // pass empty excludeArray. Matches everything that is included.
    filter = getFilterWithoutCatchAll(mergedPatterns, EMPTY_ARRAY, NOT_JAVA);
    advancedPaths.testAdvancedPath(getAntFilter(mergedPatterns, EMPTY_ARRAY,
        DEFAULT_EXCLUDES, NOT_JAVA, "ant_mergedPatterns_emptyArray"),
        new ResourceFilterString(filter, "custom_mergedPatterns_emptyArray"));

    // pass non-empty include and exclude array. Matches nothing
    filter = getFilterWithoutCatchAll(mergedPatterns, mergedPatterns, NOT_JAVA);
    advancedPaths.testAdvancedPath(
        getAntFilter(mergedPatterns, mergedPatterns, DEFAULT_EXCLUDES,
            NOT_JAVA, "ant_mergedPatterns_mergedPatterns"),
        new ResourceFilterString(filter, "custom_mergedPatterns_mergedPatterns"));
  }

  // finish, catchAll filter is not-null
  public void testNonEmptyFiltersAnt() {
    AdvancedPaths advancedPaths = new AdvancedPaths();

    ResourceFilter filter = null;
    // pass empty includeArray. Matches everything that is not excluded.
    filter = getFilterWithCatchAll(EMPTY_ARRAY, mergedPatterns, NOT_JAVA);
    advancedPaths.testAdvancedPathAnt(getAntFilter(EMPTY_ARRAY, mergedPatterns,
        DEFAULT_EXCLUDES, NOT_JAVA, "ant_emptyArray_mergedPatterns"),
        new ResourceFilterString(filter, "custom_emptyArray_mergedPatterns"));

    // pass empty excludeArray. Matches everything that is included.
    filter = getFilterWithCatchAll(mergedPatterns, EMPTY_ARRAY, NOT_JAVA);
    advancedPaths.testAdvancedPathAnt(getAntFilter(mergedPatterns, EMPTY_ARRAY,
        DEFAULT_EXCLUDES, NOT_JAVA, "ant_emptyArray_mergedPatterns"),
        new ResourceFilterString(filter, "custom_emptyArray_mergedPatterns"));

    // pass non-empty include and exclude array. Matches nothing
    filter = getFilterWithCatchAll(mergedPatterns, mergedPatterns, NOT_JAVA);
    advancedPaths.testAdvancedPathAnt(getAntFilter(mergedPatterns,
        mergedPatterns, DEFAULT_EXCLUDES, NOT_JAVA,
        "ant_mergedPatterns_mergedPatterns"), new ResourceFilterString(filter,
        "custom_mergedPatterns_mergedPatterns"));
  }

  // no ant, catchAll filter is null
  public void testNonEmptyJavaFilters() {
    AdvancedPaths advancedPaths = new AdvancedPaths();

    String newMergedPatterns[] = new String[mergedPatterns.length];
    for (int i = 0; i < mergedPatterns.length; i++) {
      if (mergedPatterns[i].endsWith("*")) {
        newMergedPatterns[i] = mergedPatterns[i];
      } else {
        newMergedPatterns[i] = mergedPatterns[i] + ".java";
      }
    }
    ResourceFilter filter = null;
    // pass empty includeArray. Means catch all
    filter = getFilterWithoutCatchAll(EMPTY_ARRAY, newMergedPatterns, YES_JAVA);
    advancedPaths.testAdvancedJavaPath(getAntFilter(EMPTY_ARRAY,
        newMergedPatterns, DEFAULT_EXCLUDES, YES_JAVA,
        "ant_emptyArray_newMergedPatterns"), new ResourceFilterString(filter,
        "custom_emptyArray_newMergedPatterns"));

    // pass empty excludeArray. Means catch only the pattern
    filter = getFilterWithoutCatchAll(newMergedPatterns, EMPTY_ARRAY, YES_JAVA);
    advancedPaths.testAdvancedJavaPath(getAntFilter(newMergedPatterns,
        EMPTY_ARRAY, DEFAULT_EXCLUDES, YES_JAVA,
        "ant_newMergedPatterns_emptyArray"), new ResourceFilterString(filter,
        "custom_newMergedPatterns_emptyArray"));

    // pass non-empty include and exclude array.
    filter = getFilterWithoutCatchAll(newMergedPatterns, newMergedPatterns,
        YES_JAVA);
    advancedPaths.testAdvancedJavaPath(getAntFilter(newMergedPatterns,
        newMergedPatterns, DEFAULT_EXCLUDES, YES_JAVA,
        "ant_newMergedPatterns_newMergedPatterns"), new ResourceFilterString(
        filter, "custom_newMergedPatterns_newMergedPatterns"));
  }

  public void testNonEmptyJavaFiltersAnt() {
    AdvancedPaths advancedPaths = new AdvancedPaths();

    String newMergedPatterns[] = new String[mergedPatterns.length];
    for (int i = 0; i < mergedPatterns.length; i++) {
      if (mergedPatterns[i].endsWith("*")) {
        newMergedPatterns[i] = mergedPatterns[i];
      } else {
        newMergedPatterns[i] = mergedPatterns[i] + ".java";
      }
    }
    ResourceFilter filter = null;
    // pass empty includeArray. Means catch all
    filter = getFilterWithCatchAll(EMPTY_ARRAY, newMergedPatterns, YES_JAVA);
    advancedPaths.testAdvancedJavaPathAnt(getAntFilter(EMPTY_ARRAY,
        newMergedPatterns, DEFAULT_EXCLUDES, YES_JAVA,
        "ant_emptyArray_newMergedPatterns"), new ResourceFilterString(filter,
        "custom_emptyArray_newMergedPatterns"));

    // pass empty excludeArray. Means catch only the pattern
    filter = getFilterWithCatchAll(newMergedPatterns, EMPTY_ARRAY, YES_JAVA);
    advancedPaths.testAdvancedJavaPathAnt(getAntFilter(newMergedPatterns,
        EMPTY_ARRAY, DEFAULT_EXCLUDES, YES_JAVA,
        "ant_newMergedPatterns_emptyArray"), new ResourceFilterString(filter,
        "custom_newMergedPatterns_emptyArray"));

    // pass non-empty include and exclude array.
    filter = getFilterWithCatchAll(newMergedPatterns, newMergedPatterns,
        YES_JAVA);
    advancedPaths.testAdvancedJavaPathAnt(getAntFilter(newMergedPatterns,
        newMergedPatterns, DEFAULT_EXCLUDES, YES_JAVA,
        "ant_newMergedPatterns_newMergedPatterns"), new ResourceFilterString(
        filter, "custom_newMergedPatterns_newMergedPatterns"));
  }

  private ResourceFilterString getAntFilter(String includes[],
      String excludes[], boolean defaultExcludes, final boolean isJava,
      String tag) {
    final ZipScanner scanner = DefaultFilters.getScanner(includes, excludes,
        defaultExcludes, true);
    return new ResourceFilterString(new ResourceFilter() {
      public boolean allows(String path) {
        if (isJava && !path.endsWith(".java")) {
          return false;
        }
        return scanner.match(path);
      }
    }, tag != null ? tag : "includes: " + Arrays.toString(includes)
        + ", excludes: " + Arrays.toString(excludes));
  }

  private ResourceFilter getFilterWithCatchAll(String includesList[],
      String excludesList[], boolean isJava) {
    if (isJava) {
      return new DefaultFilters().customJavaFilter(includesList, excludesList,
          true, true);
    }
    return new DefaultFilters().customResourceFilter(includesList,
        excludesList, true, true);
  }

  // caseSensitive and excludeDefaults are set
  private ResourceFilter getFilterWithoutCatchAll(String includesList[],
      String excludesList[], boolean isJava) {
    return new DefaultFilters().customFilterWithCatchAll(includesList,
        excludesList, true, null, isJava);
  }
}
