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
import com.google.gwt.uibinder.rebind.XMLElement.Interpreter;

/**
 * This is the most generally useful interpreter, and the most likely to be used
 * by a custom parser when calling {@link XMLElement#consumeInnerHtml}
 * <ul>
 * <li>Assigns computed values to element attributes (e.g.
 * class="{style.pretty}")
 * <li>Generates fields to hold named dom elements (e.g. &lt;div
 * gwt:field="importantDiv"&gt;)
 * <li>Turns &lt;ui:msg&gt; and &lt;ui:attr&gt; elements into methods on a
 * generated Messages interface
 * </ul>
 */
public class HtmlInterpreter implements XMLElement.Interpreter<String> {

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
  public static HtmlInterpreter newInterpreterForUiObject(
      UiBinderWriter writer, String uiExpression) {
    String ancestorExpression = uiExpression + ".getElement()";
    return new HtmlInterpreter(writer, ancestorExpression,
        new HtmlMessageInterpreter(writer, ancestorExpression));
  }

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
    this.pipe = new InterpreterPipe<String>();

    pipe.add(new FieldInterpreter(writer, ancestorExpression));
    pipe.add(new ComputedAttributeInterpreter(writer));
    pipe.add(new AttributeMessageInterpreter(writer));
    pipe.add(messageInterpreter);
  }

  public String interpretElement(XMLElement elem)
      throws UnableToCompleteException {
    return pipe.interpretElement(elem);
  }
}
