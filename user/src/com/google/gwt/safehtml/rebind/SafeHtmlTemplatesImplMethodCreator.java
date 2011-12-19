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
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safehtml.client.SafeHtmlTemplates.Template;
import com.google.gwt.safehtml.rebind.ParsedHtmlTemplate.HtmlContext;
import com.google.gwt.safehtml.rebind.ParsedHtmlTemplate.LiteralChunk;
import com.google.gwt.safehtml.rebind.ParsedHtmlTemplate.ParameterChunk;
import com.google.gwt.safehtml.rebind.ParsedHtmlTemplate.TemplateChunk;
import com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;
import com.google.gwt.user.rebind.AbstractMethodCreator;

/**
 * Method body code generator for implementations of
 * {@link com.google.gwt.safehtml.client.SafeHtmlTemplates}.
 */
public class SafeHtmlTemplatesImplMethodCreator extends AbstractMethodCreator {

  /**
   * Fully-qualified class name of the {@link String} class.
   */
  private static final String JAVA_LANG_STRING_FQCN = String.class.getName();

  /**
   * Simple class name of the {@link SafeUri} interface.
   */
  private static final String SAFE_URI_CN = SafeUri.class.getSimpleName();

  /**
   * Fully-qualified class name of the {@link SafeUri} interface.
   */
  private static final String SAFE_URI_FQCN = SafeUri.class.getName();

  /**
   * Simple class name of the {@link SafeStyles} interface.
   */
  private static final String SAFE_STYLES_CN = SafeStyles.class.getSimpleName();

  /**
   * Fully-qualified class name of the {@link SafeStyles} interface.
   */
  private static final String SAFE_STYLES_FQCN = SafeStyles.class.getName();

  /**
   * Simple class name of the {@link SafeHtml} interface.
   */
  private static final String SAFE_HTML_CN = SafeHtml.class.getSimpleName();

  /**
   * Fully-qualified class name of the {@link SafeHtml} interface.
   */
  private static final String SAFE_HTML_FQCN = SafeHtml.class.getName();

  /**
   * Fully-qualified class name of the StringBlessedAsSafeHtml class.
   */
  private static final String BLESSED_STRING_FQCN =
      OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml.class.getName();

  /**
   * Fully-qualified class name of the {@link SafeHtmlUtils} class.
   */
  private static final String SAFE_HTML_UTILS_FQCN = SafeHtmlUtils.class.getName();

  /**
   * Fully-qualified class name of the {@link UriUtils} class.
   */
  private static final String URI_UTILS_FQCN = UriUtils.class.getName();

