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
package com.google.gwt.sample.showcase.client.content.text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle({
    ".gwt-RichTextArea", ".hasRichTextToolbar", ".gwt-RichTextToolbar",
    ".cw-RichText"})
public class CwRichText extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwRichTextDescription();

    String cwRichTextName();
  }

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwRichText(CwConstants constants) {
    super(constants.cwRichTextName(), constants.cwRichTextDescription(), true);
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create the text area and toolbar
    RichTextArea area = new RichTextArea();
    area.ensureDebugId("cwRichText-area");
    area.setSize("100%", "14em");
    RichTextToolbar toolbar = new RichTextToolbar(area);
    toolbar.ensureDebugId("cwRichText-toolbar");
    toolbar.setWidth("100%");

    // Add the components to a panel
    Grid grid = new Grid(2, 1);
    grid.setStyleName("cw-RichText");
    grid.setWidget(0, 0, toolbar);
    grid.setWidget(1, 0, area);
    return grid;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwRichText.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
