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
 * filterList has "common" patterns. Exception: When pattern or path ends in
 * '/', we defer to ant.
 * 
 * TODO: This code could be made more general and cleaner by removing the
 * dependency on Ant completely. All ant patterns could be compiled into
 * reg-exps. That could also make the code faster. Plus, at several places,
 * Ant's documentation seems to be incomplete. Instead, perhaps, we should
 * specify our own rules for writing patterns.
 */
public class DefaultFilters {

  private static final boolean IS_EXCLUDES = false;
  private static final boolean IS_INCLUDES = true;
  private static final boolean NOT_JAVA = false;
  private static final boolean YES_JAVA = true;

  static ZipScanner getScanner(String[] includeList, String[] excludeList,
      boolean defaultExcludes, boolean caseSensitive) {
    /*
     * Hijack Ant's ZipScanner to handle inclusions/exclusions exactly as Ant
     * does. We're only using its pattern-matching capabilities; the code path
     * I'm using never tries to hit the filesystem in Ant 1.6.5.
     */
    ZipScanner scanner = new ZipScanner();
    if (includeList.length > 0) {
      scanner.setIncludes(includeList);
    }
    if (excludeList.length > 0) {
      scanner.setExcludes(excludeList);
    }
    if (defaultExcludes) {
      scanner.addDefaultExcludes();
    }
    scanner.setCaseSensitive(caseSensitive);
    scanner.init();

    return scanner;
  }

  /* used when defaultExcludes is true */
  final ResourceFilter defaultResourceFilter = new ResourceFilter() {

    public boolean allows(String path) {
      return defaultAntIncludes.allows(path)
          && !defaultExcludesPattern.matcher(path).matches();
    }
  };

  /* used when defaultExcludes is true */
  final ResourceFilter defaultJavaFilter = new ResourceFilter() {

    public boolean allows(String path) {
      return justJavaFilter.allows(path)
          && !defaultJavaExcludesPattern.matcher(path).matches();
    }

  };
  /* used when defaultExcludes is false */
  final ResourceFilter justResourceFilter = new ResourceFilter() {

    public boolean allows(String path) {
      return defaultAntIncludes.allows(path);
    }
  };

  /* used when defaultExcludes is false */
  final ResourceFilter justJavaFilter = new ResourceFilter() {

    public boolean allows(String path) {
      return defaultAntIncludes.allows(path) && isJavaFile(path);
    }
  };

  private final Pattern defaultExcludesPattern;
  private final Pattern defaultJavaExcludesPattern;
  // \w (word character), ., $, /, -, *, ~, #, %
  private final Pattern antPattern = Pattern.compile("^[\\w\\.\\$/\\-\\*~#%]*$");

  // accepts all but paths starting with '/'. Default include list is '**'
  private final ResourceFilter defaultAntIncludes = new ResourceFilter() {
    public boolean allows(String path) {
      return path.charAt(0) != '/';
    }
  };

  private final ResourceFilter rejectAll = new ResourceFilter() {
    public boolean allows(String path) {
      return false;
    }
  };

  public DefaultFilters() {

    /*
     * list copied from {@link org.apache.tools.ant.DirectoryScanner}
     */
    String defaultExcludes[] = new String[] {
    // Miscellaneous typical temporary files
        "**/*~", "**/#*#", "**/.#*", "**/%*%", "**/._*",

        // CVS
        "**/CVS", "**/CVS/**",
        // to not hit the weird formatting error.
        "**/.cvsignore",

        // SCCS
        "**/SCCS", "**/SCCS/**",

        // Visual SourceSafe
        "**/vssver.scc",

        // Subversion
        "**/.svn", "**/.svn/**",

        // Mac
        "**/.DS_Store",};

    defaultExcludesPattern = getPatternFromAntStrings(defaultExcludes);

    String defaultExcludesJava[] = new String[] {
    // Miscellaneous typical temporary files
        "**/.#*", "**/._*",

        // CVS
        "**/CVS/**",

        // SCCS
        "**/SCCS/**",

        // Subversion
        "**/.svn/**",};
    defaultJavaExcludesPattern = getPatternFromAntStrings(defaultExcludesJava);
  }

  public ResourceFilter customJavaFilter(String includeList[],
      String excludeList[], boolean defaultExcludes, boolean caseSensitive) {
    return getCustomFilter(includeList, excludeList, defaultExcludes,
        caseSensitive, YES_JAVA);
  }

