/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.uibinder.rebind.model;

import com.google.gwt.uibinder.attributeparsers.SafeUriAttributeParser;
import com.google.gwt.uibinder.rebind.FieldReference;
import com.google.gwt.uibinder.rebind.IndentedWriter;
import com.google.gwt.uibinder.rebind.Tokenator;
import com.google.gwt.uibinder.rebind.Tokenator.Resolver;
import com.google.gwt.uibinder.rebind.Tokenator.ValueAndInfo;
import com.google.gwt.uibinder.rebind.XMLElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Models an individual SafeHtmlTemplates method in an
 * {@link HtmlTemplatesWriter}.
 */
public class HtmlTemplateMethodWriter {
  private class Argument {
    final XMLElement source;
    /**
     * Type of the parameter.
     */
    final ArgumentType type;
    /**
     * The expression to fill this parameter when the template method is called.
     */
    final String expression;

    Argument(XMLElement source, ArgumentType type, String expression) {
      this.source = source;
      this.type = type;
      this.expression = expression;
    }

    public XMLElement getSource() {
      return source;
    }

    @Override
    public String toString() {
      return "HtmlTemplateMethod.Argument: " + expression;
    }

    FieldReference getFieldReference() {
      FieldReference fieldReference = templates.getFieldManager().findFieldReference(expression);
      return fieldReference;
    }
  }

  private enum ArgumentType {
    STRING("String"), HTML("SafeHtml"), URI("SafeUri");

    final String typeString;

    ArgumentType(String typeString) {
      this.typeString = typeString;
    }

    @Override
    public String toString() {
      return typeString;
    }
  }

  private final List<String> strings = new ArrayList<String>();
  private final String methodName;
  private final ArrayList<Argument> methodArgs = new ArrayList<Argument>();
  private final HtmlTemplatesWriter templates;
  private final String html;
  private final Tokenator tokenator;
  private boolean argumentsResolved = false;

  public HtmlTemplateMethodWriter(String html, Tokenator tokenator, HtmlTemplatesWriter templates)
      throws IllegalArgumentException {
    assertNotNull("html", html);
    assertNotNull("tokenator", tokenator);
    assertNotNull("templates", templates);

    this.templates = templates;
    methodName = "html" + this.templates.nextTemplateId();

    this.html = html;
    this.tokenator = tokenator;
  }

  public String getDirectTemplateCall() {
    ensureArgumentsResolved();
    return String.format("template.%s(%s)", methodName, getTemplateCallArguments());
  }

  /**
   * Returns an expression that will return the results of a call to this
   * method.
   * 
   * @return
   */
  public String getIndirectTemplateCall() {
    return "template_" + methodName + "()";
  }

  public boolean isStringReference(Argument arg) {
    FieldReference fieldReference = arg.getFieldReference();
    return fieldReference != null
        && fieldReference.getReturnType().getSimpleSourceName().equals("String");
  }

  /**
   * Creates the template method invocation.
   * 
   * @param w
   * 
   * @return String the template method call with parameters
   */
  public void writeTemplateCaller(IndentedWriter w) {
    ensureArgumentsResolved();

    w.write("SafeHtml template_%s() {", methodName);
    w.indent();
    w.write("return %s;", getDirectTemplateCall());
    w.outdent();
    w.write("}");
  }

  /**
   * Writes all templates to the provided {@link IndentedWriter}.
   * 
   * @param w the writer to write the template to
   */
  public void writeTemplateMethod(IndentedWriter w) {
    ensureArgumentsResolved();
    for (String s : strings) {
      w.write(s);
    }
  }

  /**
   * Creates the argument string for the generated SafeHtmlTemplate function.
   */
  private String addTemplateParameters() {
    StringBuilder b = new StringBuilder();
    int i = 0;

    for (Argument arg : methodArgs) {
      if (b.length() > 0) {
        b.append(", ");
      }
      b.append(arg.type + " arg" + i);
      i++;
    }

    return b.toString();
  }

  /**
   * Replaces string tokens with {} placeholders for SafeHtml templating.
   * 
   * @return the rendering string, with tokens replaced by {} placeholders
   */
  private String addTemplatePlaceholders(String html) {
    String rtn = Tokenator.detokenate(html, new Resolver() {
      int tokenId = 0;

      public String resolveToken(String token) {
        return "{" + tokenId++ + "}";
      }
    });
    return rtn;
  }

  private void assertNotNull(String name, Object value) {
    if (value == null) {
      throw new IllegalArgumentException(name + " cannot be null");
    }
  }

  private void ensureArgumentsResolved() {
    if (argumentsResolved) {
      return;
    }

    if (tokenator != null) {
      List<ValueAndInfo> valuesAndSources = tokenator.getOrderedValues(html);

      for (ValueAndInfo valueAndSource : valuesAndSources) {
        XMLElement source = (XMLElement) valueAndSource.info;
        String expression = valueAndSource.value;

        if (templates.isSafeConstant(expression)) {
          methodArgs.add(new Argument(source, ArgumentType.HTML, expression));
        } else if (templates.isUri(expression)) {
          methodArgs.add(new Argument(source, ArgumentType.URI, expression));
        } else {
          // Nasty. Chop off the "" + stuff surrounding spring expressions
          String guts = expression.substring(4, expression.length() - 4);
          methodArgs.add(new Argument(source, ArgumentType.STRING, guts));
        }
      }
    }

    strings.add("@Template(\"" + addTemplatePlaceholders(html) + "\")");
    strings.add("SafeHtml " + methodName + "(" + addTemplateParameters() + ");");
    strings.add(" ");

    argumentsResolved = true;
  }

  /**
   * Retrieves the arguments for SafeHtml template function call from the
   * {@link Tokenator}.
   */
  private String getTemplateCallArguments() {
    StringBuilder b = new StringBuilder();

    for (Argument arg : methodArgs) {
      if (b.length() > 0) {
        b.append(", ");
      }
      String argExpression = processArgExpression(arg);

      b.append(argExpression);
    }

    return b.toString();
  }

  private String processArgExpression(Argument arg) {
    String raw = arg.expression;
    if (arg.type == ArgumentType.URI) {
      if (isStringReference(arg)) {
        return SafeUriAttributeParser.wrapUnsafeStringAndWarn(templates.getLogger(),
            arg.getSource(), raw);
      }
    }
    return raw;
  }
}
