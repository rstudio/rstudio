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

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 * 
 * @gwt.CSS .gwt-RichTextArea
 * @gwt.CSS .hasRichTextToolbar
 * @gwt.CSS .gwt-RichTextToolbar
 * @gwt.CSS .cw-RichText
 */
public class CwRichText extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   * 
   * @gwt.SRC
   */
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String cwRichTextDescription();

    String cwRichTextName();
  }

  /**
   * An instance of the constants.
   * 
   * @gwt.DATA
   */
  private CwConstants constants;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public CwRichText(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwRichTextDescription();
  }

  @Override
  public String getName() {
    return constants.cwRichTextName();
  }

  /**
   * Initialize this example.
   * 
   * @gwt.SRC
   */
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
}
