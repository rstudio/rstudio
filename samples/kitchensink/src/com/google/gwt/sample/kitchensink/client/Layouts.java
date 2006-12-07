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

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Demonstrates various panels and the way they lay widgets out.
 */
public class Layouts extends Sink {

  public static SinkInfo init() {
    return new SinkInfo(
        "Layouts",
        "This page demonstrates some of the basic GWT panels, each of which "
            + "arranges its contained widgets differently.  "
            + "These panels are designed to take advantage of the browser's "
            + "built-in layout mechanics, which keeps the user interface snappy "
            + "and helps your AJAX code play nicely with existing HTML.  "
            + "On the other hand, if you need pixel-perfect control, "
            + "you can tweak things at a low level using the "
            + "<code>DOM</code> class.") {
      public Sink createInstance() {
        return new Layouts();
      }
    };
  }

  public Layouts() {
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

    MenuBar menu = new MenuBar();
    MenuBar menu0 = new MenuBar(true), menu1 = new MenuBar(true);
    menu.addItem("menu0", menu0);
    menu.addItem("menu1", menu1);
    menu0.addItem("child00", (Command) null);
    menu0.addItem("child01", (Command) null);
    menu0.addItem("child02", (Command) null);
    menu1.addItem("child10", (Command) null);
    menu1.addItem("child11", (Command) null);
    menu1.addItem("child12", (Command) null);

    String id = HTMLPanel.createUniqueId();
    HTMLPanel html = new HTMLPanel(
        "This is an <code>HTMLPanel</code>.  It allows you to add "
            + "components inside existing HTML, like this: " + "<span id='" + id
            + "'></span>"
            + "Notice how the menu just fits snugly in there?  Cute.");
    DOM.setStyleAttribute(menu.getElement(), "display", "inline");
    html.add(menu, id);

    VerticalPanel panel = new VerticalPanel();
    panel.setSpacing(8);
    panel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);

    panel.add(makeLabel("Dock Panel"));
    panel.add(dock);
    panel.add(makeLabel("Flow Panel"));
    panel.add(flow);
    panel.add(makeLabel("Horizontal Panel"));
    panel.add(horz);
    panel.add(makeLabel("Vertical Panel"));
    panel.add(vert);
    panel.add(makeLabel("HTML Panel"));
    panel.add(html);

    initWidget(panel);
    setStyleName("ks-layouts");
  }

  public void onShow() {
  }

  private HTML makeLabel(String caption) {
    HTML html = new HTML(caption);
    html.setStyleName("ks-layouts-Label");
    return html;
  }
}
