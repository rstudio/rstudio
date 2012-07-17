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
package com.google.gwt.resources.css;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.resources.css.ast.CssDef;
import com.google.gwt.resources.css.ast.CssEval;
import com.google.gwt.resources.css.ast.CssExternalSelectors;
import com.google.gwt.resources.css.ast.CssFontFace;
import com.google.gwt.resources.css.ast.CssIf;
import com.google.gwt.resources.css.ast.CssMediaRule;
import com.google.gwt.resources.css.ast.CssNoFlip;
import com.google.gwt.resources.css.ast.CssNode;
import com.google.gwt.resources.css.ast.CssPageRule;
import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssRule;
import com.google.gwt.resources.css.ast.CssSelector;
import com.google.gwt.resources.css.ast.CssSprite;
import com.google.gwt.resources.css.ast.CssStylesheet;
import com.google.gwt.resources.css.ast.CssUnknownAtRule;
import com.google.gwt.resources.css.ast.CssUrl;
import com.google.gwt.resources.css.ast.HasNodes;
import com.google.gwt.resources.css.ast.HasProperties;
import com.google.gwt.resources.css.ast.CssProperty.DotPathValue;
import com.google.gwt.resources.css.ast.CssProperty.FunctionValue;
import com.google.gwt.resources.css.ast.CssProperty.IdentValue;
import com.google.gwt.resources.css.ast.CssProperty.ListValue;
import com.google.gwt.resources.css.ast.CssProperty.NumberValue;
import com.google.gwt.resources.css.ast.CssProperty.StringValue;
import com.google.gwt.resources.css.ast.CssProperty.TokenValue;
import com.google.gwt.resources.css.ast.CssProperty.Value;

import org.w3c.css.sac.AttributeCondition;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.CharacterDataSelector;
import org.w3c.css.sac.CombinatorCondition;
import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.ContentCondition;
import org.w3c.css.sac.DescendantSelector;
import org.w3c.css.sac.DocumentHandler;
import org.w3c.css.sac.ElementSelector;
import org.w3c.css.sac.ErrorHandler;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.LangCondition;
import org.w3c.css.sac.LexicalUnit;
import org.w3c.css.sac.NegativeCondition;
import org.w3c.css.sac.NegativeSelector;
import org.w3c.css.sac.PositionalCondition;
import org.w3c.css.sac.ProcessingInstructionSelector;
import org.w3c.css.sac.SACMediaList;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;
import org.w3c.css.sac.SiblingSelector;
import org.w3c.flute.parser.Parser;

import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates a CssStylesheet from the contents of a URL.
 */
@SuppressWarnings("unused")
public class GenerateCssAst {

  /**
   * Maps SAC CSSParseExceptions into a TreeLogger. All parsing errors will be
   * recorded in a single TreeLogger branch, which will be created only if a
   * loggable error message is emitted.
   */
  private static class Errors implements ErrorHandler {
    /**
     * A flag that controls whether or not the exec method will fail.
     */
    private boolean fatalErrorEncountered;
    private TreeLogger logger;
    private final TreeLogger parentLogger;

    /**
     * Constructor.
     * 
     * @param parentLogger the TreeLogger that should be branched to produce the
     *          CSS parsing messages.
     */
    public Errors(TreeLogger parentLogger) {
      this.parentLogger = parentLogger;
    }

    public TreeLogger branch(TreeLogger.Type type, String message) {
      return branch(type, message, null);
    }

    public TreeLogger branch(TreeLogger.Type type, String message, Throwable t) {
      return logOrBranch(type, message, t, true);
    }

    public void error(CSSParseException exception) throws CSSException {
      // TODO Since this indicates a loss of data, should this be a fatal error?
      log(TreeLogger.WARN, exception);
    }

    public void fatalError(CSSParseException exception) throws CSSException {
      log(TreeLogger.ERROR, exception);
    }

    public void log(TreeLogger.Type type, String message) {
      log(type, message, null);
    }

    public void log(TreeLogger.Type type, String message, Throwable t) {
      logOrBranch(type, message, t, false);
    }

    public void warning(CSSParseException exception) throws CSSException {
      log(TreeLogger.DEBUG, exception);
    }

    private void log(TreeLogger.Type type, CSSParseException e) {
      log(type, "Line " + e.getLineNumber() + " column " + e.getColumnNumber()
          + ": " + e.getMessage());
    }

    private TreeLogger logOrBranch(TreeLogger.Type type, String message,
        Throwable t, boolean branch) {
      fatalErrorEncountered |= type == TreeLogger.ERROR;
      if (parentLogger.isLoggable(type)) {
        maybeBranch();
        if (branch) {
          return logger.branch(type, message, t);
        } else {
          logger.log(type, message, t);
          return null;
        }
      } else {
        return TreeLogger.NULL;
      }
    }

