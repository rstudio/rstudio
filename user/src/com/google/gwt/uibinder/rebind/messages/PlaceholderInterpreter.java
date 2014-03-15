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
package com.google.gwt.uibinder.rebind.messages;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.Tokenator;
import com.google.gwt.uibinder.rebind.Tokenator.Resolver;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.XMLElement.PostProcessingInterpreter;

/**
 * Abstract class performs the heavy lifting for processing ph children of a msg
 * element.
 */
public abstract class PlaceholderInterpreter implements
    PostProcessingInterpreter<String> {

  protected final UiBinderWriter uiWriter;
  protected final MessageWriter message;
  protected final Tokenator tokenator = new Tokenator();

  public PlaceholderInterpreter(UiBinderWriter writer, MessageWriter message) {
    this.uiWriter = writer;
    this.message = message;
  }

  public String interpretElement(XMLElement elem)
      throws UnableToCompleteException {
    if (isPlaceholderElement(elem)) {
      /*
       * The innerHTML or innerText of the <ui:ph> will be provided as the value
       * of the appropriate parameter when the Messages method is called.
       *
       * E.g.
       *
       *   <ui:msg>Able <ui:ph name="foo" example"baz">baker</ui:ph> charlie</ui:msg>
       *
       * becomes
       *
       *   @Default("Able {0} charlie)
       *   String message1(@Example("baz") String foo)
       *
       * and in the generated innerHTML is invoked
       *
       *   message1("baker")
       */

      MessagesWriter mw = getMessagesWriter();
      String name = mw.consumeRequiredMessageElementAttribute("name", elem);
      String example = mw.consumeMessageElementAttribute("example", elem);
      String value = consumePlaceholderInnards(elem);

      // Use the value as the example if none is provided
      if ("".equals(example)) {

        /*
         * The value may contain tokens vended by the TemplateWriter, which will
         * be substituted for runtime-computed values (like unique dom ids). We
         * don't want those in the example text shown to the translator, so snip
         * them
         */
        example = stripTokens(value);
      }

      /*
       * Likewise, if there are tokens from the UiWriter in the value string, we
       * need it to replace them with the real expresions.
       */
      value = uiWriter.detokenate(value);
      return nextPlaceholder(name, example, value);
    }

    if (uiWriter.isWidgetElement(elem)) {
      uiWriter.die(elem, "Found widget in a message that cannot contain widgets");
    }
    return null;
  }

  /**
   * Called by various {@link XMLElement} consumeInner*() methods after all
   * elements have been handed to {@link #interpretElement}.
   * <p>
   * Performs escaping on the consumed text to make it safe for use as a
   * Messages {@literal @}Default value
   */
  @SuppressWarnings("unused")
  public String postProcess(String consumed) throws UnableToCompleteException {
    return tokenator.detokenate(MessageWriter.escapeMessageFormat(consumed));
  }

  protected abstract String consumePlaceholderInnards(XMLElement elem)
      throws UnableToCompleteException;

  /**
   * To be called from {@link #interpretElement(XMLElement)}. Creates the next
   * placeholder in the {@link MessageWriter} we're building, and returns the
   * text to stand in its place.
   *
   * @param name
   * @param example
   * @param value
   * @return the token to replace the input String's placeholder
   */
  protected String nextPlaceholder(String name, String example, String value) {
    message.addPlaceholder(new PlaceholderWriter(name, example, value));

    /*
     * We can't just return the {0} placeholder text, because it will be
     * clobbered by the escaping performed in postProcess. We use a tokenator to
     * hide the placeholder from the escaping step, and postProcess resolves the
     * tokens when the escaping is done.
     */
    String placeholder = String.format("{%d}",
        message.getPlaceholderCount() - 1);

    return tokenator.nextToken(placeholder);
  }

  protected String stripTokens(String value) {
    String rtn = Tokenator.detokenate(value, new Resolver() {
      public String resolveToken(String token) {
        return "";
      }
    });
    return rtn;
  }

  private MessagesWriter getMessagesWriter() {
    return uiWriter.getMessages();
  }

  private boolean isPlaceholderElement(XMLElement elem) {
    return getMessagesWriter().isMessagePrefixed(elem)
        && "ph".equals(elem.getLocalName());
  }
}
