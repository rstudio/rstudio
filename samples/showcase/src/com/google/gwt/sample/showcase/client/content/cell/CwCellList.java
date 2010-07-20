/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.showcase.client.content.cell;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseRaw;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.content.cell.ContactDatabase.ContactInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionModel.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel.SelectionChangeHandler;
import com.google.gwt.view.client.SingleSelectionModel;

/**
 * Example file.
 */
@ShowcaseRaw({
    "ContactDatabase.java", "CwCellList.ui.xml", "ContactInfoForm.java",
    "ShowMorePagerPanel.java"})
public class CwCellList extends ContentWidget {

  /**
   * The UiBinder interface used by this example.
   */
  @ShowcaseSource
  interface Binder extends UiBinder<Widget, CwCellList> {
  }

  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants
      extends Constants, ContentWidget.CwConstants {
    String cwCellListDescription();

    String cwCellListName();
  }

  /**
   * The images used for this example.
   */
  @ShowcaseSource
  static interface Images extends ClientBundle {
    ImageResource contact();
  }

  /**
   * The Cell used to render a {@link ContactInfo}.
   */
  @ShowcaseSource
  static class ContactCell extends AbstractCell<ContactInfo> {

    /**
     * The html of the image used for contacts.
     */
    private final String imageHtml;

    public ContactCell(ImageResource image) {
      this.imageHtml = AbstractImagePrototype.create(image).getHTML();
    }

    @Override
    public void render(ContactInfo value, Object key, StringBuilder sb) {
      // Value can be null, so do a null check.
      if (value == null) {
        return;
      }

      sb.append("<table>");

      // Add the contact image.
      sb.append("<tr><td rowspan='3'>");
      sb.append(imageHtml);
      sb.append("</td>");

      // Add the name.
      sb.append("<td style='font-size:95%;'>");
      sb.append(value.getFullName());
      sb.append("</td>");
      sb.append("</tr>");

      // Add the address.
      sb.append("<tr><td>");
      sb.append(value.getAddress());
      sb.append("</td></tr>");

      sb.append("</table>");
    }
  }

  /**
   * The CellList.
   */
  @ShowcaseData
  @UiField(provided = true)
  CellList<ContactInfo> cellList;

  /**
   * The contact form used to update contacts.
   */
  @ShowcaseData
  @UiField
  ContactInfoForm contactForm;

  /**
   * The button used to generate more contacts.
   */
  @ShowcaseData
  @UiField
  Button generateButton;

  /**
   * The pager used to change the range of data.
   */
  @ShowcaseData
  @UiField(provided = true)
  ShowMorePagerPanel<ContactInfo> pager;

  /**
   * The {@link ScrollPanel} that wraps the list.
   */
  @ShowcaseData
  @UiField(provided = true)
  ScrollPanel scrollPanel;

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private CwConstants constants;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwCellList(CwConstants constants) {
    super(constants);
    this.constants = constants;
    registerSource("ContactDatabase.java");
    registerSource("CwCellList.ui.xml");
    registerSource("ContactInfoForm.java");
    registerSource("ShowMorePagerPanel.java");
  }

  @Override
  public String getDescription() {
    return constants.cwCellListDescription();
  }

  @Override
  public String getName() {
    return constants.cwCellListName();
  }

  @Override
  public boolean hasStyle() {
    return false;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    Images images = GWT.create(Images.class);

    // Create a CellList.
    ContactCell contactCell = new ContactCell(images.contact());
    cellList = new CellList<ContactInfo>(contactCell);
    cellList.setPageSize(30);

    // Add a selection model so we can select cells.
    final SingleSelectionModel<ContactInfo> selectionModel = new SingleSelectionModel<ContactInfo>();
    cellList.setSelectionModel(selectionModel);
    selectionModel.addSelectionChangeHandler(new SelectionChangeHandler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        contactForm.setContact(selectionModel.getSelectedObject());
      }
    });

    // Create the pager.
    scrollPanel = new ScrollPanel();
    pager = new ShowMorePagerPanel<ContactInfo>(cellList, scrollPanel);

    // Set a key provider that provides a unique key for each contact. If key is
    // used to identify contacts when fields (such as the name and address)
    // change.
    cellList.setKeyProvider(ContactDatabase.ContactInfo.KEY_PROVIDER);
    selectionModel.setKeyProvider(ContactDatabase.ContactInfo.KEY_PROVIDER);

    // Add the CellList to the adapter in the database.
    ContactDatabase.get().addView(cellList);

    // Create the UiBinder.
    Binder uiBinder = GWT.create(Binder.class);
    Widget widget = uiBinder.createAndBindUi(this);

    // Handle events from the generate button.
    generateButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        ContactDatabase.get().generateContacts(50);
      }
    });

    return widget;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwCellList.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  @Override
  protected void setRunAsyncPrefetches() {
    prefetchCellWidgets();
  }
}
