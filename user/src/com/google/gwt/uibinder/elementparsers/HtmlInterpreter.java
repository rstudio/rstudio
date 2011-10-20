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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLAttribute;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.XMLElement.Interpreter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This is the most generally useful interpreter, and the most likely to be used
 * by a custom parser when calling {@link XMLElement#consumeInnerHtml}.
 * <ul>
 * <li>Assigns computed values to element attributes (e.g.
 * class="{style.pretty}")
 * <li>Generates fields to hold named dom elements (e.g. &lt;div
 * gwt:field="importantDiv"&gt;)
 * <li>Turns &lt;ui:msg&gt; and &lt;ui:attr&gt; elements into methods on a
 * generated Messages interface
 * <li>Fails if any element encountered is a widget
 * </ul>
 */
public class HtmlInterpreter implements XMLElement.Interpreter<String> {
  private class AttributeTypist implements ComputedAttributeInterpreter.Delegate {
    @Override
    public String getAttributeToken(XMLAttribute attribute) throws UnableToCompleteException {
      if (URI_ATTRIBUTES.contains(attribute.getLocalName())) {
        return writer.tokenForSafeUriExpression(attribute.getElement(),
            attribute.consumeSafeUriOrStringAttribute());
      }

      return writer.tokenForStringExpression(attribute.getElement(), attribute.consumeStringValue());
    }
  }

  private static final Set<String> URI_ATTRIBUTES;
  static {

    /*
     * Perhaps we "should" check the element names as well, but the spec implies
     * that an attribute name always has the same meaning wherever it appears.
     * It's less work this way, and we're more likely to catch new elements.
     * 
     * http://dev.w3.org/html5/spec/index.html#attributes-1
     * http://www.w3.org/TR/REC-html40/index/attributes.html
     */
    String[] urlAttributes =
        {
            "action", "background", "classid", "cite", "codebase", "data", "formaction", "href",
            "icon", "longdesc", "manifest", "profile", "poster", "src", "usemap"};

    URI_ATTRIBUTES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(urlAttributes)));
  }

  /**
   * A convenience factory method for the most common use of this class, to work
   * with HTML that will eventually be rendered under a
   * {@link com.google.gwt.user.client.ui.UIObject} (or really, any object that
   * responds to <code>getElement()</code>). Uses an instance of
   * {@link HtmlMessageInterpreter} to process message elements.
   * 
   * @param uiExpression An expression that can be evaluated at runtime to find
   *          an object whose getElement() method can be called to get an
   *          ancestor of all Elements generated from the interpreted HTML.
   */
  public static HtmlInterpreter newInterpreterForUiObject(UiBinderWriter writer, String uiExpression) {
    String ancestorExpression =
        writer.useLazyWidgetBuilders() ? uiExpression : uiExpression + ".getElement()";
    return new HtmlInterpreter(writer, ancestorExpression, new HtmlMessageInterpreter(writer,
        ancestorExpression));
  }

  private final UiBinderWriter writer;
  private final InterpreterPipe<String> pipe;

  /**
   * Rather than using this constructor, you probably want to use the
   * {@link #newInterpreterForUiObject} factory method.
   * 
   * @param ancestorExpression An expression that can be evaluated at runtime to
   *          find an Element that will be an ancestor of all Elements generated
   *          from the interpreted HTML.
   * @param messageInterpreter an interpreter to handle msg and ph elements,
   *          typically an instance of {@link HtmlMessageInterpreter}. This
   *          interpreter gets last crack
   */
  public HtmlInterpreter(UiBinderWriter writer, String ancestorExpression,
      Interpreter<String> messageInterpreter) {
    this.writer = writer;

    this.pipe = new InterpreterPipe<String>();

    pipe.add(new FieldInterpreter(writer, ancestorExpression));
    /*
     * UiTextInterpreter and UiSafeHtmlInterpreter must be invoked before
     * ComputedAttributeInterpreter to function properly
     */
    pipe.add(new UiTextInterpreter(writer));
    pipe.add(new UiSafeHtmlInterpreter(writer));
    pipe.add(new ComputedAttributeInterpreter(writer, new AttributeTypist()));
    pipe.add(new AttributeMessageInterpreter(writer));
    pipe.add(messageInterpreter);
  }

  public String interpretElement(XMLElement elem) throws UnableToCompleteException {
    if (writer.useLazyWidgetBuilders() && writer.isElementAssignableTo(elem, SafeHtml.class)) {
      FieldWriter childField = writer.parseElementToField(elem);
      return writer.tokenForSafeHtmlExpression(elem, childField.getNextReference());
    }
    if (writer.isImportedElement(elem)) {
      writer.die(elem, "Not allowed in an HTML context");
    }

    if (elem.getNamespaceUri() != null && !writer.isBinderElement(elem)) {
      // It's not a widget, and it's not a ui: element.
      writer.die(elem, "Prefix \"%s:\" has unrecognized xmlns \"%s\" (bad import?)",
        elem.getPrefix(), elem.getNamespaceUri());
    }

    return pipe.interpretElement(elem);
  }
}
