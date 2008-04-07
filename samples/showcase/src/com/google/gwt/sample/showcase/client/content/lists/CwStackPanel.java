/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.sample.showcase.client.content.lists;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeImages;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 * 
 * @gwt.CSS .gwt-StackPanel
 * @gwt.CSS .gwt-StackPanelItem
 * @gwt.CSS html>body .gwt-StackPanelItem
 * @gwt.CSS * html .gwt-StackPanelItem
 * @gwt.CSS .cw-StackPanelHeader
 */
public class CwStackPanel extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   * 
   * @gwt.SRC
   */
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String[] cwStackPanelContacts();

    String cwStackPanelContactsHeader();

    String cwStackPanelDescription();

    String[] cwStackPanelFilters();

    String cwStackPanelFiltersHeader();

    String[] cwStackPanelMailFolders();

    String cwStackPanelMailHeader();

    String cwStackPanelName();
  }

  /**
   * Specifies the images that will be bundled for this example.
   * 
   * We will override the leaf image used in the tree. Instead of using a blank
   * 16x16 image, we will use a blank 1x1 image so it does not take up any
   * space. Each TreeItem will use its own custom image.
   * 
   * @gwt.SRC
   */
  public interface Images extends TreeImages {
    AbstractImagePrototype contactsgroup();

    AbstractImagePrototype drafts();

    AbstractImagePrototype filtersgroup();

    AbstractImagePrototype inbox();

    AbstractImagePrototype mailgroup();

    AbstractImagePrototype sent();

    AbstractImagePrototype templates();

    AbstractImagePrototype trash();

    /**
     * Use noimage.png, which is a blank 1x1 image.
     */
    @Resource("noimage.png")
    AbstractImagePrototype treeLeaf();
  }

  /**
   * An instance of the constants.
   * 
   * @gwt.DATA
   */
  private CwConstants constants;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public CwStackPanel(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwStackPanelDescription();
  }

  @Override
  public String getName() {
    return constants.cwStackPanelName();
  }

  @Override
  public boolean hasStyle() {
    return true;
  }

  /**
   * Initialize this example.
   * 
   * @gwt.SRC
   */
  @Override
  public Widget onInitialize() {
    // Get the images
    Images images = (Images) GWT.create(Images.class);

    // Create a new stack panel
    StackPanel stackPanel = new StackPanel();
    stackPanel.setWidth("200px");

    // Add links to Mail folders
    Tree mailPanel = new Tree(images);
    TreeItem mailPanelRoot = mailPanel.addItem("foo@example.com");
    String[] mailFolders = constants.cwStackPanelMailFolders();
    mailPanelRoot.addItem(images.inbox().getHTML() + " " + mailFolders[0]);
    mailPanelRoot.addItem(images.drafts().getHTML() + " " + mailFolders[1]);
    mailPanelRoot.addItem(images.templates().getHTML() + " " + mailFolders[2]);
    mailPanelRoot.addItem(images.sent().getHTML() + " " + mailFolders[3]);
    mailPanelRoot.addItem(images.trash().getHTML() + " " + mailFolders[4]);
    mailPanelRoot.setState(true);
    String mailHeader = getHeaderString(constants.cwStackPanelMailHeader(),
        images.mailgroup());
    stackPanel.add(mailPanel, mailHeader, true);

    // Add a list of filters
    VerticalPanel filtersPanel = new VerticalPanel();
    filtersPanel.setSpacing(4);
    for (String filter : constants.cwStackPanelFilters()) {
      filtersPanel.add(new CheckBox(filter));
    }
    String filtersHeader = getHeaderString(
        constants.cwStackPanelFiltersHeader(), images.filtersgroup());
    stackPanel.add(filtersPanel, filtersHeader, true);

    // Add links to each contact
    VerticalPanel contactsPanel = new VerticalPanel();
    contactsPanel.setSpacing(4);
    for (String contact : constants.cwStackPanelContacts()) {
      contactsPanel.add(new Label(contact));
    }
    String contactsHeader = getHeaderString(
        constants.cwStackPanelContactsHeader(), images.contactsgroup());
    stackPanel.add(contactsPanel, contactsHeader, true);

    // Return the stack panel
    stackPanel.ensureDebugId("cwStackPanel");
    return stackPanel;
  }

  /**
   * Get a string representation of the header that includes an image and some
   * text.
   * 
   * @param text the header text
   * @param image the {@link AbstractImagePrototype} to add next to the header
   * @return the header as a string
   * @gwt.SRC
   */
  private String getHeaderString(String text, AbstractImagePrototype image) {
    // Add the image and text to a horizontal panel
    HorizontalPanel hPanel = new HorizontalPanel();
    hPanel.setSpacing(0);
    hPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    hPanel.add(image.createImage());
    HTML headerText = new HTML(text);
    headerText.setStyleName("cw-StackPanelHeader");
    hPanel.add(headerText);

    // Return the HTML string for the panel
    return hPanel.toString();
  }
}
