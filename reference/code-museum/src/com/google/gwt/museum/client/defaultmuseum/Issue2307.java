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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * <h1>Normalize design of CaptionPanel</h1>
 * 
 * <p>
 * Methods should be set/getCaptionHTML, set/getCaptionText,
 * set/getContentWidget. Write lots of unit tests.
 * </p>
 */
public class Issue2307 extends AbstractIssue {

  private CaptionPanel captionPanel;

  /**
   * A set of options used to set the caption and content in the caption panel.
   */
  private class ControlPanel extends Composite {
    private final Grid grid = new Grid(3, 2);

    public ControlPanel() {
      initWidget(grid);

      // Add option to set the text
      final TextBox textBox = new TextBox();
      textBox.setText("<b>CaptionPanel</b>");
      grid.setWidget(0, 1, textBox);
      grid.setWidget(0, 0, new Button("setCaptionText", new ClickHandler() {
        public void onClick(ClickEvent event) {
          captionPanel.setCaptionText(textBox.getText());
        }
      }));

      // Add option to set the html
      final TextBox htmlBox = new TextBox();
      htmlBox.setText("<b>CaptionPanel</b>");
      grid.setWidget(1, 1, htmlBox);
      grid.setWidget(1, 0, new Button("setCaptionHTML", new ClickHandler() {
        public void onClick(ClickEvent event) {
          captionPanel.setCaptionHTML(htmlBox.getText());
        }
      }));

      // Add option to set the content
      final TextBox contentBox = new TextBox();
      contentBox.setText("<b><i>I am a Button</i></b>");
      grid.setWidget(2, 1, contentBox);
      grid.setWidget(2, 0, new Button("setContentWidget", new ClickHandler() {
        public void onClick(ClickEvent event) {
          captionPanel.setContentWidget(new Button(contentBox.getText()));
        }
      }));
    }
  }

  @Override
  public Widget createIssue() {
    captionPanel = new CaptionPanel("CaptionPanel");
    VerticalPanel p = new VerticalPanel();
    p.setSpacing(6);
    p.add(captionPanel);
    p.add(new ControlPanel());
    return p;
  }

  @Override
  public String getInstructions() {
    return "Verify the usage of various CaptionPanel methods.";
  }

  @Override
  public String getSummary() {
    return "CaptionPanel tests";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }
}
