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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;

/**
 * A {@link Cell} used to render a button.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 */
public class ButtonCell extends AbstractSafeHtmlCell<String> {

  public ButtonCell() {
    super(SimpleSafeHtmlRenderer.getInstance(), "mouseup");
  }

  public ButtonCell(SafeHtmlRenderer<String> renderer) {
    super(renderer, "mouseup");
  }

  @Override
  public void onBrowserEvent(Element parent, String value, Object key,
      NativeEvent event, ValueUpdater<String> valueUpdater) {
    if (valueUpdater != null && "mouseup".equals(event.getType())) {
      valueUpdater.update(value);
    }
  }

  @Override
  public void render(SafeHtml data, Object key, SafeHtmlBuilder sb) {
    sb.appendHtmlConstant("<button>");
    if (data != null) {
      sb.append(data);
    }
    sb.appendHtmlConstant("</button>");
  }
}
