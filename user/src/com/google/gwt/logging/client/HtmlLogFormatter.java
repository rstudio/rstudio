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

import com.google.gwt.logging.impl.FormatterImpl;
import com.google.gwt.logging.impl.StackTracePrintStream;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Formats LogRecords into HTML. Subclasses should override the GetHtmlPrefix
 * and GetHtmlSuffix methods rather than format to ensure that the message
 * is properly escaped.
 */
public class HtmlLogFormatter extends FormatterImpl {
  private boolean showStackTraces;

  public HtmlLogFormatter(boolean showStackTraces) {
    this.showStackTraces = showStackTraces;
  }

  @Override
  public String format(LogRecord event) {
    final StringBuilder html = new StringBuilder();
    html.append(getHtmlPrefix(event));
    html.append(getRecordInfo(event, " "));
    html.append(getEscaped(event.getMessage()));
    if (showStackTraces && event.getThrown() != null) {
      event.getThrown().printStackTrace(new StackTracePrintStream(html) {
        @Override
        public void append(String str) {
          html.append(getEscaped(str));
        }

        @Override
        public void newLine() {
          html.append("<br>");
        }
      });
    }
    html.append(getHtmlSuffix(event));
    return html.toString();
  }

  protected String getHtmlPrefix(LogRecord event) {
    StringBuilder prefix = new StringBuilder();
    prefix.append("<span style='color:");
    prefix.append(getColor(event.getLevel().intValue()));
    prefix.append("'>");
    prefix.append("<code>");
    return prefix.toString();
  }

  protected String getHtmlSuffix(LogRecord event) {
    return "</code></span>";
  }

  private String getColor(int logLevel) {
    if (logLevel == Level.OFF.intValue()) {
      return "#000"; // black
    }
    if (logLevel >= Level.SEVERE.intValue()) {
      return "#F00"; // bright red
    }
    if (logLevel >= Level.WARNING.intValue()) {
      return "#E56717"; // dark orange
    }
    if (logLevel >= Level.INFO.intValue()) {
      return "#20b000"; // green
    }
    if (logLevel >= Level.CONFIG.intValue()) {
      return "#2B60DE"; // blue
    }
    if (logLevel >= Level.FINE.intValue()) {
      return "#F0F"; // purple
    }
    if (logLevel >= Level.FINER.intValue()) {
      return "#F0F"; // purple
    }
    if (logLevel >= Level.FINEST.intValue()) {
      return "#F0F"; // purple
    }
    return "#000"; // black
  }

  private String getEscaped(String text) {
    text = text.replaceAll("<", "&lt;");
    text = text.replaceAll(">", "&gt;");
    text = text.replaceAll("\t", "&nbsp;&nbsp;&nbsp;");
    return text;
  }

}
