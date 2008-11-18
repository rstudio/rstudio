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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * When you change to a different widget in the DeckPanel, there is a flicker in
 * IE7 where the new widget is completely visible for an instant, and then the
 * animation continues normally.
 */
public class Issue2339 extends AbstractIssue {
  private static final String[] TAB_BACKGROUNDS = {
      "#f88", "#88f", "#8f8", "#8ff", "#f8f"};

  @Override
  public Widget createIssue() {
    final TabPanel tabPanel = new TabPanel();
    String contentText = "";
    for (int i = 0; i < TAB_BACKGROUNDS.length; i++) {
      contentText += "Each tab has more text.<br>";
      HTML content = new HTML(contentText);
      content.getElement().getStyle().setProperty("background",
          TAB_BACKGROUNDS[i]);
      tabPanel.add(content, "Tab " + i);
    }

    tabPanel.selectTab(0);
    tabPanel.getDeckPanel().setAnimationEnabled(true);
    return tabPanel;
  }

  @Override
  public String getInstructions() {
    return "Switch to a different tab and you should not see a flicker";
  }

  @Override
  public String getSummary() {
    return "DeckPanel flickers when switching between widgets";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
