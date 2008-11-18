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
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.Cell;

/**
 * Visual test for table events.
 */
public class VisualsForTableEvents extends AbstractIssue {
  final Grid g = new Grid(5, 5);

  public void fillInGrid() {
    int row = 0;

    for (int i = 0; i < 5; i++) {
      g.setText(row, i, "click on cell in row to change");
    }
    ++row;
    for (int i = 0; i < 5; i++) {
      g.setText(row, i, "cell(0," + i + ")");
    }
    ++row;
    for (int i = 0; i < 5; i++) {
      g.setHTML(row, i, "<b>cell(1," + i + "</b>)");
    }

    ++row;
    for (int i = 0; i < 5; i++) {
      g.setWidget(row, i, new CheckBox("cell(1," + i + "</b>)"));
    }

    ++row;
    for (int i = 0; i < 5; i++) {
      final CheckBox box = new CheckBox("eventful cell(1," + i + "</b>)");
      box.addFocusHandler(new FocusHandler() {
        public void onFocus(FocusEvent event) {
          Window.setTitle(box.getText() + " has focus");
        }
      });
      final int index = i;
      box.addClickHandler(new ClickHandler() {

        public void onClick(ClickEvent event) {
          g.setText(0, index, "checkbox click");
        }
      });

      g.setWidget(row, i, box);
    }
  }

  @Override
  public Widget createIssue() {
    VerticalPanel p = new VerticalPanel();
    fillInGrid();
    p.add(g);
    g.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        Cell cell = g.getCellForEvent(event);
        g.setText(0, cell.getCellIndex(), "clicked on " + cell.getCellIndex()
            + "," + cell.getRowIndex());
      }

    });
    return p;
  }

  @Override
  public String getInstructions() {
    return "Click on the cells";
  }

  @Override
  public String getSummary() {
    return "table events";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
