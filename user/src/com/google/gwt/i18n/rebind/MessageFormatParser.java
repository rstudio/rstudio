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

import com.google.gwt.core.ext.UnableToCompleteException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for parsing MessageFormat-style format strings.
 *
 * @deprecated use {@link com.google.gwt.i18n.server.MessageFormatUtils} instead
 */
@Deprecated
public class MessageFormatParser {

  /**
   * Represents an argument in a template string.
   */
  public static class ArgumentChunk extends TemplateChunk {

    private final int argNumber;
    private final String format;
    private final Map<String, String> formatArgs;
    private final String subFormat;
    private final Map<String, String> listArgs;

    public ArgumentChunk(int argNumber, Map<String, String> listArgs,
        String format, Map<String, String> formatArgs, String subformat) {
      this.argNumber = argNumber;
      this.format = format;
      this.subFormat = subformat;
      this.listArgs = listArgs;
      this.formatArgs = formatArgs;
    }

    @Override
    public void accept(TemplateChunkVisitor visitor)
        throws UnableToCompleteException {
      visitor.visit(this);
    }

    /**
     * Get the argument number this chunk refers to.
     * 
     * @return the argument number or -1 to refer to the right-most plural
     *     argument
     */
    public int getArgumentNumber() {
      return argNumber;
    }

    public String getFormat() {
      return format;
    }

    public Map<String, String> getFormatArgs() {
      return formatArgs;
    }

    public Map<String, String> getListArgs() {
      return listArgs;
    }

    public String getSubFormat() {
      return subFormat;
    }

    public boolean isList() {
      return listArgs != null;
    }

    @Override
    public String toString() {
      return "Argument: #=" + argNumber + ", format=" + format + ", subformat="
          + subFormat;
    }

    @Override
    protected String getStringValue(boolean quote) {
      StringBuilder buf = new StringBuilder();
      buf.append('{');
      if (argNumber < 0) {
        buf.append('#');
      } else {
        buf.append(argNumber);
      }
      Map<String, String> map = listArgs;
      if (map != null) {
        buf.append(",list");
        appendArgs(buf, map, quote);
      }
      if (format != null || subFormat != null) {
        buf.append(',');
      }
      if (format != null) {
        buf.append(quoteMessageFormatChars(format, quote));
        appendArgs(buf, formatArgs, quote);
      }
      if (subFormat != null) {
        buf.append(',');
        buf.append(subFormat);
      }
      buf.append('}');
      return buf.toString();
    }

    /**
     * @param buf
     * @param map
     * @param quote
     */
    private void appendArgs(
        StringBuilder buf, Map<String, String> map, boolean quote) {
      char prefix = ':';
      for (Map.Entry<String, String> entry : map.entrySet()) {
        String key = entry.getKey();
        if (quote) {
          key = quoteMessageFormatChars(key);
        }
        buf.append(prefix).append(key);
        String value = entry.getValue();
        if (value != null) {
          if (quote) {
            value = quoteMessageFormatChars(value);
          }
          buf.append('=').append(value);
        }
        prefix = ',';
      }
    }
  }

  /**
   * Default implementation of TemplateChunkVisitor -- other implementations
   * should extend this if possible to avoid breakage when new TemplateChunk
   * subtypes are added.
   */
  public static class DefaultTemplateChunkVisitor
      implements TemplateChunkVisitor {

    public void visit(ArgumentChunk argChunk) throws UnableToCompleteException {
    }

    public void visit(StaticArgChunk staticArgChunk)
        throws UnableToCompleteException {
    }

    public void visit(StringChunk stringChunk)
        throws UnableToCompleteException {
    }
  }

  /**
   * Represents a static argument, which is used to remove markup from
   * translator view without having to supply it at each callsite.
   */
  public static class StaticArgChunk extends TemplateChunk {

    private final String argName;
    private final String replacement;

    public StaticArgChunk(String argName, String replacement) {
      this.argName = argName;
      this.replacement = replacement;
    }

    @Override
    public void accept(TemplateChunkVisitor visitor)
        throws UnableToCompleteException {
      visitor.visit(this);
    }

    public String getArgName() {
      return argName;
    }

    public String getReplacement() {
      return replacement;
    }

    @Override
    protected String getStringValue(boolean quoted) {
      StringBuilder buf = new StringBuilder();
      buf.append('{').append(argName);
      if (replacement != null) {
        buf.append(',').append(quoteMessageFormatChars(replacement, quoted));
      }
      buf.append('}');
      return buf.toString();
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

    @Override
    public void accept(TemplateChunkVisitor visitor)
        throws UnableToCompleteException {
      visitor.visit(this);
    }

    public void append(String str) {
      buf.append(str);
    }

    @Override
    public boolean isLiteral() {
      return true;
    }

    @Override
    public String toString() {
      return "StringLiteral: \"" + buf.toString() + "\"";
    }

    @Override
    protected String getStringValue(boolean quote) {
      String str = buf.toString();
      return quoteMessageFormatChars(str, quote);
    }
  }

  /**
   * Represents a parsed chunk of a template.
   */
  public abstract static class TemplateChunk {

    /**
     * Quote a string in the MessageFormat-style.
     *
     * @param str string to quote, must not be null
     * @return quoted string
     */
    protected static String quoteMessageFormatChars(String str) {
      str = str.replace("'", "''");
      str = str.replace("{", "'{'");
      str = str.replace("}", "'}'");
      return str;
    }

