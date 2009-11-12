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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

/**
 * Overlay type representing the set of recognized platforms.
 */
public final class DownloadInfo extends JavaScriptObject {

  protected DownloadInfo() {
  }

  public native JsArray<DownloadEntry> getAllDownloads() /*-{
    var a = [];
    for (var k in this.allDownloads) {
      a[a.length] = this.allDownloads[k];
    }
    return a;
  }-*/;

  /**
   * @return the {@link DownloadEntry} that was inferred, or <code>null</code>
   *         if inference failed
   */
  public native DownloadEntry getInferredDownload() /*-{
    var d = this.allDownloads[this.inferredDownloadId];
    if (d) return d;
    return this.allDownloads["unknown"];
  }-*/;

  public native String getTroubleshootingUrl() /*-{
    return this.troubleshootingUrl;
  }-*/;

}
