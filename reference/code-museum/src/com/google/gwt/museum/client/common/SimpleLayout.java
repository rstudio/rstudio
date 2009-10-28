/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.museum.client.common;

import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;

/**
 * Helper class to avoid manually assembling vertical and horizontal panels for
 * these tests. It acts like a "typewriter" layout.
 * 
 * So, for instance
 * <pre>
 *   // assume the existence of widgets a-z
 *   SimpleLayout layout = new SimpleLayout();
 *   layout.add(a);
 *   layout.add(b);
 *   layout.nextRow();
 *   layout.add(c);
 *   layout.nextRow();
 *   layout.add(d);
 *   layout.add(e);
 *</pre>
 * 
 * would be rendered as:
 * <pre>
 *  a b 
 *  c 
 *  d e
 *  </pre>
 * */
public class SimpleLayout extends Composite implements HasWidgets {

  private final VerticalPanel master = new VerticalPanel();
  private VerticalPanel allRows = new VerticalPanel();
  private HorizontalPanel currentRow;

  public SimpleLayout() {
    initWidget(master);
    master.add(allRows);
    nextRow();
  }

  /**
   * Adds a widget to the current row with the given caption.
   */
  public void add(String caption, Widget w) {
    CaptionPanel c = new CaptionPanel(caption);
    c.add(w);
    add(c);
  }

  /**
   * Adds a widget to the current row.
   */
  public void add(Widget w) {
    currentRow.add(w);
  }

  /**
   * Adds a footer to this layout table, the footer is guaranteed to be beneath
   * all the rows.
   */
  public void addFooter(Widget w) {
    master.add(w);
  }

  public void clear() {
    master.clear();
  }

  public Iterator<Widget> iterator() {
    return master.iterator();
  }

  /**
   * Creates another row. After creating this row, all widgets added using
   * {@link #add(Widget)} and {@link #add(String, Widget)} will be added to the
   * new row.
   */
  public void nextRow() {
    currentRow = new HorizontalPanel();
    allRows.add(currentRow);
  }

  public boolean remove(Widget w) {
    return master.remove(w);
  }
}
