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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * GWT wrapper for the Javascript RegExp class extended with the Javascript
 * String class's replace and split methods, which can take a RegExp parameter.
 */
public class RegExp extends JavaScriptObject {

  /**
   * Creates a regular expression object from a pattern with no flags.
   *
   * @param pattern the Javascript regular expression pattern to compile
   * @return a new regular expression
   * @throws RuntimeException if the pattern is invalid
   */
  public static native RegExp compile(String pattern) /*-{
     return new RegExp(pattern);
   }-*/;

  /**
   * Creates a regular expression object from a pattern with no flags.
   *
   * @param pattern the Javascript regular expression pattern to compile
   * @param flags the flags string, containing at most one occurence of {@code
   *          'g'} ({@link #getGlobal()}), {@code 'i'} ({@link #getIgnoreCase()}
   *          ), or {@code 'm'} ({@link #getMultiline()}).
   * @return a new regular expression
   * @throws RuntimeException if the pattern or the flags are invalid
   */
  public static native RegExp compile(String pattern, String flags) /*-{
     return new RegExp(pattern, flags);
   }-*/;

  protected RegExp() {
  }

  /**
   * Applies the regular expression to the given string. This call affects the
   * value returned by {@link #getLastIndex()} if the global flag is set.
   *
   * @param input the string to apply the regular expression to
   * @return a match result if the string matches, else {@code null}
   */
  public final native MatchResult exec(String input) /*-{
     return this.exec(input);
   }-*/;

  /**
   * Returns whether the regular expression captures all occurences of the
   * pattern.
   */
  public final native boolean getGlobal() /*-{
    return this.global;
  }-*/;

  /**
   * Returns whether the regular expression ignores case.
   */
  public final native boolean getIgnoreCase() /*-{
    return this.ignoreCase;
  }-*/;

  /**
   * Returns the zero-based position at which to start the next match. The
   * return value is not defined if the global flag is not set. After a call
   * to {@link #exec(String)} or {@link #test(String)}, this method returns
   * the next position following the most recent match.
   *
   * @see #getGlobal()
   */
  public final native int getLastIndex() /*-{
     return this.lastIndex;
   }-*/;

  /**
   * Returns whether '$' and '^' match line returns ('\n' and '\r') in addition
   * to the beginning or end of the string.
   */
  public final native boolean getMultiline() /*-{
    return this.multiline;
  }-*/;

  /**
   * Returns the pattern string of the regular expression.
   */
  public final native String getSource() /*-{
     return this.source;
   }-*/;

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
   *
   * @param input the string in which the regular expression is to be searched.
   * @param replacement the replacement string.
   * @return the input string with the regular expression replaced with the
   *         replacement string.
   * @throws RuntimeException if {@code replacement} is invalid
   */
  public final native String replace(String input, String replacement) /*-{
     return input.replace(this, replacement);
   }-*/;

  /**
   * Sets the zero-based position at which to start the next match.
   */
  public final native void setLastIndex(int lastIndex) /*-{
     this.lastIndex = lastIndex;
   }-*/;

  /**
   * Splits the input string around matches of the regular expression. If the
   * regular expression is completely empty, splits the input string into its
   * constituent characters. If the regular expression is not empty but matches
   * an empty string, the results are not well defined.
   *
   * @param input the string to be split.
   *
   * @return the strings split off, any of which may be empty.
   */
  public final native SplitResult split(String input) /*-{
     return input.split(this);
   }-*/;

  /**
   * Splits the input string around matches of the regular expression. If the
   * regular expression is completely empty, splits the input string into its
   * constituent characters. If the regular expression is not empty but matches
   * an empty string, the results are not well defined.
   *
   * @param input the string to be split.
   * @param limit the the maximum number of strings to split off and return,
   *          ignoring the rest of the input string. If negative, there is no
   *          limit.
   *
   * @return the strings split off, any of which may be empty.
   */
  public final native SplitResult split(String input, int limit) /*-{
     return input.split(this, limit);
   }-*/;

  /**
   * Determines if the regular expression matches the given string. This call
   * affects the value returned by {@link #getLastIndex()} if the global flag is
   * not set. Equivalent to: {@code exec(input) != null}
   *
   * @param input the string to apply the regular expression to
   * @return whether the regular expression matches the given string.
   */
  public final native boolean test(String input) /*-{
     return this.test(input);
   }-*/;
}
