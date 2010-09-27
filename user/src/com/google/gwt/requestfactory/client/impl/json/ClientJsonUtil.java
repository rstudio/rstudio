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
package com.google.gwt.requestfactory.client.impl.json;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.requestfactory.client.impl.ProxyJsoImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Direct port of json2.js at http://www.json.org/json2.js to GWT.
 */
public class ClientJsonUtil {

  /**
   * Callback invoked during a RegExp replace for each match. The return value
   * is used as a substitution into the matched string.
   */
  private interface RegExpReplacer {

    String replace(String match);
  }

  private static class StringifyJsonVisitor extends JsonVisitor {

    private static final Set<String> skipKeys;

    static {
      Set<String> toSkip = new HashSet<String>();
      toSkip.add("$H");
      toSkip.add("__gwt_ObjectId");
      toSkip.add(ProxyJsoImpl.REQUEST_FACTORY_FIELD);
      toSkip.add(ProxyJsoImpl.SCHEMA_FIELD);
      skipKeys = Collections.unmodifiableSet(toSkip);
    }

    private String indentLevel;

    private Set<JavaScriptObject> visited;

    private final String indent;

    private final StringBuffer sb;

    private final boolean pretty;

    public StringifyJsonVisitor(String indent, StringBuffer sb,
        boolean pretty) {
      this.indent = indent;
      this.sb = sb;
      this.pretty = pretty;
      indentLevel = indent;
      visited = new HashSet<JavaScriptObject>();
    }

    @Override
    public void endVisit(JsArray array, JsonContext ctx) {
      if (pretty) {
        indentLevel = indentLevel.substring(0,
            indentLevel.length() - indent.length());
        sb.append('\n');
        sb.append(indentLevel);
      }
      sb.append("]");
    }

    @Override
    public void endVisit(JsonMap object, JsonContext ctx) {
      if (pretty) {
        indentLevel = indentLevel.substring(0,
            indentLevel.length() - indent.length());
        sb.append('\n');
        sb.append(indentLevel);
      }
      sb.append("}");
    }

    @Override
    public void visit(double number, JsonContext ctx) {
      sb.append(Double.isInfinite(number) ? "null" : format(number));
    }

    @Override
    public void visit(String string, JsonContext ctx) {
      sb.append(quote(string));
    }

    @Override
    public void visit(boolean bool, JsonContext ctx) {
      sb.append(bool);
    }

    @Override
    public boolean visit(JsArray array, JsonContext ctx) {
      checkCycle(array);
      sb.append("[");
      if (pretty) {
        sb.append('\n');
        indentLevel += indent;
        sb.append(indentLevel);
      }
      return true;
    }

    @Override
    public boolean visit(JsonMap object, JsonContext ctx) {
      checkCycle(object);
      sb.append("{");
      if (pretty) {
        sb.append('\n');
        indentLevel += indent;
        sb.append(indentLevel);
      }
      return true;
    }

    @Override
    public boolean visitIndex(int index, JsonContext ctx) {
      commaIfNotFirst(ctx);
      return true;
    }

    @Override
    public boolean visitKey(String key, JsonContext ctx) {
      if ("".equals(key)) {
        return true;
      }
      // skip properties injected by GWT runtime on JSOs
      if (skipKeys.contains(key)) {
        return false;
      }
      commaIfNotFirst(ctx);
      sb.append(quote(key) + ":");
      if (pretty) {
        sb.append(' ');
      }
      return true;
    }

    @Override
    public void visitNull(JsonContext ctx) {
      sb.append("null");
    }

    private void checkCycle(JavaScriptObject array) {
      if (visited.contains(array)) {
        throw new JsonException("Cycled detected during stringify");
      } else {
        visited.add(array);
      }
    }

    private void commaIfNotFirst(JsonContext ctx) {
      if (!ctx.isFirst()) {
        sb.append(",");
        if (pretty) {
          sb.append('\n');
          sb.append(indentLevel);
        }
      }
    }

    private String format(double number) {
      String n = String.valueOf(number);
      if (n.endsWith(".0")) {
        n = n.substring(0, n.length() - 2);
      }
      return n;
    }
  }

  /**
   * Convert special control characters into unicode escape format.
   */
  public static String escapeControlChars(String text) {
    RegExp controlChars = RegExp.compile(
        "[\\u0000\\u00ad\\u0600-\\u0604\\u070f\\u17b4\\u17b5\\u200c-\\u200f"
            + "\\u2028-\\u202f\\u2060-\\u206f\\ufeff\\ufff0-\\uffff]", "g");
    return replace(controlChars, text, new RegExpReplacer() {
      public String replace(String match) {
        return escapeStringAsUnicode(match);
      }
    });
  }