    /**
     * Possibly quote a string in the MessageFormat-style.
     *
     * @param str
     * @return quoted string
     */
    protected static String quoteMessageFormatChars(String str, boolean quote) {
      return quote ? quoteMessageFormatChars(str) : str;
    }

    public abstract void accept(TemplateChunkVisitor visitor)
        throws UnableToCompleteException;

    /**
     * Returns the string as this chunk would be represented in a MessageFormat
     * template, with any required quoting such that reparsing this value would
     * produce an equivalent (note, not identical) parse.
     *
     * Note that the default implementation may not be sufficient for all
     * subclasses.
     */
    public String getAsMessageFormatString() {
      return getStringValue(true);
    }

    /**
     * Returns the string as this chunk would be represented in a MessageFormat
     * template, with any quoting removed. Note that this is distinct from
     * toString in that the latter is intend for human consumption.
     */
    public String getString() {
      return getStringValue(false);
    }

    public boolean isLiteral() {
      return false;
    }

    /**
     * Returns the optionally quoted string value of this chunk as represented
     * in a MessgeFormat string.
     *
     * @param quote true if the result should be quoted
     * @return optionally quoted MessageFormat string
     */
    protected abstract String getStringValue(boolean quote);
  }

  /**
   * Visitor for template chunks.
   */
  public interface TemplateChunkVisitor {
    void visit(ArgumentChunk argChunk) throws UnableToCompleteException;

    void visit(StaticArgChunk staticArgChunk) throws UnableToCompleteException;

    void visit(StringChunk stringChunk) throws UnableToCompleteException;
  }

  /**
   * Generate a MessageFormat-style string representing the supplied components,
   * properly quoting any special characters in string literal portions.
   *
   *  Note that additional quoting may be required depending on how it will be
   * used, such as backslash-escaping double quotes if it will be used in a
   * generated string constant.
   *
   * @param parts list of TemplateChunks to assemble
   * @return assembled/quoted string
   */
  public static String assemble(Iterable<TemplateChunk> parts) {
    final StringBuilder buf = new StringBuilder();
    for (TemplateChunk part : parts) {
      buf.append(part.getAsMessageFormatString());
    }
    return buf.toString();
  }

  public static List<TemplateChunk> parse(String template)
      throws ParseException {
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
          StringBuilder argBuf = new StringBuilder();
          boolean argQuote = false;
          while (curPos < templateLen) {
            ch = template.charAt(curPos++);
            if (ch == '\'') {
              if (curPos < templateLen && template.charAt(curPos) == '\'') {
                argBuf.append(ch);
                ++curPos;
              } else {
                argQuote = !argQuote;
              }
            } else {
              if (!argQuote && ch == '}') {
                break;
              }
              argBuf.append(ch);
            }
          }
          if (ch != '}') {
            throw new ParseException(
                "Invalid message format - { not start of valid argument"
                    + template, curPos);
          }
          if (curChunk != null) {
            chunks.add(curChunk);
          }
          String arg = argBuf.toString();
          int firstComma = arg.indexOf(',');
          String format = null;
          if (firstComma > 0) {
            format = arg.substring(firstComma + 1);
            arg = arg.substring(0, firstComma);
          }
          if (!"#".equals(arg) && !Character.isDigit(arg.charAt(0))) {
            // static argument
            chunks.add(new StaticArgChunk(arg, format));
          } else {
            int argNumber = -1;
            if (!"#".equals(arg)) {
              argNumber = Integer.valueOf(arg);
            }
            Map<String, String> formatArgs = new HashMap<String, String>();
            Map<String, String> listArgs = null;
            String subFormat = null;
            if (format != null) {
              int comma = format.indexOf(',');
              if (comma >= 0) {
                subFormat = format.substring(comma + 1);
                format = format.substring(0, comma);
              }
              format = parseFormatArgs(format, formatArgs);
              if ("list".equals(format)) {
                listArgs = formatArgs;
                formatArgs = new HashMap<String, String>();
                format = subFormat;
                subFormat = null;
                if (format != null) {
                  comma = format.indexOf(',');
                  if (comma >= 0) {
                    subFormat = format.substring(comma + 1);
                    format = format.substring(0, comma);
                  }
                  format = parseFormatArgs(format, formatArgs);
                }
              }
            }
            chunks.add(new ArgumentChunk(
                argNumber, listArgs, format, formatArgs, subFormat));
          }
          curChunk = null;
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
      throw new ParseException(
          "Unterminated single quote: " + template, template.length());
    }
    if (curChunk != null) {
      chunks.add(curChunk);
    }
    return chunks;
  }

  private static TemplateChunk appendString(
      ArrayList<TemplateChunk> chunks, TemplateChunk curChunk, String string) {
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

  /**
   * Parse any arguments appended to a format. The syntax is:
   * format[:tag[=value][:tag[=value]]... for example: "date:tz=EST:showoffset"
   *
   * @param format format value to parse
   * @param formatArgs map to add tag/value pairs to
   * @return format portion of supplied string
   */
  private static String parseFormatArgs(
      String format, Map<String, String> formatArgs) {
    int colon = format.indexOf(':');
    if (colon >= 0) {
      for (String tagValue : format.substring(colon + 1).split(":")) {
        int equals = tagValue.indexOf('=');
        String value = "";
        if (equals >= 0) {
          value = tagValue.substring(equals + 1).trim();
          tagValue = tagValue.substring(0, equals);
        }
        formatArgs.put(tagValue.trim(), value);
      }
      format = format.substring(0, colon);
    }
    return format;
  }
}
