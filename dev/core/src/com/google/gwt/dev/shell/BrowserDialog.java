/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import java.net.URL;

/**
 * A composite containing a browser widget.
 */
public class BrowserDialog extends DialogBase {

  private final String html;

  private final URL url;

  private final TreeLogger logger;

  public BrowserDialog(Shell parent, TreeLogger logger, String html) {
    super(parent, 550, 520);
    this.logger = logger;
    this.html = html;
    this.url = null;
  }

  @Override
  protected Control createContents(Composite parent) {
    Browser browser = new Browser(parent, SWT.BORDER);

    browser.addTitleListener(new TitleListener() {
      public void changed(TitleEvent event) {
        BrowserDialog.this.setText(event.title);
      }
    });

    if (html != null) {
      browser.setText(html);
    } else if (url != null) {
      browser.setUrl(url.toString());
    }

    browser.addLocationListener(new LocationListener() {
      public void changed(LocationEvent event) {
      }

      public void changing(LocationEvent event) {
        event.doit = false;
        BrowserWidget.launchExternalBrowser(logger, event.location);
      }
    });

    return browser;
  }
}
