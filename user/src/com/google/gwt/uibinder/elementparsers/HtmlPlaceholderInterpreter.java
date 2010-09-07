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
 * Interprets the interior of an html message. Extends the basic
 * PlaceholderInterpreter to handle dom elements with field names and computed
 * attributes--basically any dom attribute that cannot live in a static value
 * and so must be wrapped in a placeholder. (Note that the interior of such an
 * element is processed with a recursive call to
 * {@link XMLElement#consumeInnerHtml}.)
 */
class HtmlPlaceholderInterpreter extends PlaceholderInterpreter {
  
  private static final String EXAMPLE_OPEN_TAG = "<tag>";
  private static final String EXAMPLE_CLOSE_TAG = "</tag>";
  
  private int serial = 0;
  private final XMLElement.Interpreter<String> fieldAndComputed;

  HtmlPlaceholderInterpreter(UiBinderWriter writer,
      MessageWriter message, String ancestorExpression) {
    super(writer, message);
    fieldAndComputed =
        InterpreterPipe.newPipe(new FieldInterpreter(writer,
            ancestorExpression), new ComputedAttributeInterpreter(writer));
  }

  @Override
  public String interpretElement(XMLElement elem)
      throws UnableToCompleteException {
    fieldAndComputed.interpretElement(elem);

    if (isDomPlaceholder(elem)) {
      MessagesWriter mw = uiWriter.getMessages();

      String name = mw.consumeMessageAttribute("ph", elem);
      if ("".equals(name)) {
        name = "htmlElement" + (++serial);
      }

      String openTag = elem.consumeOpeningTag();
      String openPlaceholder =
          nextOpenPlaceholder(name + "Begin", uiWriter.detokenate(openTag));

      /*
       * This recursive innerHtml call has already been escaped. Hide it in a
       * token to avoid double escaping
       */
      String body = tokenator.nextToken(elem.consumeInnerHtml(this));

      String closeTag = elem.getClosingTag();
      String closePlaceholder = nextClosePlaceholder(name + "End", closeTag);

      return openPlaceholder + body + closePlaceholder;
    }

    return super.interpretElement(elem);
  }

  @Override
  protected String consumePlaceholderInnards(XMLElement elem)
      throws UnableToCompleteException {
    return elem.consumeInnerHtml(fieldAndComputed);
  }

  /**
   * Returns the {@link #nextPlaceholder(String, String, String)}, using the
   * given {@code name} and {@code value} and a standard closing tag as example
   * text.
   */
  protected String nextClosePlaceholder(String name, String value) {
    return nextPlaceholder(name, EXAMPLE_CLOSE_TAG, value);
  }

  /**
   * Returns the {@link #nextPlaceholder(String, String, String)}, using the
   * given {@code name} and {@code value} and a standard opening tag as example
   * text.
   */
  protected String nextOpenPlaceholder(String name, String value) {
    return nextPlaceholder(name, EXAMPLE_OPEN_TAG, value);
  }

  /**
   * An element will require a placeholder if the user has called it out with a
   * ui:ph attribute, or if it will require run time swizzling (e.g. has a
   * ui:field). These latter we can identify easily because they'll have an
   * attribute that holds a tokenator token that was vended by
   * {@link UiBinderWriter}, typically in id.
   *
   * @return true if it has an ui:ph attribute, or has a token in any attribute
   */
  private boolean isDomPlaceholder(XMLElement elem) {
    MessagesWriter mw = uiWriter.getMessages();
    if (mw.hasMessageAttribute("ph", elem)) {
      return true;
    }
    for (int i = elem.getAttributeCount() - 1; i >= 0; i--) {
      if (elem.getAttribute(i).hasToken()) {
        return true;
      }
    }
    return false;
  }
}
