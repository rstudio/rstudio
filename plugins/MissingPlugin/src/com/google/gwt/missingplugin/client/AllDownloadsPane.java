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
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Shows a download box for each supported download.
 */
public class AllDownloadsPane extends Composite {

  private static final DownloadListPaneBinder uiBinder = GWT.create(DownloadListPaneBinder.class);

  interface DownloadListPaneBinder extends UiBinder<Widget, AllDownloadsPane> {
  }

  interface MyStyle extends CssResource {
    String platforms();
  }
  
  @UiField
  MyStyle style;
  
  @UiField
  VerticalPanel downloadBoxes;

  @UiField(provided = true)
  Anchor inferredDownloadLink = new Anchor("");

  private final MissingPluginPane.Controller controller;

  public AllDownloadsPane(MissingPluginPane.Controller controller, DownloadInfo linkInfo) {
    this.controller = controller;
    initWidget(uiBinder.createAndBindUi(this));

    JsArray<DownloadEntry> links = linkInfo.getAllDownloads();
    for (int i = 0, n = links.length(); i < n; ++i) {
      DownloadEntry link = links.get(i);
      if (link.isSupported()) {
        String linkContentHtml = link.getLinkContentHtml();
        String href = link.getHref();
        String platforms = link.getPlatforms();
        if (platforms != null) {
          linkContentHtml += "<br><span class=\"" + style.platforms() + "\">"
              + platforms + "</span>";
        }
        DownloadBox box = new DownloadBox(linkContentHtml, href, true);
        downloadBoxes.add(box);
        downloadBoxes.setCellWidth(box, "100%");
      }
    }
  }

  @UiHandler("inferredDownloadLink")
  void onInferredDownloadLinkClicked(ClickEvent e) {
    controller.onInferredDownloadClicked();
  }
}
