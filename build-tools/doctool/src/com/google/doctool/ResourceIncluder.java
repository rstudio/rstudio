/*
 * Copyright 2006 Google Inc.
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
package com.google.doctool;

import com.sun.javadoc.Tag;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods related to including external resources in doc.
 */
public class ResourceIncluder {

  /**
   * Copied from {@link com.google.gwt.util.tools.Utility#close(InputStream)}.
   */
  public static void close(InputStream is) {
    try {
      if (is != null) {
        is.close();
      }
    } catch (IOException e) {
    }
  }

  public static String getResourceFromClasspathScrubbedForHTML(Tag tag) {
    String partialPath = tag.text();
    try {
      String contents;
      contents = getFileFromClassPath(partialPath);
      contents = scrubForHtml(contents);
      return contents;
    } catch (IOException e) {
      System.err.println(tag.position().toString()
          + ": unable to include resource " + partialPath + " for tag " + tag);
      System.exit(1);
      return null;
    }
  }

  /**
   * Copied from
   * {@link com.google.gwt.util.tools.Utility#getFileFromClassPath(String)}.
   */
  private static String getFileFromClassPath(String partialPath)
      throws IOException {
    InputStream in = ResourceIncluder.class.getClassLoader().getResourceAsStream(
        partialPath);
    try {
      if (in == null) {
        throw new FileNotFoundException(partialPath);
      }
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      int ch;
      while ((ch = in.read()) != -1) {
        os.write(ch);
      }
      return new String(os.toByteArray(), "UTF-8");
    } finally {
      close(in);
    }
  }

  private static String scrubForHtml(String contents) {
    char[] chars = contents.toCharArray();
    int len = chars.length;
    StringBuffer sb = new StringBuffer(len);
    for (int i = 0; i < len; ++i) {
      char c = chars[i];
      switch (c) {
        case '\r':
          // collapse \r\n into \n
          if (i == len - 1 || chars[i + 1] != '\n') {
            sb.append('\n');
          }
          break;
        case '&':
          sb.append("&amp;");
          break;
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        default:
          sb.append(c);
          break;
      }
    }
    return sb.toString();
  }
}