    private void maybeBranch() {
      if (logger == null) {
        logger = parentLogger.branch(TreeLogger.INFO,
            "The following problems were detected");
      }
    }
  }

  /**
   * Maps the SAC model into our own CSS AST nodes.
   */
  private static class GenerationHandler implements DocumentHandler {
    /**
     * The stylesheet that is being composed.
     */
    private final CssStylesheet css = new CssStylesheet();

    /**
     * Accumulates CSS nodes as they are created.
     */
    private final Stack<HasNodes> currentParent = new Stack<HasNodes>();

    /**
     * Accumulates CSS properties as they are seen.
     */
    private HasProperties currentRule;

    /**
     * Records references to {@code @def} rules.
     */
    private final Map<String, CssDef> defs = new HashMap<String, CssDef>();

    /**
     * Used when parsing the contents of meta-styles.
     */
    private final Errors errors;

    /**
     * Used by {@link #startSelector(SelectorList)} to suppress the creation of
     * new CssRules in favor of retaining {@link #currentRule}.
     */
    private boolean nextSelectorCreatesRule = true;

    public GenerationHandler(Errors errors) {
      this.errors = errors;
      currentParent.push(css);
    }

    public void comment(String text) throws CSSException {
      // Ignore comments
      // TODO Should comments be retained but not generally printed?
    }

    public void endDocument(InputSource source) throws CSSException {
    }

    public void endFontFace() throws CSSException {
    }

    public void endMedia(SACMediaList media) throws CSSException {
      currentParent.pop();
    }

    public void endPage(String name, String pseudoPage) throws CSSException {
    }

    public void endSelector(SelectorList selectors) throws CSSException {
    }

    /**
     * Reflectively invoke a method named parseRule on this instance.
     */
    public void ignorableAtRule(String atRule) throws CSSException {
      int idx = atRule.indexOf(" ");
      if (idx == -1) {
        // Empty rule like @foo;
        addNode(new CssUnknownAtRule(atRule));
        return;
      }
      String ruleName = atRule.substring(1, idx);
      String methodName = "parse" + (Character.toUpperCase(ruleName.charAt(0)))
          + ruleName.substring(1).toLowerCase();
      try {
        Method parseMethod = getClass().getDeclaredMethod(methodName,
            String.class);
        parseMethod.invoke(this, atRule);
      } catch (NoSuchMethodException e) {
        // A rule like @-webkit-keyframe {...} that we can't process
        addNode(new CssUnknownAtRule(atRule));
      } catch (IllegalAccessException e) {
        errors.log(TreeLogger.ERROR, "Unable to invoke parse method ", e);
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause();

        if (cause instanceof CSSException) {
          // Unwind a CSSException normally
          throw (CSSException) cause;
        } else if (cause != null) {
          // Otherwise, report the message nicely
          TreeLogger details = errors.branch(TreeLogger.ERROR,
              cause.getMessage());
          details.log(TreeLogger.DEBUG, "Full stack trace", cause);
        } else {
          TreeLogger details = errors.branch(TreeLogger.ERROR,
              "Unknown failure parsing " + ruleName);
          details.log(TreeLogger.DEBUG, "Full stack trace", e);
        }
      }
    }

    public void importStyle(String uri, SACMediaList media,
        String defaultNamespaceURI) throws CSSException {
    }

    public void namespaceDeclaration(String prefix, String uri)
        throws CSSException {
    }

    public void property(String name, LexicalUnit value, boolean important)
        throws CSSException {
      List<Value> values = new ArrayList<Value>();
      if (value != null) {
        extractValueOf(values, value);
      }
      currentRule.getProperties().add(
          new CssProperty(escapeIdent(name), new ListValue(values), important));
    }

    public void startDocument(InputSource source) throws CSSException {
    }

    public void startFontFace() throws CSSException {
      CssFontFace rule = new CssFontFace();
      addNode(rule);
      currentRule = rule;
    }

    public void startMedia(SACMediaList media) throws CSSException {
      CssMediaRule r = new CssMediaRule();
      for (int i = 0; i < media.getLength(); i++) {
        r.getMedias().add(media.item(i));
      }

      pushParent(r);
    }

    public void startPage(String name, String pseudoPage) throws CSSException {
      CssPageRule r = new CssPageRule();
      // name appears to be unused in CSS2
      r.setPseudoPage(pseudoPage);
      addNode(r);
      currentRule = r;
    }

    public void startSelector(SelectorList selectors) throws CSSException {
      CssRule r;

      if (nextSelectorCreatesRule) {
        r = new CssRule();
        addNode(r);
        currentRule = r;
      } else {
        r = (CssRule) currentRule;
        nextSelectorCreatesRule = true;
      }

      for (int i = 0; i < selectors.getLength(); i++) {
        r.getSelectors().add(new CssSelector(valueOf(selectors.item(i))));
      }
    }