  public ResourceFilter customResourceFilter(String includeList[],
      String excludeList[], boolean defaultExcludes, boolean caseSensitive) {

    return getCustomFilter(includeList, excludeList, defaultExcludes,
        caseSensitive, NOT_JAVA);
  }

  /**
   * return a customResourceFiter that handles all the argument. If unable to
   * create a customResourceFilter that handles the arguments, catchAll is used
   * as the final ResourceFilter.
   */
  ResourceFilter customFilterWithCatchAll(final String includeList[],
      final String excludeList[], final boolean defaultExcludes,
      final ResourceFilter catchAll, final boolean isJava) {

    assert includeList.length > 0 || excludeList.length > 0;

    final ResourceFilter includeFilter = getFilterPart(includeList, IS_INCLUDES);
    final ResourceFilter excludeFilter = getFilterPart(excludeList, IS_EXCLUDES);

    if (includeFilter == null || excludeFilter == null) {
      return catchAll;
    }
    // another common-case
    ResourceFilter filter = new ResourceFilter() {
      public boolean allows(String path) {
        // do not handle the case when pattern ends in '/'
        if (path.endsWith("/")) {
          return catchAll.allows(path);
        }
        return isPathAllowedByDefaults(path, defaultExcludes, isJava)
            && includeFilter.allows(path) && !excludeFilter.allows(path);
      }

      private boolean isPathAllowedByDefaults(String path,
          boolean defaultExcludes, boolean isJava) {
        if (defaultExcludes) {
          return isJava ? !defaultJavaExcludesPattern.matcher(path).matches()
              && isJavaFile(path)
              : !defaultExcludesPattern.matcher(path).matches();
        }
        return isJava ? isJavaFile(path) : true;
      }
    };
    return filter;
  }

  ResourceFilter getCustomFilter(final String includeList[],
      final String excludeList[], final boolean defaultExcludes,
      final boolean caseSensitive, final boolean isJava) {
    if (includeList.length == 0 && excludeList.length == 0 && caseSensitive) {
      // optimize for the common case.
      return getMatchingDefaultFilter(defaultExcludes, isJava);
    }

    // don't create a catchAll in default cases
    ResourceFilter catchAll = new ResourceFilter() {
      ZipScanner scanner = getScanner(includeList, excludeList,
          defaultExcludes, caseSensitive);

      public boolean allows(String path) {
        if (isJava) {
          return isJavaFile(path) && scanner.match(path);
        }
        return scanner.match(path);
      }
    };

    // for now, don't handle case sensitivity
    if (!caseSensitive) {
      return catchAll;
    }
    return customFilterWithCatchAll(includeList, excludeList, defaultExcludes,
        catchAll, isJava);
  }

  ResourceFilter getFilterPart(final String list[], final boolean defaultValue) {
    if (list.length == 0) {
      return defaultValue ? defaultAntIncludes : rejectAll;
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
      public boolean allows(String path) {
        return pattern.matcher(path).matches();
      }
    };
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
  String getPatternFromAntPattern(String antPatternString) {
    if (!antPattern.matcher(antPatternString).matches()) {
      return null;
    }
    // do not handle patterns that have ***
    if (antPatternString.indexOf("***") != -1) {
      return null;
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
            /*
             * TODO: handle patterns that end in /. For now, ant's matching seem
             * inconsistent.
             * 
             * ant pattern = testing/, path = testing/foo, result = true.
             */
            if (i == length - 1) {
              return null;
            }
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

  /**
   * Obtain the appropriate resourceFilter based on defaultExcludes and isJava
   * values. Assumptions: caseSensitive = true,and the includesList and
   * excludesList are empty
   */
  private ResourceFilter getMatchingDefaultFilter(boolean defaultExcludes,
      boolean isJava) {
    if (defaultExcludes) {
      return isJava ? defaultJavaFilter : defaultResourceFilter;
    }
    return isJava ? justJavaFilter : justResourceFilter;
  }

  private Pattern getPatternFromAntStrings(String... antPatterns) {
    String patternStrings[] = new String[antPatterns.length];
    int count = 0;
    for (String antPatternString : antPatterns) {
      String patternString = getPatternFromAntPattern(antPatternString);
      if (patternString == null) {
        throw new RuntimeException("Unable to convert " + antPatternString
            + " to java code");
      }
      patternStrings[count++] = patternString;
    }
    return getPatternFromStrings(patternStrings);
  }

  private Pattern getPatternFromStrings(String... patterns) {
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

  private boolean isJavaFile(String path) {
    return path.endsWith(".java");
  }
}
