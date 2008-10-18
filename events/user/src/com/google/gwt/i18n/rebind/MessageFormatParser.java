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
package com.google.gwt.i18n.rebind;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for parsing MessageFormat-style format strings.
 */
public class MessageFormatParser {

  /**
   * Represents an argument in a template string.
   */
  public static class ArgumentChunk extends TemplateChunk {

    private int argNumber;
    private String format;
    private String subFormat;
    
    public ArgumentChunk(int argNumber, String format, String subformat) {
      this.argNumber = argNumber;
      this.format = format;
      this.subFormat = subformat;
    }

    public int getArgumentNumber() {
      return argNumber;
    }
    
    public String getFormat() {
      return format;
    }

    @Override
    public String getString() {
      StringBuilder buf = new StringBuilder();
      buf.append('{');
      buf.append(argNumber);
      if (format != null || subFormat != null) {
        buf.append(',');
      }
      if (format != null) {
        buf.append(format);
      }
      if (subFormat != null) {
        buf.append(',');
        buf.append(subFormat);
      }
      buf.append('}');
      return buf.toString();
    }

    public String getSubFormat() {
      return subFormat;
    }

    @Override
    public String toString() {
      return "Argument: #=" + argNumber + ", format=" + format + ", subformat="
          + subFormat;
    }
  }

  /**
   * Represents a literal string portion of a template string.
   */
  public static class StringChunk extends TemplateChunk {

    private StringBuilder buf = new StringBuilder();
    
    public StringChunk() {
    }
    
    public StringChunk(String str) {
      buf.append(str);
    }

    public void append(String str) {
      buf.append(str);
    }

    @Override
    public String getString() {
      return buf.toString();
    }

    @Override
    public boolean isLiteral() {
      return true;
    }
    
    @Override
    public String toString() {
      return "StringLiteral: \"" + buf.toString() + "\"";
    }
  }

  /**
   * Represents a parsed chunk of a template.
   */
  public static class TemplateChunk {

    /**
     * @return the string as this chunk would be represented in a MessageFormat
     * template.  Note that this is distinct from toString in that the latter
     * is intend for human consumption.
     */
    public String getString() {
      return toString();
    }
    
    public boolean isLiteral() {
      return false;
    }
  }

  /**
   * Pattern to find MessageFormat argument references, including format and
   * subformat pieces, if present.
   */
  private static final Pattern argPattern = Pattern.compile(
      "\\{(\\d+)(,(\\w+)(,([^\\}]+))?)?\\}");

  /**
   * Generate a MessageFormat-style string representing the supplied components,
   * properly quoting any special characters in string literal portions.
   * 
   * Note that additional quoting may be required depending on how it will be
   * used, such as backslash-escaping double quotes if it will be used in a
   * generated string constant.
   * 
   * @param parts list of TemplateChunks to assemble
   * @return assembled/quoted string
   */
  public static String assemble(Iterable<TemplateChunk> parts) {
    StringBuilder buf = new StringBuilder();
    for (TemplateChunk part : parts) {
      String str = part.getString();
      if (part.isLiteral()) {
        // quote MessageFormat special characters
        str = str.replace("'", "''");
        str = str.replace("{", "'{'").replace("}", "'}'");
      }
      buf.append(str);
    }
    return buf.toString();
  }

  public static List<TemplateChunk> parse(String template) throws ParseException {
    Matcher match = argPattern.matcher(template);
    int curPos = 0;
    boolean inQuote = false;
    int templateLen = template.length();
    ArrayList<TemplateChunk> chunks = new ArrayList<TemplateChunk>();
    TemplateChunk curChunk = null;
    while (curPos < templateLen) {
      char ch = template.charAt(curPos++);
      switch (ch) {
        case '\'':
          if (curPos < templateLen && template.charAt(curPos) == '\'') {
            curChunk = appendString(chunks, curChunk, "'");
            ++curPos;
            break;
          }
          inQuote = !inQuote;
          break;

        case '{':
          if (inQuote) {
            curChunk = appendString(chunks, curChunk, "{");
            break;
          }
          if (match.find(curPos - 1) && match.start() == curPos - 1) {
            // match group 1 is the argument number (zero-based)
            // match group 3 is the format name or null if none
            // match group 5 is the subformat string or null if none
            int argNumber = Integer.valueOf(match.group(1));
            curPos = match.end();
            String format = match.group(3);
            String subformat = match.group(5);
            if (curChunk != null) {
              chunks.add(curChunk);
            }
            chunks.add(new ArgumentChunk(argNumber, format, subformat));
            curChunk = null;
          } else {
            throw new ParseException(
                "Invalid message format - { not start of valid argument"
                    + template, curPos);
          }
          break;

        case '\n':
          curChunk = appendString(chunks, curChunk, "\\n");
          break;

        case '\r':
          curChunk = appendString(chunks, curChunk, "\\r");
          break;

        case '\\':
          curChunk = appendString(chunks, curChunk, "\\\\");
          break;

        case '"':
          curChunk = appendString(chunks, curChunk, "\\\"");
          break;

        default:
          curChunk = appendString(chunks, curChunk, String.valueOf(ch));
          break;
      }
    }
    if (inQuote) {
      throw new ParseException("Unterminated single quote: " + template,
          template.length());
    }
    if (curChunk != null) {
      chunks.add(curChunk);
    }
    return chunks;
  }

  private static TemplateChunk appendString(ArrayList<TemplateChunk> chunks,
      TemplateChunk curChunk, String string) {
    if (curChunk != null && !curChunk.isLiteral()) {
      chunks.add(curChunk);
      curChunk = null;
    }
    if (curChunk == null) {
      curChunk = new StringChunk(string);
    } else {
      ((StringChunk) curChunk).append(string);
    }
    return curChunk;
  }
}
