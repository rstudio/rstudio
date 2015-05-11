/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.resources.converter;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssCharset;
import com.google.gwt.resources.css.ast.CssDef;
import com.google.gwt.resources.css.ast.CssEval;
import com.google.gwt.resources.css.ast.CssExternalSelectors;
import com.google.gwt.resources.css.ast.CssFontFace;
import com.google.gwt.resources.css.ast.CssIf;
import com.google.gwt.resources.css.ast.CssMediaRule;
import com.google.gwt.resources.css.ast.CssNoFlip;
import com.google.gwt.resources.css.ast.CssPageRule;
import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssProperty.DotPathValue;
import com.google.gwt.resources.css.ast.CssProperty.FunctionValue;
import com.google.gwt.resources.css.ast.CssProperty.Value;
import com.google.gwt.resources.css.ast.CssRule;
import com.google.gwt.resources.css.ast.CssSelector;
import com.google.gwt.resources.css.ast.CssSprite;
import com.google.gwt.resources.css.ast.CssUnknownAtRule;
import com.google.gwt.resources.css.ast.CssUrl;
import com.google.gwt.thirdparty.common.css.SourceCode;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssParser;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssParserException;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Splitter;
import com.google.gwt.thirdparty.guava.common.base.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The GssGenerationVisitor turns a css tree into a gss string.
 */
public class GssGenerationVisitor extends ExtendedCssVisitor {
  /* templates and tokens list */
  private static final String AND = " && ";
  private static final String CHARSET = "@charset \"%s\";";
  private static final String DEF = "@def ";
  private static final String ELSE = "@else ";
  private static final String ELSE_IF = "@elseif (%s)";
  private static final String EVAL = "eval('%s')";
  private static final String EXTERNAL = "@external";
  private static final String GWT_SPRITE = "gwt-sprite: \"%s\"";
  private static final String IF = "@if (%s)";
  private static final String IMPORTANT = " !important";
  private static final String IS = "is(\"%s\", \"%s\")";
  private static final String NO_FLIP = "/* @noflip */";
  private static final String NOT = "!";
  private static final String OR = " || ";
  private static final Pattern UNESCAPE = Pattern.compile("\\\\");
  private static final Pattern UNESCAPE_EXTERNAL = Pattern.compile("\\\\|@external|,|\\n|\\r");
  private static final String URL = "resourceUrl(\"%s\")";
  private static final String VALUE = "value('%s')";
  private static final String VALUE_WITH_SUFFIX = "value('%s', '%s')";

  // Used to quote font family name that contains white space(s) and aren't quoted yet.
  private static Pattern NOT_QUOTED_WITH_WITHESPACE = Pattern.compile("^[^'\"].*\\s.*[^'\"]$");

  // Used to sanitize the boolean conditions
  private static Pattern BANG_OPERATOR = Pattern.compile("^(!+)(.*)");

  // GSS impose constant names to be in uppercase. This Map will contains the mapping between
  // the name of constants defined in the CSS and the corresponding name that will be used in GSS.
  private final Map<String, String> cssToGssConstantMapping;
  private final TextOutput out;
  private final boolean lenient;
  private final TreeLogger treeLogger;
  private final Predicate<String> simpleBooleanConditionPredicate;
  // list of external at-rules defined inside a media at-rule.
  // In lenient mode, these nodes will be extracted and print outside the media at-rule.
  private final List<CssExternalSelectors> wrongExternalNodes = new
      ArrayList<CssExternalSelectors>();

  // list of constant definition nodes defined inside a media at-rule.
  // In lenient mode, these nodes will be extracted and print outside the media at-rule.
  private final List<CssDef> wrongDefNodes = new ArrayList<CssDef>();

  private boolean insideNoFlipNode;
  private boolean needsNewLine;
  private boolean needsOpenBrace;
  private boolean needsComma;
  private boolean insideMediaAtRule;
  // used to group a sequence of @def in one block
  private boolean previousNodeIsDef;
  // used to group asequence of @external in one block
  private boolean previousNodeIsExternal;