    void parseDef(String atRule) {
      String value = atRule.substring(4, atRule.length()).trim();

      InputSource s = new InputSource();
      s.setCharacterStream(new StringReader(value));
      Parser parser = new Parser();
      parser.setErrorHandler(errors);

      final List<Value> values = new ArrayList<Value>();
      parser.setDocumentHandler(new PropertyExtractor(values));

      try {
        String dummy = "* { prop : " + value + "}";
        parser.parseStyleSheet(new InputSource(new StringReader(dummy)));
      } catch (IOException e) {
        assert false : "Should never happen";
      }

      if (values.size() < 2) {
        throw new CSSException(CSSException.SAC_SYNTAX_ERR,
            "@def rules must specify an identifier and one or more values",
            null);
      }

      IdentValue defName = values.get(0).isIdentValue();

      if (defName == null) {
        throw new CSSException(CSSException.SAC_SYNTAX_ERR,
            "First lexical unit must be an identifier", null);
      }

      /*
       * Replace any references to previously-seen @def constructs. We do
       * expansion up-front to prevent the need for cycle-detection later.
       */
      for (ListIterator<Value> it = values.listIterator(1); it.hasNext();) {
        IdentValue maybeDefReference = it.next().isIdentValue();
        if (maybeDefReference != null) {
          CssDef previousDef = defs.get(maybeDefReference.getIdent());
          if (previousDef != null) {
            it.remove();
            for (Value previousValue : previousDef.getValues()) {
              it.add(previousValue);
            }
          }
        }
      }

      CssDef def = new CssDef(defName.getIdent());
      def.getValues().addAll(values.subList(1, values.size()));
      addNode(def);

      defs.put(defName.getIdent(), def);
    }

    /**
     * The elif nodes are processed as though they were {@code @if} nodes. The
     * newly-generated CssIf node will be attached to the last CssIf in the
     * if/else chain.
     */
    void parseElif(String atRule) throws CSSException {
      List<CssNode> nodes = currentParent.peek().getNodes();
      CssIf lastIf = findLastIfInChain(nodes);

      if (lastIf == null) {
        throw new CSSException(CSSException.SAC_SYNTAX_ERR,
            "@elif must immediately follow an @if or @elif", null);
      }

      assert lastIf.getElseNodes().isEmpty();

      // @elif -> lif (because parseIf strips the first three chars)
      parseIf(atRule.substring(2));

      // Fix up the structure by remove the newly-created node from the parent
      // context and moving it to the end of the @if chain
      lastIf.getElseNodes().add(nodes.remove(nodes.size() - 1));
    }

    /**
     * The else nodes are processed as though they were written as {@code @elif
     * true} rules.
     */
    void parseElse(String atRule) throws CSSException {
      // The last CssIf in the if/else chain
      CssIf lastIf = findLastIfInChain(currentParent.peek().getNodes());

      if (lastIf == null) {
        throw new CSSException(CSSException.SAC_SYNTAX_ERR,
            "@else must immediately follow an @if or @elif", null);
      }

      // Create the CssIf to hold the @else rules
      String fakeElif = "@elif (true) " + atRule.substring(atRule.indexOf("{"));
      parseElif(fakeElif);
      CssIf elseIf = findLastIfInChain(currentParent.peek().getNodes());

      assert lastIf.getElseNodes().size() == 1
          && lastIf.getElseNodes().get(0) == elseIf;
      assert elseIf.getElseNodes().isEmpty();

      // Merge the rules into the last CssIf to break the chain and prevent
      // @else followed by @else
      lastIf.getElseNodes().clear();
      lastIf.getElseNodes().addAll(elseIf.getNodes());
    }

    void parseEval(String atRule) throws CSSException {
      // @eval key com.google.Type.staticFunction
      String[] parts = atRule.substring(0, atRule.length() - 1).split("\\s");

      if (parts.length != 3) {
        throw new CSSException(CSSException.SAC_SYNTAX_ERR,
            "Incorrect number of parts for @eval", null);
      }

      CssEval eval = new CssEval(parts[1], parts[2]);
      addNode(eval);
    }

    void parseExternal(String atRule) throws CSSException {
      // @external .foo, bar; Drop the dots and commas
      String[] parts = atRule.substring(10, atRule.length() - 1).replaceAll(
          "(, *)|( +)", " ").replaceAll("\\.", "").split(" ");

      CssExternalSelectors externals = new CssExternalSelectors();
      Collections.addAll(externals.getClasses(), parts);
      addNode(externals);
    }

