// Copyright 2006 Google Inc. All Rights Reserved.
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

  public BrowserDialog(Shell parent, TreeLogger logger, String html) {
    super(parent, 550, 520, true, false);
    fLogger = logger;
    fHtml = html;
    fUrl = null;
  }

  protected Control createContents(Composite parent) {
    Browser browser = new Browser(parent, SWT.BORDER);

    browser.addTitleListener(new TitleListener() {
      public void changed(TitleEvent event) {
        BrowserDialog.this.
        setText(event.title);
      }});

    if (fHtml != null) {
      browser.setText(fHtml);
    } else if (fUrl != null) {
      browser.setUrl(fUrl.toString());
    }
    
    browser.addLocationListener(new LocationListener() {
      public void changed(LocationEvent event) {
      }
      public void changing(LocationEvent event) {
        event.doit = false;
        BrowserWidget.launchExternalBrowser(fLogger, event.location);
      }
    });

    return browser;
  }

  private final String fHtml;
  private final URL fUrl;
  private final TreeLogger fLogger;
}
