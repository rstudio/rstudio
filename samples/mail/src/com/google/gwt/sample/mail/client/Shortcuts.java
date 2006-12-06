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
import com.google.gwt.user.client.ui.StackPanel;

/**
 * A composite that contains the shortcut stack panel on the left side. The
 * mailbox tree and shortcut lists don't actually do anything, but serve to show
 * how you can construct an interface using
 * {@link com.google.gwt.user.client.ui.StackPanel},
 * {@link com.google.gwt.user.client.ui.Tree}, and other custom widgets.
 */
public class Shortcuts extends Composite {

  private StackPanel stackPanel = new StackPanel();

  public Shortcuts() {
    // Create the groups within the stack panel.
    stackPanel.add(new Mailboxes(), createHeaderHTML("mailgroup.gif", "Mail"), true);
    stackPanel.add(new Tasks(), createHeaderHTML("tasksgroup.gif", "Tasks"), true);
    stackPanel.add(new Contacts(), createHeaderHTML("contactsgroup.gif", "Contacts"), true);

    // Show the mailboxes group by default.
    stackPanel.showStack(0);

    initWidget(stackPanel);
  }

  /**
   * Creates an HTML fragment that places an image & caption together, for use
   * in a group header.
   * 
   * @param imageUrl the url of the icon image to be used
   * @param caption the group caption
   * @return the header HTML fragment
   */
  private String createHeaderHTML(String imageUrl, String caption) {
    return "<table align='left'><tr>" + "<td><img src='" + imageUrl + "'></td>"
      + "<td style='vertical-align:middle'><b style='white-space:nowrap'>"
      + caption + "</b></td>" + "</tr></table>";
  }
}
