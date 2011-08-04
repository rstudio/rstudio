/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.regexp.shared;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for regular expressions with features like Javascript's RegExp, plus
 * Javascript String's replace and split methods (which can take a RegExp
 * parameter). The pure Java implementation (for server-side use) uses Java's
 * Pattern class, unavailable under GWT. The super-sourced GWT implementation
 * simply calls on to the native Javascript classes.
 * <p>
 * There are a few small incompatibilities between the two implementations.
 * Java-specific constructs in the regular expression syntax (e.g. [a-z&&[^bc]],
 * (?<=foo), \A, \Q) work only in the pure Java implementation, not the GWT
 * implementation, and are not rejected by either. Also, the Javascript-specific
 * constructs $` and $' in the replacement expression work only in the GWT
 * implementation, not the pure Java implementation, which rejects them.
 */
public class RegExp {

  // In JS syntax, a \ in the replacement string has no special meaning.
  // In Java syntax, a \ in the replacement string escapes the next character,
  // so we have to translate \ to \\ before passing it to Java.
  private static final Pattern REPLACEMENT_BACKSLASH = Pattern.compile("\\\\");
  // To get \\, we have to say \\\\\\\\:
  // \\\\\\\\ --> Java string unescape --> \\\\
  // \\\\ ---> Pattern replacement unescape in replacement preprocessing --> \\
  private static final String REPLACEMENT_BACKSLASH_FOR_JAVA = "\\\\\\\\";

  // In JS syntax, a $& in the replacement string stands for the whole match.
  // In Java syntax, the equivalent is $0, so we have to translate $& to
  // $0 before passing it to Java. However, we have to watch out for $$&, which
  // is actually a Javascript $$ (see below) followed by a & with no special
  // meaning, and must not get translated.
  private static final Pattern REPLACEMENT_DOLLAR_AMPERSAND =
      Pattern.compile("((?:^|\\G|[^$])(?:\\$\\$)*)\\$&");
  private static final String REPLACEMENT_DOLLAR_AMPERSAND_FOR_JAVA = "$1\\$0";

  // In JS syntax, a $` and $' in the replacement string stand for everything
  // before the match and everything after the match.
  // In Java syntax, there is no equivalent, so we detect and reject $` and $'.
  // However, we have to watch out for $$` and $$', which are actually a JS $$
  // (see below) followed by a ` or ' with no special meaning, and must not be
  // rejected.
  private static final Pattern REPLACEMENT_DOLLAR_APOSTROPHE =
      Pattern.compile("(?:^|[^$])(?:\\$\\$)*\\$[`']");

  // In JS syntax, a $$ in the replacement string stands for a (single) dollar
  // sign, $.
  // In Java syntax, the equivalent is \$, so we have to translate $$ to \$
  // before passing it to Java.
  private static final Pattern REPLACEMENT_DOLLAR_DOLLAR =
      Pattern.compile("\\$\\$");
  // To get \$, we have to say \\\\\\$:
  // \\\\\\$ --> Java string unescape --> \\\$
  // \\\$ ---> Pattern replacement unescape in replacement preprocessing --> \$
  private static final String REPLACEMENT_DOLLAR_DOLLAR_FOR_JAVA = "\\\\\\$";

  /**
   * Creates a regular expression object from a pattern with no flags.
   *
   * @param pattern the Javascript regular expression pattern to compile
   * @return a new regular expression
   * @throws RuntimeException if the pattern is invalid
   */
  public static RegExp compile(String pattern) {
    return compile(pattern, "");
  }

  /**
   * Creates a regular expression object from a pattern using the given flags.
   *
   * @param pattern the Javascript regular expression pattern to compile
   * @param flags the flags string, containing at most one occurrence of {@code
   *        'g'} ({@link #getGlobal()}), {@code 'i'} ({@link #getIgnoreCase()}),
   *        or {@code 'm'} ({@link #getMultiline()}).
   * @return a new regular expression
   * @throws RuntimeException if the pattern or the flags are invalid
   */
  public static RegExp compile(String pattern, String flags) {
    // Parse flags
    boolean globalFlag = false;
    int javaPatternFlags = Pattern.UNIX_LINES;
    for (char flag : parseFlags(flags)) {
      switch (flag) {
        case 'g':
          globalFlag = true;
          break;

        case 'i':
          javaPatternFlags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
          break;

        case 'm':
          javaPatternFlags |= Pattern.MULTILINE;
          break;

        default:
          throw new IllegalArgumentException("Unknown regexp flag: '" + flag
              + "'");
      }
    }

    Pattern javaPattern = Pattern.compile(pattern, javaPatternFlags);

    return new RegExp(pattern, javaPattern, globalFlag);
  }

  /**
   * Parses a flags string as a set of characters. Does not reject unknown
   * flags.
   *
   * @param flags the flag string to parse
   * @return a set of flags
   * @throws IllegalArgumentException if a flag is duplicated
   */
  private static Set<Character> parseFlags(String flags) {
    Set<Character> flagsSet = new HashSet<Character>(flags.length());
    for (int flagIndex = 0; flagIndex < flags.length(); flagIndex++) {
      char flag = flags.charAt(flagIndex);
      if (!flagsSet.add(flag)) {
        throw new IllegalArgumentException("Flag cannot be specified twice: '"
            + flag + "'");
      }
    }
    return flagsSet;
  }

  private final boolean globalFlag;

  private int lastIndex;

  private final Pattern pattern;

  private final String source;

  private RegExp(String source, Pattern pattern, boolean globalFlag) {
    this.source = source;
    this.pattern = pattern;
    this.globalFlag = globalFlag;
    lastIndex = 0;
  }

  /**
   * Applies the regular expression to the given string. This call affects the
   * value returned by {@link #getLastIndex()} if the global flag is set.
   *
   * @param input the string to apply the regular expression to
   * @return a match result if the string matches, else {@code null}
   */
  public MatchResult exec(String input) {
    // Start the search at lastIndex if the global flag is true.
    int searchStartIndex = (globalFlag) ? lastIndex : 0;

    Matcher matcher;
    if (input == null || searchStartIndex < 0
        || searchStartIndex > input.length()) {
      // Avoid exceptions: Javascript is more tolerant than Java
      matcher = null;
    } else {
      matcher = pattern.matcher(input);
      if (!matcher.find(searchStartIndex)) {
        matcher = null;
      }
    }

    if (matcher != null) {
      // Match: create a result

      // Retrieve the matched groups.
      int groupCount = matcher.groupCount();
      List<String> groups = new ArrayList<String>(1 + groupCount);
      for (int group = 0; group <= groupCount; group++) {
        groups.add(matcher.group(group));
      }

      if (globalFlag) {
        lastIndex = matcher.end();
      }

      return new MatchResult(matcher.start(), input, groups);
    } else {
      // No match
      if (globalFlag) {
        lastIndex = 0;
      }
      return null;
    }
  }

  /**
   * Returns whether the regular expression captures all occurrences of the
   * pattern.
   */
  public boolean getGlobal() {
    return globalFlag;
  }

  /**
   * Returns whether the regular expression ignores case.
   */
  public boolean getIgnoreCase() {
    return (pattern.flags() & Pattern.CASE_INSENSITIVE) != 0;
  }

  /**
   * Returns the zero-based position at which to start the next match. The
   * return value is not defined if the global flag is not set. After a call
   * to {@link #exec(String)} or {@link #test(String)}, this method returns
   * the next position following the most recent match.
   *
   * @see #getGlobal()
   */
  public int getLastIndex() {
    return lastIndex;
  }

  /**
   * Returns whether '$' and '^' match line returns ('\n' and '\r') in addition
   * to the beginning or end of the string.
   */
  public boolean getMultiline() {
    return (pattern.flags() & Pattern.MULTILINE) != 0;
  }

  /**
   * Returns the pattern string of the regular expression.
   */
  public String getSource() {
    return source;
  }

  /**
   * Returns the input string with the part(s) matching the regular expression
   * replaced with the replacement string. If the global flag is set, replaces
   * all matches of the regular expression. Otherwise, replaces the first match
   * of the regular expression. As per Javascript semantics, backslashes in the
   * replacement string get no special treatment, but the replacement string can
   * use the following special patterns:
   * <ul>
   * <li>$1, $2, ... $99 - inserts the n'th group matched by the regular
   * expression.
   * <li>$&amp; - inserts the entire string matched by the regular expression.
   * <li>$$ - inserts a $.
   * </ul>
   * Note: $` and $' are *not* supported in the pure Java implementation, and
   * throw an exception.
   *
   * @param input the string in which the regular expression is to be searched.
   * @param replacement the replacement string.
   * @return the input string with the regular expression replaced by the
   *         replacement string.
   * @throws RuntimeException if {@code replacement} is invalid
   */
  public String replace(String input, String replacement) {
    // Replace \ in the replacement with \\ to escape it for Java replace.
    replacement = REPLACEMENT_BACKSLASH.matcher(replacement).replaceAll(
        REPLACEMENT_BACKSLASH_FOR_JAVA);

    // Replace the Javascript-ese $& in the replacement with Java-ese $0, but
    // watch out for $$&, which should stay $$&, to be changed to \$& below.
    replacement = REPLACEMENT_DOLLAR_AMPERSAND.matcher(replacement).replaceAll(
        REPLACEMENT_DOLLAR_AMPERSAND_FOR_JAVA);

    // Test for Javascript-ese $` and $', which we do not support in the pure
    // Java version.
    if (REPLACEMENT_DOLLAR_APOSTROPHE.matcher(replacement).find()) {
      throw new UnsupportedOperationException(
          "$` and $' replacements are not supported");
    }

    // Replace the Javascript-ese $$ in the replacement with Java-ese \$.
    replacement = REPLACEMENT_DOLLAR_DOLLAR.matcher(replacement).replaceAll(
        REPLACEMENT_DOLLAR_DOLLAR_FOR_JAVA);

    return globalFlag ? pattern.matcher(input).replaceAll(replacement)
        : pattern.matcher(input).replaceFirst(replacement);
  }

  /**
   * Sets the zero-based position at which to start the next match.
   */
  public void setLastIndex(int lastIndex) {
    this.lastIndex = lastIndex;
  }

  /**
   * Splits the input string around matches of the regular expression. If the
   * regular expression is completely empty, splits the input string into its
   * constituent characters. If the regular expression is not empty but matches
   * an empty string, the results are not well defined.
   *
   * @param input the string to be split.
   * @return the strings split off, any of which may be empty.
   */
  public SplitResult split(String input) {
    return split(input, -1);
  }

  /**
   * Splits the input string around matches of the regular expression. If the
   * regular expression is completely empty, splits the input string into its
   * constituent characters. If the regular expression is not empty but matches
   * an empty string, the results are not well defined.
   *
   * Note: There are some browser inconsistencies with this implementation, as
   * it is delegated to the browser, and no browser follows the spec completely.
   * A major difference is that IE will exclude empty strings in the result.
   *
   * @param input the string to be split.
   * @param limit the the maximum number of strings to split off and return,
   *        ignoring the rest of the input string. If negative, there is no
   *        limit.
   * @return the strings split off, any of which may be empty.
   */
  public SplitResult split(String input, int limit) {
    String[] result;
    if (source.length() == 0) {
      // Javascript split using a completely empty regular expression splits the
      // string into its constituent characters.
      int resultLength = input.length();
      if (resultLength > limit && limit >= 0) {
        resultLength = limit;
      }
      result = new String[resultLength];
      for (int i = 0; i < resultLength; i++) {
        result[i] = input.substring(i, i + 1);
      }
    } else {
      result = pattern.split(input, limit < 0 ? -1 : (limit + 1));
      if (result.length > limit && limit >= 0) {
        // Chop off the unsplit part of the string which has been put in
        // result[limit]. Javascript split does not return it.
        String[] realResult = new String[limit];
        for (int i = 0; i < limit; i++) {
          realResult[i] = result[i];
        }
        result = realResult;
      }
    }
    return new SplitResult(result);
  }

  /**
   * Determines if the regular expression matches the given string. This call
   * affects the value returned by {@link #getLastIndex()} if the global flag is
   * set. Equivalent to: {@code exec(input) != null}
   *
   * @param input the string to apply the regular expression to
   * @return whether the regular expression matches the given string.
   */
  public boolean test(String input) {
    return exec(input) != null;
  }
}
