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

package com.google.gwt.logging.client;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A handler which can log to any widget which extends the HasWidgets interface.
 * This allows users to log to anywhere they would like in their UI - see the
 * LogConfiguration class for an example. Note that the widget passed in must
 * handle multiple calls to widget.add(widget) gracefully.
 */
public class HasWidgetsLogHandler extends Handler {

  private HasWidgets widgetContainer;

  public HasWidgetsLogHandler(HasWidgets container) {
    this.widgetContainer = container;
    setFormatter(new HtmlLogFormatter(true));
    setLevel(Level.ALL);
  }

  public void clear() {
    widgetContainer.clear();
  }

  @Override
  public void close() {
    // Do nothing
  }

  @Override
  public void flush() {
    // Do nothing
  }

  @Override
  public void publish(LogRecord record) {
    if (!isLoggable(record)) {
      return;
    }
    Formatter formatter = getFormatter();
    String msg = formatter.format(record);
    // We want to make sure that unescaped messages are not output as HTML to
    // the window and the HtmlLogFormatter ensures this. If you want to write a
    // new formatter, subclass HtmlLogFormatter and override the getHtmlPrefix
    // and getHtmlSuffix methods.
    if (formatter instanceof HtmlLogFormatter) {
      widgetContainer.add(new HTML(msg));
    } else {
      widgetContainer.add(new Label(msg));
    }
  }
}

