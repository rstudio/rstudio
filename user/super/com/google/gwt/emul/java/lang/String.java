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

/**
 * Notes: For efficiency we handle String in a specialized way, in fact, a
 * java.lang.String is actually implemented as a native JavaScript String. Then
 * we just load up the prototype of the JavaScript String object with the
 * appropriate instance methods.
 */
package java.lang;

import com.google.gwt.core.client.JavaScriptObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;

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

  /**
   * Hashcode caching for strings.
   */
  static final class HashCache {
    /**
     * The "old" cache; it will be dumped when front is full.
     */
    static JavaScriptObject back = JavaScriptObject.createObject();
    /**
     * Tracks the number of entries in front.
     */
    static int count = 0;
    /**
     * The "new" cache; it will become back when it becomes full.
     */
    static JavaScriptObject front = JavaScriptObject.createObject();
    /**
     * Pulled this number out of thin air.
     */
    static final int MAX_CACHE = 256;

    public static native int getHashCode(String str) /*-{
      // Accesses must to be prefixed with ':' to prevent conflict with built-in
      // JavaScript properties.
      var key = ':' + str;

      // Check the front store.
      var result = @java.lang.String.HashCache::front[key];
      if (result != null) {
        return result;
      }

      // Check the back store.
      result = @java.lang.String.HashCache::back[key];
      if (result == null) {
        // Compute the value.
        result = @java.lang.String.HashCache::compute(Ljava/lang/String;)(str);
      }
      // Increment can trigger the swap/flush; call after checking back but
      // before writing to front.
      @java.lang.String.HashCache::increment()();
      return @java.lang.String.HashCache::front[key] = result;
    }-*/;

    static int compute(String str) {
      int hashCode = 0;
      int n = str.length();
      int nBatch = n - 4;
      int i = 0;

      // Process batches of 4 characters at a time
      while (i < nBatch) {
        // Add the next 4 characters to the hash.
        // After every 4 characters, we force the result to fit into 32 bits
        // by doing a bitwise operation on it.
        hashCode = (str.charAt(i + 3)
            + 31 * (str.charAt(i + 2)
            + 31 * (str.charAt(i + 1)
            + 31 * (str.charAt(i)
            + 31 * hashCode)))) | 0;

        i += 4;
      }

      // Now process the leftovers
      while (i < n) {
        hashCode = hashCode * 31 + str.charAt(i++);
      }

      // TODO: make a JSNI call in case JDT gets smart about removing this
      // Do a final fitting to 32 bits
      return hashCode | 0;
    }

    static void increment() {
      if (count == MAX_CACHE) {
        back = front;
        front = JavaScriptObject.createObject();
        count = 0;
      }
      ++count;
    }
  }

  public static final Comparator<String> CASE_INSENSITIVE_ORDER = new Comparator<String>() {
    public int compare(String a, String b) {
      return a.compareToIgnoreCase(b);
    }
  };

  // names for standard character sets that are supported
  private static final String CHARSET_8859_1 = "ISO-8859-1";
  private static final String CHARSET_LATIN1 = "ISO-LATIN-1";
  private static final String CHARSET_UTF8 = "UTF-8";

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
    __checkBounds(x.length, offset, end);
    return __valueOf(x, offset, end);
  }

  public static String valueOf(char[] x) {
    return __valueOf(x, 0, x.length);
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
   * Checks that bounds are correct.
   *
   * @param legalCount the end of the legal range
   * @param start must be >= 0
   * @param end must be <= legalCount and must be >= start
   * @throw StringIndexOutOfBoundsException if the range is not legal
   * @skip
   */
  static void __checkBounds(int legalCount, int start, int end) {
    if (start < 0) {
      throw new StringIndexOutOfBoundsException(start);
    }
    if (end < start) {
      throw new StringIndexOutOfBoundsException(end - start);
    }
    if (end > legalCount) {
      throw new StringIndexOutOfBoundsException(end);
    }
  }

  /**
   * @skip
   */
  static String[] __createArray(int numElements) {
    return new String[numElements];
  }

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

  static native String __valueOf(char x[], int start, int end) /*-{
    // Work around function.prototype.apply call stack size limits.
    // Performance: http://jsperf.com/string-fromcharcode-test/13
    var s = "";
    for (var batchStart = start; batchStart < end;) { // increment in block
      var batchEnd = Math.min(batchStart + 10000, end);
      s += String.fromCharCode.apply(null, x.slice(batchStart, batchEnd));
      batchStart = batchEnd;
    }
    return s;
  }-*/;

  /**
   * @skip
   */
  static String _String() {
    return "";
  }

  /**
   * @skip
   */
  static String _String(byte[] bytes) {
    return _String(bytes, 0, bytes.length);
  }

  /**
   * @skip
   */
  static String _String(byte[] bytes, int ofs, int len) {
    return utf8ToString(bytes, ofs, len);
  }

  /**
   * @skip
   */
  static String _String(byte[] bytes, int ofs, int len, String charset)
      throws UnsupportedEncodingException {
    if (CHARSET_UTF8.equalsIgnoreCase(charset)) {
      return utf8ToString(bytes, ofs, len);
    } else if (CHARSET_8859_1.equalsIgnoreCase(charset) || CHARSET_LATIN1.equalsIgnoreCase(charset)) {
      return latin1ToString(bytes, ofs, len);
    } else {
      throw new UnsupportedEncodingException("Charset " + charset
          + " not supported");
    }
  }

  /**
   * @skip
   */
  static String _String(byte[] bytes, String charsetName)
      throws UnsupportedEncodingException {
    return _String(bytes, 0, bytes.length, charsetName);
  }

  /**
   * @skip
   */
  static String _String(char value[]) {
    return valueOf(value);
  }

  /**
   * @skip
   */
  static String _String(char value[], int offset, int count) {
    return valueOf(value, offset, count);
  }

  /**
   * @skip
   */
  static String _String(int[] codePoints, int offset, int count) {
    char[] chars = new char[count * 2];
    int charIdx = 0;
    while (count-- > 0) {
      charIdx += Character.toChars(codePoints[offset++], chars, charIdx);
    }
    return valueOf(chars, 0, charIdx);
  }

  /**
   * @skip
   */
  static String _String(String other) {
    return other;
  }

  /**
   * @skip
   */
  static String _String(StringBuffer sb) {
    return valueOf(sb);
  }

  /**
   * @skip
   */
  static String _String(StringBuilder sb) {
    return valueOf(sb);
  }

  private static native boolean __equals(String me, Object other) /*-{
    // Coerce me to a primitive string to force string comparison
    return String(me) == other;
  }-*/;

  // CHECKSTYLE_ON

  private static native int compareTo(String thisStr, String otherStr) /*-{
    // Coerce to a primitive string to force string comparison
    thisStr = String(thisStr);
    if (thisStr == otherStr) {
      return 0;
    }
    return thisStr < otherStr ? -1 : 1;
  }-*/;

  /**
   * Encode a single character in UTF8.
   *
   * @param bytes byte array to store character in
   * @param ofs offset into byte array to store first byte
   * @param codePoint character to encode
   * @return number of bytes consumed by encoding the character
   * @throws IllegalArgumentException if codepoint >= 2^26
   */
  private static int encodeUtf8(byte[] bytes, int ofs, int codePoint) {
    if (codePoint < (1 << 7)) {
      bytes[ofs] = (byte) (codePoint & 127);
      return 1;
    } else if (codePoint < (1 << 11)) {
      // 110xxxxx 10xxxxxx
      bytes[ofs++] = (byte) (((codePoint >> 6) & 31) | 0xC0);
      bytes[ofs] = (byte) ((codePoint & 63) | 0x80);
      return 2;
    } else if (codePoint < (1 << 16)) {
      // 1110xxxx 10xxxxxx 10xxxxxx
      bytes[ofs++] = (byte) (((codePoint >> 12) & 15) | 0xE0);
      bytes[ofs++] = (byte) (((codePoint >> 6) & 63) | 0x80);
      bytes[ofs] = (byte) ((codePoint & 63) | 0x80);
      return 3;
    } else if (codePoint < (1 << 21)) {
      // 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
      bytes[ofs++] = (byte) (((codePoint >> 18) & 7) | 0xF0);
      bytes[ofs++] = (byte) (((codePoint >> 12) & 63) | 0x80);
      bytes[ofs++] = (byte) (((codePoint >> 6) & 63) | 0x80);
      bytes[ofs] = (byte) ((codePoint & 63) | 0x80);
      return 4;
    } else if (codePoint < (1 << 26)) {
      // 111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
      bytes[ofs++] = (byte) (((codePoint >> 24) & 3) | 0xF8);
      bytes[ofs++] = (byte) (((codePoint >> 18) & 63) | 0x80);
      bytes[ofs++] = (byte) (((codePoint >> 12) & 63) | 0x80);
      bytes[ofs++] = (byte) (((codePoint >> 6) & 63) | 0x80);
      bytes[ofs] = (byte) ((codePoint & 63) | 0x80);
      return 5;
    }
    throw new IllegalArgumentException("Character out of range: " + codePoint);
  }

  private static native String fromCharCode(char ch) /*-{
    return String.fromCharCode(ch);
  }-*/;

  private static String fromCodePoint(int codePoint) {
    if (codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
      char hiSurrogate = Character.getHighSurrogate(codePoint);
      char loSurrogate = Character.getLowSurrogate(codePoint);
      return String.fromCharCode(hiSurrogate)
          + String.fromCharCode(loSurrogate);
    } else {
      return String.fromCharCode((char) codePoint);
    }
  }

  private static byte[] getBytesLatin1(String str) {
    int n = str.length();
    byte[] bytes = new byte[n];
    for (int i = 0; i < n; ++i) {
      bytes[i] = (byte) (str.charAt(i) & 255);
    }
    return bytes;
  }

  private static byte[] getBytesUtf8(String str) {
    // TODO(jat): consider using unescape(encodeURIComponent(bytes)) instead
    int n = str.length();
    int byteCount = 0;
    for (int i = 0; i < n; ) {
      int ch = str.codePointAt(i);
      i += Character.charCount(ch);
      if (ch < (1 << 7)) {
        byteCount++;
      } else if (ch < (1 << 11)) {
        byteCount += 2;
      } else if (ch < (1 << 16)) {
        byteCount += 3;
      } else if (ch < (1 << 21)) {
        byteCount += 4;
      } else if (ch < (1 << 26)) {
        byteCount += 5;
      }
    }
    byte[] bytes = new byte[byteCount];
    int out = 0;
    for (int i = 0; i < n; ) {
      int ch = str.codePointAt(i);
      i += Character.charCount(ch);
      out += encodeUtf8(bytes, out, ch);
    }
    return bytes;
  }

  private static String latin1ToString(byte[] bytes, int ofs, int len) {
    char[] chars = new char[len];
    for (int i = 0; i < len; ++i) {
      chars[i] = (char) (bytes[ofs + i] & 255);
    }
    return valueOf(chars);
  }

  private static native boolean regionMatches(String thisStr,
      boolean ignoreCase, int toffset, String other, int ooffset, int len) /*-{
    if (toffset < 0 || ooffset < 0 || len <= 0) {
      return false;
    }

    if (toffset + len > thisStr.length || ooffset + len > other.length) {
      return false;
    }

    var left = thisStr.substr(toffset, len);
    var right = other.substr(ooffset, len);

    if (ignoreCase) {
      left = left.toLowerCase();
      right = right.toLowerCase();
    }

    return left == right;
  }-*/;

  private static String utf8ToString(byte[] bytes, int ofs, int len) {
    // TODO(jat): consider using decodeURIComponent(escape(bytes)) instead
    int charCount = 0;
    for (int i = 0; i < len; ) {
      ++charCount;
      byte ch = bytes[ofs + i];
      if ((ch & 0xC0) == 0x80) {
        throw new IllegalArgumentException("Invalid UTF8 sequence");
      } else if ((ch & 0x80) == 0) {
        ++i;
      } else if ((ch & 0xE0) == 0xC0) {
        i += 2;
      } else if ((ch & 0xF0) == 0xE0) {
        i += 3;
      } else if ((ch & 0xF8) == 0xF0) {
        i += 4;
      } else {
        // no 5+ byte sequences since max codepoint is less than 2^21
        throw new IllegalArgumentException("Invalid UTF8 sequence");
      }
      if (i > len) {
        throw new IndexOutOfBoundsException("Invalid UTF8 sequence");
      }
    }
    char[] chars = new char[charCount];
    int outIdx = 0;
    int count = 0;
    for (int i = 0; i < len; ) {
      int ch = bytes[ofs + i++];
      if ((ch & 0x80) == 0) {
        count = 1;
        ch &= 127;
      } else if ((ch & 0xE0) == 0xC0) {
        count = 2;
        ch &= 31;
      } else if ((ch & 0xF0) == 0xE0) {
        count = 3;
        ch &= 15;
      } else if ((ch & 0xF8) == 0xF0) {
        count = 4;
        ch &= 7;
      } else if ((ch & 0xFC) == 0xF8) {
        count = 5;
        ch &= 3;
      }
      while (--count > 0) {
        byte b = bytes[ofs + i++];
        if ((b & 0xC0) != 0x80) {
          throw new IllegalArgumentException("Invalid UTF8 sequence at "
              + (ofs + i - 1) + ", byte=" + Integer.toHexString(b));
        }
        ch = (ch << 6) | (b & 63);
      }
      outIdx += Character.toChars(ch, chars, outIdx);
    }
    return valueOf(chars);
  }

  public String() {
    // magic delegation to _String
    _String();
  }

  public String(byte[] bytes) {
    // magic delegation to _String
    _String(bytes);
  }

  public String(byte[] bytes, int ofs, int len) {
    // magic delegation to _String
    _String(bytes, ofs, len);
  }

  public String(byte[] bytes, int ofs, int len, String charsetName)
      throws UnsupportedEncodingException {
    // magic delegation to _String
    _String(bytes, ofs, len, charsetName);
  }

  public String(byte[] bytes, String charsetName)
      throws UnsupportedEncodingException {
    // magic delegation to _String
    _String(bytes, charsetName);
  }

  public String(char value[]) {
    // magic delegation to _String
    _String(value);
  }

  public String(char value[], int offset, int count) {
    // magic delegation to _String
    _String(value, offset, count);
  }

  public String(int codePoints[], int offset, int count) {
    // magic delegation to _String
    _String(codePoints, offset, count);
  }

  public String(String other) {
    // magic delegation to _String
    _String(other);
  }

  public String(StringBuffer sb) {
    // magic delegation to _String
    _String(sb);
  }

  public String(StringBuilder sb) {
    // magic delegation to _String
    _String(sb);
  }

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

  public native boolean endsWith(String suffix) /*-{
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
  }-*/;

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof String)) {
      return false;
    }
    return __equals(this, other);
  }

  public native boolean equalsIgnoreCase(String other) /*-{
    if (other == null)
      return false;
    return (this == other) || (this.toLowerCase() == other.toLowerCase());
  }-*/;

  public byte[] getBytes() {
    // default character set for GWT is UTF-8
    return getBytesUtf8(this);
  }

  public byte[] getBytes(String charSet) throws UnsupportedEncodingException {
    if (CHARSET_UTF8.equalsIgnoreCase(charSet)) {
      return getBytesUtf8(this);
    }
    if (CHARSET_8859_1.equalsIgnoreCase(charSet) || CHARSET_LATIN1.equalsIgnoreCase(charSet)) {
      return getBytesLatin1(this);
    }
    throw new UnsupportedEncodingException(charSet + " is not supported");
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
   * The devirtualizer {@link JsoDevirtualizer} will insert a trampoline that uses this
   * implementation.
   */
  @Override
  public Class<? extends Object> getClass() {
    return String.class;
  }

  @Override
  public int hashCode() {
    return HashCache.getHashCode(this);
  }

  public int indexOf(int codePoint) {
    return indexOf(fromCodePoint(codePoint));
  }

  public int indexOf(int codePoint, int startIndex) {
    return this.indexOf(String.fromCodePoint(codePoint), startIndex);
  }

  public native int indexOf(String str) /*-{
    return this.indexOf(str);
  }-*/;

  public native int indexOf(String str, int startIndex) /*-{
    return this.indexOf(str, startIndex);
  }-*/;

  public native String intern() /*-{
    return String(this);
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
    return regionMatches(this, ignoreCase, toffset, other, ooffset, len);
  }

  public boolean regionMatches(int toffset, String other, int ooffset, int len) {
    if (other == null) {
      throw new NullPointerException();
    }
    return regionMatches(this, false, toffset, other, ooffset, len);
  }

  public native String replace(char from, char to) /*-{

    // We previously used \\uXXXX, but Safari 2 doesn't match them properly
// in RegExp
    // See http://bugs.webkit.org/show_bug.cgi?id=8043
    //     http://bugs.webkit.org/show_bug.cgi?id=6257
    //     http://bugs.webkit.org/show_bug.cgi?id=7253
    var regex;
    if (from < 256) {
      regex = @java.lang.Integer::toHexString(I)(from);
      regex = '\\x' + "00".substring(regex.length) + regex;
    } else {
      // this works because characters above 255 can't be regex special chars
      regex = String.fromCharCode(from);
    }
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
    return indexOf(prefix) == 0;
  }

  public boolean startsWith(String prefix, int toffset) {
    if (toffset < 0 || toffset >= length()) {
      return false;
    } else {
      return indexOf(prefix, toffset) == toffset;
    }
  }

  public CharSequence subSequence(int beginIndex, int endIndex) {
    return this.substring(beginIndex, endIndex);
  }

  public native String substring(int beginIndex) /*-{
    return this.substr(beginIndex, this.length - beginIndex);
  }-*/;

  public native String substring(int beginIndex, int endIndex) /*-{
    return this.substr(beginIndex, endIndex - beginIndex);
  }-*/;

  public char[] toCharArray() {
    int n = this.length();
    char[] charArr = new char[n];
    getChars(0, n, charArr, 0);
    return charArr;
  }

  public native String toLowerCase() /*-{
    return this.toLowerCase();
  }-*/;

  @Override
  public String toString() {
    /*
     * Magic: this method is only used during compiler optimizations; the generated JS will instead alias
     * this method to the native String.prototype.toString() function.
     */
    return this;
  }

  public native String toUpperCase() /*-{
    return this.toUpperCase();
  }-*/;

  public native String trim() /*-{
    if (this.length == 0 || (this[0] > '\u0020' && this[this.length - 1] > '\u0020')) {
      return this;
    }
    return this.replace(/^[\u0000-\u0020]*|[\u0000-\u0020]*$/g, '');
  }-*/;
}
