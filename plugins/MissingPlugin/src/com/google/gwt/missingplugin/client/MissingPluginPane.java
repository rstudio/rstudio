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
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

/**
 * The main UI. Also manages subviews.
 */
public class MissingPluginPane extends Composite {

  /**
   * Abstract the relationships between the subviews and this one.
   */
  public class Controller {
    void onAllDownloadsClicked() {
      allDownloadsPane.setVisible(true);
      inferredDownloadPane.setVisible(false);
    }

    void onInferredDownloadClicked() {
      allDownloadsPane.setVisible(false);
      inferredDownloadPane.setVisible(true);
    }
  }

  interface MissingPluginPaneUiBinder extends UiBinder<HTMLPanel, MissingPluginPane> {
  }

  @UiField(provided = true)
  AllDownloadsPane allDownloadsPane;

  @UiField(provided = true)
  InferredDownloadPane inferredDownloadPane;

  @UiField
  AnchorElement troubleshootingLink;

  private final Controller controller = new Controller();

  private final MissingPluginPaneUiBinder uiBinder = GWT.create(MissingPluginPaneUiBinder.class);

  public MissingPluginPane(DownloadInfo linkInfo) {
    inferredDownloadPane = new InferredDownloadPane(controller, linkInfo);
    allDownloadsPane = new AllDownloadsPane(controller, linkInfo);

    initWidget(uiBinder.createAndBindUi(this));
    troubleshootingLink.setHref(linkInfo.getTroubleshootingUrl());
  }

}
