/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.sample.kitchensink.client;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Grid;

/**
 * Demonstrates {@link com.google.gwt.user.client.ui.Table}.
 */
public class Tables extends Sink {

  public static SinkInfo init() {
    return new SinkInfo(
      "Tables",
      "The <code>FlexTable</code> widget doubles as a tabular data formatter "
        + "and a panel.  In "
        + "this example, you'll see that there is an outer table with four cells, "
        + "two of which contain nested components.") {
      public Sink createInstance() {
        return new Tables();
      }
    };
  }

  private Grid inner = new Grid(10, 5);
  private FlexTable outer = new FlexTable();

  public Tables() {
    outer.setWidget(0, 0, new Image("rembrandt/LaMarcheNocturne.jpg"));
    outer.getFlexCellFormatter().setColSpan(0, 0, 2);
    outer.getFlexCellFormatter().setHorizontalAlignment(0, 0,
      HasHorizontalAlignment.ALIGN_CENTER);

    outer.setHTML(1, 0, "Look to the right...<br>"
      + "That's a nested table component ->");
    outer.setWidget(1, 1, inner);
    ((FlexTable.FlexCellFormatter) outer.getCellFormatter())
      .setColSpan(1, 1, 2);

    for (int i = 0; i < 10; ++i) {
      for (int j = 0; j < 5; ++j)
        inner.setText(i, j, "" + i + "," + j);
    }

    inner.setWidth("100%");
    outer.setWidth("100%");

    inner.setBorderWidth(1);
    outer.setBorderWidth(1);

    initWidget(outer);
  }

  public void onShow() {
  }
}
