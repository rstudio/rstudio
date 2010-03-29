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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.uibinder.rebind.DomCursor;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.messages.MessageWriter;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HasText;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Used by {@link HTMLPanelParser}. Refines {@link HtmlPlaceholderInterpreter}
 * to allow widgets to appear inside msg elements in an HTMLPanel.
 * <p>
 * HasText and HasHTML get special treatment, where their innerText or innerHTML
 * become part of the {@literal @}Default value of the message being generated.
 * E.g., this markup in an HTMLPanel:
 *
 * <pre>
 * <m:msg>Hello &lt;gwt:HyperLink&gt;click here&lt;/gwt:HyperLink&gt; thank you.</m:msg></pre>
 *
 * becomes a message like this: <pre>
 * {@literal @}Default("Hello {0}click here{1} thank you.")
 * String getMessage1(
 *   {@literal @}Example("&lt;span&gt;") String widget1Begin,
 *   {@literal @}Example("&lt;/span&gt;") String widget1End
 * );</pre>
 *
 * <p>
 * The contents of other widget types are opaque to the message, and are covered
 * by a single placeholder. One implication of this is that the content of an
 * HTMLPanel inside a msg in another HTMLPanel must always be in a separate
 * message.
 */
class WidgetPlaceholderInterpreter extends HtmlPlaceholderInterpreter {
  // Could break this up into three further classes, for HasText, HasHTML
  // and Other, but that seems more trouble than it's worth.

  private int serial = 0;
  private final String ancestorExpression;
  private final String fieldName;
  private final Map<String, XMLElement> elementHolderToWidgetElement = 
      new HashMap<String, XMLElement>();
  private final Set<String> elementIsHasHTML = new HashSet<String>();
  private final Set<String> elementIsHasText = new HashSet<String>();

  WidgetPlaceholderInterpreter(String fieldName, UiBinderWriter writer,
      MessageWriter message, String ancestorExpression) {
    super(writer, message, ancestorExpression);
    this.fieldName = fieldName;
    this.ancestorExpression = ancestorExpression;
 }

  @Override
  public String interpretElement(XMLElement elem)
      throws UnableToCompleteException {

    if (!uiWriter.isWidgetElement(elem)) {
      return super.interpretElement(elem);
    }

    uiWriter.addInitComment("WidgetPlaceholderInterpreter.interpretElement");
    JClassType type = uiWriter.findFieldType(elem);
    TypeOracle oracle = uiWriter.getOracle();

    MessagesWriter mw = uiWriter.getMessages();
    String name = mw.consumeMessageAttribute("ph", elem);
    if ("".equals(name)) {
      name = "widget" + (++serial);
    }

    DomCursor cursor = uiWriter.getCurrentDomCursor();
    String elementHolder = cursor.getAccessExpression();
    // We are going to generate another element right here, so we advance the child pointer.
    cursor.advanceChild();
    elementHolderToWidgetElement.put(elementHolder, elem);

    if (oracle.findType(HasHTML.class.getName()).isAssignableFrom(type)) {
      return handleHasHTMLPlaceholder(elem, name, elementHolder);
    }

    if (oracle.findType(HasText.class.getName()).isAssignableFrom(type)) {
      return handleHasTextPlaceholder(elem, name, elementHolder);
   }

    return handleOpaqueWidgetPlaceholder(name);
  }

 
  /**
   * Called by {@link XMLElement#consumeInnerHtml} after all elements
   * have been handed to {@link #interpretElement}.
   */
  @Override
  public String postProcess(String consumed) throws UnableToCompleteException {
    for (Map.Entry<String, XMLElement> entry : elementHolderToWidgetElement.entrySet()) {
      String element = entry.getKey();
      XMLElement childElem = entry.getValue();
      String childField = uiWriter.parseElementToField(childElem);

      genSetWidgetTextCall(element, childField);
      
      uiWriter.addInitStatement("%1$s.addAndReplaceElement(%2$s, " +
          "(com.google.gwt.user.client.Element)%3$s);",
          fieldName, childField, element);
    }
    /*
     * We get used recursively, so this will be called again. Empty the map 
     * or else we'll re-register things.
     */
    elementHolderToWidgetElement.clear();
    return super.postProcess(consumed);
  }

  private String genCloseTag(String name) {
    String closePlaceholder =
        nextPlaceholder(name + "End", "</span>", "</span>");
    return closePlaceholder;
  }

  private String genOpenTag(String name) {
    String openTag = "<span>";
    String openPlaceholder =
        nextPlaceholder(name + "Begin", "<span>", openTag);
    return openPlaceholder;
  }

  private void genSetWidgetTextCall(String elementHolder, String childField) {
    if (elementIsHasText.contains(elementHolder)) {
      uiWriter.addInitStatement(
          "%s.setText(((com.google.gwt.user.client.Element)%s).getInnerText());", 
          childField, elementHolder);
    }
    if (elementIsHasHTML.contains(elementHolder)) {
      uiWriter.addInitStatement(
          "%s.setHTML(((com.google.gwt.user.client.Element)%s).getInnerHTML());", 
          childField, elementHolder);
    }
  }

  private String handleHasHTMLPlaceholder(XMLElement elem, String name,
      String elementHolder) throws UnableToCompleteException {
    elementIsHasHTML.add(elementHolder);
    String openPlaceholder = genOpenTag(name);

    DomCursor cursor = uiWriter.beginDomSection(elementHolder);
    String body =
        elem.consumeInnerHtml(new HtmlPlaceholderInterpreter(uiWriter,
            message, ancestorExpression), cursor);
    uiWriter.endDomSection();
    String bodyToken = tokenator.nextToken(body);

    String closePlaceholder = genCloseTag(name);
    return openPlaceholder + bodyToken + closePlaceholder;
  }

  private String handleHasTextPlaceholder(XMLElement elem, String name,
      String elementHolder) throws UnableToCompleteException {
    elementIsHasText.add(elementHolder);
    String openPlaceholder = genOpenTag(name);

    String body =
        elem.consumeInnerText(new TextPlaceholderInterpreter(uiWriter,
            message));
    String bodyToken = tokenator.nextToken(body);

    String closePlaceholder = genCloseTag(name);
    return openPlaceholder + bodyToken + closePlaceholder;
  }

  private String handleOpaqueWidgetPlaceholder(String name) {
    String tag = "<span></span>";
    String placeholder = nextPlaceholder(name, "<span></span>", tag);
    return placeholder;
  }
}
