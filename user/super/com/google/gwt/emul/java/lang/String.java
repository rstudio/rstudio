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

package java.lang;

import static javaemul.internal.InternalPreconditions.checkStringBounds;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Comparator;
import java.util.Locale;

import javaemul.internal.ArrayHelper;
import javaemul.internal.EmulatedCharset;
import javaemul.internal.HashCodes;
import javaemul.internal.annotations.DoNotInline;

/**
 * Intrinsic string class.
 */

public final class String implements Comparable<String>, CharSequence,
    Serializable {
  /* TODO(jat): consider whether we want to support the following methods;
   *
   * <ul>
   * <li>deprecated methods dealing with bytes (I assume not since I can't see
   * much use for them)
   * <ul>
   * <li>String(byte[] ascii, int hibyte)
   * <li>String(byte[] ascii, int hibyte, int offset, int count)
   * <li>getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin)
   * </ul>
   * <li>methods which in JS will essentially do nothing or be the same as other
   * methods
   * <ul>
   * <li>copyValueOf(char[] data)
   * <li>copyValueOf(char[] data, int offset, int count)
   * </ul>
   * <li>methods added in Java 1.6 (the issue is how will it impact users
   * building against Java 1.5)
   * <ul>
   * <li>isEmpty()
   * </ul>
   * <li>other methods which are not straightforward in JS
   * <ul>
   * <li>format(String format, Object... args)
   * </ul>
   * </ul>
   *
   * <p>Also, in general, we need to improve our support of non-ASCII characters. The
   * problem is that correct support requires large tables, and we don't want to
   * make users who aren't going to use that pay for it. There are two ways to do
   * that:
   * <ol>
   * <li>construct the tables in such a way that if the corresponding method is
   * not called the table will be elided from the output.
   * <li>provide a deferred binding target selecting the level of compatibility
   * required. Those that only need ASCII (or perhaps a different relatively small
   * subset such as Latin1-5) will not pay for large tables, even if they do call
   * toLowercase(), for example.
   * </ol>
   *
   * Also, if we ever add multi-locale support, there are a number of other
   * methods such as toLowercase(Locale) we will want to consider supporting. This
   * is probably rare, but there will be some apps (such as a translation tool)
   * which cannot be written without this support.
   *
   * Another category of incomplete support is that we currently just use the JS
   * regex support, which is not exactly the same as Java. We should support Java
   * syntax by mapping it into equivalent JS patterns, or emulating them.
   *
   * IMPORTANT NOTE: if newer JREs add new interfaces to String, please update
   * {@link Devirtualizer} and {@link JavaResourceBase}
   */
  public static final Comparator<String> CASE_INSENSITIVE_ORDER = new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
      return a.compareToIgnoreCase(b);
    }
  };

  public static String copyValueOf(char[] v) {
    return valueOf(v);
  }

  public static String copyValueOf(char[] v, int offset, int count) {
    return valueOf(v, offset, count);
  }

  public static String valueOf(boolean x) {
    return "" + x;
  }

  public static native String valueOf(char x) /*-{
    return String.fromCharCode(x);
  }-*/;

  public static String valueOf(char x[], int offset, int count) {
    int end = offset + count;
    checkStringBounds(offset, end, x.length);
    // Work around function.prototype.apply call stack size limits:
    // https://code.google.com/p/v8/issues/detail?id=2896
    // Performance: http://jsperf.com/string-fromcharcode-test/13
    int batchSize = ArrayHelper.ARRAY_PROCESS_BATCH_SIZE;
    String s = "";
    for (int batchStart = offset; batchStart < end;) {
      int batchEnd = Math.min(batchStart + batchSize, end);
      s += fromCharCode(ArrayHelper.unsafeClone(x, batchStart, batchEnd));
      batchStart = batchEnd;
    }
    return s;
  }

  private static native String fromCharCode(Object array) /*-{
    return String.fromCharCode.apply(null, array);
  }-*/;

  public static String valueOf(char[] x) {
    return valueOf(x, 0, x.length);
  }

  public static String valueOf(double x) {
    return "" + x;
  }

  public static String valueOf(float x) {
    return "" + x;
  }

  public static String valueOf(int x) {
    return "" + x;
  }

  public static String valueOf(long x) {
    return "" + x;
  }

  public static String valueOf(Object x) {
    return "" + x;
  }

  // CHECKSTYLE_OFF: This class has special needs.

  /**
   * @skip
   */
  static String[] __createArray(int numElements) {
    return new String[numElements];
  }

  static native String __substr(String str, int beginIndex, int len) /*-{
    return str.substr(beginIndex, len);
  }-*/;

  /**
   * This method converts Java-escaped dollar signs "\$" into JavaScript-escaped
   * dollar signs "$$", and removes all other lone backslashes, which serve as
   * escapes in Java but are passed through literally in JavaScript.
   *
   * @skip
   */
  static String __translateReplaceString(String replaceStr) {
    int pos = 0;
    while (0 <= (pos = replaceStr.indexOf("\\", pos))) {
      if (replaceStr.charAt(pos + 1) == '$') {
        replaceStr = replaceStr.substring(0, pos) + "$"
            + replaceStr.substring(++pos);
      } else {
        replaceStr = replaceStr.substring(0, pos) + replaceStr.substring(++pos);
      }
    }
    return replaceStr;
  }

 

  // CHECKSTYLE_ON

  private static native int compareTo(String thisStr, String otherStr) /*-{
    if (thisStr == otherStr) {
      return 0;
    }
    return thisStr < otherStr ? -1 : 1;
  }-*/;

  private static Charset getCharset(String charsetName) throws UnsupportedEncodingException {
    try {
      return Charset.forName(charsetName);
    } catch (UnsupportedCharsetException e) {
      throw new UnsupportedEncodingException(charsetName);
    }
  }

  private static String fromCodePoint(int codePoint) {
    if (codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
      char hiSurrogate = Character.getHighSurrogate(codePoint);
      char loSurrogate = Character.getLowSurrogate(codePoint);
      return String.valueOf(hiSurrogate)
          + String.valueOf(loSurrogate);
    } else {
      return String.valueOf((char) codePoint);
    }
  }

  public String() {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString();
  }

  public String(byte[] bytes) {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(bytes);
  }

  public String(byte[] bytes, int ofs, int len) {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(bytes, ofs, len);
  }

  public String(byte[] bytes, int ofs, int len, String charsetName)
      throws UnsupportedEncodingException {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(bytes, ofs, len, charsetName);
  }

  public String(byte[] bytes, int ofs, int len, Charset charset) {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(bytes, ofs, len, charset);
  }

  public String(byte[] bytes, String charsetName)
      throws UnsupportedEncodingException {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(bytes, charsetName);
  }

  public String(byte[] bytes, Charset charset)
      throws UnsupportedEncodingException {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(bytes, charset);
  }

  public String(char value[]) {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(value);
  }

  public String(char value[], int offset, int count) {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(value, offset, count);
  }

  public String(int codePoints[], int offset, int count) {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(codePoints, offset, count);
  }

  public String(String other) {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(other);
  }

  public String(StringBuffer sb) {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(sb);
  }

  public String(StringBuilder sb) {
    /*
     * Call to $createString(args) must be here so that the method is referenced and not
     * pruned before new String(args) is replaced by $createString(args) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createString(sb);
  }

  @Override
  public native char charAt(int index) /*-{
    return this.charCodeAt(index);
  }-*/;

  public int codePointAt(int index) {
    return Character.codePointAt(this, index, length());
  }

  public int codePointBefore(int index) {
    return Character.codePointBefore(this, index, 0);
  }

  public int codePointCount(int beginIndex, int endIndex) {
    return Character.codePointCount(this, beginIndex, endIndex);
  }

  @Override
  public int compareTo(String other) {
    return compareTo(this, other);
  }

  public int compareToIgnoreCase(String other) {
    return compareTo(toLowerCase(), other.toLowerCase());
  }

  public native String concat(String str) /*-{
    return this + str;
  }-*/;

  public boolean contains(CharSequence s) {
    return indexOf(s.toString()) != -1;
  }

  public boolean contentEquals(CharSequence cs) {
    return equals(cs.toString());
  }

  public boolean contentEquals(StringBuffer sb) {
    return equals(sb.toString());
  }

  public boolean endsWith(String suffix) {
    // If IE8 supported negative start index, we could have just used "-suffixlength".
    int suffixlength = suffix.length();
    return __substr(this, length() - suffixlength, suffixlength).equals(suffix);
  }

  // Marked with @DoNotInline because we don't have static eval for "==" yet.
  @DoNotInline
  @Override
  public boolean equals(Object other) {
    // Java equality is translated into triple equality which is a quick to compare strings for
    // equality without any instanceOf checks.
    return this == other;
  }

  public native boolean equalsIgnoreCase(String other) /*-{
    if (other == null) {
      return false;
    }
    if (this == other) {
      return true;
    }
    return (this.length == other.length) && (this.toLowerCase() == other.toLowerCase());
  }-*/;

  public byte[] getBytes() {
    // default character set for GWT is UTF-8
    return getBytes(EmulatedCharset.UTF_8);
  }

  public byte[] getBytes(String charsetName) throws UnsupportedEncodingException {
    return getBytes(getCharset(charsetName));
  }

  public byte[] getBytes(Charset charset) {
    return ((EmulatedCharset) charset).getBytes(this);
  }

  public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
    for (int srcIdx = srcBegin; srcIdx < srcEnd; ++srcIdx) {
      dst[dstBegin++] = charAt(srcIdx);
    }
  }

  /**
   * Magic; JSODevirtualizer will use this implementation.<p>
   *
   * Each class gets a synthetic stubs for getClass at AST construction time with the exception of
   * Object, JavaScriptObject and subclasses and String; see {@link GwtAstBuilder.createMembers()}.
   * <p>
   *
   * These stubs are replaced in {@link ReplaceGetClassOverrides} by an access to field __clazz
   * which is initialized in each class prototype to point to the class literal. String is
   * implemented as a plain JavaScript string hence lacking said field.<p>
   *
   * The devirtualizer {@code JsoDevirtualizer} will insert a trampoline that uses this
   * implementation.
   */
  @Override
  public Class<? extends Object> getClass() {
    return String.class;
  }

  @Override
  public int hashCode() {
    return HashCodes.hashCodeForString(this);
  }

  public int indexOf(int codePoint) {
    return indexOf(fromCodePoint(codePoint));
  }

  public int indexOf(int codePoint, int startIndex) {
    return indexOf(fromCodePoint(codePoint), startIndex);
  }

  public native int indexOf(String str) /*-{
    return this.indexOf(str);
  }-*/;

  public native int indexOf(String str, int startIndex) /*-{
    return this.indexOf(str, startIndex);
  }-*/;

  public native String intern() /*-{
    return this;
  }-*/;

  public native boolean isEmpty() /*-{
    return !this.length;
  }-*/;

  public int lastIndexOf(int codePoint) {
    return lastIndexOf(fromCodePoint(codePoint));
  }

  public int lastIndexOf(int codePoint, int startIndex) {
    return lastIndexOf(fromCodePoint(codePoint), startIndex);
  }

  public native int lastIndexOf(String str) /*-{
    return this.lastIndexOf(str);
  }-*/;

  public native int lastIndexOf(String str, int start) /*-{
    return this.lastIndexOf(str, start);
  }-*/;

  @Override
  public native int length() /*-{
    return this.length;
  }-*/;

  /**
   * Regular expressions vary from the standard implementation. The
   * <code>regex</code> parameter is interpreted by JavaScript as a JavaScript
   * regular expression. For consistency, use only the subset of regular
   * expression syntax common to both Java and JavaScript.
   *
   * TODO(jat): properly handle Java regex syntax
   */
  public native boolean matches(String regex) /*-{
    // We surround the regex with '^' and '$' because it must match
    // the entire string.
    return new RegExp('^(' + regex + ')$').test(this);
  }-*/;

  public int offsetByCodePoints(int index, int codePointOffset) {
    return Character.offsetByCodePoints(this, index, codePointOffset);
  }

  public boolean regionMatches(boolean ignoreCase, int toffset, String other,
      int ooffset, int len) {
    if (other == null) {
      throw new NullPointerException();
    }
    if (toffset < 0 || ooffset < 0 || len <= 0) {
      return false;
    }
    if (toffset + len > this.length() || ooffset + len > other.length()) {
      return false;
    }

    String left = __substr(this, toffset, len);
    String right = __substr(other, ooffset, len);
    return ignoreCase ? left.equalsIgnoreCase(right) : left.equals(right);
  }

  public boolean regionMatches(int toffset, String other, int ooffset, int len) {
    return regionMatches(false, toffset, other, ooffset, len);
  }

  public native String replace(char from, char to) /*-{
    // Translate 'from' into unicode escape sequence (\\u and a four-digit hexadecimal number).
    // Escape sequence replacement is used instead of a string literal replacement
    // in order to escape regexp special characters (e.g. '.').
    var hex = @java.lang.Integer::toHexString(I)(from);
    var regex = "\\u" + "0000".substring(hex.length) + hex;
    return this.replace(RegExp(regex, "g"), String.fromCharCode(to));
  }-*/;

  public String replace(CharSequence from, CharSequence to) {
    // Implementation note: This uses a regex replacement instead of
    // a string literal replacement because Safari does not
    // follow the spec for "$$" in the replacement string: it
    // will insert a literal "$$". IE and Firefox, meanwhile,
    // treat "$$" as "$".

    // Escape regex special characters from literal replacement string.
    String regex = from.toString().replaceAll("([/\\\\\\.\\*\\+\\?\\|\\(\\)\\[\\]\\{\\}$^])", "\\\\$1");
    // Escape $ since it is for match backrefs and \ since it is used to escape
    // $.
    String replacement = to.toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\$");

    return replaceAll(regex, replacement);
  }

  /**
   * Regular expressions vary from the standard implementation. The
   * <code>regex</code> parameter is interpreted by JavaScript as a JavaScript
   * regular expression. For consistency, use only the subset of regular
   * expression syntax common to both Java and JavaScript.
   *
   * TODO(jat): properly handle Java regex syntax
   */
  public native String replaceAll(String regex, String replace) /*-{
    replace = @java.lang.String::__translateReplaceString(Ljava/lang/String;)(replace);
    return this.replace(RegExp(regex, "g"), replace);
  }-*/;

  /**
   * Regular expressions vary from the standard implementation. The
   * <code>regex</code> parameter is interpreted by JavaScript as a JavaScript
   * regular expression. For consistency, use only the subset of regular
   * expression syntax common to both Java and JavaScript.
   *
   * TODO(jat): properly handle Java regex syntax
   */
  public native String replaceFirst(String regex, String replace) /*-{
    replace = @java.lang.String::__translateReplaceString(Ljava/lang/String;)(replace);
    return this.replace(RegExp(regex), replace);
  }-*/;

  /**
   * Regular expressions vary from the standard implementation. The
   * <code>regex</code> parameter is interpreted by JavaScript as a JavaScript
   * regular expression. For consistency, use only the subset of regular
   * expression syntax common to both Java and JavaScript.
   */
  public String[] split(String regex) {
    return split(regex, 0);
  }

  /**
   * Regular expressions vary from the standard implementation. The
   * <code>regex</code> parameter is interpreted by JavaScript as a JavaScript
   * regular expression. For consistency, use only the subset of regular
   * expression syntax common to both Java and JavaScript.
   *
   * TODO(jat): properly handle Java regex syntax
   */
  public native String[] split(String regex, int maxMatch) /*-{
    // The compiled regular expression created from the string
    var compiled = new RegExp(regex, "g");
    // the Javascipt array to hold the matches prior to conversion
    var out = [];
    // how many matches performed so far
    var count = 0;
    // The current string that is being matched; trimmed as each piece matches
    var trail = this;
    // used to detect repeated zero length matches
    // Must be null to start with because the first match of "" makes no
    // progress by intention
    var lastTrail = null;
    // We do the split manually to avoid Javascript incompatibility
    while (true) {
      // None of the information in the match returned are useful as we have no
      // subgroup handling
      var matchObj = compiled.exec(trail);
      if (matchObj == null || trail == "" || (count == (maxMatch - 1) && maxMatch > 0)) {
        out[count] = trail;
        break;
      } else {
        out[count] = trail.substring(0, matchObj.index);
        trail = trail.substring(matchObj.index + matchObj[0].length, trail.length);
        // Force the compiled pattern to reset internal state
        compiled.lastIndex = 0;
        // Only one zero length match per character to ensure termination
        if (lastTrail == trail) {
          out[count] = trail.substring(0, 1);
          trail = trail.substring(1);
        }
        lastTrail = trail;
        count++;
      }
    }
    // all blank delimiters at the end are supposed to disappear if maxMatch == 0;
    // however, if the input string is empty, the output should consist of a
    // single empty string
    if (maxMatch == 0 && this.length > 0) {
      var lastNonEmpty = out.length;
      while (lastNonEmpty > 0 && out[lastNonEmpty - 1] == "") {
        --lastNonEmpty;
      }
      if (lastNonEmpty < out.length) {
        out.splice(lastNonEmpty, out.length - lastNonEmpty);
      }
    }
    var jr = @java.lang.String::__createArray(I)(out.length);
    for ( var i = 0; i < out.length; ++i) {
      jr[i] = out[i];
    }
    return jr;
  }-*/;

  public boolean startsWith(String prefix) {
    return startsWith(prefix, 0);
  }

  public boolean startsWith(String prefix, int toffset) {
    return toffset >= 0 && __substr(this, toffset, prefix.length()).equals(prefix);
  }

  @Override
  public CharSequence subSequence(int beginIndex, int endIndex) {
    return this.substring(beginIndex, endIndex);
  }

  public String substring(int beginIndex) {
    return __substr(this, beginIndex, this.length() - beginIndex);
  }

  public String substring(int beginIndex, int endIndex) {
    return __substr(this, beginIndex, endIndex - beginIndex);
  }

  public char[] toCharArray() {
    int n = this.length();
    char[] charArr = new char[n];
    getChars(0, n, charArr, 0);
    return charArr;
  }

  /**
   * Transforms the String to lower-case in a locale insensitive way.
   * <p>
   * Unlike JRE, we don't do locale specific transformation by default. That is backward compatible
   * for GWT and in most of the cases that is what the developer actually wants. If you want to make
   * a transformation based on native locale of the browser, you can do
   * {@code toLowerCase(Locale.getDefault())} instead.
   */
  public native String toLowerCase() /*-{
    return this.toLowerCase();
  }-*/;

  /**
   * Transforms the String to lower-case based on the native locale of the browser.
   */
  private native String toLocaleLowerCase() /*-{
    return this.toLocaleLowerCase();
  }-*/;

  /**
   * If provided {@code locale} is {@link Locale#getDefault()}, uses javascript's
   * {@code toLocaleLowerCase} to do a locale specific transformation. Otherwise, it will fallback
   * to {@code toLowerCase} which performs the right thing for the limited set of Locale's
   * predefined in GWT Locale emulation.
   */
  public String toLowerCase(Locale locale) {
    return locale == Locale.getDefault() ? toLocaleLowerCase() : toLowerCase();
  }

  // See the notes in lowerCase pair.
  public native String toUpperCase() /*-{
    return this.toUpperCase();
  }-*/;

  // See the notes in lowerCase pair.
  private native String toLocaleUpperCase() /*-{
    return this.toLocaleUpperCase();
  }-*/;

  // See the notes in lowerCase pair.
  public String toUpperCase(Locale locale) {
    return locale == Locale.getDefault() ? toLocaleUpperCase() : toUpperCase();
  }

  @Override
  public String toString() {
    /*
     * Magic: this method is only used during compiler optimizations; the generated JS will instead alias
     * this method to the native String.prototype.toString() function.
     */
    return this;
  }

  public String trim() {
    int length = length();
    int start = 0;
    while (start < length && charAt(start) <= ' ') {
      start++;
    }
    int end = length;
    while (end > start && charAt(end - 1) <= ' ') {
      end--;
    }
    return start > 0 || end < length ? substring(start, end) : this;
  }

  // CHECKSTYLE_OFF: Utility Methods for unboxed String.

  static String $createString() {
    return "";
  }

  static String $createString(byte[] bytes) {
    return $createString(bytes, 0, bytes.length);
  }

  static String $createString(byte[] bytes, int ofs, int len) {
    return $createString(bytes, ofs, len, EmulatedCharset.UTF_8);
  }

  static String $createString(byte[] bytes, int ofs, int len, String charsetName)
      throws UnsupportedEncodingException {
    return $createString(bytes, ofs, len, String.getCharset(charsetName));
  }

  static String $createString(byte[] bytes, int ofs, int len, Charset charset) {
    return String.valueOf(((EmulatedCharset) charset).decodeString(bytes, ofs, len));
  }

  static String $createString(byte[] bytes, String charsetName)
      throws UnsupportedEncodingException {
    return $createString(bytes, 0, bytes.length, charsetName);
  }

  static String $createString(byte[] bytes, Charset charset)
      throws UnsupportedEncodingException {
    return $createString(bytes, 0, bytes.length, charset.name());
  }

  static String $createString(char value[]) {
    return String.valueOf(value);
  }

  static String $createString(char value[], int offset, int count) {
    return String.valueOf(value, offset, count);
  }

  static String $createString(int[] codePoints, int offset, int count) {
    char[] chars = new char[count * 2];
    int charIdx = 0;
    while (count-- > 0) {
      charIdx += Character.toChars(codePoints[offset++], chars, charIdx);
    }
    return String.valueOf(chars, 0, charIdx);
  }

  static String $createString(String other) {
    return other;
  }

  static String $createString(StringBuffer sb) {
    return String.valueOf(sb);
  }

  static String $createString(StringBuilder sb) {
    return String.valueOf(sb);
  }
  // CHECKSTYLE_ON: end utility methods
}