    void parseIf(String atRule) throws CSSException {
      String predicate = atRule.substring(3, atRule.indexOf('{') - 1).trim();
      String blockContents = atRule.substring(atRule.indexOf('{') + 1,
          atRule.length() - 1);

      CssIf cssIf = new CssIf();

      if (predicate.startsWith("(") && predicate.endsWith(")")) {
        cssIf.setExpression(predicate);
      } else {

        String[] predicateParts = predicate.split("\\s");

        switch (predicateParts.length) {
          case 0:
            throw new CSSException(CSSException.SAC_SYNTAX_ERR,
                "Incorrect format for @if predicate", null);
          case 1:
            if (predicateParts[0].length() == 0) {
              throw new CSSException(CSSException.SAC_SYNTAX_ERR,
                  "Incorrect format for @if predicate", null);
            }
            errors.log(
                TreeLogger.WARN,
                "Deprecated syntax for Java expression detected. Enclose the expression in parentheses");
            cssIf.setExpression(predicateParts[0]);
            break;
          default:
            if (predicateParts[0].startsWith("!")) {
              cssIf.setNegated(true);
              cssIf.setProperty(predicateParts[0].substring(1));
            } else {
              cssIf.setProperty(predicateParts[0]);
            }
            String[] values = new String[predicateParts.length - 1];
            System.arraycopy(predicateParts, 1, values, 0, values.length);
            cssIf.setPropertyValues(values);
        }
      }

      parseInnerStylesheet("@if", cssIf, blockContents);
    }

    void parseNoflip(String atRule) throws CSSException {
      String blockContents = atRule.substring(atRule.indexOf('{') + 1,
          atRule.length() - 1);

      parseInnerStylesheet("@noflip", new CssNoFlip(), blockContents);
    }

    void parseSprite(String atRule) throws CSSException {
      CssSprite sprite = new CssSprite();
      currentRule = sprite;
      addNode(sprite);

      // Flag to tell startSelector() to use the CssSprite instead of creating
      // its own CssRule.
      nextSelectorCreatesRule = false;

      // parse the inner text
      InputSource s = new InputSource();
      s.setCharacterStream(new StringReader(atRule.substring(7)));
      Parser parser = new Parser();
      parser.setDocumentHandler(this);
      parser.setErrorHandler(errors);

      try {
        parser.parseRule(s);
      } catch (IOException e) {
        throw new CSSException(CSSException.SAC_SYNTAX_ERR,
            "Unable to parse @sprite", e);
      }
    }

    void parseUrl(String atRule) throws CSSException {
      // @url key dataResourceFunction
      String[] parts = atRule.substring(0, atRule.length() - 1).split("\\s");

      if (parts.length != 3) {
        throw new CSSException(CSSException.SAC_SYNTAX_ERR,
            "Incorrect number of parts for @url", null);
      }

      CssUrl url = new CssUrl(parts[1], parts[2]);
      addNode(url);
    }

    /**
     * Add a node to the current parent.
     */
    private void addNode(CssNode node) {
      currentParent.peek().getNodes().add(node);
    }

    private <T extends CssNode & HasNodes> void parseInnerStylesheet(
        String tagName, T parent, String blockContents) {
      pushParent(parent);

      // parse the inner text
      InputSource s = new InputSource();
      s.setCharacterStream(new StringReader(blockContents));
      Parser parser = new Parser();
      parser.setDocumentHandler(this);
      parser.setErrorHandler(errors);

      try {
        parser.parseStyleSheet(s);
      } catch (IOException e) {
        throw new CSSException(CSSException.SAC_SYNTAX_ERR, "Unable to parse "
            + tagName, e);
      }

      if (currentParent.pop() != parent) {
        // This is a coding error
        throw new RuntimeException("Incorrect element popped");
      }
    }

    /**
     * Adds a node to the current parent and then makes the node the current
     * parent node.
     */
    private <T extends CssNode & HasNodes> void pushParent(T newParent) {
      addNode(newParent);
      currentParent.push(newParent);
    }
  }

  /**
   * Extracts all properties in a document into a List.
   */
  private static class PropertyExtractor implements DocumentHandler {
    private final List<Value> values;

    private PropertyExtractor(List<Value> values) {
      this.values = values;
    }

    public void comment(String text) throws CSSException {
    }

    public void endDocument(InputSource source) throws CSSException {
    }

    public void endFontFace() throws CSSException {
    }

    public void endMedia(SACMediaList media) throws CSSException {
    }

    public void endPage(String name, String pseudoPage) throws CSSException {
    }

    public void endSelector(SelectorList selectors) throws CSSException {
    }

    public void ignorableAtRule(String atRule) throws CSSException {
    }

    public void importStyle(String uri, SACMediaList media,
        String defaultNamespaceURI) throws CSSException {
    }

    public void namespaceDeclaration(String prefix, String uri)
        throws CSSException {
    }

