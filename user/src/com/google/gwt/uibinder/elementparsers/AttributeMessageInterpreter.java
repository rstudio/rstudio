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
import com.google.gwt.uibinder.rebind.messages.AttributeMessage;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;

/**
 * Examines each element for child &lt;ui:attr/&gt; elements, and replaces the
 * corresponding attributes of the examinee with references to the translatable
 * messages created.
 * <p>
 * That is, when examining element foo in
 * <pre>
 *   &lt;foo bar="baz"&gt;
 *     &lt;ui:attr name="baz"&gt;
 *   &lt;/foo&gt;</pre>
 * cosume the ui:attr element, and declare a method on the Messages interface
 * with {@literal @}Default("baz")
 */
 class AttributeMessageInterpreter implements XMLElement.Interpreter<String> {

  private final UiBinderWriter writer;

  public AttributeMessageInterpreter(UiBinderWriter writer) {
    this.writer = writer;
  }

  public String interpretElement(XMLElement elem)
      throws UnableToCompleteException {
    MessagesWriter messages = writer.getMessages();
    for (AttributeMessage am : messages.consumeAttributeMessages(elem)) {
      String message = am.getMessageUnescaped();
      if (!writer.useSafeHtmlTemplates()) {
        /*
         * We have to do our own simple escaping to if the SafeHtml integration
         * is off
         */
        message += ".replaceAll(\"&\", \"&amp;\").replaceAll(\"'\", \"&#39;\")";
      }
      elem.setAttribute(am.getAttribute(),
        writer.tokenForStringExpression(elem, message));
    }

    /*
     * Return null because we don't want to replace the dom element with any
     * particular string (though we may have consumed its id or ui:field)
     */
    return null;
  }
}