  public GssGenerationVisitor(TextOutput out, Map<String, String> cssToGssConstantMapping,
      boolean lenient, TreeLogger treeLogger, Predicate<String> simpleBooleanConditionPredicate) {
    this.cssToGssConstantMapping = cssToGssConstantMapping;
    this.out = out;
    this.lenient = lenient;
    this.treeLogger = treeLogger;
    this.simpleBooleanConditionPredicate = simpleBooleanConditionPredicate;
  }

  public String getContent() {
    return out.toString();
  }

  @Override
  public void endVisit(CssFontFace x, Context ctx) {
    closeBrace();
  }

  @Override
  public void endVisit(CssMediaRule x, Context ctx) {
    closeBrace();

    insideMediaAtRule = false;

    maybePrintWrongExternalNodes();
    maybePrintWrongDefNodes(ctx);
  }

  @Override
  public void endVisit(CssPageRule x, Context ctx) {
    closeBrace();
  }

  @Override
  public void endVisit(CssUnknownAtRule x, Context ctx) {
    // The old CSS resource has no support for many at-rules, like animations. There is no way
    // for us to parse them using the old CSS parser, so we will just output them as a string to
    // the GSS stylesheet and hope that the GSS parser is okay with the rule
    out.print(x.getRule());
  }

  @Override
  public boolean visit(CssSprite x, Context ctx) {
    return false;
  }

  @Override
  public void endVisit(CssSprite x, Context ctx) {
    needsComma = false;

    accept(x.getSelectors());
    openBrace();

    out.print(String.format(GWT_SPRITE, x.getResourceFunction().getPath()));
    semiColon();

    accept(x.getProperties());

    closeBrace();
  }

  @Override
  public boolean visit(CssDef x, Context ctx) {
    printDef(x, null, "def", false);

    previousNodeIsDef = true;
    previousNodeIsExternal = false;

    return false;
  }

  @Override
  public boolean visit(CssEval x, Context ctx) {
    printDef(x, EVAL, "eval", false);

    return false;
  }

  @Override
  public boolean visit(CssUrl x, Context ctx) {
    printDef(x, URL, "url", true);

    return false;
  }

  @Override
  public boolean visit(CssRule x, Context ctx) {
    maybePrintNewLine();

    needsOpenBrace = true;
    needsComma = false;
    needsNewLine = false;
    previousNodeIsDef = false;
    previousNodeIsExternal = false;

    return true;
  }

  @Override
  public void endVisit(CssRule x, Context ctx) {
    // empty rule block case.
    maybePrintOpenBrace();

    closeBrace();

    needsNewLine = true;
  }

  @Override
  public boolean visit(CssNoFlip x, Context ctx) {
    insideNoFlipNode = true;
    previousNodeIsDef = false;
    previousNodeIsExternal = false;
    return true;
  }

  @Override
  public boolean visit(CssExternalSelectors x, Context ctx) {
    if (insideMediaAtRule) {
      if (lenient) {
        treeLogger.log(Type.WARN, "An external at-rule is not allowed inside a @media at-rule. " +
            "The following external at-rule [" + x + "] will be moved in the upper scope");
        wrongExternalNodes.add(x);
      } else {
        treeLogger.log(Type.ERROR, "An external at-rule is not allowed inside a @media at-rule. " +
            "[" + x + "].");
        throw new Css2GssConversionException("An external at-rule is not allowed inside a @media" +
            " at-rule.");
      }
    } else {
      printExternal(x);
    }

    return false;
  }

  @Override
  public boolean visit(CssCharset x, Context ctx) {
    out.print(String.format(CHARSET, x.getCharset()));
    out.newlineOpt();

    needsNewLine = true;
    previousNodeIsDef = false;
    previousNodeIsExternal = false;

    return true;
  }

  private void maybePrintWrongExternalNodes() {
    if (!lenient) {
      return;
    }

    for (CssExternalSelectors external : wrongExternalNodes) {
      printExternal(external);
    }
    wrongExternalNodes.clear();
  }

  private void maybePrintWrongDefNodes(Context ctx) {
    if (!lenient) {
      return;
    }

    for (CssDef def : wrongDefNodes) {
      if (def instanceof CssUrl) {
        visit((CssUrl) def, ctx);
      } else if (def instanceof CssEval) {
        visit((CssEval) def, ctx);
      } else {
        visit(def, ctx);
      }
    }
    wrongDefNodes.clear();
  }