    public void property(String name, LexicalUnit value, boolean important)
        throws CSSException {
      extractValueOf(values, value);
    }

    public void startDocument(InputSource source) throws CSSException {
    }

    public void startFontFace() throws CSSException {
    }

    public void startMedia(SACMediaList media) throws CSSException {
    }

    public void startPage(String name, String pseudoPage) throws CSSException {
    }

    public void startSelector(SelectorList selectors) throws CSSException {
    }
  }

  /**
   * Associates a template CssStylesheet with a timestamp.
   */
  private static class CachedStylesheet {
    private final CssStylesheet sheet;
    private final long timestamp;

    public CachedStylesheet(CssStylesheet sheet, long timestamp) {
      this.sheet = sheet;
      this.timestamp = timestamp;
    }

    public CssStylesheet getCopyOfStylesheet() {
      return new CssStylesheet(sheet);
    }

    public long getTimestamp() {
      return timestamp;
    }
  }

  private static final String LITERAL_FUNCTION_NAME = "literal";
  /**
   * We cache the stylesheets to prevent repeated parsing of the same source
   * material. This is a common case if the user is using UiBinder's implicit
   * stylesheets. It is necessary to use the list of URLs passed to exec because
   * of the eager variable expansion performed by
   * {@link GenerationHandler#parseDef(String)}.
   */
  private static final Map<List<URL>, SoftReference<CachedStylesheet>> SHEETS = Collections.synchronizedMap(new HashMap<List<URL>, SoftReference<CachedStylesheet>>());
  private static final String VALUE_FUNCTION_NAME = "value";

  /**
   * Create a CssStylesheet from the contents of one or more URLs. If multiple
   * URLs are provided, the generated stylesheet will be created as though the
   * contents of the URLs had been concatenated.
   */
  public static CssStylesheet exec(TreeLogger logger, URL... stylesheets)
      throws UnableToCompleteException {

    long mtime = 0;
    for (URL url : stylesheets) {
      long lastModified;
      try {
        lastModified = url.openConnection().getLastModified();
      } catch (IOException e) {
        // Non-fatal, assuming we can re-open the stream later
        logger.log(TreeLogger.DEBUG, "Could not determine cached time", e);
        lastModified = 0;
      }
      if (lastModified == 0) {
        /*
         * We have to refresh, since the modification date can't be determined,
         * either due to IOException or getLastModified() not providing useful
         * data.
         */
        mtime = Long.MAX_VALUE;
        break;
      } else {
        mtime = Math.max(mtime, lastModified);
      }
    }

    List<URL> sheets = Arrays.asList(stylesheets);
    SoftReference<CachedStylesheet> ref = SHEETS.get(sheets);
    CachedStylesheet toReturn = ref == null ? null : ref.get();
    if (toReturn != null) {
      if (mtime <= toReturn.getTimestamp()) {
        logger.log(TreeLogger.DEBUG, "Using cached result");
        return toReturn.getCopyOfStylesheet();
      } else {
        logger.log(TreeLogger.DEBUG, "Invalidating cached stylesheet");
      }
    }

    Parser p = new Parser();
    Errors errors = new Errors(logger);
    GenerationHandler g = new GenerationHandler(errors);
    p.setDocumentHandler(g);
    p.setErrorHandler(errors);

    for (URL stylesheet : sheets) {
      TreeLogger branchLogger = logger.branch(TreeLogger.DEBUG,
          "Parsing CSS stylesheet " + stylesheet.toExternalForm());
      try {
        p.parseStyleSheet(stylesheet.toURI().toString());
        continue;
      } catch (CSSException e) {
        branchLogger.log(TreeLogger.ERROR, "Unable to parse CSS", e);
      } catch (IOException e) {
        branchLogger.log(TreeLogger.ERROR, "Unable to parse CSS", e);
      } catch (URISyntaxException e) {
        branchLogger.log(TreeLogger.ERROR, "Unable to parse CSS", e);
      }
      throw new UnableToCompleteException();
    }

    if (errors.fatalErrorEncountered) {
      // Logging will have been performed by the Errors instance, just exit
      throw new UnableToCompleteException();
    }

    toReturn = new CachedStylesheet(g.css, mtime == Long.MAX_VALUE ? 0 : mtime);
    SHEETS.put(new ArrayList<URL>(sheets), new SoftReference<CachedStylesheet>(
        toReturn));
    return toReturn.getCopyOfStylesheet();
  }

