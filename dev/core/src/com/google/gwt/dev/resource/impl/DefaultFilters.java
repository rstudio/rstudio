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

import org.apache.tools.ant.types.ZipScanner;

import java.util.regex.Pattern;

/**
 * A singleton class that provides blazingly fast implementation of the default
 * excludes of Ant's {@link org.apache.tools.ant.DirectoryScanner}, assuming
 * case-sensitiveness.
 *
 * TODO: this class needs to be revisited, when Gwt's Ant is upgraded.
 *
 * Currently, we do not go to ant if (a) the filterList is empty, or (b) the
 * filterList has "common" patterns. Exception: When path ends in '/', we defer
 * to ant.
 *
 * TODO: This code could be made more general and cleaner by removing the
 * dependency on Ant completely. All ant patterns could be compiled into
 * reg-exps. That could also make the code faster. Plus, at several places,
 * Ant's documentation seems to be incomplete. Instead, perhaps, we should
 * specify our own rules for writing patterns.
 */
public class DefaultFilters {

  /**
   * Constants to represent the type of files that will be filtered.
   */
  public static enum FilterFileType {
    RESOURCE_FILES(null), //
    JAVA_FILES(".java"), //
    CLASS_FILES(".class");

    private final String suffix;

    /* used when defaultExcludes is false */
    private final ResourceFilter justThisFileTypeFilter = new ResourceFilter() {
      @Override
      public boolean allows(String path) {
        return defaultAntIncludes.allows(path) && matches(path);
      }
    };

    private final ResourceFilter defaultFilter = new ResourceFilter() {

      @Override
      public boolean allows(String path) {
        return getFileTypeFilter().allows(path)
        && !isDefaultExcluded(path);
      }
    };

    private FilterFileType(String suffix) {
      this.suffix = suffix;
    }

    public ResourceFilter getDefaultFilter() {
      return defaultFilter;
    }

    /* used when defaultExcludes is false */
    public ResourceFilter getFileTypeFilter() {
      return justThisFileTypeFilter;
    }

    public String getSuffix() {
      return suffix;
    }

    public boolean matches(String path) {
      if (suffix == null) {
        return true;
      }
      return path.endsWith(suffix);
    }
  }

  // \w (word character), ., $, /, -, *, ~, #, %
  private static final Pattern antPattern = Pattern.compile("^[\\w\\.\\$/\\-\\*~#%]*$");

  // accepts all but paths starting with '/'. Default include list is '**'
  private static final ResourceFilter defaultAntIncludes = new ResourceFilter() {
    @Override
    public boolean allows(String path) {
      return path.charAt(0) != '/';
    }
  };

  /**
   * @return <code>true</code> if given path should be excluded from resources.
   */
  private static boolean isDefaultExcluded(String path) {
    // CVS
    if (path.endsWith("/CVS") || path.contains("/CVS/") || path.startsWith("CVS/")
        || path.endsWith("/.cvsignore")) {
      return true;
    }
    // Subversion
    if (path.endsWith("/.svn") || path.contains("/.svn/") || path.startsWith(".svn/")
        || path.endsWith("/.svnignore")) {
      return true;
    }
    // Git
    if (path.endsWith("/.git") || path.contains("/.git/") || path.startsWith(".git/")
        || path.endsWith("/.gitignore")) {
      return true;
    }
    // SCCS
    if (path.endsWith("/SCCS") || path.contains("/SCCS/")) {
      return true;
    }
    // Visual SourceSafe
    if (path.endsWith("/vssver.scc")) {
      return true;
    }
    // Mac
    if (path.endsWith("/.DS_Store")) {
      return true;
    }
    return false;
  }

