/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.widget.client;

import com.google.gwt.cell.client.TextButtonCell;
import com.google.gwt.cell.client.TextButtonCell.Appearance;
import com.google.gwt.user.client.ui.HasText;

/**
 * A button that displays text and an optional icon.
 */
public class TextButton extends ButtonBase<String> implements HasText {

  /**
   * Construct a new {@link TextButton} using the default {@link Appearance}.
   * 
   * <p>
   * The default {@link Appearance} may be replaced with a more modern
   * appearance in the future. If you do not want the appearance to be updated
   * with successive versions of GWT, create an {@link Appearance} and pass it
   * to {@link #TextButton(Appearance, String)}.
   */
  public TextButton() {
    this((String) null);
  }

  /**
   * Construct a new {@link TextButton} with the specified text using the
   * default {@link Appearance}.
   * 
   * <p>
   * The default {@link Appearance} may be replaced with a more modern
   * appearance in the future. If you do not want the appearance to be updated
   * with successive versions of GWT, create an {@link Appearance} and pass it
   * to {@link #TextButton(Appearance, String)}.
   */
  public TextButton(String text) {
    this(initializeCell(new TextButtonCell()), text);
  }

  /**
   * Construct a new {@link TextButton} with the specified text using the
   * specified {@link Appearance} to render the widget.
   * 
   * @param appearance the {@link Appearance} used to render the widget
   * @param text the text content
   */
  public TextButton(Appearance appearance, String text) {
    this(initializeCell(new TextButtonCell(appearance)), text);
  }

  /**
   * Construct a new {@link TextButton} with the specified text using the
   * specified {@link TextButtonCell} to render the widget.
   * 
   * @param cell the {@link TextButtonCell} used to render the widget
   * @param text the text content
   */
  protected TextButton(TextButtonCell cell, String text) {
    super(cell, text);
  }

  public String getText() {
    return getValue();
  }

  public void setText(String text) {
    setValue(text);
  }
}
