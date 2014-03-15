/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.dev.codeserver;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Writes HTML to a stream.
 */
class HtmlWriter {
  private static final Set<String> ALLOWED_TAGS =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
          "html", "head", "title", "style",
          "body", "h1", "h2", "h3", "h4", "h5", "h6", "a", "pre", "span",
          "table", "tr", "td")));
  private static final Set<String> ALLOWED_ATTS =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
          "class=", "href=")));

  private final Writer out;

  HtmlWriter(Writer out) {
    this.out = out;
  }

  HtmlWriter startTag(String tag) throws IOException {
    checkTag(tag);
    out.write('<');
    out.write(tag);
    out.write('>');
    return this;
  }

  HtmlWriter startTag(String tag, String att, String value) throws IOException {
    checkTag(tag);
    out.write('<');
    out.write(tag);
    writeAtt(att, value);
    out.write('>');
    return this;
  }

  HtmlWriter endTag(String tag) throws IOException {
    checkTag(tag);
    out.write("</");
    out.write(tag);
    out.write('>');
    return this;
  }

  /**
   * Writes plain text (escaped).
   */
  HtmlWriter text(String plainText) throws IOException {
    for (char c : plainText.toCharArray()) {
      text(c);
    }
    return this;
  }

  /**
   * Writes plain text (escaped).
   */
  void text(char c) throws IOException {
    switch(c) {
      case '<':
        out.write("&lt;");
        break;
      case '>':
        out.write("&gt;");
        break;
      case '&':
        out.write("&amp;");
        break;
      case '"':
        out.write("&quot;");
        break;
      default:
        out.write(c);
    }
  }

  /**
   * Writes a newline.
   */
  void nl() throws IOException {
    out.append('\n');
  }

  private void writeAtt(String att, String value) throws IOException {
    checkAtt(att);
    out.write(' ');
    out.write(att);
    out.write('"');
    text(value);
    out.write('"');
  }

  private void checkTag(String tag) {
    if (!ALLOWED_TAGS.contains(tag)) {
      throw new IllegalArgumentException("unknown tag: " + tag);
    }
  }

  private void checkAtt(String att) {
    if (!ALLOWED_ATTS.contains(att)) {
      throw new IllegalArgumentException("unknown att: " + att);
    }
  }
}
