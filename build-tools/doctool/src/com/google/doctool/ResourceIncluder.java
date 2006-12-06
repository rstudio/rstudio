package com.google.doctool;

import com.sun.javadoc.Tag;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ResourceIncluder {

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
   * Copied from {@link com.google.gwt.util.tools.Utility#close(InputStream)}.
   */
  public static void close(InputStream is) {
    try {
      if (is != null)
        is.close();
    } catch (IOException e) {
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