  private void printExternal(CssExternalSelectors x) {
    boolean first = true;
    for (String selector : x.getClasses()) {
      String unescaped = unescapeExternalClass(selector);
      if (validateExternalClass(selector) && !Strings.isNullOrEmpty(unescaped)) {
        if (first) {
          if (!previousNodeIsExternal) {
            maybePrintNewLine();
          }

          out.print(EXTERNAL);
          first = false;
        }

        out.print(" ");

        boolean needQuote = selector.endsWith("*");

        if (needQuote) {
          out.print("'");
        }

        out.printOpt(unescaped);

        if (needQuote) {
          out.print("'");
        }
      }
    }

    if (!first) {
      semiColon();
    }

    previousNodeIsDef = false;
    previousNodeIsExternal = true;
  }

  private boolean validateExternalClass(String selector) {
    if (selector.contains(":")) {
      if (lenient) {
        treeLogger.log(Type.WARN, "This invalid external selector will be skipped: " + selector);
        return false;
      } else {
        throw new Css2GssConversionException(
            "One of your external statements contains a pseudo class: " + selector);
      }
    }
    return true;
  }

  @Override
  public void endVisit(CssNoFlip x, Context ctx) {
    insideNoFlipNode = false;
  }

  @Override
  public boolean visit(CssProperty x, Context ctx) {
    maybePrintOpenBrace();

    StringBuilder propertyBuilder = new StringBuilder();

    if (insideNoFlipNode) {
      propertyBuilder.append(NO_FLIP);
      propertyBuilder.append(' ');
    }

    propertyBuilder.append(x.getName());
    propertyBuilder.append(": ");

    String valueListCss = printValuesList(x.getValues().getValues(), false);

    if ("font-family".equals(x.getName())) {
      // Font family names containing whitespace should be quoted.
      valueListCss = quoteFontFamilyWithWhiteSpace(valueListCss);
    }

    propertyBuilder.append(valueListCss);

    if (x.isImportant()) {
      propertyBuilder.append(IMPORTANT);
    }

    String cssProperty = propertyBuilder.toString();

    // See if we can parse the rule using the GSS parser and thus verify that the
    // rule is indeed correct CSS.
    try {
      new GssParser(new SourceCode(null, "body{" + cssProperty + "}")).parse();
    } catch (GssParserException e) {
      if (lenient) {
        // print a warning message and don't print the rule.
        treeLogger.log(Type.WARN, "The following rule is not valid and will be skipped: " +
            cssProperty);
        return false;
      } else {
        treeLogger.log(Type.ERROR, "The following rule is not valid. " +
            cssProperty);
        throw new Css2GssConversionException("Invalid css rule", e);
      }
    }

    out.print(cssProperty);

    semiColon();

    return true;
  }

  /**
   * Quotes the font family names that contains white space but aren't quoted yet. thus allowing
   * usage of fonts that might be mistaken for constants. RTis is also recommended by the CSS
   * specification: http://www.w3.org/TR/CSS2/fonts.html#propdef-font-family
   * <p>It's important to notice that the converter doesn't manage the case where a constant is
   * used inside a font family name with whitespace. The font family name will be quoted and
   * won't be replaced.
   * {@code
   *   @def myFontFamily Comic;
   *
   *   .div {
   *     font-family: Arial, myFontFamily sans MS;
   *   }
   * }
   *
   * will be converted to:
   *
   * {@code
   *   @def MY_FONT_FAMILY Comic;
   *
   *   .div {
   *    font-family: Arial, "MY_FONT_FAMILY sans MS";
   *   }
   * }
   * @param cssProperty
   */
  private String quoteFontFamilyWithWhiteSpace(String cssProperty) {
    StringBuilder valueBuilder = new StringBuilder();

    boolean first = true;

    for (String subProperty : Splitter.on(",").trimResults().omitEmptyStrings().split(cssProperty)) {
      if (first) {
        first = false;
      } else {
        valueBuilder.append(",");
      }

      if (NOT_QUOTED_WITH_WITHESPACE.matcher(subProperty).matches()) {
        valueBuilder.append("'" + subProperty + "'");
      } else {
        valueBuilder.append(subProperty);
      }
    }

    return valueBuilder.toString();
  }

