/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Generates a web.xml file matching the servlets declared in GWT modules.
 */
class ServletWriter {

  static void generateMapping(FileWriter xmlWriter, String servletClass,
      String servletPath) throws IOException {
    String servletName = servletClass.replace('.', '_');
    xmlWriter.write('\n');
    xmlWriter.write(generateServletTag(servletName, servletClass));
    xmlWriter.write('\n');
    xmlWriter.write(generateServletMappingTag(servletName, servletPath));
    xmlWriter.write('\n');
  }

  static String generateServletMappingTag(String servletName, String servletPath) {
    StringBuilder sb = new StringBuilder();
    sb.append("<servlet-mapping>\n");
    sb.append("  <servlet-name>" + servletName + "</servlet-name>\n");
    sb.append("  <url-pattern>" + servletPath + "</url-pattern>\n");
    sb.append("</servlet-mapping>");
    return sb.toString();
  }

  static String generateServletTag(String servletName, String servletClass) {
    StringBuilder sb = new StringBuilder();
    sb.append("<servlet>\n");
    sb.append("  <servlet-name>" + servletName + "</servlet-name>\n");
    sb.append("  <servlet-class>" + servletClass + "</servlet-class>\n");
    sb.append("</servlet>");
    return sb.toString();
  }

  Map<String, String> mappings = new LinkedHashMap<String, String>();

  public void addMapping(String servletClass, String servletPath) {
    mappings.put(servletClass, servletPath);
  }

  public void realize(File webXml) throws IOException {
    if (mappings.size() == 0) {
      // Only generate a file if necessary.
      return;
    }
    webXml.getParentFile().mkdirs();
    FileWriter xmlWriter = new FileWriter(webXml);
    xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xmlWriter.write("<web-app>\n");

    for (Entry<String, String> entry : mappings.entrySet()) {
      String servletClass = entry.getKey();
      String servletPath = entry.getValue();
      String servletName = servletClass.replace('.', '_');
      xmlWriter.write('\n');
      xmlWriter.write(generateServletTag(servletName, servletClass));
      xmlWriter.write('\n');
      xmlWriter.write(generateServletMappingTag(servletName, servletPath));
      xmlWriter.write('\n');
    }

    xmlWriter.write("\n</web-app>\n");
    Utility.close(xmlWriter);
  }
}
