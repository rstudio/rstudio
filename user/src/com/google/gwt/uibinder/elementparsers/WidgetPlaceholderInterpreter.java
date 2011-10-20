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
import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.client.LazyDomElement;
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
  /*
   * Could break this up into three further classes, for HasText, HasHTML and
   * Other, but that seems more trouble than it's worth.
   */

  private final FieldManager fieldManager;
  private int serial = 0;
  private final String ancestorExpression;
  private final String fieldName;
  private final Map<String, XMLElement> idToWidgetElement =
    new HashMap<String, XMLElement>();
  private final Set<String> idIsHasHTML = new HashSet<String>();
  private final Set<String> idIsHasText = new HashSet<String>();

  WidgetPlaceholderInterpreter(String fieldName, UiBinderWriter writer,
      MessageWriter message, String ancestorExpression) {
    super(writer, message, ancestorExpression);
    this.fieldName = fieldName;
    this.ancestorExpression = ancestorExpression;
    this.fieldManager = uiWriter.getFieldManager();
 }

  @Override
  public String interpretElement(XMLElement elem)
      throws UnableToCompleteException {

    if (!uiWriter.isWidgetElement(elem)) {
      return super.interpretElement(elem);
    }

    JClassType type = uiWriter.findFieldType(elem);
    TypeOracle oracle = uiWriter.getOracle();

    MessagesWriter mw = uiWriter.getMessages();
    String name = mw.consumeMessageAttribute("ph", elem);
    if ("".equals(name)) {
      name = "widget" + (++serial);
    }

    String idHolder = uiWriter.declareDomIdHolder(null);
    idToWidgetElement.put(idHolder, elem);

    if (oracle.findType(HasHTML.class.getName()).isAssignableFrom(type)) {
      return handleHasHTMLPlaceholder(elem, name, idHolder);
    }

    if (oracle.findType(HasText.class.getName()).isAssignableFrom(type)) {
      return handleHasTextPlaceholder(elem, name, idHolder);
   }

    return handleOpaqueWidgetPlaceholder(elem, name, idHolder);
  }

  /**
   * Called by {@link XMLElement#consumeInnerHtml} after all elements
   * have been handed to {@link #interpretElement}.
   */
  @Override
  public String postProcess(String consumed) throws UnableToCompleteException {
    FieldWriter fieldWriter = fieldManager.require(fieldName);

    for (String idHolder : idToWidgetElement.keySet()) {
      XMLElement childElem = idToWidgetElement.get(idHolder);
      FieldWriter childFieldWriter = uiWriter.parseElementToField(childElem);

      genSetWidgetTextCall(idHolder, childFieldWriter.getName());

      if (uiWriter.useLazyWidgetBuilders()) {
        // Register a DOM id field.
        String lazyDomElementPath = LazyDomElement.class.getCanonicalName();
        String elementPointer = idHolder + "Element";
        FieldWriter elementWriter = fieldManager.registerField(
            lazyDomElementPath, elementPointer);
        elementWriter.setInitializer(String.format("new %s<Element>(%s)",
            lazyDomElementPath, fieldManager.convertFieldToGetter(idHolder)));

        // Add attach/detach sections for this element.
        fieldWriter.addAttachStatement("%s.get();",
            fieldManager.convertFieldToGetter(elementPointer));
        fieldWriter.addDetachStatement(
            "%s.addAndReplaceElement(%s, %s.get());",
            fieldName,
            fieldManager.convertFieldToGetter(childFieldWriter.getName()),
            fieldManager.convertFieldToGetter(elementPointer));
      } else {
        uiWriter.addInitStatement(
            "%1$s.addAndReplaceElement(%2$s, %3$s);",
            fieldName, childFieldWriter.getName(), idHolder);
      }
    }

    /*
     * We get used recursively, so this will be called again. Empty the map
     * or else we'll re-register things.
     */
    idToWidgetElement.clear();
    return super.postProcess(consumed);
  }

  private String genCloseTag(String name) {
    String closePlaceholder = nextClosePlaceholder(name + "End", "</span>");
    return closePlaceholder;
  }

  private String genOpenTag(XMLElement source, String name, String idHolder) {
    idHolder = fieldManager.convertFieldToGetter(idHolder);
    if (uiWriter.useSafeHtmlTemplates()) {
      idHolder = uiWriter.tokenForStringExpression(source, idHolder);
    } else {
      idHolder = "\" + " + idHolder + " + \"";
    }
    String openTag = String.format("<span id='%s'>", idHolder);
    String openPlaceholder = nextOpenPlaceholder(name + "Begin", openTag);
    return openPlaceholder;
  }

  private void genSetWidgetTextCall(String idHolder, String childField) {

    if (uiWriter.useLazyWidgetBuilders()) {
      if (idIsHasText.contains(idHolder)) {
        fieldManager.require(fieldName).addAttachStatement(
            "%s.setText(%s.getElementById(%s).getInnerText());",
            fieldManager.convertFieldToGetter(childField),
            fieldName,
            fieldManager.convertFieldToGetter(idHolder));
      } else if (idIsHasHTML.contains(idHolder)) {
        fieldManager.require(fieldName).addAttachStatement(
            "%s.setHTML(%s.getElementById(%s).getInnerHTML());",
            fieldManager.convertFieldToGetter(childField),
            fieldName,
            fieldManager.convertFieldToGetter(idHolder));
      }
    } else {
      if (idIsHasText.contains(idHolder)) {
        uiWriter.addInitStatement(
            "%s.setText(%s.getElementById(%s).getInnerText());", childField,
            fieldName, idHolder);
      }
      if (idIsHasHTML.contains(idHolder)) {
        uiWriter.addInitStatement(
            "%s.setHTML(%s.getElementById(%s).getInnerHTML());", childField,
            fieldName, idHolder);
      }
    }
  }

  private String handleHasHTMLPlaceholder(XMLElement elem, String name,
      String idHolder) throws UnableToCompleteException {
    idIsHasHTML.add(idHolder);
    String openPlaceholder = genOpenTag(elem, name, idHolder);

    String body =
        elem.consumeInnerHtml(new HtmlPlaceholderInterpreter(uiWriter,
            message, ancestorExpression));
    String bodyToken = tokenator.nextToken(body);

    String closePlaceholder = genCloseTag(name);
    return openPlaceholder + bodyToken + closePlaceholder;
  }

  private String handleHasTextPlaceholder(XMLElement elem, String name,
      String idHolder) throws UnableToCompleteException {
    idIsHasText.add(idHolder);
    String openPlaceholder = genOpenTag(elem, name, idHolder);

    String body =
        elem.consumeInnerText(new TextPlaceholderInterpreter(uiWriter,
            message));
    String bodyToken = tokenator.nextToken(body);

    String closePlaceholder = genCloseTag(name);
    return openPlaceholder + bodyToken + closePlaceholder;
  }

  private String handleOpaqueWidgetPlaceholder(XMLElement source, String name, String idHolder) {
    idHolder = fieldManager.convertFieldToGetter(idHolder);
    if (uiWriter.useSafeHtmlTemplates()) {
      idHolder = uiWriter.tokenForStringExpression(source, idHolder);
    } else {
      idHolder = "\" + " + idHolder + " + \"";
    }
    String tag = String.format("<span id='%s'></span>", idHolder);
    String placeholder = nextPlaceholder(name, "<span></span>", tag);
    return placeholder;
  }
}
