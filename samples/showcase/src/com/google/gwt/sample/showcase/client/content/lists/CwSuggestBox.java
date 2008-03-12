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
package com.google.gwt.sample.showcase.client.content.lists;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 * 
 * @gwt.CSS .gwt-SuggestBox
 * @gwt.CSS .gwt-SuggestBoxPopup
 * @gwt.CSS html>body .gwt-SuggestBoxPopup
 * @gwt.CSS * html .gwt-SuggestBoxPopup
 */
public class CwSuggestBox extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   * 
   * @gwt.SRC
   */
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String cwSuggestBoxDescription();

    String cwSuggestBoxLabel();

    String cwSuggestBoxName();

    String[] cwSuggestBoxWords();
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
  public CwSuggestBox(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwSuggestBoxDescription();
  }

  @Override
  public String getName() {
    return constants.cwSuggestBoxName();
  }

  /**
   * Initialize this example.
   * 
   * @gwt.SRC
   */
  @Override
  public Widget onInitialize() {
    // Define the oracle that finds suggestions
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    String[] words = constants.cwSuggestBoxWords();
    for (int i = 0; i < words.length; ++i) {
      oracle.add(words[i]);
    }

    // Create the suggest box
    final SuggestBox suggestBox = new SuggestBox(oracle);
    suggestBox.ensureDebugId("cwSuggestBox");
    VerticalPanel suggestPanel = new VerticalPanel();
    suggestPanel.add(new HTML(constants.cwSuggestBoxLabel()));
    suggestPanel.add(suggestBox);

    // Return the panel
    return suggestPanel;
  }
}
