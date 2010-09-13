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
package com.google.gwt.sample.showcase.client.content.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.Showcase;
import com.google.gwt.sample.showcase.client.ShowcaseConstants;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle(".gwt-Hyperlink")
public class CwHyperlink extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwHyperlinkChoose();

    String cwHyperlinkDescription();

    String cwHyperlinkName();
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
  public CwHyperlink(CwConstants constants) {
    super(
        constants.cwHyperlinkName(), constants.cwHyperlinkDescription(), true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Add a label
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(new HTML(constants.cwHyperlinkChoose()));
    vPanel.setSpacing(5);

    // Add a hyper link to each section in the Widgets category
    ShowcaseConstants allConstants = (ShowcaseConstants) constants;
    vPanel.add(getHyperlink(CwCheckBox.class, allConstants.cwCheckBoxName()));
    vPanel.add(
        getHyperlink(CwRadioButton.class, allConstants.cwRadioButtonName()));
    vPanel.add(
        getHyperlink(CwBasicButton.class, allConstants.cwBasicButtonName()));
    vPanel.add(
        getHyperlink(CwCustomButton.class, allConstants.cwCustomButtonName()));
    vPanel.add(
        getHyperlink(CwFileUpload.class, allConstants.cwFileUploadName()));
    vPanel.add(
        getHyperlink(CwDatePicker.class, allConstants.cwDatePickerName()));

    // Return the panel
    return vPanel;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwHyperlink.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  /**
   * Get a {@link Hyperlink} to a section based on the name of the
   * {@link ContentWidget} example.
   *
   * @param cwClass the {@link ContentWidget} class
   * @param name the name to display for the link
   * @return a {@link Hyperlink}
   */
  @ShowcaseSource
  private <C extends ContentWidget> Hyperlink getHyperlink(
      Class<C> cwClass, String name) {
    Hyperlink link = new Hyperlink(
        name, Showcase.getContentWidgetToken(cwClass));
    link.ensureDebugId("cwHyperlink-" + cwClass.getName());
    return link;
  }
}
