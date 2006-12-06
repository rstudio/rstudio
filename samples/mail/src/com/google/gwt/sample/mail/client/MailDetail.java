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
package com.google.gwt.sample.mail.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A composite for displaying the details of an email message.
 */
public class MailDetail extends Composite {

  private VerticalPanel panel = new VerticalPanel();
  private VerticalPanel headerPanel = new VerticalPanel();
  private HTML subject = new HTML();
  private HTML sender = new HTML();
  private HTML recipient = new HTML();
  private HTML body = new HTML();
  private ScrollPanel scroller = new ScrollPanel(body);

  public MailDetail() {
    body.setWordWrap(true);

    headerPanel.add(subject);
    headerPanel.add(sender);
    headerPanel.add(recipient);
    headerPanel.setWidth("100%");

    DockPanel innerPanel = new DockPanel();
    innerPanel.add(headerPanel, DockPanel.NORTH);
    innerPanel.add(scroller, DockPanel.CENTER);

    innerPanel.setCellHeight(scroller, "100%");
    panel.add(innerPanel);
    innerPanel.setSize("100%", "100%");
    scroller.setSize("100%", "100%");
    initWidget(panel);

    setStyleName("mail-Detail");
    headerPanel.setStyleName("mail-DetailHeader");
    innerPanel.setStyleName("mail-DetailInner");
    subject.setStyleName("mail-DetailSubject");
    sender.setStyleName("mail-DetailSender");
    recipient.setStyleName("mail-DetailRecipient");
    body.setStyleName("mail-DetailBody");
  }

  public void setItem(MailItem item) {
    subject.setHTML(item.subject);
    sender.setHTML("<b>From:</b>&nbsp;" + item.sender);
    recipient.setHTML("<b>To:</b>&nbsp;foo@example.com");
    body.setHTML(item.body);
  }

  /**
   * Adjusts the widget's size such that it fits within the window's client
   * area.
   */
  public void adjustSize(int windowWidth, int windowHeight) {
    int scrollWidth = windowWidth - scroller.getAbsoluteLeft() - 9;
    if (scrollWidth < 1)
      scrollWidth = 1;

    int scrollHeight = windowHeight - scroller.getAbsoluteTop() - 9;
    if (scrollHeight < 1)
      scrollHeight = 1;

    scroller.setSize("" + scrollWidth, "" + scrollHeight);
  }
}
