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
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseRaw;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.content.cell.ContactDatabase.ContactInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy.KeyboardPagingPolicy;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

/**
 * Example file.
 */
@ShowcaseRaw({
    "ContactDatabase.java", "CwCellList.ui.xml", "ContactInfoForm.java",
    "ShowMorePagerPanel.java", "RangeLabelPager.java"})
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
  public static interface CwConstants extends Constants {
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
    public void render(Context context, ContactInfo value, SafeHtmlBuilder sb) {
      // Value can be null, so do a null check..
      if (value == null) {
        return;
      }

      sb.appendHtmlConstant("<table>");

      // Add the contact image.
      sb.appendHtmlConstant("<tr><td rowspan='3'>");
      sb.appendHtmlConstant(imageHtml);
      sb.appendHtmlConstant("</td>");

      // Add the name and address.
      sb.appendHtmlConstant("<td style='font-size:95%;'>");
      sb.appendEscaped(value.getFullName());
      sb.appendHtmlConstant("</td></tr><tr><td>");
      sb.appendEscaped(value.getAddress());
      sb.appendHtmlConstant("</td></tr></table>");
    }
  }

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
  @UiField
  ShowMorePagerPanel pagerPanel;

  /**
   * The pager used to display the current range.
   */
  @ShowcaseData
  @UiField
  RangeLabelPager rangeLabelPager;

  /**
   * The CellList.
   */
  @ShowcaseData
  private CellList<ContactInfo> cellList;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwCellList(CwConstants constants) {
    super(constants.cwCellListName(), constants.cwCellListDescription(), false,
        "ContactDatabase.java", "CwCellList.ui.xml", "ContactInfoForm.java",
        "ShowMorePagerPanel.java", "RangeLabelPager.java");
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

    // Set a key provider that provides a unique key for each contact. If key is
    // used to identify contacts when fields (such as the name and address)
    // change.
    cellList = new CellList<ContactInfo>(contactCell,
        ContactDatabase.ContactInfo.KEY_PROVIDER);
    cellList.setPageSize(30);
    cellList.setKeyboardPagingPolicy(KeyboardPagingPolicy.INCREASE_RANGE);
    cellList.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);

    // Add a selection model so we can select cells.
    final SingleSelectionModel<ContactInfo> selectionModel = new SingleSelectionModel<ContactInfo>(
        ContactDatabase.ContactInfo.KEY_PROVIDER);
    cellList.setSelectionModel(selectionModel);
    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        contactForm.setContact(selectionModel.getSelectedObject());
      }
    });

    // Create the UiBinder.
    Binder uiBinder = GWT.create(Binder.class);
    Widget widget = uiBinder.createAndBindUi(this);

    // Add the CellList to the data provider in the database.
    ContactDatabase.get().addDataDisplay(cellList);

    // Set the cellList as the display of the pagers. This example has two
    // pagers. pagerPanel is a scrollable pager that extends the range when the
    // user scrolls to the bottom. rangeLabelPager is a pager that displays the
    // current range, but does not have any controls to change the range.
    pagerPanel.setDisplay(cellList);
    rangeLabelPager.setDisplay(cellList);

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
}
