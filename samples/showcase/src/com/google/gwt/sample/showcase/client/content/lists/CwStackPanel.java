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
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DecoratedStackPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle({
    ".gwt-DecoratedStackPanel", "html>body .gwt-DecoratedStackPanel",
    "* html .gwt-DecoratedStackPanel", ".cw-StackPanelHeader"})
public class CwStackPanel extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String[] cwStackPanelContacts();

    String[] cwStackPanelContactsEmails();

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
   */
  @ShowcaseSource
  public interface Images extends Tree.Resources {
    ImageResource contactsgroup();

    ImageResource defaultContact();

    ImageResource drafts();

    ImageResource filtersgroup();

    ImageResource inbox();

    ImageResource mailgroup();

    ImageResource sent();

    ImageResource templates();

    ImageResource trash();

    /**
     * Use noimage.png, which is a blank 1x1 image.
     */
    @Override
    @Source("noimage.png")
    ImageResource treeLeaf();
  }

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwStackPanel(CwConstants constants) {
    super(constants.cwStackPanelName(), constants.cwStackPanelDescription(),
        true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Get the images
    Images images = (Images) GWT.create(Images.class);

    // Create a new stack panel
    DecoratedStackPanel stackPanel = new DecoratedStackPanel();
    stackPanel.setWidth("200px");

    // Add the Mail folders
    String mailHeader = getHeaderString(
        constants.cwStackPanelMailHeader(), images.mailgroup());
    stackPanel.add(createMailItem(images), mailHeader, true);

    // Add a list of filters
    String filtersHeader = getHeaderString(
        constants.cwStackPanelFiltersHeader(), images.filtersgroup());
    stackPanel.add(createFiltersItem(), filtersHeader, true);

    // Add a list of contacts
    String contactsHeader = getHeaderString(
        constants.cwStackPanelContactsHeader(), images.contactsgroup());
    stackPanel.add(createContactsItem(images), contactsHeader, true);

    // Return the stack panel
    stackPanel.ensureDebugId("cwStackPanel");
    return stackPanel;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwStackPanel.class, new RunAsyncCallback() {

      @Override
      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      @Override
      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  private void addItem(TreeItem root, ImageResource image, String label) {
    SafeHtmlBuilder itemHtml = new SafeHtmlBuilder();
    itemHtml.append(AbstractImagePrototype.create(image).getSafeHtml());
    itemHtml.appendHtmlConstant(" ").appendEscaped(label);
    root.addItem(itemHtml.toSafeHtml());
  }

  /**
   * Create the list of Contacts.
   *
   * @param images the {@link Images} used in the Contacts
   * @return the list of contacts
   */
  @ShowcaseSource
  private VerticalPanel createContactsItem(Images images) {
    // Create a popup to show the contact info when a contact is clicked
    HorizontalPanel contactPopupContainer = new HorizontalPanel();
    contactPopupContainer.setSpacing(5);
    contactPopupContainer.add(new Image(images.defaultContact()));
    final HTML contactInfo = new HTML();
    contactPopupContainer.add(contactInfo);
    final PopupPanel contactPopup = new PopupPanel(true, false);
    contactPopup.setWidget(contactPopupContainer);

    // Create the list of contacts
    VerticalPanel contactsPanel = new VerticalPanel();
    contactsPanel.setSpacing(4);
    String[] contactNames = constants.cwStackPanelContacts();
    String[] contactEmails = constants.cwStackPanelContactsEmails();
    for (int i = 0; i < contactNames.length; i++) {
      final String contactName = contactNames[i];
      final String contactEmail = contactEmails[i];
      final Anchor contactLink = new Anchor(contactName);
      contactsPanel.add(contactLink);

      // Open the contact info popup when the user clicks a contact
      contactLink.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          // Set the info about the contact
          contactInfo.setHTML(contactName + "<br><i>" + contactEmail + "</i>");

          // Show the popup of contact info
          int left = contactLink.getAbsoluteLeft() + 14;
          int top = contactLink.getAbsoluteTop() + 14;
          contactPopup.setPopupPosition(left, top);
          contactPopup.show();
        }
      });
    }
    return contactsPanel;
  }

  /**
   * Create the list of filters for the Filters item.
   *
   * @return the list of filters
   */
  @ShowcaseSource
  private VerticalPanel createFiltersItem() {
    VerticalPanel filtersPanel = new VerticalPanel();
    filtersPanel.setSpacing(4);
    for (String filter : constants.cwStackPanelFilters()) {
      filtersPanel.add(new CheckBox(filter));
    }
    return filtersPanel;
  }

  /**
   * Create the {@link Tree} of Mail options.
   *
   * @param images the {@link Images} used in the Mail options
   * @return the {@link Tree} of mail options
   */
  @ShowcaseSource
  private Tree createMailItem(Images images) {
    Tree mailPanel = new Tree(images);
    TreeItem mailPanelRoot = mailPanel.addTextItem("foo@example.com");
    String[] mailFolders = constants.cwStackPanelMailFolders();
    addItem(mailPanelRoot, images.inbox(), mailFolders[0]);
    addItem(mailPanelRoot, images.drafts(), mailFolders[1]);
    addItem(mailPanelRoot, images.templates(), mailFolders[2]);
    addItem(mailPanelRoot, images.sent(), mailFolders[3]);
    addItem(mailPanelRoot, images.trash(), mailFolders[4]);
    mailPanelRoot.setState(true);
    return mailPanel;
  }

  /**
   * Get a string representation of the header that includes an image and some
   * text.
   *
   * @param text the header text
   * @param image the {@link ImageResource} to add next to the header
   * @return the header as a string
   */
  @ShowcaseSource
  private String getHeaderString(String text, ImageResource image) {
    // Add the image and text to a horizontal panel
    HorizontalPanel hPanel = new HorizontalPanel();
    hPanel.setSpacing(0);
    hPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    hPanel.add(new Image(image));
    HTML headerText = new HTML(text);
    headerText.setStyleName("cw-StackPanelHeader");
    hPanel.add(headerText);

    // Return the HTML string for the panel
    return hPanel.getElement().getString();
  }
}
