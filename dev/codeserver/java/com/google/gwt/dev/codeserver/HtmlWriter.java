// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.gwt.dev.codeserver;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Writes HTML to a stream.
 *
 * @author skybrian@google.com (Brian Slesinsky)
 */
class HtmlWriter {
  private static final Set<String> ALLOWED_TAGS =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
          "html", "head", "title", "style", "body", "pre", "span")));
  private static final Set<String> ALLOWED_ATTS =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("class=")));

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
