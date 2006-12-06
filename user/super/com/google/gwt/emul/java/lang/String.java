/*
 * Copyright 2006 Google Inc.
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

/**
 * Intrinsic string class.
 */
public final class String implements Comparable, CharSequence {

  // CHECKSTYLE_OFF: This class has special needs.

  /**
   * @skip
   */
  protected static Object hashCache;

  public static native String valueOf(boolean x) /*-{ return x ? "true" : "false"; }-*/;

  public static native String valueOf(char x) /*-{ return String.fromCharCode(x); }-*/;

  public static String valueOf(char x[], int offset, int count) {
    if (offset < 0) {
      throw new StringIndexOutOfBoundsException(offset);
    }
    if (count < 0) {
      throw new StringIndexOutOfBoundsException(count);
    }
    if (offset > x.length - count) {
      throw new StringIndexOutOfBoundsException(offset + count);
    }

    String s = "";
    int stop = offset + count;
    while (offset < stop) {
      s += Character.toString(x[offset++]);
    }
    return s;
  }

  public static String valueOf(char[] x) {
    return valueOf(x, 0, x.length);
  }

  public static native String valueOf(double x) /*-{ return x.toString(); }-*/;

  public static native String valueOf(float x) /*-{ return x.toString(); }-*/;

  public static native String valueOf(int x) /*-{ return x.toString(); }-*/;

  public static native String valueOf(long x) /*-{ return x.toString(); }-*/;

  public static String valueOf(Object x) {
    return x != null ? x.toString() : "null";
  }

  /**
   * @skip
   */
  protected static String _String() {
    return "";
  }

  /**
   * @skip
   */
  protected static String _String(char value[]) {
    return valueOf(value);
  }

  /**
   * @skip
   */
  protected static String _String(char value[], int offset, int count) {
    return valueOf(value, offset, count);
  }

  /**
   * @skip
   */
  protected static String _String(String other) {
    return other;
  }

  static String[] __createArray(int numElements) {
    return new String[numElements];
  }

  /*
   * This method converts Java-escaped dollar signs "\$" into JavaScript-escaped
   * dollar signs "$$", and removes all other lone backslashes, which serve as
   * escapes in Java but are passed through literally in JavaScript.
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

  private static native boolean __equals(String me, Object other) /*-{
    // Coerce me to a primitive string to force string comparison
    return me.toString() == other;
  }-*/;

  private static native int __hashCode(String me) /*-{
    var hashCode = @java.lang.String::hashCache[me];
    if (hashCode) {
      return hashCode;
    }
   
    hashCode = 0;
    var len = me.length;
    var i = len;
    while (--i >= 0) {
      hashCode <<= 1;
      hashCode += me.charCodeAt(i);
    }
    @java.lang.String::hashCache[me] = hashCode;
    return hashCode;
  }-*/;

  private static native void __initHashCache() /*-{
    @java.lang.String::hashCache = {};
  }-*/;

  public String() {
    // magic delegation to _String
  }

  public String(char value[]) {
    // magic delegation to _String
  }

  public String(char value[], int offset, int count) {
    // magic delegation to _String
  }

  public String(String other) {
    // magic delegation to _String
  }

  public native char charAt(int index) /*-{ 
    return this.charCodeAt(index);
  }-*/;

  public int compareTo(Object other) {
    if (other instanceof String) {
      return this.compareTo((String) other);
    } else {
      throw new ClassCastException("Cannot compare " + other + " with String '"
          + this + "'");
    }
  }

  public int compareTo(String other) {
    int length = Math.min(this.length(), other.length());
    for (int i = 0; i < length; i++) {
      if (this.charAt(i) != other.charAt(i)) {
        return this.charAt(i) - other.charAt(i);
      }
    }
    return this.length() - other.length();
  }

  public native String concat(String other) /*-{
    return this+other;
  }-*/;

  public native boolean endsWith(String suffix) /*-{
    return this.lastIndexOf(suffix) != -1 && (this.lastIndexOf(suffix) == (this.length - suffix.length));
  }-*/;

  public boolean equals(Object other) {
    if (!(other instanceof String))
      return false;
    return __equals(this, other);
  }

  public native boolean equalsIgnoreCase(String other) /*-{
    if (other == null)
      return false;
    return (this == other) || (this.toLowerCase() == other.toLowerCase());
  }-*/;

  public int hashCode() {
    return __hashCode(this);
  }

  public native int indexOf(int ch) /*-{
    return this.indexOf(String.fromCharCode(ch));
  }-*/;

  public native int indexOf(int ch, int startIndex) /*-{
    return this.indexOf(String.fromCharCode(ch), startIndex);
  }-*/;

  public native int indexOf(String str) /*-{
    return this.indexOf(str);
  }-*/;

  public native int indexOf(String other, int startIndex) /*-{
    return this.indexOf(other, startIndex);
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
    if (toffset < 0 || toffset >= length())
      return false;
    else
      return indexOf(prefix, toffset) == toffset;
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
    for (int i = 0; i < n; ++i)
      charArr[i] = this.charAt(i);
    return charArr;
  }

  public native String toLowerCase() /*-{
    return this.toLowerCase();
  }-*/;

  public String toString() {
    return this;
  }

  public native String toUpperCase() /*-{
    return this.toUpperCase();
  }-*/;

  public native String trim() /*-{
    var r1 = this.replace(/^(\s*)/, '');
    var r2 = r1.replace(/\s*$/, '');
    return r2;
  }-*/;

  static {
    String.__initHashCache();
  }

  // CHECKSTYLE_ON
}
