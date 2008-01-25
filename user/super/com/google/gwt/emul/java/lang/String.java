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

/**
 * Intrinsic string class.
 */
public final class String implements Comparable<String>, CharSequence, 
    Serializable {

  /**
   * Accesses need to be prefixed with ':' to prevent conflict with built-in
   * JavaScript properties.
   * 
   * @skip
   */
  static JavaScriptObject hashCache;

  public static String valueOf(boolean x) {
    return "" + x;
  };

  public static native String valueOf(char x) /*-{
    return String.fromCharCode(x);
  }-*/;

  public static String valueOf(char x[], int offset, int count) {
    int end = offset + count;
    __checkBounds(x.length, offset, end);
    return __valueOf(x, offset, end);
  }

  public static native String valueOf(char[] x) /*-{
    // Trick: fromCharCode is a vararg method, so we can use apply() to pass the
    // entire input in one shot.
    return String.fromCharCode.apply(null, x);
  }-*/;

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

  /**
   * @skip
   */
  static String _String() {
    return "";
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
  static String _String(String other) {
    return other;
  }

  private static native boolean __equals(String me, Object other) /*-{
    // Coerce me to a primitive string to force string comparison
    return String(me) == other;
  }-*/;

  private static native String __valueOf(char x[], int start, int end) /*-{
    // Trick: fromCharCode is a vararg method, so we can use apply() to pass the
    // entire input in one shot.
    x = x.slice(start, end);
    return String.fromCharCode.apply(null, x);
  }-*/;

  // CHECKSTYLE_ON

  public String() {
    // magic delegation to _String
    _String();
  }

  public String(char value[]) {
    // magic delegation to _String
    _String(value);
  }

  public String(char value[], int offset, int count) {
    // magic delegation to _String
    _String(value, offset, count);
  }

  public String(String other) {
    // magic delegation to _String
    _String(other);
  }

  public native char charAt(int index) /*-{ 
    return this.charCodeAt(index);
  }-*/;

  public int compareTo(String other) {
    if (__equals(this, other)) {
      return 0;
    }
    int thisLength = this.length();
    int otherLength = other.length();
    int length = Math.min(thisLength, otherLength);
    for (int i = 0; i < length; i++) {
      char thisChar = this.charAt(i);
      char otherChar = other.charAt(i);
      if (thisChar != otherChar) {
        return thisChar - otherChar;
      }
    }
    return thisLength - otherLength;
  }

  public native String concat(String str) /*-{
    return this + str;
  }-*/;

  public boolean contains(CharSequence s) {
    return indexOf(s.toString()) != -1;
  }

  public native boolean endsWith(String suffix) /*-{
    return (this.lastIndexOf(suffix) != -1)
       && (this.lastIndexOf(suffix) == (this.length - suffix.length));
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

  @Override
  public native int hashCode() /*-{
    var hashCache = @java.lang.String::hashCache;
    if (!hashCache) {
      hashCache = @java.lang.String::hashCache = {};
    }
  
    // Prefix needed to prevent conflict with built-in JavaScript properties.
    var key  = ':' + this;
    var hashCode = hashCache[key];
    // Must check null/undefined because 0 is a legal hashCode
    if (hashCode == null) {
      hashCode = 0;
      var n = this.length;
      // In our hash code calculation, only 32 characters will actually affect
      // the final value, so there's no need to sample more than 32 characters.
      // To get a better hash code, we'd like to evenly distribute these
      // characters throughout the string.  That means that for lengths between
      // 0 and 63 (inclusive), we increment by 1.  For 64-95, 2; 96-127, 3; and
      // so on.  The complicated formula below computes just that.  The "| 0"
      // operation is a fast way to coerce the division result to an integer.
      var inc = (n < 64) ? 1 : ((n / 32) | 0);
      for (var i = 0; i < n; i += inc) {
        hashCode <<= 1;
        hashCode += this.charCodeAt(i);
      }
      hashCode |= 0; // force to 32-bits
      hashCache[key] = hashCode
    }
    return hashCode;
  }-*/;

  public native int indexOf(int ch) /*-{
    return this.indexOf(String.fromCharCode(ch));
  }-*/;

  public native int indexOf(int ch, int startIndex) /*-{
    return this.indexOf(String.fromCharCode(ch), startIndex);
  }-*/;

  public native int indexOf(String str) /*-{
    return this.indexOf(str);
  }-*/;

  public native int indexOf(String str, int startIndex) /*-{
    return this.indexOf(str, startIndex);
  }-*/;

  public native int lastIndexOf(int ch) /*-{
    return this.lastIndexOf(String.fromCharCode(ch));
  }-*/;

  public native int lastIndexOf(int ch, int startIndex) /*-{
    return this.lastIndexOf(String.fromCharCode(ch), startIndex);
  }-*/;

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
   */
  public native boolean matches(String regex) /*-{
    var matchObj = new RegExp(regex).exec(this);
    // if there is no match at all, matchObj will be null 
    // matchObj[0] is the entire matched string
    return (matchObj == null) ? false : (this == matchObj[0]);
  }-*/;

  public native String replace(char from, char to) /*-{
    var code = @java.lang.Long::toHexString(J)(from);
    return this.replace(RegExp("\\x" + code, "g"), String.fromCharCode(to));
  }-*/;

  /**
   * Regular expressions vary from the standard implementation. The
   * <code>regex</code> parameter is interpreted by JavaScript as a JavaScript
   * regular expression. For consistency, use only the subset of regular
   * expression syntax common to both Java and JavaScript.
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
    while(true) {
      // None of the information in the match returned are useful as we have no 
      // subgroup handling
      var matchObj = compiled.exec(trail);
      if( matchObj == null || trail == "" || 
        (count == (maxMatch - 1) && maxMatch > 0)) {
        out[count] = trail;
        break;
      } else {
        out[count] = trail.substring(0,matchObj.index);
        trail = trail.substring(matchObj.index + matchObj[0].length, trail.length);
        // Force the compiled pattern to reset internal state
        compiled.lastIndex = 0;
        // Only one zero length match per character to ensure termination
        if (lastTrail == trail) {  
          out[count] = trail.substring(0,1);
          trail = trail.substring(1);          
        }
        lastTrail = trail;
        count++;
      }
    }
    // all blank delimiters at the end are supposed to disappear if maxMatch ==0
    if (maxMatch == 0) {
      for (var i = out.length - 1; i >= 0; i--) {
        if(out[i] != "") {
          out.splice(i + 1,out.length - (i + 1));
          break;
        }
      }
    }
    var jr = @java.lang.String::__createArray(I)(out.length);
    var i = 0;
    for(i = 0; i < out.length; ++i) {
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
    return this.substr(beginIndex, endIndex-beginIndex);
  }-*/;

  public char[] toCharArray() {
    int n = this.length();
    char[] charArr = new char[n];
    for (int i = 0; i < n; ++i) {
      charArr[i] = this.charAt(i);
    }
    return charArr;
  }

  public native String toLowerCase() /*-{
    return this.toLowerCase();
  }-*/;

  @Override
  public String toString() {
    return this;
  }

  public native String toUpperCase() /*-{
    return this.toUpperCase();
  }-*/;

  public native String trim() /*-{
    if(this.length == 0 || (this[0] > '\u0020' && this[this.length-1] > '\u0020')) {
      return this;
    }
    var r1 = this.replace(/^(\s*)/, '');
    var r2 = r1.replace(/\s*$/, '');
    return r2;
  }-*/;

}
