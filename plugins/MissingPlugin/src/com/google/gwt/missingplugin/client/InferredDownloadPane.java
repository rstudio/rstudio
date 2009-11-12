/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.missingplugin.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * A subview that shows only the download that is recommended.
 */
public class InferredDownloadPane extends Composite {

  private static InferredDownloadPaneUiBinder uiBinder = GWT.create(InferredDownloadPaneUiBinder.class);

  @UiField(provided = true)
  Anchor allDownloadsLink;

  @UiField(provided = true)
  DownloadBox downloadBox;

  private final MissingPluginPane.Controller controller;

  interface InferredDownloadPaneUiBinder extends UiBinder<Widget, InferredDownloadPane> {
  }

  public InferredDownloadPane(MissingPluginPane.Controller controller, DownloadInfo linkInfo) {
    this.controller = controller;
    this.allDownloadsLink = new Anchor("");
    DownloadEntry inferredDownload = linkInfo.getInferredDownload();
    if (inferredDownload != null) {
      String linkContentHtml = inferredDownload.getLinkContentHtml();
      String href = inferredDownload.getHref();
      boolean supported = inferredDownload.isSupported();
      this.downloadBox = new DownloadBox(linkContentHtml, href, supported);
    } else {
      // Unsupported/unknown browser
      this.downloadBox = new DownloadBox("Unsupported or unrecognized browser", "", false);
    }

    initWidget(uiBinder.createAndBindUi(this));
  }

  @UiHandler("allDownloadsLink")
  void onOtherLinkClicked(ClickEvent e) {
    controller.onAllDownloadsClicked();
  }
}
