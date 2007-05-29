/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Demonstrates various panels and the way they lay widgets out.
 */
public class Panels extends Sink {

  public static SinkInfo init(final Sink.Images images) {
    return new SinkInfo(
        "Panels",
        "<h2>Panels</h2><p>This page demonstrates some of the basic GWT panels, each of which "
            + "arranges its contained widgets differently.  "
            + "These panels are designed to take advantage of the browser's "
            + "built-in layout mechanics, which keeps the user interface snappy "
            + "and helps your AJAX code play nicely with existing HTML.  "
            + "On the other hand, if you need pixel-perfect control, "
            + "you can tweak things at a low level using the "
            + "<code>DOM</code> class.</p>") {

      public Sink createInstance() {
        return new Panels(images);
      }

      public String getColor() {
        return "#fe9915";
      }
    };
  }

  public Panels(Sink.Images images) {
    HTML contents = new HTML("This is a <code>ScrollPanel</code> contained at "
        + "the center of a <code>DockPanel</code>.  "
        + "By putting some fairly large contents "
        + "in the middle and setting its size explicitly, it becomes a "
        + "scrollable area within the page, but without requiring the use of "
        + "an IFRAME."
        + "Here's quite a bit more meaningless text that will serve primarily "
        + "to make this thing scroll off the bottom of its visible area.  "
        + "Otherwise, you might have to make it really, really small in order "
        + "to see the nifty scroll bars!");
    ScrollPanel scroller = new ScrollPanel(contents);
    scroller.setStyleName("ks-layouts-Scroller");

    DockPanel dock = new DockPanel();
    dock.setHorizontalAlignment(DockPanel.ALIGN_CENTER);
    HTML north0 = new HTML("This is the <i>first</i> north component", true);
    HTML east = new HTML(
        "<center>This<br>is<br>the<br>east<br>component</center>", true);
    HTML south = new HTML("This is the south component");
    HTML west = new HTML(
        "<center>This<br>is<br>the<br>west<br>component</center>", true);
    HTML north1 = new HTML("This is the <b>second</b> north component", true);
    dock.add(north0, DockPanel.NORTH);
    dock.add(east, DockPanel.EAST);
    dock.add(south, DockPanel.SOUTH);
    dock.add(west, DockPanel.WEST);
    dock.add(north1, DockPanel.NORTH);
    dock.add(scroller, DockPanel.CENTER);

    DisclosurePanel disc = new DisclosurePanel("Click to disclose something:");
    disc.setContent(new HTML("This widget is is shown and hidden<br>by the "
        + "disclosure panel that wraps it."));

    FlowPanel flow = new FlowPanel();
    for (int i = 0; i < 8; ++i) {
      flow.add(new CheckBox("Flow " + i));
    }

    HorizontalPanel horz = new HorizontalPanel();
    horz.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
    horz.add(new Button("Button"));
    horz.add(new HTML("<center>This is a<br>very<br>tall thing</center>", true));
    horz.add(new Button("Button"));

    VerticalPanel vert = new VerticalPanel();
    vert.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
    vert.add(new Button("Small"));
    vert.add(new Button("--- BigBigBigBig ---"));
    vert.add(new Button("tiny"));

    VerticalPanel vp = new VerticalPanel();
    vp.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
    vp.setSpacing(8);
    vp.add(makeLabel("Disclosure Panel"));
    vp.add(disc);
    vp.add(makeLabel("Flow Panel"));
    vp.add(flow);
    vp.add(makeLabel("Horizontal Panel"));
    vp.add(horz);
    vp.add(makeLabel("Vertical Panel"));
    vp.add(vert);

    Grid grid = new Grid(4, 4);
    for (int r = 0; r < 4; ++r) {
      for (int c = 0; c < 4; ++c) {
        grid.setWidget(r, c, images.gwtLogo().createImage());
      }
    }

    TabPanel tabs = new TabPanel();
    tabs.add(vp, "Basic Panels");
    tabs.add(dock, "Dock Panel");
    tabs.add(grid, "Tables");
    tabs.setWidth("100%");
    tabs.selectTab(0);

    HorizontalSplitPanel hSplit = new HorizontalSplitPanel();
    hSplit.setLeftWidget(tabs);
    hSplit.setRightWidget(new HTML(
      "This is some text to make the right side of this " +
      "splitter look a bit more interesting... " +
      "This is some text to make the right side of this " +
      "splitter look a bit more interesting... " +
      "This is some text to make the right side of this " +
      "splitter look a bit more interesting... " +
      "This is some text to make the right side of this " +
      "splitter look a bit more interesting... "));

    initWidget(hSplit);
    hSplit.setSize("100%", "450px");
  }

  public void onShow() {
  }

  private HTML makeLabel(String caption) {
    HTML html = new HTML(caption);
    html.setStyleName("ks-layouts-Label");
    return html;
  }
}
