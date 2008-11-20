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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * <h1>DeckPanel children getOffsetWidth/Height() return 0 after r2060</h1>
 * 
 * <p>
 * Child widgets of DeckPanel used to be able to call getOffsetWidth/Height() in
 * onLoad() to obtain the widget's offset dimensions. These methods now return 0
 * (zero) in onLoad(), and are only correct later, e.g. in a deferred command.
 * </p>
 */
public class Issue2321 extends AbstractIssue {
  /**
   * A set of options used to set the caption and content in the caption panel.
   */
  private class ControlPanel extends Composite {
    private final Grid grid = new Grid(3, 2);

    public ControlPanel() {
      initWidget(grid);

      // Add option to detach the deck panel
      Button addWidgetButton = new Button("Add widget", new ClickHandler() {
        public void onClick(ClickEvent event) {
          addWidgetToDeckPanel();
        }
      });
      grid.setWidget(0, 0, addWidgetButton);

      // Add option to retrieve the dimensions of the content
      Button updateDimButton = new Button("Get Current Dimensions",
          new ClickHandler() {
            public void onClick(ClickEvent event) {
              updateContentDimensions();
            }
          });
      grid.setWidget(0, 1, updateDimButton);

      // Add labels for the content height and width
      grid.setHTML(1, 0, "Content Height:");
      grid.setHTML(2, 0, "Content Width:");
    }

    /**
     * Add another widget to the deck panel.
     */
    public void addWidgetToDeckPanel() {
      int numWidgets = deck.getWidgetCount();
      HTML content = new HTML("Content " + numWidgets) {
        @Override
        protected void onLoad() {
          updateContentDimensions();
        }
      };

      content.setStylePrimaryName("deckPanel-content");
      deck.add(content);
      deck.showWidget(numWidgets);
    }

    /**
     * Retrieve the size of the content widgets.
     */
    public void updateContentDimensions() {
      Widget content = deck.getWidget(deck.getWidgetCount() - 1);
      grid.setHTML(1, 1, content.getOffsetHeight() + "");
      grid.setHTML(2, 1, content.getOffsetWidth() + "");
    }
  }

  /**
   * The options panel to control this test.
   */
  private ControlPanel controlPanel = new ControlPanel();

  /**
   * The {@link DeckPanel} to be tested.
   */
  private DeckPanel deck;

  /**
   * The main container that holds the control panel and deck panel.
   */
  private VerticalPanel vPanel;

  @Override
  public Widget createIssue() {
    // Create the deck panel and grab the size of the contents on load
    deck = new DeckPanel() {
      @Override
      protected void onLoad() {
        controlPanel.addWidgetToDeckPanel();
      }
    };
    deck.setStylePrimaryName("deckPanel");

    // Combine the control panel and DeckPanel
    vPanel = new VerticalPanel();
    vPanel.add(controlPanel);
    vPanel.add(deck);
    return vPanel;
  }

  @Override
  public String getInstructions() {
    return "The height and width of the content should be greater than 0.";
  }

  @Override
  public String getSummary() {
    return "DeckPanel should return content dimensions onLoad";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }
}
