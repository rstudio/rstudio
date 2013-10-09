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

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

/**
 * A tree displaying a set of email folders.
 */
public class Mailboxes extends Composite {

  /**
   * Specifies the images that will be bundled for this Composite and specify
   * that tree's images should also be included in the same bundle.
   */
  public interface Images extends Tree.Resources {
    ImageResource drafts();

    ImageResource home();

    ImageResource inbox();

    ImageResource sent();

    ImageResource templates();

    ImageResource trash();

    @Override
    @Source("noimage.png")
    ImageResource treeLeaf();
  }

  private Tree tree;

  /**
   * Constructs a new mailboxes widget.
   */
  public Mailboxes() {
    Images images = GWT.create(Images.class);

    tree = new Tree(images);
    TreeItem root = new TreeItem(
        imageItemHTML(images.home(), "foo@example.com"));
    tree.addItem(root);

    addImageItem(root, "Inbox", images.inbox());
    addImageItem(root, "Drafts", images.drafts());
    addImageItem(root, "Templates", images.templates());
    addImageItem(root, "Sent", images.sent());
    addImageItem(root, "Trash", images.trash());

    root.setState(true);
    initWidget(tree);
  }

  /**
   * A helper method to simplify adding tree items that have attached images.
   * {@link #addImageItem(TreeItem, String, ImageResource) code}
   * 
   * @param root the tree item to which the new item will be added.
   * @param title the text associated with this item.
   */
  private TreeItem addImageItem(TreeItem root, String title,
      ImageResource imageProto) {
    TreeItem item = new TreeItem(imageItemHTML(imageProto, title));
    root.addItem(item);
    return item;
  }

  /**
   * Generates HTML for a tree item with an attached icon.
   * 
   * @param imageProto the image prototype to use
   * @param title the title of the item
   * @return the resultant HTML
   */
  private SafeHtml imageItemHTML(ImageResource imageProto, String title) {
    SafeHtmlBuilder builder = new SafeHtmlBuilder();
    builder.append(AbstractImagePrototype.create(imageProto).getSafeHtml());
    builder.appendHtmlConstant(" ");
    builder.appendEscaped(title);
    return builder.toSafeHtml();
  }
}
