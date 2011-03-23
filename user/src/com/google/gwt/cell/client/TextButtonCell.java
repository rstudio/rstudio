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
package com.google.gwt.cell.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;

/**
 * {@code TextButtonCell} is a simple button with text content.
 */
public class TextButtonCell extends ButtonCellBase<String> {

  /**
   * The appearance used to render this Cell.
   */
  public interface Appearance extends ButtonCellBase.Appearance<String> {
  }

  /**
   * The default implementation of the {@link Appearance}.
   */
  public static class DefaultAppearance extends ButtonCellBase.DefaultAppearance<String> implements
      Appearance {

    /**
     * Construct a new {@link DefaultAppearance} using the default resources.
     * 
     * <p>
     * The {@link DefaultAppearance} may be replaced with a more modern
     * appearance in the future. If you want to stay up to date with the latest
     * appearance, use {@link TextButtonCell#createDefaultAppearance()} instead
     * of this constructor. If you do not want the appearance to be updated with
     * successive versions of GWT, use this constructor.
     */
    public DefaultAppearance() {
      super(SimpleSafeHtmlRenderer.getInstance());
    }

    /**
     * Construct a new {@link DefaultAppearance} using the specified resources.
     * 
     * @param resources the resources and styles to apply to the button
     */
    public DefaultAppearance(Resources resources) {
      super(SimpleSafeHtmlRenderer.getInstance(), resources);
    }
  }

  /**
   * Construct a {@link TextButtonCell} using the {@link DefaultAppearance}.
   * 
   * <p>
   * The {@link DefaultAppearance} may be replaced with a more modern appearance
   * in the future. If you want to stay up to date with the latest appearance,
   * use this constructor. If you do not want the appearance to be updated with
   * successive versions of GWT, create an {@link Appearance} and pass it to
   * {@link #TextButtonCell(Appearance)}.
   */
  public TextButtonCell() {
    this(GWT.<Appearance> create(Appearance.class));
  }

  /**
   * Construct a {@link TextButtonCell} using the specified {@link Appearance}
   * to render the cell.
   * 
   * @param appearance the appearance of the cell
   */
  public TextButtonCell(Appearance appearance) {
    super(appearance);
  }
}