  @Override
  public boolean visit(CssElse x, Context ctx) {
    closeBrace();

    out.print(ELSE);

    openBrace();

    needsNewLine = false;
    previousNodeIsDef = false;
    previousNodeIsExternal = false;

    return true;
  }

  @Override
  public boolean visit(CssElIf x, Context ctx) {
    closeBrace();

    openConditional(ELSE_IF, x);

    return true;
  }

  @Override
  public void endVisit(CssIf x, Context ctx) {
    closeBrace();
    needsNewLine = true;
  }

  @Override
  public boolean visit(CssIf x, Context ctx) {
    maybePrintNewLine();

    openConditional(IF, x);

    return true;
  }

  private void openConditional(String template, CssIf ifOrElif) {
    String condition;

    String runtimeCondition = extractExpression(ifOrElif);

    if (runtimeCondition != null) {
      if (simpleBooleanConditionPredicate.apply(runtimeCondition)) {
        condition = runtimeCondition;
      } else {
        condition = String.format(EVAL, runtimeCondition);
      }
    } else {
      condition = printConditionnalExpression(ifOrElif);
    }

    out.print(String.format(template, condition));

    openBrace();

    needsNewLine = false;
    previousNodeIsDef = false;
    previousNodeIsExternal = false;
  }

  private String extractExpression(CssIf ifOrElif) {
    String condition = ifOrElif.getExpression();

    if (condition == null) {
      return null;
    }

    if (condition.trim().startsWith("(")) {
      condition = condition.substring(1, condition.length() - 1);
    }

    // sanitize the expression. GSS doesn't accept more than one ! operator
    Matcher m = BANG_OPERATOR.matcher(condition);
    if (m.matches()) {
      String bangs = m.group(1);
      String replacement;
      if (bangs.length() % 2 == 0) {
        replacement = "";
      } else {
        replacement = "!";
      }

      condition = m.replaceFirst(replacement + "$2");
    }

    return condition;
  }

  @Override
  public boolean visit(CssFontFace x, Context ctx) {
    out.print("@font-face");

    openBrace();

    previousNodeIsDef = false;
    previousNodeIsExternal = false;

    return true;
  }

  @Override
  public boolean visit(CssMediaRule x, Context ctx) {
    maybePrintNewLine();

    insideMediaAtRule = true;

    out.print("@media");
    boolean isFirst = true;
    for (String m : x.getMedias()) {
      if (isFirst) {
        out.print(" ");
        isFirst = false;
      } else {
        comma();
      }
      out.print(m);
    }
    spaceOpt();
    out.print("{");
    out.newlineOpt();
    out.indentIn();

    needsNewLine = false;
    previousNodeIsDef = false;
    previousNodeIsExternal = false;

    return true;
  }

  @Override
  public boolean visit(CssPageRule x, Context ctx) {
    out.print("@page");
    if (x.getPseudoPage() != null) {
      out.print(" :");
      out.print(x.getPseudoPage());
    }
    spaceOpt();
    out.print("{");
    out.newlineOpt();
    out.indentIn();

    previousNodeIsDef = false;
    previousNodeIsExternal = false;

    return true;
  }

  @Override
  public boolean visit(CssSelector x, Context ctx) {
    if (needsComma) {
      comma(false);
    }

    maybePrintNewLine();

    needsComma = true;

    needsNewLine = true;

    out.print(unescape(x.getSelector()));

    return true;
  }

  private void printDef(CssDef def, String valueTemplate, String atRule, boolean insideUrlNode) {
    if (validateDefNode(def, atRule)) {
      if (!previousNodeIsDef) {
        maybePrintNewLine();
      }

      out.print(DEF);

      String name = cssToGssConstantMapping.get(def.getKey());

      if (name == null) {
        throw new Css2GssConversionException("unknown @" + atRule + " rule [" + def.getKey() + "]");
      }

      out.print(name);
      out.print(' ');

      String values = printValuesList(def.getValues(), insideUrlNode);

      if (valueTemplate != null) {
        out.print(String.format(valueTemplate, values));
      } else {
        out.print(values);
      }

      semiColon();

      previousNodeIsDef = true;
      needsNewLine = true;
    }
  }