  public SafeHtmlTemplatesImplMethodCreator(AbstractGeneratorClassCreator classCreator) {
    super(classCreator);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createMethodFor(TreeLogger logger, JMethod targetMethod, String key,
      ResourceList resourceList, GwtLocale locale) throws UnableToCompleteException {
    if (!targetMethod.getReturnType().getQualifiedSourceName().equals(
        SafeHtmlTemplatesImplMethodCreator.SAFE_HTML_FQCN)) {
      throw error(logger, "All methods in interfaces extending "
          + "SafeHtmlTemplates must have a return type of "
          + SafeHtmlTemplatesImplMethodCreator.SAFE_HTML_FQCN + ".");
    }
    Template templateAnnotation = targetMethod.getAnnotation(Template.class);
    if (templateAnnotation == null) {
      throw error(logger, "Required annotation @Template not present on interface method "
          + targetMethod.toString());
    }

    String template = templateAnnotation.value();
    JParameter[] params = targetMethod.getParameters();
    emitMethodBodyFromTemplate(logger, template, params);
  }

  /**
   * Emits an expression corresponding to a template variable in "attribute"
   * context.
   *
   * <p>The expression emitted applies appropriate escaping and/or sanitization
   * to the parameter's value depending the Java type of the corresponding
   * template method parameter:
   *
   * <ul>
   *   <li>If the parameter is of type {@link SafeStyles}, it is converted to a
   *       string using {@link SafeStyles#asString()}.
   *   <li>Otherwise, if the parameter is of type {@link SafeUri}, it is converted to a
   *       string using {@link SafeUri#asString()}.
   *   <li>Otherwise, if the parameter is not of type {@link String}, it is
   *       first converted to {@link String}.
   *   <li>If the template parameter occurs at the start, or as the entire value,
   *       of a URI-valued attribute within the template, and the parameter isn't
   *       of type {@link SafeUri}, it is sanitized to ensure that it is safe in
   *       this context.  This is done by passing the value through
   *       {@link UriUtils#sanitizeUri(String)}.
   *   <li>The result is then HTML-escaped by passing it through
   *       {@link SafeHtmlUtils#htmlEscape(String)}.
   * </ul>
   *
   * <i>Note</i>: Template method parameters of type {@link SafeHtml} are
   * <i>not</i> treated specially in an attribute context, and will be HTML-
   * escaped like regular strings. This is because {@link SafeHtml} values can
   * contain non-escaped HTML markup, which is not valid within attributes.
   *
   * @param logger the logger to log failures to
   * @param htmlContext the HTML context in which the corresponding template
   *        variable occurs in
   * @param formalParameterName the name of the template method's formal
   *        parameter corresponding to the expression being emitted
   * @param parameterType the Java type of the corresponding template method's
   *        parameter
   */
  private void emitAttributeContextParameterExpression(TreeLogger logger, HtmlContext htmlContext,
      String formalParameterName, JType parameterType) {

    /*
     * Build up the expression from the "inside out", i.e. start with the formal
     * parameter, convert to string if necessary, then wrap in validators if
     * necessary, and finally HTML-escape if necessary.
     */
    String expression = formalParameterName;

    if (isSafeUri(parameterType) || isSafeStyles(parameterType)) {
      // SafeUri is safe in a URL context, and SafeStyles is safe in a CSS context,
      // so we can use their string (but we still escape it).
      expression = expression + ".asString()";
    } else {
      if (!JAVA_LANG_STRING_FQCN.equals(parameterType.getQualifiedSourceName())) {
        // The parameter's value must be explicitly converted to String unless it
        // is already of that type.
        expression = "String.valueOf(" + expression + ")";
      }

      if ((htmlContext.getType() == HtmlContext.Type.URL_ATTRIBUTE_START) ||
          (htmlContext.getType() == HtmlContext.Type.URL_ATTRIBUTE_ENTIRE)) {
        expression = URI_UTILS_FQCN + ".sanitizeUri(" + expression + ")";
      }
    }

    // TODO(xtof): Handle EscapedString subtype of SafeHtml, once it's been
    //     introduced.
    expression = SAFE_HTML_UTILS_FQCN + ".htmlEscape(" + expression + ")";

    print(expression);
  }

  /**
   * Generates code that renders the provided HTML template into an instance
   * of the {@link SafeHtml} type.
   *
   * <p>The template is parsed as a HTML template (see
   * {@link HtmlTemplateParser}).  From the template's parsed form, code is
   * generated that, when executed, will emit an instantiation of the template.
   * The generated code appropriately escapes and/or sanitizes template
   * parameters such that evaluating the emitted string as HTML in a browser
   * will not result in script execution.
   *
   * <p>As such, strings emitted from generated template methods satisfy the
   * type contract of the {@link SafeHtml} type, and can therefore be returned
   * wrapped as {@link SafeHtml}.
   *
   * @param logger the logger to log failures to
   * @param template the (X)HTML template to generate code for
   * @param params the parameters of the corresponding template method
   * @throws UnableToCompleteException if an error occurred that prevented
   *         code generation for the template
   */
  private void emitMethodBodyFromTemplate(TreeLogger logger, String template, JParameter[] params)
      throws UnableToCompleteException {
    println("StringBuilder sb = new java.lang.StringBuilder();");

    HtmlTemplateParser parser = new HtmlTemplateParser(logger);
    parser.parseTemplate(template);

    for (TemplateChunk chunk : parser.getParsedTemplate().getChunks()) {
      if (chunk.getKind() == TemplateChunk.Kind.LITERAL) {
        emitStringLiteral(((LiteralChunk) chunk).getLiteral());
      } else if (chunk.getKind() == TemplateChunk.Kind.PARAMETER) {
        ParameterChunk parameterChunk = (ParameterChunk) chunk;

        int formalParameterIndex = parameterChunk.getParameterIndex();
        if (formalParameterIndex < 0 || formalParameterIndex >= params.length) {
          throw error(logger, "Argument " + formalParameterIndex + " beyond range of arguments: "
              + template);
        }
        String formalParameterName = "arg" + formalParameterIndex;
        JType paramType = params[formalParameterIndex].getType();

        emitParameterExpression(
            logger, parameterChunk.getContext(), formalParameterName, paramType);
      } else {
        throw error(logger, "Unexpected chunk kind in parsed template " + template);
      }
    }
    outdent();
    outdent();
    println("return new " + BLESSED_STRING_FQCN + "(sb.toString());");
  }

  /**
   * Emits an expression corresponding to a template parameter.
   *
   * <p>
   * The expression emitted applies appropriate escaping/sanitization to the
   * parameter's value, depending on the parameter's HTML context, and the Java
   * type of the corresponding template method parameter.
   *
   * @param logger the logger to log failures to
   * @param htmlContext the HTML context in which the corresponding template
   *          variable occurs in
   * @param formalParameterName the name of the template method's formal
   *          parameter corresponding to the expression being emitted
   * @param parameterType the Java type of the corresponding template method's
   *          parameter
   * @throws UnableToCompleteException if the parameterType is not valid for the
   *           htmlContext
   */
  private void emitParameterExpression(TreeLogger logger, HtmlContext htmlContext,
      String formalParameterName, JType parameterType) throws UnableToCompleteException {

    /*
     * Verify that the parameter type is used in the correct context. Safe
     * expressions are only safe in specific contexts.
     */
    HtmlContext.Type contextType = htmlContext.getType();
    if (isSafeHtml(parameterType) && HtmlContext.Type.TEXT != contextType) {
      /*
       * SafeHtml used in a non-text context. SafeHtml is escaped for a text
       * context. In a non-text context, the string is not guaranteed to be
       * safe.
       */
      throw error(logger, SAFE_HTML_CN + " used in a non-text context. Did you mean to use "
          + JAVA_LANG_STRING_FQCN + " or " + SAFE_STYLES_CN + " instead?");
    } else if (isSafeStyles(parameterType) && HtmlContext.Type.CSS_ATTRIBUTE_START != contextType) {
      if (HtmlContext.Type.CSS_ATTRIBUTE == contextType) {
        // SafeStyles can only be used at the start of a CSS attribute.
        throw error(logger, SAFE_STYLES_CN + " cannot be used in the middle of a CSS attribute. "
            + "It must be used at the start a CSS attribute.");
      } else {
        /*
         * SafeStyles used in a non-css attribute context. SafeStyles is only
         * safe in a CSS attribute context. We could treat it as a normal
         * parameter and escape the string value of the parameter, but it almost
         * definitely isn't what the developer intended to do.
         */
        throw error(logger, SAFE_STYLES_CN
            + " used in a non-CSS attribute context. Did you mean to use " + JAVA_LANG_STRING_FQCN
            + " or " + SAFE_HTML_CN + " instead?");
      }
    } else if (isSafeUri(parameterType) && HtmlContext.Type.URL_ATTRIBUTE_ENTIRE != contextType) {
      // TODO(xtof): refactor HtmlContext with isStart/isEnd/isEntire accessors and simplified type.
      if (HtmlContext.Type.URL_ATTRIBUTE_START == contextType) {
        // SafeUri can only be used as the entire value of an URL attribute.
        throw error(logger, SAFE_URI_CN + " cannot be used in a URL attribute if it isn't the "
            + "entire attribute value.");
      } else {
        /*
         * SafeUri outside a URL-attribute context (or in a URL-attribute, but
         * not at start). SafeUri is only safe if it comprises the entire URL
         * attribute's value. We could treat it as a normal parameter and escape
         * the string value of the parameter, but it almost definitely isn't
         * what the developer intended to do.
         */
        throw error(logger, SAFE_URI_CN + " can only be used as the entire value of a URL "
            + "attribute. Did you mean to use " + JAVA_LANG_STRING_FQCN + " or " + SAFE_HTML_CN
            + " instead?");
      }
    }

    print("sb.append(");
    switch (contextType) {
      case CSS:
        /*
         * TODO(jlabanca): Handle CSS in a text context.
         *
         * The stream parser does not parse CSS; we could however improve safety
         * via sub-formats that specify the in-css context.
         *
         * SafeStyles is safe in a CSS context when used inside of a CSS style
         * rule, but they are not always safe. We could implement SafeCssRules,
         * which would consist of SafeStyles inside of a CSS style rules found
         * in a style tag.
         */
        logger.log(TreeLogger.WARN, "Template with variable in CSS context: "
            + "The template code generator cannot guarantee HTML-safety of "
            + "the template -- please inspect manually");
        emitTextContextParameterExpression(formalParameterName, parameterType);
        break;
      case TEXT:
        emitTextContextParameterExpression(formalParameterName, parameterType);
        break;

      case CSS_ATTRIBUTE:
      case CSS_ATTRIBUTE_START:
        /*
         * We already checked if the user tried to use SafeStyles in an invalid
         * (non-CSS_ATTRIBUTE) context, but now we check if the user could have
         * used SafeStyles in the current context.
         */
        if (!isSafeStyles(parameterType)) {
          // Warn against using unsafe parameters in a CSS attribute context.
          logger.log(TreeLogger.WARN,
              "Template with variable in CSS attribute context: The template code generator cannot"
                  + " guarantee HTML-safety of the template -- please inspect manually or use "
                  + SAFE_STYLES_CN + " to specify arguments in a CSS attribute context");
        }
        emitAttributeContextParameterExpression(logger, htmlContext, formalParameterName,
            parameterType);
        break;
      case URL_ATTRIBUTE_START:
      case URL_ATTRIBUTE_ENTIRE:
        /*
         * We already checked if the user tried to use SafeUri in an invalid
         * (non-URL_ATTRIBUTE) context, but now we check if the user could have
         * used SafeUri in the current context.
         */
        if (!isSafeUri(parameterType)) {
          // Warn against using unsafe parameters in a URL attribute context.
          logger.log(TreeLogger.INFO,
              "Template with variable in URL attribute context: The template code generator will"
                  + " sanitize the URL.  Use " + SAFE_URI_CN
                  + " to specify arguments in a URL attribute context that should not be"
                  + " sanitized.");
        }
        emitAttributeContextParameterExpression(logger, htmlContext, formalParameterName,
            parameterType);
        break;
      case ATTRIBUTE_VALUE:
        emitAttributeContextParameterExpression(logger, htmlContext, formalParameterName,
            parameterType);
        break;

      default:
        throw error(logger, "unknown HTML context for formal template parameter "
            + formalParameterName + ": " + htmlContext);
    }
    println(");");
  }

  /**
   * Emits a string literal.
   *
   * @param str the {@link String} to emit as a literal
   */
  private void emitStringLiteral(String str) {
    print("sb.append(");
    print(wrap(str));
    println(");");
  }

  /**
   * Emits an expression corresponding to a template variable in "inner text"
   * context.
   *
   * <p>The expression emitted applies appropriate escaping to the parameter's
   * value depending the Java type of the corresponding template method
   * parameter:
   *
   * <ul>
   *   <li>If the parameter is of a primitive (e.g., numeric, boolean) type, or
   *       of type {@link SafeHtml}, it is emitted as is, without escaping.
   *   <li>Otherwise, an expression that passes the parameter's value through
   *       {@link SafeHtmlUtils#htmlEscape(String)} is emitted. If the value is
   *       of type {@link SafeUri}, it is converted to string using
   *       {@link SafeUri#asString()}.
   * </ul>
   *
   * @param formalParameterName the name of the template method's formal
   *        parameter corresponding to the expression being emitted
   * @param parameterType the Java type of the corresponding template method's
   *        parameter
   */
  private void emitTextContextParameterExpression(String formalParameterName, JType parameterType) {
    boolean parameterIsPrimitiveType = (parameterType.isPrimitive() != null);
    boolean parameterIsNotStringTyped =
        !(JAVA_LANG_STRING_FQCN.equals(parameterType.getQualifiedSourceName()));

    if (isSafeHtml(parameterType)) {
      // The parameter is of type SafeHtml and its wrapped string can
      // therefore be emitted safely without escaping.
      print(formalParameterName + ".asString()");
    } else if (parameterIsPrimitiveType) {
      // The string representations of primitive types never contain HTML
      // special characters and can therefore be emitted without escaping.
      print(formalParameterName);
    } else {
      // The parameter is of some other type, and its value must be HTML
      // escaped. Furthermore, unless the parameter's type is {@link String},
      // it must be explicitly converted to {@link String}.
      String expression = formalParameterName;
      if (parameterIsNotStringTyped) {
        expression = "String.valueOf(" + expression + ")";
      }
      print(SAFE_HTML_UTILS_FQCN + ".htmlEscape(" + expression + ")");
    }
  }

  /**
   * Check if the specified parameter type represents a {@link SafeHtml}.
   *
   * @param parameterType the Java parameter type
   * @return true if the type represents a {@link SafeHtml}
   */
  private boolean isSafeHtml(JType parameterType) {
    return parameterType.getQualifiedSourceName().equals(SAFE_HTML_FQCN);
  }

  /**
   * Check if the specified parameter type represents a {@link SafeStyles}.
   *
   * @param parameterType the Java parameter type
   * @return true if the type represents a {@link SafeStyles}
   */
  private boolean isSafeStyles(JType parameterType) {
    return parameterType.getQualifiedSourceName().equals(SAFE_STYLES_FQCN);
  }

  /**
   * Check if the specified parameter type represents a {@link SafeUri}.
   *
   * @param parameterType the Java parameter type
   * @return true if the type represents a {@link SafeUri}
   */
  private boolean isSafeUri(JType parameterType) {
    return parameterType.getQualifiedSourceName().equals(SAFE_URI_FQCN);
  }
}