  /**
   * Expresses an rgb function as a hex expression.
   * 
   * @param colors a sequence of LexicalUnits, assumed to be
   *          <code>(VAL COMMA VAL COMMA VAL)</code> where VAL can be an INT or
   *          a PERCENT (which is then converted to INT)
   * @return the minimal hex expression for the RGB color values
   */
  private static Value colorValue(LexicalUnit colors) {
    LexicalUnit red = colors;
    int r = getRgbComponentValue(red);
    LexicalUnit green = red.getNextLexicalUnit().getNextLexicalUnit();
    int g = getRgbComponentValue(green);
    LexicalUnit blue = green.getNextLexicalUnit().getNextLexicalUnit();
    int b = getRgbComponentValue(blue);

    String sr = Integer.toHexString(r);
    if (sr.length() == 1) {
      sr = "0" + sr;
    }

    String sg = Integer.toHexString(g);
    if (sg.length() == 1) {
      sg = "0" + sg;
    }

    String sb = Integer.toHexString(b);
    if (sb.length() == 1) {
      sb = "0" + sb;
    }

    // #AABBCC --> #ABC
    if (sr.charAt(0) == sr.charAt(1) && sg.charAt(0) == sg.charAt(1)
        && sb.charAt(0) == sb.charAt(1)) {
      sr = sr.substring(1);
      sg = sg.substring(1);
      sb = sb.substring(1);
    }

    return new IdentValue("#" + sr + sg + sb);
  }

  private static String escapeIdent(String selector) {
    assert selector.length() > 0;

    StringBuilder toReturn = new StringBuilder();

    if (selector.charAt(0) == '-') {
      // Allow leading hyphen
      selector = selector.substring(1);
      toReturn.append('-');
    }

    if (!isIdentStart(selector.charAt(0))) {
      toReturn.append('\\');
    }
    toReturn.append(selector.charAt(0));

    if (selector.length() > 1) {
      for (char c : selector.substring(1).toCharArray()) {
        if (!isIdentPart(c)) {
          toReturn.append('\\');
        }
        toReturn.append(c);
      }
    }
    return toReturn.toString();
  }

  /**
   * Convert a LexicalUnit list into a List of Values.
   */
  private static void extractValueOf(List<Value> accumulator, LexicalUnit value) {
    do {
      accumulator.add(valueOf(value));
      value = value.getNextLexicalUnit();
    } while (value != null);
  }

  /**
   * The elif and else constructs are modeled as nested if statements in the
   * CssIf's elseNodes field. This method will search a list of CssNodes and
   * remove the last chained CssIf from the last element in the list of nodes.
   */
  private static CssIf findLastIfInChain(List<CssNode> nodes) {
    if (nodes.isEmpty()) {
      return null;
    }

    CssNode lastNode = nodes.get(nodes.size() - 1);
    if (lastNode instanceof CssIf) {
      CssIf asIf = (CssIf) lastNode;
      if (asIf.getElseNodes().isEmpty()) {
        return asIf;
      } else {
        return findLastIfInChain(asIf.getElseNodes());
      }
    }
    return null;
  }

  /**
   * Return an integer value from 0-255 for a component of an RGB color.
   * 
   * @param color typed value from the CSS parser, which may be an INTEGER or a
   *          PERCENTAGE
   * @return integer value from 0-255
   * @throws IllegalArgumentException if the color is not an INTEGER or
   *           PERCENTAGE value
   */
  private static int getRgbComponentValue(LexicalUnit color) {
    switch (color.getLexicalUnitType()) {
      case LexicalUnit.SAC_INTEGER:
        return Math.min(color.getIntegerValue(), 255);
      case LexicalUnit.SAC_PERCENTAGE:
        return (int) Math.min(color.getFloatValue() * 255, 255);
      default:
        throw new CSSException(CSSException.SAC_SYNTAX_ERR,
            "RGB component value must be integer or percentage, was " + color,
            null);
    }
  }

  private static boolean isIdentPart(char c) {
    return Character.isLetterOrDigit(c) || (c == '\\') || (c == '-')
        || (c == '_');
  }

  private static boolean isIdentStart(char c) {
    return Character.isLetter(c) || (c == '\\') || (c == '_');
  }

  /**
   * Utility method to concatenate strings.
   */
  private static String join(Iterable<Value> elements, String separator) {
    StringBuilder b = new StringBuilder();
    for (Iterator<Value> i = elements.iterator(); i.hasNext();) {
      b.append(i.next().toCss());
      if (i.hasNext()) {
        b.append(separator);
      }
    }
    return b.toString();
  }