  private boolean validateDefNode(CssDef def, String atRule) {
    if (insideMediaAtRule) {
      if (lenient) {
        treeLogger.log(Type.WARN, "A " + atRule + " is not allowed inside a @media at-rule." +
            "The following " + atRule + " [" + def + "] will be moved in the upper scope");
        wrongDefNodes.add(def);
        return false;
      } else {
        treeLogger.log(Type.ERROR, "A " + atRule + " is not allowed inside a @media at-rule. ["
            + def + "]");
        throw new Css2GssConversionException("A " + atRule + " is not allowed inside a @media " +
            "at-rule.");
      }
    }
    return true;
  }

  private void closeBrace() {
    out.indentOut();
    out.print('}');
    out.newlineOpt();
  }

  private void comma() {
    comma(true);
  }

  private void comma(boolean addSpace) {
    out.print(',');

    if (addSpace) {
      spaceOpt();
    }
  }

  private void openBrace() {
    spaceOpt();
    out.print('{');
    out.newlineOpt();
    out.indentIn();
  }

  private void semiColon() {
    out.print(';');
    out.newlineOpt();
  }

  private void spaceOpt() {
    out.printOpt(' ');
  }

  private void maybePrintOpenBrace() {
    if (needsOpenBrace) {
      openBrace();
      needsOpenBrace = false;
    }
  }

  private void maybePrintNewLine() {
    if (needsNewLine) {
      out.newlineOpt();
    }
  }

  private String printConditionnalExpression(CssIf x) {
    if (x == null || x.getExpression() != null) {
      throw new IllegalStateException();
    }

    StringBuilder builder = new StringBuilder();

    String propertyName = x.getPropertyName();

    for (String propertyValue : x.getPropertyValues()) {
      if (builder.length() != 0) {
        if (x.isNegated()) {
          builder.append(AND);
        } else {
          builder.append(OR);
        }
      }

      if (x.isNegated()) {
        builder.append(NOT);
      }

      builder.append(String.format(IS, propertyName, propertyValue));
    }

    return builder.toString();
  }

  private String printValuesList(List<Value> values, boolean insideUrlNode) {
    StringBuilder builder = new StringBuilder();

    for (Value value : values) {
      if (value.isSpaceRequired() && builder.length() != 0) {
        builder.append(' ');
      }

      String expression = value.toCss();

      if (value.isIdentValue() != null && cssToGssConstantMapping.containsKey(expression)) {
        expression = cssToGssConstantMapping.get(expression);
      } else if (value.isExpressionValue() != null) {
        expression = value.getExpression();
      } else if (value.isDotPathValue() != null) {
        DotPathValue dotPathValue = value.isDotPathValue();
        if (insideUrlNode) {
          expression = dotPathValue.getPath();
        } else {
          if (Strings.isNullOrEmpty(dotPathValue.getSuffix())) {
            expression = String.format(VALUE, dotPathValue.getPath());
          } else {
            expression =
                String.format(VALUE_WITH_SUFFIX, dotPathValue.getPath(), dotPathValue.getSuffix());
          }
        }
      } else if (value.isFunctionValue() != null) {
        FunctionValue functionValue = value.isFunctionValue();

        // process the argument list values
        String arguments = printValuesList(functionValue.getValues().getValues(), insideUrlNode);

        expression = unescape(functionValue.getName()) + "(" + arguments + ")";
      }

      // don't escape content of quoted string and don't escape isFunctionValue because the
      // arguments and the name of the functions are already unescaped if needed.
      if (value.isStringValue() != null || value.isFunctionValue() != null) {
        builder.append(expression);
      } else {
        builder.append(unescape(expression));
      }
    }

    return builder.toString();
  }

  private String unescape(String toEscape) {
    return UNESCAPE.matcher(toEscape).replaceAll("");
  }

  private String unescapeExternalClass(String external) {
    return UNESCAPE_EXTERNAL.matcher(external).replaceAll("");
  }
}
