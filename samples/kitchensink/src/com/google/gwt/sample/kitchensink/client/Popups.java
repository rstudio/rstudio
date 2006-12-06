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

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Demonstrates {@link com.google.gwt.user.client.ui.PopupPanel} and
 * {@link com.google.gwt.user.client.ui.DialogBox}.
 */
public class Popups extends Sink implements ClickListener {

  /**
   * A simple dialog box that displays a message, a Frame, and a close button.
   */
  private static class MyDialog extends DialogBox implements ClickListener {
    public MyDialog() {
      setText("Sample DialogBox with embedded Frame");

      Frame iframe = new Frame("rembrandt/LaMarcheNocturne.html");
      Button closeButton = new Button("Close", this);
      HTML msg = new HTML(
        "<center>This is an example of a standard dialog box component.<br>  "
          + "You can put pretty much anything you like into it,<br>such as the "
          + "following IFRAME:</center>", true);

      DockPanel dock = new DockPanel();
      dock.setSpacing(4);

      dock.add(closeButton, DockPanel.SOUTH);
      dock.add(msg, DockPanel.NORTH);
      dock.add(iframe, DockPanel.CENTER);

      dock.setCellHorizontalAlignment(closeButton, DockPanel.ALIGN_RIGHT);
      dock.setCellWidth(iframe, "100%");
      dock.setWidth("100%");
      iframe.setWidth("36em");
      iframe.setHeight("20em");
      setWidget(dock);
    }

    public void onClick(Widget sender) {
      hide();
    }
  }

  /**
   * A very simple popup that closes automatically when you click off of it.
   */
  private static class MyPopup extends PopupPanel {
    public MyPopup() {
      super(true);

      HTML contents = new HTML(
        "Click anywhere outside this popup to make it disappear.");
      contents.setWidth("128px");
      setWidget(contents);

      setStyleName("ks-popups-Popup");
    }
  }

  public static SinkInfo init() {
    return new SinkInfo(
      "Popups",
      "This page demonstrates GWT's built-in support for in-page "
        + "popups.  The first is a very simple informational popup that closes "
        + "itself automatically when you click off of it.  The second is a more "
        + "complex draggable dialog box. If you're wondering why there's "
        + "a list box at the bottom, it's to demonstrate that you can drag the "
        + "dialog box over it.  "
        + "This is noteworthy because some browsers render lists and combos in "
        + "a funky way that, if GWT didn't do some magic for you, would " 
        + "normally cause the dialog box to appear to hover <i>underneath</i> "
        + "the list box.  Fortunately, you don't have to worry about it -- "
        + "just use the GWT <code>DialogBox</code> class.") {
      public Sink createInstance() {
        return new Popups();
      }
    };
  }

  private Button fDialogButton = new Button("Show Dialog", this);
  private Button fPopupButton = new Button("Show Popup", this);

  public Popups() {
    VerticalPanel panel = new VerticalPanel();
    panel.add(fPopupButton);
    panel.add(fDialogButton);

    ListBox list = new ListBox();
    list.setVisibleItemCount(5);
    for (int i = 0; i < 10; ++i)
      list.addItem("list item " + i);
    panel.add(list);

    panel.setSpacing(8);
    initWidget(panel);
  }

  public void onClick(Widget sender) {
    if (sender == fPopupButton) {
      MyPopup p = new MyPopup();
      int left = sender.getAbsoluteLeft() + 10;
      int top = sender.getAbsoluteTop() + 10;
      p.setPopupPosition(left, top);
      p.show();
    } else if (sender == fDialogButton) {
      DialogBox dlg = new MyDialog();
      int left = fDialogButton.getAbsoluteLeft() + 10;
      int top = fDialogButton.getAbsoluteTop() + 10;
      dlg.setPopupPosition(left, top);
      dlg.show();
    }
  }

  public void onShow() {
  }
}