  private static String maybeUnquote(String s) {
    if (s.startsWith("\"") && s.endsWith("\"")) {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }

  /**
   * Used when evaluating literal() rules.
   */
  private static String unescapeLiteral(String s) {
    s = s.replaceAll(Pattern.quote("\\\""), "\"");
    s = s.replaceAll(Pattern.quote("\\\\"), Matcher.quoteReplacement("\\"));
    return s;
  }

  private static String valueOf(Condition condition) {
    if (condition instanceof AttributeCondition) {
      AttributeCondition c = (AttributeCondition) condition;
      switch (c.getConditionType()) {
        case Condition.SAC_ATTRIBUTE_CONDITION:
          return "[" + c.getLocalName()
              + (c.getValue() != null ? "=\"" + c.getValue() + '"' : "") + "]";
        case Condition.SAC_ONE_OF_ATTRIBUTE_CONDITION:
          return "[" + c.getLocalName() + "~=\"" + c.getValue() + "\"]";
        case Condition.SAC_BEGIN_HYPHEN_ATTRIBUTE_CONDITION:
          return "[" + c.getLocalName() + "|=\"" + c.getValue() + "\"]";
        case Condition.SAC_ID_CONDITION:
          return "#" + c.getValue();
        case Condition.SAC_CLASS_CONDITION:
          return "." + c.getValue();
        case Condition.SAC_PSEUDO_CLASS_CONDITION:
          return ":" + c.getValue();
      }

    } else if (condition instanceof CombinatorCondition) {
      CombinatorCondition c = (CombinatorCondition) condition;
      switch (condition.getConditionType()) {
        case Condition.SAC_AND_CONDITION:
          return valueOf(c.getFirstCondition())
              + valueOf(c.getSecondCondition());
        case Condition.SAC_OR_CONDITION:
          // Unimplemented in CSS2?
      }

    } else if (condition instanceof ContentCondition) {
      // Unimplemented in CSS2?

    } else if (condition instanceof LangCondition) {
      LangCondition c = (LangCondition) condition;
      return ":lang(" + c.getLang() + ")";

    } else if (condition instanceof NegativeCondition) {
      // Unimplemented in CSS2?
    } else if (condition instanceof PositionalCondition) {
      // Unimplemented in CSS2?
    }

    throw new RuntimeException("Unhandled condition of type "
        + condition.getConditionType() + " " + condition.getClass().getName());
  }

  private static Value valueOf(LexicalUnit value) {
    switch (value.getLexicalUnitType()) {
      case LexicalUnit.SAC_ATTR:
        return new IdentValue("attr(" + value.getStringValue() + ")");
      case LexicalUnit.SAC_IDENT:
        return new IdentValue(escapeIdent(value.getStringValue()));
      case LexicalUnit.SAC_STRING_VALUE:
        return new StringValue(value.getStringValue());
      case LexicalUnit.SAC_RGBCOLOR:
        // flute models the commas as operators so no separator needed
        return colorValue(value.getParameters());
      case LexicalUnit.SAC_INTEGER:
        return new NumberValue(value.getIntegerValue());
      case LexicalUnit.SAC_REAL:
        return new NumberValue(value.getFloatValue());
      case LexicalUnit.SAC_CENTIMETER:
      case LexicalUnit.SAC_DEGREE:
      case LexicalUnit.SAC_DIMENSION:
      case LexicalUnit.SAC_EM:
      case LexicalUnit.SAC_EX:
      case LexicalUnit.SAC_GRADIAN:
      case LexicalUnit.SAC_HERTZ:
      case LexicalUnit.SAC_KILOHERTZ:
      case LexicalUnit.SAC_MILLIMETER:
      case LexicalUnit.SAC_MILLISECOND:
      case LexicalUnit.SAC_PERCENTAGE:
      case LexicalUnit.SAC_PICA:
      case LexicalUnit.SAC_PIXEL:
      case LexicalUnit.SAC_POINT:
      case LexicalUnit.SAC_RADIAN:
      case LexicalUnit.SAC_SECOND:
        return new NumberValue(value.getFloatValue(),
            value.getDimensionUnitText());
      case LexicalUnit.SAC_URI:
        return new IdentValue("url(" + value.getStringValue() + ")");
      case LexicalUnit.SAC_OPERATOR_COMMA:
        return new TokenValue(",");
      case LexicalUnit.SAC_COUNTER_FUNCTION:
      case LexicalUnit.SAC_COUNTERS_FUNCTION:
      case LexicalUnit.SAC_FUNCTION: {
        if (value.getFunctionName().equals(VALUE_FUNCTION_NAME)) {
          // This is a call to value()
          List<Value> params = new ArrayList<Value>();
          extractValueOf(params, value.getParameters());

          if (params.size() != 1 && params.size() != 3) {
            throw new CSSException(CSSException.SAC_SYNTAX_ERR,
                "Incorrect number of parameters to " + VALUE_FUNCTION_NAME,
                null);
          }

          Value dotPathValue = params.get(0);
          String dotPath = maybeUnquote(((StringValue) dotPathValue).getValue());
          String suffix = params.size() == 3
              ? maybeUnquote(((StringValue) params.get(2)).getValue()) : "";

          return new DotPathValue(dotPath, suffix);
        } else if (value.getFunctionName().equals(LITERAL_FUNCTION_NAME)) {
          // This is a call to value()
          List<Value> params = new ArrayList<Value>();
          extractValueOf(params, value.getParameters());

          if (params.size() != 1) {
            throw new CSSException(CSSException.SAC_SYNTAX_ERR,
                "Incorrect number of parameters to " + LITERAL_FUNCTION_NAME,
                null);
          }

          Value expression = params.get(0);
          if (!(expression instanceof StringValue)) {
            throw new CSSException(CSSException.SAC_SYNTAX_ERR,
                "The single argument to " + LITERAL_FUNCTION_NAME
                    + " must be a string value", null);
          }

          String s = maybeUnquote(((StringValue) expression).getValue());
          s = unescapeLiteral(s);

          return new IdentValue(s);

        } else {
          List<Value> parameters = new ArrayList<Value>();
          extractValueOf(parameters, value.getParameters());
          return new FunctionValue(value.getFunctionName(),
              new ListValue(parameters));
        }
      }
      case LexicalUnit.SAC_INHERIT:
        return new IdentValue("inherit");
      case LexicalUnit.SAC_OPERATOR_EXP:
        return new TokenValue("^");
      case LexicalUnit.SAC_OPERATOR_GE:
        return new TokenValue(">=");
      case LexicalUnit.SAC_OPERATOR_GT:
        return new TokenValue(">");
      case LexicalUnit.SAC_OPERATOR_LE:
        return new TokenValue("<=");
      case LexicalUnit.SAC_OPERATOR_LT:
        return new TokenValue("<");
      case LexicalUnit.SAC_OPERATOR_MINUS:
        return new TokenValue("-");
      case LexicalUnit.SAC_OPERATOR_MOD:
        return new TokenValue("%");
      case LexicalUnit.SAC_OPERATOR_MULTIPLY:
        return new TokenValue("*");
      case LexicalUnit.SAC_OPERATOR_PLUS:
        return new TokenValue("+");
      case LexicalUnit.SAC_OPERATOR_SLASH:
        return new TokenValue("/");
      case LexicalUnit.SAC_OPERATOR_TILDE:
        return new IdentValue("~");
      case LexicalUnit.SAC_RECT_FUNCTION: {
        // Just return this as a String
        List<Value> parameters = new ArrayList<Value>();
        extractValueOf(parameters, value.getParameters());
        return new IdentValue("rect(" + join(parameters, "") + ")");
      }
      case LexicalUnit.SAC_SUB_EXPRESSION:
        // Should have been taken care of by our own traversal
      case LexicalUnit.SAC_UNICODERANGE:
        // Cannot be expressed in CSS2
    }
    throw new RuntimeException("Unhandled LexicalUnit type "
        + value.getLexicalUnitType());
  }

  private static String valueOf(Selector selector) {
    if (selector instanceof CharacterDataSelector) {
      // Unimplemented in CSS2?

    } else if (selector instanceof ConditionalSelector) {
      ConditionalSelector s = (ConditionalSelector) selector;
      String simpleSelector = valueOf(s.getSimpleSelector());

      if ("*".equals(simpleSelector)) {
        // Don't need the extra * for compound selectors
        return valueOf(s.getCondition());
      } else {
        return simpleSelector + valueOf(s.getCondition());
      }

    } else if (selector instanceof DescendantSelector) {
      DescendantSelector s = (DescendantSelector) selector;
      switch (s.getSelectorType()) {
        case Selector.SAC_CHILD_SELECTOR:
          if (s.getSimpleSelector().getSelectorType() == Selector.SAC_PSEUDO_ELEMENT_SELECTOR) {
            return valueOf(s.getAncestorSelector()) + ":"
                + valueOf(s.getSimpleSelector());
          } else {
            return valueOf(s.getAncestorSelector()) + ">"
                + valueOf(s.getSimpleSelector());
          }
        case Selector.SAC_DESCENDANT_SELECTOR:
          return valueOf(s.getAncestorSelector()) + " "
              + valueOf(s.getSimpleSelector());
      }

    } else if (selector instanceof ElementSelector) {
      ElementSelector s = (ElementSelector) selector;
      if (s.getLocalName() == null) {
        return "*";
      } else {
        return escapeIdent(s.getLocalName());
      }

    } else if (selector instanceof NegativeSelector) {
      // Unimplemented in CSS2?

    } else if (selector instanceof ProcessingInstructionSelector) {
      // Unimplemented in CSS2?

    } else if (selector instanceof SiblingSelector) {
      SiblingSelector s = (SiblingSelector) selector;
      return valueOf(s.getSelector()) + "+" + valueOf(s.getSiblingSelector());
    }

    throw new RuntimeException("Unhandled selector of type "
        + selector.getClass().getName());
  }

  /**
   * Utility class.
   */
  private GenerateCssAst() {
  }
}
