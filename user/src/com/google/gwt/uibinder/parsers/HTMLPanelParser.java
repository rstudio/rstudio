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
package com.google.gwt.uibinder.parsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.uibinder.parsers.HtmlMessageInterpreter.PlaceholderInterpreterProvider;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.messages.MessageWriter;
import com.google.gwt.uibinder.rebind.messages.PlaceholderInterpreter;

/**
 * Parses {@link com.google.gwt.user.client.ui.HTMLPanel} widgets.
 */
public class HTMLPanelParser implements ElementParser {

  public void parse(XMLElement elem, String fieldName, JClassType type,
      final UiBinderWriter writer) throws UnableToCompleteException {
    String customTag = UiBinderWriter.escapeTextForJavaStringLiteral(elem.consumeRawAttribute("tag"));

    /*
     * Gathers up elements that indicate nested widgets (but only those that are
     * not inside msg elements).
     */
    WidgetInterpreter widgetInterpreter = new WidgetInterpreter(fieldName,
        writer);

    /*
     * Handles non-widget elements like msg, and dom elements with ui:field
     * attributes. There may be widgets inside a msg, which is why the
     * construction in makeHtmlInterpreter is so complicated.
     */
    HtmlInterpreter htmlInterpreter = makeHtmlInterpreter(fieldName, writer);

    writer.beginAttachedSection(fieldName + ".getElement()");
    String html = elem.consumeInnerHtml(InterpreterPipe.newPipe(
        widgetInterpreter, htmlInterpreter));
    writer.endAttachedSection();

    /*
     * HTMLPanel has no no-arg ctor, so we have to generate our own, using the
     * element's innerHTML and perhaps its tag attribute. Do this in a way that
     * will not break subclasses if they happen to have the same constructor
     * signature (by passing in type).
     */
    if ("".equals(customTag)) {
      writer.setFieldInitializerAsConstructor(fieldName, type, "\"" + html
          + "\"");
    } else {
      writer.setFieldInitializerAsConstructor(fieldName, type, "\"" + customTag
          + "\"", "\"" + html + "\"");
    }
  }

  /**
   * Creates an HtmlInterpreter with our specialized placeholder interpreter,
   * which will allow widget instances to be declared inside of ui:msg elements.
   */
  private HtmlInterpreter makeHtmlInterpreter(final String fieldName,
      final UiBinderWriter uiWriter) {
    final String ancestorExpression = fieldName + ".getElement()";

    PlaceholderInterpreterProvider placeholderInterpreterProvider = new PlaceholderInterpreterProvider() {
      public PlaceholderInterpreter get(MessageWriter message) {
        return new WidgetPlaceholderInterpreter(fieldName, uiWriter, message,
            ancestorExpression);
      }
    };

    HtmlInterpreter htmlInterpreter = new HtmlInterpreter(uiWriter,
        ancestorExpression, new HtmlMessageInterpreter(uiWriter,
            placeholderInterpreterProvider));

    return htmlInterpreter;
  }
}
