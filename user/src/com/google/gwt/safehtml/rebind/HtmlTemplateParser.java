/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.safehtml.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Preconditions;
import com.google.gwt.safehtml.rebind.ParsedHtmlTemplate.HtmlContext;
import com.google.gwt.safehtml.rebind.ParsedHtmlTemplate.ParameterChunk;
import com.google.gwt.thirdparty.streamhtmlparser.HtmlParser;
import com.google.gwt.thirdparty.streamhtmlparser.HtmlParserFactory;
import com.google.gwt.thirdparty.streamhtmlparser.ParseException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A HTML context-aware parser for a simple HTML template language.
 *
 * <p>
 * This parser parses templates consisting of HTML markup, with template
 * variables of the form {@code {n}}. For example, a template might look like,
 *
 * <pre>  {@code
 *   <span style="{0}"><a href="{1}/{2}">{3}</a></span>
 * }</pre>
 *
 * <p>
 * The parser is lenient, and will accept HTML that is not well-formed; the
 * accepted set of HTML is similar to what is typically accepted by browsers.
 * However, the following constraints on the HTML template are enforced:
 *
 * <ol>
 * <li>Template variables may not appear in a JavaScript context (inside a
 * {@code <script>} tag, or in an {@code onClick} etc handler).</li>
 * <li>Template variables may not appear inside HTML comments.</li>
 * <li>If a template variable appears inside the value of an attribute, the
 * value must be enclosed in quotes.</li>
 * <li>Template variables may not appear in the context of an attribute name,
 * nor elsewhere inside a tag except within a quoted attribute value.
 * </li>
 * <li>The template must end in "inner HTML" context, and not inside a tag or
 * attribute.</li>
 * </ol>
 *
 * <p>
 * The parser produces a parsed form of the template (returned as a
 * {@link ParsedHtmlTemplate}) consisting of a sequence of chunks corresponding
 * to the literal strings and parameters of the template. The parser is HTML
 * context aware and tags each parameter with its parameter index as well as a
 * {@link HtmlContext} that corresponds to the HTML context in which the
 * parameter occurs in the template.
 *
 * <p>
 * The following contexts are recognized and instantiated:
 * <dl>
 * <dt>{@link HtmlContext.Type#TEXT}
 * <dd>This context corresponds to basic inner text. In the above example,
 * parameter #3 would be tagged with this context.
 * <dt>{@link HtmlContext.Type#URL_ATTRIBUTE_START}
 * <dd>This context corresponds to a parameter that appears at the very start of
 * a URL-valued HTML attribute's value; in the above example this applies to
 * parameter #1.
 * <dt>{@link HtmlContext.Type#URL_ATTRIBUTE_ENTIRE}
 * <dd>This context corresponds to a parameter that comprises an entire
 * URL-valued attribute, for example in {@code <img src='{0}'/>}.
 * <dt>{@link HtmlContext.Type#CSS_ATTRIBUTE_START}
 * <dd>This context corresponds to a parameter that appears at the very
 * beginning of a {@code style} attribute's value; in the above example this
 * applies to parameter #0.
 * <dt>{@link HtmlContext.Type#CSS_ATTRIBUTE}
 * <dd>This context corresponds to a parameter that appears in the context of a
 * {@code style} attribute, except at the very beginning of the attribute's
 * value.
 * <dt>{@link HtmlContext.Type#ATTRIBUTE_VALUE}
 * <dd>This context corresponds to a parameter that appears within an attribute
 * and is not in one of the more specific in-attribute contexts above. In
 * the example, this applies to parameter #2.
 * <dt>{@link HtmlContext.Type#CSS}
 * <dd>This context corresponds to a parameter that appears within a
 * {@code <style>} tag.
 * </dl>
 *
 * <p>
 * For attribute contexts, the {@code tag} and {@code attribute} properties
 * of the context are set to the name of the enclosing tag and attribute,
 * respectively.
 *
 * <p>
 * The implementation is subject to the following limitation:
 *
 * <p>
 * There is no escaping mechanism for the parameter syntax, i.e. it is
 * impossible to write a template that results in a literal output chunk
 * containing a substring of the form "{@code {0}}".
 *
 * <p>
 * This class is not thread safe.
 */
final class HtmlTemplateParser {

  /**
   * Pattern to find template parameters references.
   */
  private static final Pattern TEMPLATE_PARAM_PATTERN =
      Pattern.compile("\\{(\\d+)\\}");

  private final TreeLogger logger;

  private final ParsedHtmlTemplate parsedTemplate;

  private final HtmlParser streamHtmlParser;

  /**
   * The template string being parsed.
   */
  private String template;

  /**
   * The index in the template up to which the template has been parsed.
   *
   * <p>
   * Used for error reporting.
   */
  private int parsePosition;

  /**
   * The character preceding a template parameter, at the time a template
   * parameter is being parsed.
   */
  private char lookBehind;

  /**
   * The character succeeding a template parameter, at the time a template
   * parameter is being parsed.
   */
  private char lookAhead;

  /**
   * Creates a {@link HtmlTemplateParser}.
   *
   * @param logger the {@link TreeLogger} to log to
   */
  public HtmlTemplateParser(TreeLogger logger) {
    this.logger = logger;
    this.parsedTemplate = new ParsedHtmlTemplate();
    this.streamHtmlParser = HtmlParserFactory.createParser();
  }

  /**
   * Returns the parsed representation of the template.
   */
  public ParsedHtmlTemplate getParsedTemplate() {
    return parsedTemplate;
  }

  /**
   * Parses a {@link String} that may contain template parameters of the form
   * {@code {n}} into corresponding literal and parameter
   * {@link ParsedHtmlTemplate.TemplateChunk}s.
   *
   * @param template the template {@link String} to parse
   * @throws UnableToCompleteException if an unrecoverable parse error occurs
   */
  // @VisibleForTesting
  void parseTemplate(String template) throws UnableToCompleteException {
    this.template = template;
    parsePosition = 0;
    lookBehind = 0;
    lookAhead = 0;
    Matcher match = TEMPLATE_PARAM_PATTERN.matcher(template);

    int endOfPreviousMatch = 0;
    while (match.find()) {
      if (match.start() > endOfPreviousMatch) {
        // There is a non-empty string between the previous match and this
        // match; add this as a literal chunk to the parsed representation.
        parseAndAppendTemplateSegment(
            template.substring(endOfPreviousMatch, match.start()));
        parsePosition = match.start();
        lookBehind = template.charAt(parsePosition - 1);
      }

      int paramIndex = Integer.parseInt(match.group(1));
      parsePosition = match.end();
      if (parsePosition < template.length()) {
        lookAhead = template.charAt(parsePosition);
      } else {
        lookAhead = 0;
      }
      parsedTemplate.addParameter(
          new ParameterChunk(getHtmlContextFromParseState(), paramIndex));

      endOfPreviousMatch = match.end();
    }

    // Add a literal chunk for the substring after the last match, if any.
    if (endOfPreviousMatch < template.length()) {
      parseAndAppendTemplateSegment(template.substring(endOfPreviousMatch));
    }

    if (!streamHtmlParser.getState().equals(HtmlParser.STATE_TEXT)) {
      logger.log(TreeLogger.ERROR,
          "Template does not end in inner-HTML context: " + template);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Determines the {@link HtmlContext} in the parser's current state.
   *
   * <p>
   * This method translates from the stream HTML parser's internal state
   * representation to our HTML context representation, and is intended to be
   * invoked at the point where a template variable is encountered.
   *
   * <p>
   * This method checks for certain illegal/unsupported template constructs,
   * such as template variables that occur in an un-quoted attribute (see this
   * class' class documentation for details).
   *
   * @throws UnableToCompleteException if an illegal/unuspported template
   *           construct is encountered
   */
  private HtmlContext getHtmlContextFromParseState()
      throws UnableToCompleteException {
    // TODO(xtof): Consider refactoring such that state related to the position
    // of the template variable in an attribute is exposed separately (as
    // HtmlContext#isAttributeStart(), etc). In doing so, consider trade off
    // between combinatorial explosion of possible states vs. complexity of
    // client code.
    if (streamHtmlParser.getState().equals(HtmlParser.STATE_ERROR)) {
      logger.log(TreeLogger.ERROR,
          "Parsing template resulted in parse error: "
              + getTemplateParsedSoFar());
      throw new UnableToCompleteException();
    }

    if (streamHtmlParser.inJavascript()) {
      logger.log(TreeLogger.ERROR,
          "Template variables in javascript context are not supported: "
              + getTemplateParsedSoFar());
      throw new UnableToCompleteException();
    }
    if (streamHtmlParser.getState().equals(HtmlParser.STATE_COMMENT)) {
      logger.log(TreeLogger.ERROR,
          "Template variables inside HTML comments are not supported: "
              + getTemplateParsedSoFar());
      throw new UnableToCompleteException();
    } else if (streamHtmlParser.getState().equals(HtmlParser.STATE_TEXT)
        && !streamHtmlParser.inCss()) {
      return new HtmlContext(HtmlContext.Type.TEXT);
    } else if (streamHtmlParser.getState().equals(HtmlParser.STATE_VALUE)) {
      final String tag = streamHtmlParser.getTag();
      final String attribute = streamHtmlParser.getAttribute();
      Preconditions.checkState(!tag.equals(""),
          "streamHtmlParser.getTag() should not be empty  while in "
              + "attribute value context; at %s", getTemplateParsedSoFar());
      Preconditions.checkState(!attribute.equals(""),
          "streamHtmlParser.getAttribute() should not be empty while in "
              + "attribute value context; at %s", getTemplateParsedSoFar());
      if (!streamHtmlParser.isAttributeQuoted()) {
        logger.log(TreeLogger.ERROR,
            "Template variable in unquoted attribute value: "
                + getTemplateParsedSoFar());
        throw new UnableToCompleteException();
      }
      if ("meta".equals(tag) && "content".equals(attribute)) {
        logger.log(TreeLogger.ERROR,
            "Template variables in content attribute of meta tag are not supported: "
                + getTemplateParsedSoFar());
        throw new UnableToCompleteException();
      }
      if (streamHtmlParser.isUrlStart()) {
        // Note that we have established above that the attribute is quoted.
        // Furthermore, we have ruled out template variables in the content
        // attribute of a meta tag, which is the only case where isUrlStart()
        // is true and the URL does not appear at the very beginning of the
        // attribute.
        Preconditions.checkState(lookBehind == '"' || lookBehind == '\'',
            "At the start of a quoted attribute, lookBehind should be a quote character; at %s",
            getTemplateParsedSoFar());
        // If the the character immediately succeeding the template parameter is
        // a quote that matches the one that started the attribute, we know
        // that the parameter comprises the entire attribute.
        if (lookAhead == lookBehind) {
          return new HtmlContext(HtmlContext.Type.URL_ATTRIBUTE_ENTIRE, tag, attribute);
        } else {
          return new HtmlContext(HtmlContext.Type.URL_ATTRIBUTE_START, tag, attribute);
        }
      } else if (streamHtmlParser.inCss()) {
        if (streamHtmlParser.getValueIndex() == 0) {
          return new HtmlContext(HtmlContext.Type.CSS_ATTRIBUTE_START, tag, attribute);
        } else {
          return new HtmlContext(HtmlContext.Type.CSS_ATTRIBUTE, tag, attribute);
        }
      } else {
        return new HtmlContext(
            HtmlContext.Type.ATTRIBUTE_VALUE, tag, attribute);
      }
    } else if (streamHtmlParser.inCss()) {
      return new HtmlContext(HtmlContext.Type.CSS);
    } else if (streamHtmlParser.getState().equals(HtmlParser.STATE_TAG)
        || streamHtmlParser.inAttribute()) {
      logger.log(TreeLogger.ERROR,
          "Template variables in tags or in attribute names are not supported: "
              + getTemplateParsedSoFar());
      throw new UnableToCompleteException();
    }

    logger.log(TreeLogger.ERROR,
        "unhandeled/illegal parse state" + streamHtmlParser.getState());
    throw new UnableToCompleteException();
  }

  /**
   * Returns the prefix of the template string that has been parsed so far.
   */
  private String getTemplateParsedSoFar() {
    return template.substring(0, parsePosition);
  }

  /**
   * Feeds a literal string to the stream parser and appends it to the parsed
   * template representation.
   *
   * @param segment the template segment to parse and append to the parsed
   *          template representation
   * @throws UnableToCompleteException if an unrecoverable parse error occurs
   */
  private void parseAndAppendTemplateSegment(String segment)
      throws UnableToCompleteException {
    try {
      streamHtmlParser.parse(segment);
    } catch (ParseException cause) {
      logger.log(TreeLogger.ERROR,
          "Parse exception when parsing segment '" + segment + "' of template '"
              + template + "'", cause);
      throw new UnableToCompleteException();
    }
    parsedTemplate.addLiteral(segment);
  }
}
