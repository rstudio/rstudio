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
import com.google.gwt.text.shared.testing.PassthroughParser;
import com.google.gwt.text.shared.testing.PassthroughRenderer;

/**
 * Abstract base class for most text entry widgets.
 *
 * <p>
 * The names of the static members of {@link TextBoxBase}, as well as simple
 * alignment names (<code>left</code>, <code>center</code>, <code>right</code>,
 * <code>justify</code>), can be used as values for a <code>textAlignment</code>
 * attribute.
 * <p>
 * For example,
 *
 * <pre>
 * &lt;g:TextBox textAlignment='ALIGN_RIGHT'/&gt;
 * &lt;g:TextBox textAlignment='right'/&gt;
 * </pre>
 */
public class TextBoxBase extends ValueBoxBase<String> implements
    SourcesChangeEvents {

  /**
   * Legacy wrapper for {@link ValueBoxBase.TextAlignment}, soon to be deprecated.
   * @deprecated use {@link #setAlignment(ValueBoxBase.TextAlignment)}
   */
  @Deprecated
  public static class TextAlignConstant {
    private TextAlignment value;

    private TextAlignConstant(TextAlignment value) {
      this.value = value;
    }

    TextAlignment getTextAlignString() {
      return value;
    }
  }

  /**
   * Center the text.
   */
  public static final TextAlignConstant ALIGN_CENTER = new TextAlignConstant(
      TextAlignment.CENTER);

  /**
   * Justify the text.
   */
  public static final TextAlignConstant ALIGN_JUSTIFY = new TextAlignConstant(
      TextAlignment.JUSTIFY);

  /**
   * Align the text to the left edge.
   */
  public static final TextAlignConstant ALIGN_LEFT = new TextAlignConstant(
      TextAlignment.LEFT);

  /**
   * Align the text to the right.
   */
  public static final TextAlignConstant ALIGN_RIGHT = new TextAlignConstant(
      TextAlignment.RIGHT);

  /**
   * Creates a text box that wraps the given browser element handle. This is
   * only used by subclasses.
   *
   * @param elem the browser element to wrap
   */
  protected TextBoxBase(Element elem) {
    super(elem, PassthroughRenderer.instance(), PassthroughParser.instance());
  }

  /**
   * @deprecated Use {@link #addChangeHandler} instead
   */
  @Deprecated
  public void addChangeListener(ChangeListener listener) {
    addChangeHandler(new ListenerWrapper.WrappedChangeListener(listener));
  }

  /**
   * Overridden to return "" from an empty text box.
   */
  @Override
  public String getValue() {
    String raw = super.getValue();
    return raw == null ? "" : raw;
  }

  /**
   * Legacy wrapper for {@link #setAlignment(TextAlignment)}.
   *
   * @deprecated use {@link #setAlignment(TextAlignment)}
   */
  @Deprecated
  public void setTextAlignment(TextAlignConstant align) {
    setAlignment(align.value);
  }
}
