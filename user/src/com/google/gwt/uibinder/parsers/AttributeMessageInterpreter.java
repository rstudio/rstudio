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
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.messages.AttributeMessage;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;

/**
 * Examines each element for child &lt;m:attr/&gt; elements, and replaces the
 * corresponding attributes of the examinee with references to the translatable
 * messages created.
 * <p>
 * That is, when examining element foo in
 * <pre>
 *   &lt;foo bar="baz"&gt;
 *     &lt;m:attr name="baz"&gt;
 *   &lt;/foo&gt;</pre>
 * cosume the m:attr element, and declare a method on the Messages interface
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
      elem.setAttribute(am.getAttribute(),
        writer.tokenForExpression(am.getMessageAsHtmlAttribute()));
    }

    /*
     * Return null because we don't want to replace the dom element with any
     * particular string (though we may have consumed its id or gwt:field)
     */
    return null;
  }
}
