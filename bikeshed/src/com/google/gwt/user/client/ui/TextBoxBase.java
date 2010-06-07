/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Element;
import com.google.gwt.input.shared.PassthroughParser;
import com.google.gwt.input.shared.PassthroughRenderer;

/**
 * Legacy abstract base class for all text entry widgets.
 */
public class TextBoxBase extends ValueBoxBase<String>  {

  /**
   * Text alignment constant, used in
   * {@link TextBoxBase#setTextAlignment(TextBoxBase.TextAlignConstant)}.
   */
  public static class TextAlignConstant {
    private String textAlignString;

    private TextAlignConstant(String textAlignString) {
      this.textAlignString = textAlignString;
    }

    String getTextAlignString() {
      return textAlignString;
    }
  }

  /**
   * Center the text.
   */
  public static final TextAlignConstant ALIGN_CENTER = new TextAlignConstant(
      "center");

  /**
   * Justify the text.
   */
  public static final TextAlignConstant ALIGN_JUSTIFY = new TextAlignConstant(
      "justify");

  /**
   * Align the text to the left edge.
   */
  public static final TextAlignConstant ALIGN_LEFT = new TextAlignConstant(
      "left");

  /**
   * Align the text to the right.
   */
  public static final TextAlignConstant ALIGN_RIGHT = new TextAlignConstant(
      "right");

  /**
   * Creates a text box that wraps the given browser element handle. This is
   * only used by subclasses.
   * 
   * @param elem the browser element to wrap
   */
  protected TextBoxBase(Element elem) {
    super(elem, PassthroughRenderer.instance(), PassthroughParser.instance());
  }
}
