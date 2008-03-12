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
package com.google.gwt.sample.showcase.client.content.widgets;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 * 
 * @gwt.CSS .gwt-CheckBox
 */
public class CwCheckBox extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   * 
   * @gwt.SRC
   */
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String cwCheckBoxCheckAll();

    String cwCheckBoxDescription();

    String cwCheckBoxFemale();

    String cwCheckBoxMale();

    String cwCheckBoxName();

    String cwCheckBoxUnknown();
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
  public CwCheckBox(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwCheckBoxDescription();
  }

  @Override
  public String getName() {
    return constants.cwCheckBoxName();
  }

  /**
   * Initialize this example.
   * 
   * @gwt.SRC
   */
  @Override
  public Widget onInitialize() {
    // Create a vertical panel to align the check boxes
    VerticalPanel vPanel = new VerticalPanel();
    HTML label = new HTML(constants.cwCheckBoxCheckAll());
    label.ensureDebugId("cwCheckBox-label");
    vPanel.add(label);

    // Add a male checkbox
    CheckBox maleCheckBox = new CheckBox(constants.cwCheckBoxMale());
    maleCheckBox.ensureDebugId("cwCheckBox-male");
    vPanel.add(maleCheckBox);
    
    // Add a female checkbox
    CheckBox femaleCheckBox = new CheckBox(constants.cwCheckBoxFemale()); 
    femaleCheckBox.ensureDebugId("cwCheckBox-female");
    vPanel.add(femaleCheckBox);

    // Add one disabled checkbox
    CheckBox disabledCheckBox = new CheckBox(constants.cwCheckBoxUnknown());
    disabledCheckBox.ensureDebugId("cwCheckBox-disabled");
    disabledCheckBox.setEnabled(false);
    vPanel.add(disabledCheckBox);

    // Return the panel of checkboxes
    return vPanel;
  }
}
