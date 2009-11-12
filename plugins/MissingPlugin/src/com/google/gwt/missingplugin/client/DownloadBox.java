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
 */package com.google.gwt.missingplugin.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

/**
 * The pretty box used to display a hyperlink for one download (or a link to
 * help for unsupported platforms).
 */
public class DownloadBox extends Composite {

  private static DownloadBoxUiBinder downloadBoxUiBinder = GWT.create(DownloadBoxUiBinder.class);
  private static UnsupportedBoxUiBinder unsupportedBoxUiBinder = GWT.create(UnsupportedBoxUiBinder.class);

  interface DownloadBoxUiBinder extends UiBinder<HTMLPanel, DownloadBox> {
  }

  @UiTemplate("UnsupportedBox.ui.xml")
  interface UnsupportedBoxUiBinder extends UiBinder<HTMLPanel, DownloadBox> {
  }

  @UiField
  AnchorElement downloadLink, downloadLinkIcon;

  public DownloadBox(String linkContentHtml, String url, boolean supported) {
    if (supported) {
      initWidget(downloadBoxUiBinder.createAndBindUi(this));
    } else {
      initWidget(unsupportedBoxUiBinder.createAndBindUi(this));
    }
    downloadLink.setInnerHTML(linkContentHtml);
    downloadLink.setHref(url);
    downloadLinkIcon.setHref(url);
  }
}
