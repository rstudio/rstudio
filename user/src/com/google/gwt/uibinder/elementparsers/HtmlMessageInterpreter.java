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
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.messages.MessageWriter;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.rebind.messages.PlaceholderInterpreter;

/**
 * Processes <ui:msg> elements inside HTML values, which themselves
 * are allowed to contain HTML. That HTML may hold elements with
 * ui:field attributes and computed attributes, which must be
 * replaced by placeholders in the generated message.
 */
public class HtmlMessageInterpreter implements XMLElement.Interpreter<String> {
  /**
   * Provides instances of {@link PlaceholderInterpreter}, to allow
   * customized handling of the innards of a msg element.
   */
  public interface PlaceholderInterpreterProvider {
    PlaceholderInterpreter get(MessageWriter message);
  }

  private final UiBinderWriter uiWriter;
  private final PlaceholderInterpreterProvider phiProvider;

  /**
   * Build a HtmlMessageInterpreter that uses customized placeholder interpreter
   * instances.
   */
  public HtmlMessageInterpreter(UiBinderWriter uiWriter,
      PlaceholderInterpreterProvider phiProvider) {
    this.uiWriter = uiWriter;
    this.phiProvider = phiProvider;
 }

  /**
   * Build a normally configured HtmlMessageInterpreter, able to handle
   * put placeholders around dom elements with ui:ph attributes and computed
   * attributes.
   */
  public HtmlMessageInterpreter(final UiBinderWriter uiWriter,
      final String ancestorExpression) {
    this(uiWriter, new PlaceholderInterpreterProvider() {
      public PlaceholderInterpreter get(MessageWriter message) {
        return new HtmlPlaceholderInterpreter(uiWriter, message, ancestorExpression);
      }
    });
  }

  public String interpretElement(XMLElement elem)
      throws UnableToCompleteException {
    MessagesWriter messages = uiWriter.getMessages();
    if (messages.isMessage(elem)) {
      if (!elem.hasChildNodes()) {
        uiWriter.die(elem, "Empty message");
      }

      MessageWriter message = messages.newMessage(elem);
      message.setDefaultMessage(elem.consumeInnerHtml(phiProvider.get(message)));
      return uiWriter.tokenForSafeConstant(elem, messages.declareMessage(message));
    }

    return null;
  }
}
