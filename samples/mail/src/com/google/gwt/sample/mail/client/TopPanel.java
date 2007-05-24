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
package com.google.gwt.sample.mail.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ImageBundle;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The top panel, which contains the 'welcome' message and various links.
 */
public class TopPanel extends Composite implements ClickListener {

  /**
   * An image bundle for this widgets images.
   */
  public interface Images extends ImageBundle {
    AbstractImagePrototype logo();
  }

  private HTML signOutLink = new HTML("<a href='javascript:;'>Sign Out</a>");
  private HTML aboutLink = new HTML("<a href='javascript:;'>About</a>");

  public TopPanel(Images images) {
    HorizontalPanel outer = new HorizontalPanel();
    VerticalPanel inner = new VerticalPanel();

    outer.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
    inner.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);

    HorizontalPanel links = new HorizontalPanel();
    links.setSpacing(4);
    links.add(signOutLink);
    links.add(aboutLink);

    final Image logo = images.logo().createImage();
    outer.add(logo);
    outer.setCellHorizontalAlignment(logo, HorizontalPanel.ALIGN_LEFT);

    outer.add(inner);
    inner.add(new HTML("<b>Welcome back, foo@example.com</b>"));
    inner.add(links);

    signOutLink.addClickListener(this);
    aboutLink.addClickListener(this);

    initWidget(outer);
    setStyleName("mail-TopPanel");
    links.setStyleName("mail-TopPanelLinks");
  }

  public void onClick(Widget sender) {
    if (sender == signOutLink) {
      Window.alert("If this were implemented, you would be signed out now.");
    } else if (sender == aboutLink) {
      // When the 'About' item is selected, show the AboutDialog.
      // Note that showing a dialog box does not block -- execution continues
      // normally, and the dialog fires an event when it is closed.
      AboutDialog dlg = new AboutDialog();
      dlg.show();
      dlg.center();
    }
  }
}
