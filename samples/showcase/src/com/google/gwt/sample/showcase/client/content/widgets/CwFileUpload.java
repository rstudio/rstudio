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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle(".gwt-FileUpload")
public class CwFileUpload extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwFileUploadButton();

    String cwFileUploadDescription();

    String cwFileUploadName();

    String cwFileUploadNoFileError();

    String cwFileUploadSelectFile();

    String cwFileUploadSuccessful();
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
  public CwFileUpload(CwConstants constants) {
    super(constants.cwFileUploadName(), constants.cwFileUploadDescription(),
        true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a vertical panel to align the content
    VerticalPanel vPanel = new VerticalPanel();

    // Add a label
    vPanel.add(new HTML(constants.cwFileUploadSelectFile()));

    // Add a file upload widget
    final FileUpload fileUpload = new FileUpload();
    fileUpload.ensureDebugId("cwFileUpload");
    vPanel.add(fileUpload);

    // Add a button to upload the file
    Button uploadButton = new Button(constants.cwFileUploadButton());
    uploadButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        String filename = fileUpload.getFilename();
        if (filename.length() == 0) {
          Window.alert(constants.cwFileUploadNoFileError());
        } else {
          Window.alert(constants.cwFileUploadSuccessful());
        }
      }
    });
    vPanel.add(new HTML("<br>"));
    vPanel.add(uploadButton);

    // Return the layout panel
    return vPanel;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwFileUpload.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
