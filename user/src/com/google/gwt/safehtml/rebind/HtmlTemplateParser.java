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
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A HTML context-aware parser for a simple HTML template language.
 *
 * <p>This parser parses templates consisting of well-formed XML or XHTML
 * markup, with template parameters of the form {@code "{n}"}. For example, a
 * template might look like,
 * <pre>  {@code
 *   <span class="{0}"><a href="{1}/{2}">{3}</a></span>
 * }</pre>
 *
 * <p>The parser produces a parsed form of the template (returned as a
 * {@link ParsedHtmlTemplate}) consisting of a sequence of chunks
 * corresponding to the literal strings and parameters of the template.  The
 * parser is HTML context aware and tags each parameter with its parameter index
 * as well as a {@link HtmlContext} that corresponds to the HTML context in
 * which the parameter occurs in the template.
 *
 * <p>The following contexts are recognized and instantiated:
 * <dl>
 *   <dt>{@link HtmlContext.Type#TEXT}
 *   <dd>This context corresponds to basic inner text. In the above example,
 *       parameter #3 would be tagged with this context.
 *   <dt>{@link HtmlContext.Type#ATTRIBUTE_START}
 *   <dd>This context corresponds to a parameter that appears at the very start
 *       of a HTML attribute's value; in the above example this applies to
 *       parameters #0 and #1.
 *   <dt>{@link HtmlContext.Type#ATTRIBUTE}
 *   <dd>This context corresponds to a parameter that appears within an
 *       attribute in a position other than at the start of the attribute's
 *       value. In the above example, this applies to parameter #2.
 * </dl>
 *
 * <p>For both attribute contexts, the {@code tag} and {@code attribute}
 * properties of the context are set to the name of the enclosing tag and
 * attribute, respectively.
 *
 * <p>Tag and attribute names are converted to lower-case in {@link HtmlContext}
 * properties and literal string chunks of the parsed form.
 *
 * <p>The implementation is subject to the following limitations:
 * <ul>
 *   <li>The input template must be well-formed XML/XHTML. If it is not,
 *       an {@link UnableToCompleteException} is thrown and details regarding
 *       the source of the parse failure are logged to this parser's logger.
 *   <li>Template parameters can only appear within inner text and within
 *       attributes. In particular, parameters cannot appear within a HTML
 *       tag or attribute name; for example, the following is not a valid
 *       template:
 *       <pre>  {@code
 *         <span><{0} class="xyz" {1}="..."/></span>
 *       }</pre>
 *   <li>The output markup will contain separate closing tags for tags without
 *       content. I.e., an input template
 *       <pre>  {@code
 *         <img src="..."/>
 *       }</pre>
 *       will result in the corresponding output
 *       <pre>  {@code
 *         <img src="..."></img>
 *       }</pre>
 *   <li>There is no escaping mechanism for the parameter syntax, i.e. it is
 *       impossible to write a template that results in a literal output chunk
 *       containing a substring of the form "{@code {0}}".
 * </ul>
 */
final class HtmlTemplateParser {

  /**
   * A SAX parser event handler for parsing HTML templates.
   */
  private class HtmlTemplateHandler extends DefaultHandler {

    /*
     * Overrides for relevant SAX event handler methods.
     */

    @Override
    public void characters(char[] ch, int start, int length) {
      parseTemplateString(
          new HtmlContext(HtmlContext.Type.TEXT),
          SafeHtmlUtils.htmlEscape(new String(ch, start, length)));
    }

    @Override
    public void endElement(String uri, String localName, String name) {
      Preconditions.checkArgument(uri.equals(""),
          "Namespace uri unexpectedly non-empty: %s", uri);
      appendLiteral("</" + name.toLowerCase() + ">");
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
      throw unsupportedError("Prefix Mapping");
    }

    @Override
    public void error(SAXParseException e) {
      getLogger().log(TreeLogger.ERROR, "Parser error: " + e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      getLogger().log(TreeLogger.ERROR, "Parser fatal error: " + e);
      throw e;
    }

    /*
     * Throw errors on various irrelevant SAX events that we don't want to
     * handle, and which should not occur in templates.
     *
     * It may be reasonable to just silently ignore these events, but failing
     * explicitly seems more helpful to developers.
     */

    @Override
    public void notationDecl(String name, String publicId, String systemId)
        throws SAXException {
      throw unsupportedError("Notation Declaration");
    }

    @Override
    public void processingInstruction(String target, String data)
        throws SAXException {
      throw unsupportedError("Processing Instruction");
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException {
      throw unsupportedError("External Entity");
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
      throw unsupportedError("Skipped Entity");
    }

    @Override
    public void startElement(String uri, String localName, String name,
        Attributes attributes) {
      Preconditions.checkArgument(uri.equals(""),
          "Namespace uri unexpectedly non-empty: %s", uri);

      name = name.toLowerCase();
      appendLiteral("<" + name);
      if (attributes != null) {
        int len = attributes.getLength();
        for (int i = 0; i < len; i++) {
          String attribute = attributes.getQName(i).toLowerCase();
          appendLiteral(" " + attribute + "=\"");
          parseTemplateString(
              new HtmlContext(HtmlContext.Type.ATTRIBUTE, name, attribute),
              new HtmlContext(HtmlContext.Type.ATTRIBUTE_START,
                  name, attribute),
                  SafeHtmlUtils.htmlEscape(attributes.getValue(i)));
          appendLiteral("\"");
        }
      }
      appendLiteral(">");
    }

    /*
     * Error handlers.
     */

    @Override
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
      throw unsupportedError("Prefix Mapping");
    }

    @Override
    public void unparsedEntityDecl(String name, String publicId,
        String systemId, String notationName) throws SAXException {
      throw unsupportedError("Unparsed Entity Declaration");
    }

    @Override
    public void warning(SAXParseException e) {
      getLogger().log(TreeLogger.WARN, "Parser warning: " + e);
    }

    /**
     * Returns exception for unsupported event in SafeHtmlTemplates.
     *
     * <p>
     * Returns an exception indicating that the event in question is not
     * supported in SafeHtmlTemplates.
     *
     * @param what unsupported SAX event that should not occur in templates
     * @return exception stating that the event is not allowed
     */
    private SAXNotSupportedException unsupportedError(String what) {
      return new SAXNotSupportedException(
          "Not allowed in SafeHtmlTemplates: " + what);
    }
  }

  /**
   * Pattern to find template parameters references.
   */
  private static final Pattern TEMPLATE_PARAM_PATTERN =
      Pattern.compile("\\{(\\d+)\\}");

  private final TreeLogger logger;

  private final ParsedHtmlTemplate parsedTemplate;

  /**
   * Creates a {@link HtmlTemplateParser}.
   *
   * @param logger the {@link TreeLogger} to log to
   */
  public HtmlTemplateParser(TreeLogger logger) {
    this.logger = logger;
    this.parsedTemplate = new ParsedHtmlTemplate();
  }

  /**
   * Returns this parser's logger.
   */
  public TreeLogger getLogger() {
    return logger;
  }

  /**
   * Returns the parsed representation of the template.
   */
  public ParsedHtmlTemplate getParsedTemplate() {
    return parsedTemplate;
  }

  /**
   * Parses a XML/XHTML document.
   *
   * @param input a {@link Reader} from which the document to be parsed will be
   *        read.
   * @throws UnableToCompleteException if the template cannot be parsed. Details
   *         on the source of the failure will have been logged to this parser's
   *         logger.
   */
  public void parseXHtml(Reader input) throws UnableToCompleteException {
    HtmlTemplateHandler saxEventHandler = new HtmlTemplateHandler();
    XMLReader xmlParser;
    try {
      xmlParser = XMLReaderFactory.createXMLReader();
    } catch (SAXException e) {
      logger.log(TreeLogger.ERROR, "Couldn't instantiate XML parser", e);
      throw new UnableToCompleteException();
    }

    xmlParser.setContentHandler(saxEventHandler);
    xmlParser.setDTDHandler(saxEventHandler);
    xmlParser.setEntityResolver(saxEventHandler);
    xmlParser.setErrorHandler(saxEventHandler);

    try {
      xmlParser.parse(new InputSource(input));
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Error during template parsing:", e);
      throw new UnableToCompleteException();
    } catch (SAXParseException e) {
      String logMessage = "Parse Error during template parsing, at line "
          + e.getLineNumber() + ", column " + e.getColumnNumber();
      // Attempt to extract (some) of the input to provide a more useful
      // error message.
      try {
        input.reset();
        char[] buf = new char[200];
        int len = input.read(buf);
        if (len > 0) {
          logMessage += " of input " + new String(buf, 0, len);
        }
      } catch (IOException e1) {
        // We tried, but resetting/reading from the input stream failed. Sorry.
        logMessage += " <failed to read input snippet>";
      }
      logger.log(TreeLogger.ERROR, logMessage + ": " + e);
      throw new UnableToCompleteException();
    } catch (SAXException e) {
      logger.log(TreeLogger.ERROR, "Error during template parsing:", e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Parses a {@link String} that may contain template parameters of the form
   * {@code {n}} into corresponding literal and parameter
   * {@link ParsedHtmlTemplate.TemplateChunk}s.
   *
   * <p>Parameters will be tagged with the {@link HtmlContext} provided.
   *
   * <p>If {@code contextAtStart} is not {@code null} and the parsed template
   * starts with a parameter (i.e., is of the form {@code "{n}..."}), this first
   * parameter will be tagged with that context; any other parameters will
   * be tagged with context {@code context}.
   *
   * @param context the context with which to tag parameters occurring in the
   *        template
   * @param contextAtStart if not {@code null}, the context with which to tag a
   *        parameter that occurs at the very beginning of the template
   * @param template the template {@link String} to parse
   */
  // @VisibleForTesting
  void parseTemplateString(HtmlContext context,
      HtmlContext contextAtStart, String template) {
    Matcher match = TEMPLATE_PARAM_PATTERN.matcher(template);

    boolean firstMatch = true;
    int endOfLastMatch = 0;
    while (match.find()) {
      if (match.start() > endOfLastMatch) {
        // There is a non-empty string between the previous match and this
        // match; add this as a literal chunk to the parsed representation.
        appendLiteral(template.substring(endOfLastMatch, match.start()));
      }

      int paramIndex = Integer.parseInt(match.group(1));
      if (firstMatch && (match.start() == 0) && (contextAtStart != null)) {
        parsedTemplate.addParameter(new ParameterChunk(contextAtStart,
            paramIndex));
      } else {
        parsedTemplate.addParameter(new ParameterChunk(context, paramIndex));
      }

      firstMatch = false;
      endOfLastMatch = match.end();
    }

    // Add a literal chunk for the substring after the last match, if any.
    if (endOfLastMatch < template.length()) {
      parsedTemplate.addLiteral(template.substring(endOfLastMatch));
    }
  }

  /**
   * Parses a {@link String} that may contain template parameters of the form
   * {@code {n}} into corresponding literal and parameter
   * {@link ParsedHtmlTemplate.TemplateChunk}s.
   *
   * <p>Parameters will be tagged with the {@link HtmlContext} provided.
   *
   * @param context the context with which to tag parameters occurring in the
   *        template
   * @param template the template to parse
   */
  // @VisibleForTesting
  void parseTemplateString(HtmlContext context, String template) {
    parseTemplateString(context, null, template);
  }

  /**
   * Appends a literal string to the parsed template representation.
   *
   * <p>The {@code literal} will be appended without processing; any XML/XHTML
   * markup as well as template parameters occurring in the {@code literal} will
   * not be parsed.
   *
   * @param literal the string to append
   */
  private void appendLiteral(String literal) {
    parsedTemplate.addLiteral(literal);
  }
}