  /**
   * Safely parse a Json formatted String.
   *
   * @param text the json formatted string
   * @return a JavaScriptObject representing a parsed Json string
   */
  public static JavaScriptObject parse(String text) {
    /*
     * Parsing happens in three stages. In the first stage, we replace certain
     * Unicode characters with escape sequences. JavaScript handles many characters
     * incorrectly, either silently deleting them, or treating them as line endings.
     */
    text = escapeControlChars(text);
    /*
     *In the second stage, we run the text against regular expressions that look
     * for non-JSON patterns. We are especially concerned with '()' and 'new'
     * because they can cause invocation, and '=' because it can cause mutation.
     * But just to be safe, we want to reject all unexpected forms.
     */
    /*
     * We split the second stage into 4 regexp operations in order to work around
     * crippling inefficiencies in IE's and Safari's regexp engines. First we
     * replace the JSON backslash pairs with '@' (a non-JSON character). Second, we
     * replace all simple value tokens with ']' characters. Third, we delete all
     * open brackets that follow a colon or comma or that begin the text. Finally,
     * we look to see that the remaining characters are only whitespace or ']' or
     * ',' or ':' or '{' or '}'. If that is so, then the text is safe for eval.
     */
    String chktext = text.replaceAll(
        "\\\\(?:[\"\\\\\\/bfnrt]|u[0-9a-fA-F]{4})", "@");
    chktext = chktext.replaceAll(
        "\"[^\"\\\\\\n\\r]*\"|true|false|null|-?\\d+(?:\\.\\d*)?(?:[eE][+\\-]?\\d+)?",
        "]");
    chktext = chktext.replaceAll("(?:^|:|,)(?:\\s*\\[)+", "");

    if (chktext.matches("^[\\],:{}\\s]*$")) {
      /*
      * In the third stage we use the eval function to compile the text into a
      * JavaScript structure. The '{' operator is subject to a syntactic ambiguity
      * in JavaScript: it can begin a block or an object literal. We wrap the text
      * in parens to eliminate the ambiguity.
      */
      return eval('(' + text + ')');
    }
    throw new JsonException(
        "Unable to parse " + text + " illegal JSON format.");
  }

  /**
   * Converts a JSO to Json format.
   *
   * @param jso javascript object to stringify
   * @return json formatted string
   */
  public static String stringify(JavaScriptObject jso) {
    return stringify(jso, 0);
  }

  /**
   * Converts a JSO to Json format.
   *
   * @param jso    javascript object to stringify
   * @param spaces number of spaces to indent in pretty print mode
   * @return json formatted string
   */
  public static String stringify(JavaScriptObject jso, int spaces) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < spaces; i++) {
      sb.append(' ');
    }
    return stringify(jso, sb.toString());
  }

  /**
   * Converts a JSO to Json format.
   *
   * @param jso    javascript object to stringify
   * @param indent optional indention prefix for pretty printing
   * @return json formatted string
   */
  public static String stringify(JavaScriptObject jso, final String indent) {
    final StringBuffer sb = new StringBuffer();
    final boolean isPretty = indent != null && !"".equals(indent);

    new StringifyJsonVisitor(indent, sb, isPretty).accept(jso);
    boolean isArray = isArray(jso);
    char openChar = isArray ? '[' : '{';
    char closeChar = isArray ? ']' : '}';
    return openChar + (isPretty ? "\n" + indent : "") + sb.toString()
        + (isPretty ? "\n" : "") + closeChar;
  }

  static native boolean isArray(JavaScriptObject obj) /*-{
    return Object.prototype.toString.apply(obj) === '[object Array]';
  }-*/;

  /**
   * Safely escape an arbitrary string as a JSON string literal.
   */
  static String quote(String value) {
    RegExp escapeable = RegExp.compile(
        "[\\\\\\\"\\x00-\\x1f\\x7f-\\x9f\\u00ad\\u0600-\\u0604\\u070f\\u17b4"
            + "\\u17b5\\u200c-\\u200f\\u2028-\\u202f\\u2060-\\u206f\\ufeff"
            + "\\ufff0-\\uffff]", "g");
    if (escapeable.test(value)) {
      return "\"" + replace(escapeable, value, new RegExpReplacer() {
        public String replace(String match) {
          char a = match.charAt(0);
          switch (a) {
            case '\b':
              return "\\b";
            case '\t':
              return "\\t";
            case '\n':
              return "\\n";
            case '\f':
              return "\\f";
            case '\r':
              return "\\r";
            case '"':
              return "\\\"";
            case '\\':
              return "\\\\";
            default:
              return escapeStringAsUnicode(match);
          }
        }
      }) + "\"";
    } else {
      return "\"" + value + "\"";
    }
  }

  /**
   * Turn a single unicode character into a 32-bit unicode hex literal.
   */
  private static String escapeStringAsUnicode(String match) {
    String hexValue = Integer.toString(match.charAt(0), 16);
    hexValue = hexValue.length() > 4 ? hexValue.substring(hexValue.length() - 4)
        : hexValue;
    return "\\u0000" + hexValue;
  }

  private static native JavaScriptObject eval(String text) /*-{
    return eval(text);
  }-*/;

  /**
   * Execute a regular expression and invoke a callback for each match
   * occurance. The return value of the callback is substituted for the match.
   *
   * @param expression a compiled regular expression
   * @param text       a String on which to perform replacement
   * @param replacer   a callback that maps matched strings into new values
   */
  private static String replace(RegExp expression, String text,
      RegExpReplacer replacer) {
    expression.setLastIndex(0);
    MatchResult mresult = expression.exec(text);
    StringBuffer toReturn = new StringBuffer();
    int lastIndex = 0;
    while (mresult != null) {
      toReturn.append(text.substring(lastIndex, mresult.getIndex()));
      toReturn.append(replacer.replace(mresult.getGroup(0)));
      lastIndex = mresult.getIndex() + 1;
      mresult = expression.exec(text);
    }
    toReturn.append(text.substring(lastIndex));
    return toReturn.toString();
  }
}
