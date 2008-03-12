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
package com.google.gwt.sample.showcase.client.content.panels;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 * 
 * @gwt.CSS .cw-FlowPanel-checkBox
 */
public class CwFlowPanel extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   * 
   * @gwt.SRC
   */
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String cwFlowPanelDescription();

    String cwFlowPanelItem();

    String cwFlowPanelName();
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
  public CwFlowPanel(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwFlowPanelDescription();
  }

  @Override
  public String getName() {
    return constants.cwFlowPanelName();
  }

  /**
   * Initialize this example.
   * 
   * @gwt.SRC
   */
  @Override
  public Widget onInitialize() {
    // Create a Flow Panel
    FlowPanel flowPanel = new FlowPanel();
    flowPanel.ensureDebugId("cwFlowPanel");

    // Add some content to the panel
    for (int i = 0; i < 30; i++) {
      CheckBox checkbox = new CheckBox(constants.cwFlowPanelItem() + " " + i);
      checkbox.addStyleName("cw-FlowPanel-checkBox");
      flowPanel.add(checkbox);
    }

    // Return the content
    return flowPanel;
  }
}