  /**
   * Returns a pattern string that can be passed in Java Pattern.compile(..).
   * For spec, see <a href="http://www.jajakarta.org/ant/ant-1.6.1/docs/ja/manual/api/org/apache/tools/ant/DirectoryScanner.html"
   * >DirectoryScanner</a> From the spec: There is a special case regarding the
   * use of File.separators at the beginning of the pattern and the string to
   * match: When a pattern starts with a File.separator, the string to match
   * must also start with a File.separator. When a pattern does not start with a
   * File.separator, the string to match may not start with a File.separator.
   *
   * </p>
   *
   * TODO: This method could accept all ant patterns, but then all characters
   * that have a special meaning in Java's regular expression would need to be
   * escaped.
   *
   * @param antPatternString the ant pattern String.
   * @return a pattern string that can be passed in Java's Pattern.compile(..),
   *         null if cannot process the pattern.
   */
  static String getPatternFromAntPattern(String antPatternString) {
    if (!antPattern.matcher(antPatternString).matches()) {
      return null;
    }
    // do not handle patterns that have ***
    if (antPatternString.indexOf("***") != -1) {
      return null;
    }
    if (antPatternString.endsWith("/")) {
      /*
       * From the DirectoryScanner.html spec: When a pattern ends with a '/' or
       * '\', "**" is appended. if ant pattern = testing/, path = testing/foo,
       * result = true.
       */
      antPatternString = antPatternString + "**";
    }
    StringBuffer sb = new StringBuffer();
    int length = antPatternString.length();
    for (int i = 0; i < length; i++) {
      char c = antPatternString.charAt(i);
      switch (c) {
        case '.':
          sb.append("\\.");
          break;
        case '$':
          sb.append("\\$");
          break;
        case '/':
          // convert /** to (/[^/]*)* except when / is the first char.
          if (i != 0 && i + 2 < length && antPatternString.charAt(i + 1) == '*'
              && antPatternString.charAt(i + 2) == '*') {
            sb.append("(/[^/]*)*");
            i += 2; // handled 2 more chars than usual
          } else {
            sb.append(c);
          }
          break;
        case '*':
          // ** to .*
          if (i + 1 < length && antPatternString.charAt(i + 1) == '*') {
            if (i + 2 < length && antPatternString.charAt(i + 2) == '/') {
              if (i == 0) {
                /*
                 * When a pattern does not start with a File.separator, the
                 * string to match may not start with a File.separator.
                 */
                sb.append("([^/]+/)*");
              } else {
                // convert **/ to ([^/]*/)*
                sb.append("([^/]*/)*");
              }
              i += 2;
            } else {
              if (i == 0) {
                /*
                 * When a pattern does not start with a File.separator, the
                 * string to match may not start with a File.separator.
                 */
                sb.append("([^/].*)*");
              } else {
                sb.append(".*");
              }
              i++;
            }
          } else {
            sb.append("[^/]*");
          }
          break;
        default:
          sb.append(c);
          break;
      }
    }
    return sb.toString();
  }

  static ZipScanner getScanner(String[] includeList, String[] excludeList,
      String[] skipList, boolean defaultExcludes, boolean caseSensitive) {
    /*
     * Hijack Ant's ZipScanner to handle inclusions/exclusions exactly as Ant
     * does. We're only using its pattern-matching capabilities; the code path
     * I'm using never tries to hit the filesystem in Ant 1.6.5.
     */
    ZipScanner scanner = new ZipScanner();
    if (includeList.length > 0) {
      scanner.setIncludes(includeList);
    }
    if (excludeList.length > 0 || skipList.length > 0) {
      String[] excludeOrSkip = concatenate(excludeList, skipList);
      scanner.setExcludes(excludeOrSkip);
    }
    if (defaultExcludes) {
      scanner.addDefaultExcludes();
    }
    scanner.setCaseSensitive(caseSensitive);
    scanner.init();

    return scanner;
  }

  private static String[] concatenate(String[] array1, String[] array2) {
    String[] answer = new String[array1.length + array2.length];
    int i = 0;
    for (String entry : array1) {
      answer[i++] = entry;
    }
    for (String entry : array2) {
      answer[i++] = entry;
    }
    return answer;
  }

  private static Pattern getPatternFromStrings(String... patterns) {
    StringBuffer entirePattern = new StringBuffer("^");
    int length = patterns.length;
    int count = 0;
    for (String pattern : patterns) {
      entirePattern.append("(" + pattern + ")");
      if (count < length - 1) {
        entirePattern.append("|");
      }
      count++;
    }
    entirePattern.append("$");
    return Pattern.compile(entirePattern.toString());
  }

  private final ResourceFilter rejectAll = new ResourceFilter() {
    @Override
    public boolean allows(String path) {
      return false;
    }
  };

  public ResourceFilter customClassFilesFilter(String includeList[],
      String excludeList[], String skipList[], boolean defaultExcludes,
      boolean caseSensitive) {
    return getCustomFilter(includeList, excludeList, skipList, defaultExcludes,
        caseSensitive, FilterFileType.CLASS_FILES);
  }

  public ResourceFilter customJavaFilter(String includeList[],
      String excludeList[], String skipList[], boolean defaultExcludes,
      boolean caseSensitive) {
    return getCustomFilter(includeList, excludeList, skipList, defaultExcludes,
        caseSensitive, FilterFileType.JAVA_FILES);
  }

