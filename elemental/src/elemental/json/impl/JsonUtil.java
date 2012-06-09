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
package elemental.json.impl;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.json.impl.JsonContext;

/**
 * Direct port of json2.js at http://www.json.org/json2.js to GWT.
 */
public class JsonUtil {

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
      skipKeys = Collections.unmodifiableSet(toSkip);
    }

    private String indentLevel;

    private Set<JsonValue> visited;

    private final String indent;

    private final StringBuffer sb;

    private final boolean pretty;

    public StringifyJsonVisitor(String indent, StringBuffer sb,
        boolean pretty) {
      this.indent = indent;
      this.sb = sb;
      this.pretty = pretty;
      indentLevel = "";
      visited = new HashSet<JsonValue>();
    }

    @Override
    public void endVisit(JsonArray array, JsonContext ctx) {
      if (pretty) {
        indentLevel = indentLevel
            .substring(0, indentLevel.length() - indent.length());
        sb.append('\n');
        sb.append(indentLevel);
      }
      sb.append("]");
      visited.remove(array);
    }

    @Override
    public void endVisit(JsonObject object, JsonContext ctx) {
      if (pretty) {
        indentLevel = indentLevel
            .substring(0, indentLevel.length() - indent.length());
        sb.append('\n');
        sb.append(indentLevel);
      }
      sb.append("}");
      visited.remove(object);
      assert !visited.contains(object);
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
    public boolean visit(JsonArray array, JsonContext ctx) {
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
    public boolean visit(JsonObject object, JsonContext ctx) {
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

    private void checkCycle(JsonValue value) {
      if (visited.contains(value)) {
        throw new JsonException("Cycled detected during stringify");
      } else {
        visited.add(value);
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
    StringBuffer toReturn = new StringBuffer();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (isControlChar(c)) {
        toReturn.append(escapeStringAsUnicode(String.valueOf(c)));
      } else {
        toReturn.append(c);
      }
    }
    return toReturn.toString();
  }

  public static <T extends JsonValue> T parse(String json) throws JsonException {
    return Json.instance().parse(json);
  }

  /**
   * Safely escape an arbitrary string as a JSON string literal.
   */
  public static String quote(String value) {
    StringBuffer toReturn = new StringBuffer();
    toReturn.append("\"");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);

      String toAppend = String.valueOf(c);
      switch (c) {
        case '\b':
          toAppend = "\\b";
          break;
        case '\t':
          toAppend = "\\t";
          break;
        case '\n':
          toAppend = "\\n";
          break;
        case '\f':
          toAppend = "\\f";
          break;
        case '\r':
          toAppend = "\\r";
          break;
        case '"':
          toAppend = "\\\"";
          break;
        case '\\':
          toAppend = "\\\\";
          break;
        default:
          if (isControlChar(c)) {
            toAppend = escapeStringAsUnicode(String.valueOf(c));
          }
      }
      toReturn.append(toAppend);
    }
    toReturn.append("\"");
    return toReturn.toString();
  }

  /**
   * Converts a Json Object to Json format.
   *
   * @param jsonValue  json object to stringify
   * @return json formatted string
   */
  public static String stringify(JsonValue jsonValue) {
    return stringify(jsonValue, 0);
  }

  /**
   * Converts a JSO to Json format.
   *
   * @param jsonValue    json object to stringify
   * @param spaces number of spaces to indent in pretty print mode
   * @return json formatted string
   */
  public static String stringify(JsonValue jsonValue, int spaces) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < spaces; i++) {
      sb.append(' ');
    }
    return stringify(jsonValue, sb.toString());
  }

  /**
   * Converts a Json object to Json formatted String.
   *
   * @param jsonValue    json object to stringify
   * @param indent optional indention prefix for pretty printing
   * @return json formatted string
   */
  public static String stringify(JsonValue jsonValue, final String indent) {
    final StringBuffer sb = new StringBuffer();
    final boolean isPretty = indent != null && !"".equals(indent);

    new StringifyJsonVisitor(indent, sb, isPretty).accept(jsonValue);
    return sb.toString();
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

  private static boolean isControlChar(char c) {
    return (c >= 0x00 && c <= 0x1f)  
        || (c >= 0x7f && c <= 0x9f) 
        || c == '\u00ad' || c == '\u070f' || c == '\u17b4' || c == '\u17b5'
        || c == '\ufeff' 
        || (c >= '\u0600' && c <= '\u0604')
        || (c >= '\u200c' && c <= '\u200f')
        || (c >= '\u2028' && c <= '\u202f')
        || (c >= '\u2060' && c <= '\u206f')
        || (c >= '\ufff0' && c <= '\uffff');
  }

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
