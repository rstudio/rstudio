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
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

/**
 * A tree displaying a set of email folders.
 */
public class Mailboxes extends Composite {

  private Tree tree = new Tree();

  public Mailboxes() {
    TreeItem root = new TreeItem(imageItemHTML("home.gif", "foo@example.com"));
    tree.addItem(root);

    TreeItem inboxItem = addImageItem(root, "Inbox");
    addImageItem(root, "Drafts");
    addImageItem(root, "Templates");
    addImageItem(root, "Sent");
    addImageItem(root, "Trash");

    root.setState(true);
    initWidget(tree);
  }

  /**
   * A helper method to simplify adding tree items that have attached images.
   * {@link #addImageItem(TreeItem, String) code}
   * 
   * @param root the tree item to which the new item will be added.
   * @param title the text associated with this item.
   */
  private TreeItem addImageItem(TreeItem root, String title) {
    TreeItem item = new TreeItem(imageItemHTML(title + ".gif", title));
    root.addItem(item);
    return item;
  }

  /**
   * Generates HTML for a tree item with an attached icon.
   * 
   * @param imageUrl the url of the icon image
   * @param title the title of the item
   * @return the resultant HTML
   */
  private String imageItemHTML(String imageUrl, String title) {
    return "<span><img style='margin-right:4px' src='" + imageUrl.toLowerCase()
      + "'>" + title + "</span>";
  }
}