  public ResourceFilter customResourceFilter(String includeList[],
      String excludeList[], String[] skipList, boolean defaultExcludes,
      boolean caseSensitive) {
    return getCustomFilter(includeList, excludeList, skipList, defaultExcludes,
        caseSensitive, FilterFileType.RESOURCE_FILES);
  }

  /**
   * Return a customResourceFiter that handles all the argument. If unable to
   * create a customResourceFilter that handles the arguments, catchAll is used
   * as the final ResourceFilter.
   */
  ResourceFilter customFilterWithCatchAll(final String includeList[],
      final String excludeList[], final String skipList[],
      final boolean defaultExcludes, final ResourceFilter catchAll,
      final FilterFileType filterFileType) {

    assert includeList.length > 0 || excludeList.length > 0
        || skipList.length > 0;

    final ResourceFilter includeFilter = getIncludesFilterPart(includeList);
    final ResourceFilter excludeFilter = getExcludesFilterPart(concatenate(
        excludeList, skipList));

    if (includeFilter == null || excludeFilter == null) {
      return catchAll;
    }
    // another common-case
    ResourceFilter filter = new ResourceFilter() {
      @Override
      public boolean allows(String path) {
        // do not handle the case when pattern ends in '/'
        if (path.endsWith("/")) {
          return catchAll.allows(path);
        }
        return isPathAllowedByDefaults(path, defaultExcludes, filterFileType)
            && includeFilter.allows(path) && !excludeFilter.allows(path);
      }

      private boolean isPathAllowedByDefaults(String path,
          boolean defaultExcludes, FilterFileType filterFileType) {
        boolean fileTypeMatch = filterFileType.matches(path);
        if (!fileTypeMatch) {
          return false;
        }
        if (defaultExcludes) {
          return !isDefaultExcluded(path);
        }
        return true;
      }
    };
    return filter;
  }

  ResourceFilter getCustomFilter(final String includeList[],
      final String excludeList[], final String skipList[],
      final boolean defaultExcludes, final boolean caseSensitive,
      final FilterFileType filterFileType) {
    if (includeList.length == 0 && excludeList.length == 0
        && skipList.length == 0 && caseSensitive) {
      // optimize for the common case.
      return getMatchingDefaultFilter(defaultExcludes, filterFileType);
    }

    // don't create a catchAll in default cases
    ResourceFilter catchAll = new ResourceFilter() {
      ZipScanner scanner = getScanner(includeList, excludeList, skipList,
          defaultExcludes, caseSensitive);

      @Override
      public boolean allows(String path) {
        return filterFileType.matches(path) && scanner.match(path);
      }
    };

    // for now, don't handle case sensitivity
    if (!caseSensitive) {
      return catchAll;
    }
    return customFilterWithCatchAll(includeList, excludeList, skipList,
        defaultExcludes, catchAll, filterFileType);
  }

  ResourceFilter getExcludesFilterPart(final String list[]) {
    return getFilterPart(list, false);
  }

  ResourceFilter getIncludesFilterPart(final String list[]) {
    return getFilterPart(list, true);
  }

  /**
   * @param list patterns to add to the filter.
   * @param isInclude Only used if the array is empty. If <code>true</code>
   *          treat this as an include. Otherwise, assume this is an excludes
   *          filter and exclude all files.
   * @return
   */
  private ResourceFilter getFilterPart(final String list[],
      final boolean isInclude) {
    if (list.length == 0) {
      return isInclude ? defaultAntIncludes : rejectAll;
    }

    String patternStrings[] = new String[list.length];
    int count = 0;
    for (String antPatternString : list) {
      String patternString = getPatternFromAntPattern(antPatternString);
      if (patternString == null) {
        return null;
      }
      patternStrings[count++] = patternString;
    }

    final Pattern pattern = getPatternFromStrings(patternStrings);
    return new ResourceFilter() {
      @Override
      public boolean allows(String path) {
        return pattern.matcher(path).matches();
      }
    };
  }

  /**
   * Obtain the appropriate resourceFilter based on defaultExcludes and isJava
   * values. Assumptions: caseSensitive = true,and the includesList and
   * excludesList are empty
   */
  private ResourceFilter getMatchingDefaultFilter(boolean defaultExcludes,
      FilterFileType filterFileType) {
    if (defaultExcludes) {
      return filterFileType.getDefaultFilter();
    }
    return filterFileType.getFileTypeFilter();
  }
}
