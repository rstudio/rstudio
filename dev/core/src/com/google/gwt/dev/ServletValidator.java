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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.jetty.JettyNullLogger;

import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletMapping;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.webapp.WebXmlConfiguration;
import org.mortbay.log.Log;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates that <code>&lt;servlet&gt;</code> tags in a GWT module match ones
 * from a <code>WEB-INF/web.xml</code>.
 */
class ServletValidator {

  static {
    // Suppress spammy Jetty log initialization.
    System.setProperty("org.mortbay.log.class", JettyNullLogger.class.getName());
    Log.getLog();
  }

  public static ServletValidator create(TreeLogger logger, File webXml) {
    try {
      return create(logger, webXml.toURI().toURL());
    } catch (MalformedURLException e) {
      logger.log(TreeLogger.WARN, "Unable to process '"
          + webXml.getAbsolutePath() + "' for servlet validation", e);
      return null;
    }
  }

  public static ServletValidator create(TreeLogger logger, URL webXmlUrl) {
    String webXmlUrlString = webXmlUrl.toExternalForm();
    String oldProp = System.getProperty("org.mortbay.xml.XmlParser.Validating",
        "false");
    try {
      System.setProperty("org.mortbay.xml.XmlParser.Validating", "false");
      WebXmlConfiguration wxc = new WebXmlConfiguration();
      ServletHandler myServletHandler = new ServletHandler();
      wxc.setWebAppContext(new WebAppContext(null, null, myServletHandler, null));
      wxc.configure(webXmlUrlString);
      ServletMapping[] mappings = myServletHandler.getServletMappings();
      ServletHolder[] servlets = myServletHandler.getServlets();
      Map<String, String> servletNameToClassName = new HashMap<String, String>();
      Map<String, Set<String>> classNameToPaths = new HashMap<String, Set<String>>();
      Map<String, String> classNameToServletName = new HashMap<String, String>();
      for (ServletHolder servlet : servlets) {
        servletNameToClassName.put(servlet.getName(), servlet.getClassName());
        classNameToServletName.put(servlet.getClassName(), servlet.getName());
        classNameToPaths.put(servlet.getClassName(), new HashSet<String>());
      }
      for (ServletMapping mapping : mappings) {
        String servletName = mapping.getServletName();
        String className = servletNameToClassName.get(servletName);
        assert (className != null);
        Set<String> paths = classNameToPaths.get(className);
        assert (paths != null);
        paths.addAll(Arrays.asList(mapping.getPathSpecs()));
      }
      return new ServletValidator(classNameToServletName, classNameToPaths);
    } catch (Exception e) {
      logger.log(TreeLogger.WARN, "Unable to process '" + webXmlUrlString
          + "' for servlet validation", e);
      return null;
    } finally {
      System.setProperty("org.mortbay.xml.XmlParser.Validating", oldProp);
    }
  }

  static String generateMissingMappingMessage(String servletClass,
      String servletPath, String servletName) {
    return "Module declares a servlet class '"
        + servletClass
        + "' with a mapping to '"
        + servletPath
        + "', but the web.xml has no corresponding mapping; please add the following lines to your web.xml:\n"
        + ServletValidator.generateServletMappingTag(servletName, servletPath);
  }

  static String generateMissingServletMessage(String servletClass,
      String servletPath) {
    String servletName = suggestServletName(servletClass);
    return "Module declares a servlet class '"
        + servletClass
        + "', but the web.xml has no corresponding declaration; please add the following lines to your web.xml:\n"
        + ServletValidator.generateServletTag(servletName, servletClass) + "\n"
        + ServletValidator.generateServletMappingTag(servletName, servletPath);
  }

  static String suggestServletName(String servletClass) {
    int pos = servletClass.lastIndexOf('.');
    String suggest = (pos < 0) ? servletClass : servletClass.substring(pos + 1);
    String firstChar = suggest.substring(0, 1).toLowerCase(Locale.ENGLISH);
    suggest = firstChar + suggest.substring(1);
    return suggest;
  }

  private static String generateServletMappingTag(String servletName,
      String servletPath) {
    StringBuilder sb = new StringBuilder();
    sb.append("<servlet-mapping>\n");
    sb.append("  <servlet-name>" + servletName + "</servlet-name>\n");
    sb.append("  <url-pattern>" + servletPath + "</url-pattern>\n");
    sb.append("</servlet-mapping>");
    return sb.toString();
  }

  private static String generateServletTag(String servletName,
      String servletClass) {
    StringBuilder sb = new StringBuilder();
    sb.append("<servlet>\n");
    sb.append("  <servlet-name>" + servletName + "</servlet-name>\n");
    sb.append("  <servlet-class>" + servletClass + "</servlet-class>\n");
    sb.append("</servlet>");
    return sb.toString();
  }

  private final Map<String, Set<String>> classNameToPaths;

  private final Map<String, String> classNameToServletName;

  private ServletValidator(Map<String, String> classNameToServletName,
      Map<String, Set<String>> classNameToPaths) {
    this.classNameToServletName = classNameToServletName;
    this.classNameToPaths = classNameToPaths;
  }

  public void validate(TreeLogger logger, String servletClass,
      String servletPath) {
    if (containsServletClass(servletClass)) {
      if (!containsServletMapping(servletClass, servletPath)) {
        String servletName = getNameForClass(servletClass);
        assert (servletName != null);
        logger.log(TreeLogger.WARN, generateMissingMappingMessage(servletClass,
            servletPath, servletName));
      }
    } else {
      logger.log(TreeLogger.WARN, generateMissingServletMessage(servletClass,
          servletPath));
    }
  }

  boolean containsServletClass(String servletClass) {
    return classNameToServletName.get(servletClass) != null;
  }

  boolean containsServletMapping(String servletClass, String servletPath) {
    Set<String> paths = classNameToPaths.get(servletClass);
    return (paths != null) && paths.contains(servletPath);
  }

  String getNameForClass(String servletClass) {
    return classNameToServletName.get(servletClass);
  }
}
