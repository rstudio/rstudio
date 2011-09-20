/*
 * Copyright 2010 Google Inc.
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

import static com.google.gwt.dom.client.BrowserEvents.CLICK;
import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;

/**
 * A {@link Cell} used to render text. Clicking on the cell causes its
 * {@link ValueUpdater} to be called.
 */
public class ClickableTextCell extends AbstractSafeHtmlCell<String> {

  /**
   * Construct a new ClickableTextCell that will use a
   * {@link SimpleSafeHtmlRenderer}.
   */
  public ClickableTextCell() {
    this(SimpleSafeHtmlRenderer.getInstance());
  }

  /**
   * Construct a new ClickableTextCell that will use a given
   * {@link SafeHtmlRenderer}.
   * 
   * @param renderer a {@link SafeHtmlRenderer SafeHtmlRenderer<String>} instance
   */
  public ClickableTextCell(SafeHtmlRenderer<String> renderer) {
    super(renderer, CLICK, KEYDOWN);
  }

  @Override
  public void onBrowserEvent(Context context, Element parent, String value,
      NativeEvent event, ValueUpdater<String> valueUpdater) {
    super.onBrowserEvent(context, parent, value, event, valueUpdater);
    if (CLICK.equals(event.getType())) {
      onEnterKeyDown(context, parent, value, event, valueUpdater);
    }
  }

  @Override
  protected void onEnterKeyDown(Context context, Element parent, String value,
      NativeEvent event, ValueUpdater<String> valueUpdater) {
    if (valueUpdater != null) {
      valueUpdater.update(value);
    }
  }

  @Override
  protected void render(Context context, SafeHtml value, SafeHtmlBuilder sb) {
    if (value != null) {
      sb.append(value);
    }
  }
}
