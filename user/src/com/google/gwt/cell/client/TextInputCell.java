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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;

/**
 * A {@link AbstractCell} used to render a text input.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 */
public class TextInputCell extends AbstractEditableCell<String, String> {

  interface Template extends SafeHtmlTemplates {
    @Template("<input type=\"text\" value=\"{0}\"></input>")
    SafeHtml input(String value);
  }

  private static Template template;

  private final SafeHtmlRenderer<String> renderer;

  /**
   * Constructs a TextInputCell that renders its text without HTML markup.
   */
  public TextInputCell() {
    this(SimpleSafeHtmlRenderer.getInstance());
  }

  /**
   * Constructs a TextInputCell that renders its text using the
   * given {@link SafeHtmlRenderer}.
   *
   * @param renderer a non-null SafeHtmlRenderer
   */
  public TextInputCell(SafeHtmlRenderer<String> renderer) {
    super("change", "keyup");
    if (template == null) {
      template = GWT.create(Template.class);
    }
    if (renderer == null) {
      throw new IllegalArgumentException("renderer == null");
    }
    this.renderer = renderer;
  }

  @Override
  public void onBrowserEvent(Element parent, String value, Object key,
      NativeEvent event, ValueUpdater<String> valueUpdater) {
    String eventType = event.getType();
    if ("change".equals(eventType)) {
      InputElement input = parent.getFirstChild().cast();
      String newValue = input.getValue();
      setViewData(key, newValue);
      if (valueUpdater != null) {
        valueUpdater.update(newValue);
      }
    } else if ("keyup".equals(eventType)) {
      // Record keys as they are typed.
      InputElement input = parent.getFirstChild().cast();
      setViewData(key, input.getValue());
    }
  }

  @Override
  public void render(String value, Object key, SafeHtmlBuilder sb) {
    // Get the view data.
    String viewData = getViewData(key);
    if (viewData != null && viewData.equals(value)) {
      clearViewData(key);
      viewData = null;
    }

    String s = (viewData != null) ? viewData : (value != null ? value : null);
    if (s != null) {
      SafeHtml html = renderer.render(s);
      // Note: template will not treat SafeHtml specially
      sb.append(template.input(html.asString()));
    } else {
      sb.appendHtmlConstant("<input type=\"text\"></input>");
    }
  }
}
