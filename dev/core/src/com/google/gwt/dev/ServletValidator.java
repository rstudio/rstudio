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
import com.google.gwt.dev.util.collect.HashSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Validates that <code>&lt;servlet&gt;</code> tags in a GWT module match ones
 * from a <code>WEB-INF/web.xml</code>.
 */
class ServletValidator {

  /**
   * Collect servlet information from a web xml.
   */
  private static final class WebXmlDataCollector extends DefaultHandler {
    /*
     * TODO(scottb): this should have been implemented as a Schema.
     */

    private static class ElementStack {
      private final Stack<String> stack = new Stack<String>();

      public boolean exactly(String elementName, int depth) {
        return (depth == stack.size() - 1)
            && elementName.equals(stack.get(depth));
      }

      public String peek() {
        return stack.peek();
      }

      public String pop() {
        return stack.pop();
      }

      public void push(String elementName) {
        stack.push(elementName);
      }

      public boolean within(String elementName, int depth) {
        return (depth < stack.size()) && elementName.equals(stack.get(depth));
      }
    }

    private Set<String> accumulateClasses = new HashSet<String>();
    private Set<String> accumulatePaths = new HashSet<String>();
    private final TreeLogger branch;
    private final Stack<StringBuilder> cdataStack = new Stack<StringBuilder>();
    private final Map<String, String> classNameToServletName;
    private final ElementStack context = new ElementStack();
    private String currentServletName;
    private String indent = "";
    private final Map<String, Set<String>> servletNameToPaths;

    private WebXmlDataCollector(TreeLogger branch,
        Map<String, String> classNameToServletName,
        Map<String, Set<String>> servletNameToPaths) {
      this.branch = branch;
      this.classNameToServletName = classNameToServletName;
      this.servletNameToPaths = servletNameToPaths;
    }

    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {
      cdataStack.peek().append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
      String cdata = cdataStack.pop().toString().trim();
      if (context.within("web-app", 0)) {
        if (context.within("servlet", 1)) {
          if (context.exactly("servlet-name", 2)) {
            currentServletName = cdata;
          } else if (context.exactly("servlet-class", 2)) {
            accumulateClasses.add(cdata);
          } else if (context.exactly("servlet", 1)) {
            if (currentServletName != null) {
              for (String className : accumulateClasses) {
                classNameToServletName.put(className, currentServletName);
              }
              currentServletName = null;
            }
            accumulateClasses.clear();
          }
        } else if (context.within("servlet-mapping", 1)) {
          if (context.exactly("servlet-name", 2)) {
            currentServletName = cdata;
          } else if (context.exactly("url-pattern", 2)) {
            accumulatePaths.add(cdata);
          } else if (context.exactly("servlet-mapping", 1)) {
            if (currentServletName != null) {
              Set<String> servletPaths = servletNameToPaths.get(currentServletName);
              if (servletPaths == null) {
                servletPaths = new HashSet<String>();
                servletNameToPaths.put(currentServletName, servletPaths);
              }
              servletPaths.addAll(accumulatePaths);
            }
            currentServletName = null;
            accumulatePaths.clear();
          }
        }
      }

      assert qName.equals(context.peek());
      context.pop();

      if (cdata.length() > 0) {
        if (branch.isLoggable(TreeLogger.DEBUG)) {
          branch.log(TreeLogger.DEBUG, "  characters: " + indent + cdata);
        }
      }
      indent = indent.substring(2);
      if (branch.isLoggable(TreeLogger.DEBUG)) {
        branch.log(TreeLogger.DEBUG, "  endElement: " + indent + qName);
      }
    }

    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) throws SAXException {
      context.push(qName);
      cdataStack.push(new StringBuilder());

      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < attributes.getLength(); ++i) {
        sb.append(' ');
        sb.append(attributes.getQName(i));
        sb.append("=\"");
        sb.append(attributes.getValue(i));
        sb.append('"');
      }
      if (branch.isLoggable(TreeLogger.DEBUG)) {
        branch.log(TreeLogger.DEBUG, "startElement: " + indent + qName
            + sb.toString());
      }
      indent += "  ";
    }
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
    try {
      final TreeLogger branch = logger.branch(TreeLogger.DEBUG, "Parsing "
          + webXmlUrlString);
      SAXParserFactory fac = SAXParserFactory.newInstance();
      fac.setValidating(false);
      fac.setNamespaceAware(false);
      fac.setFeature(
          "http://apache.org/xml/features/nonvalidating/load-external-dtd",
          false);
      SAXParser parser = fac.newSAXParser();
      parser.getXMLReader().setFeature(
          "http://xml.org/sax/features/validation", false);
      parser.getXMLReader().setFeature(
          "http://xml.org/sax/features/namespaces", false);
      parser.getXMLReader().setFeature(
          "http://xml.org/sax/features/namespace-prefixes", false);

      Map<String, String> classNameToServletName = new HashMap<String, String>();
      Map<String, Set<String>> servletNameToPaths = new HashMap<String, Set<String>>();
      parser.parse(webXmlUrlString, new WebXmlDataCollector(branch,
          classNameToServletName, servletNameToPaths));

      Map<String, Set<String>> classNameToPaths = new HashMap<String, Set<String>>();
      for (Entry<String, String> entry : classNameToServletName.entrySet()) {
        classNameToPaths.put(entry.getKey(),
            servletNameToPaths.get(entry.getValue()));
      }
      return new ServletValidator(classNameToServletName, classNameToPaths);
    } catch (Exception e) {
      logger.log(TreeLogger.WARN, "Unable to process '" + webXmlUrlString
          + "' for servlet validation", e);
      return null;
    }
  }

  static String generateMissingMappingMessage(String servletClass,
      String servletPath, String servletName) {
    return "Module declares a servlet class '"
        + servletClass
        + "' with a mapping to '"
        + servletPath
        + "', but the web.xml has no corresponding mapping; please add the following lines to your web.xml:\n"
        + ServletWriter.generateServletMappingTag(servletName, servletPath);
  }

  static String generateMissingServletMessage(String servletClass,
      String servletPath) {
    String servletName = suggestServletName(servletClass);
    return "Module declares a servlet class '"
        + servletClass
        + "', but the web.xml has no corresponding declaration; please add the following lines to your web.xml:\n"
        + ServletWriter.generateServletTag(servletName, servletClass) + "\n"
        + ServletWriter.generateServletMappingTag(servletName, servletPath);
  }

  static String suggestServletName(String servletClass) {
    int pos = servletClass.lastIndexOf('.');
    String suggest = (pos < 0) ? servletClass : servletClass.substring(pos + 1);
    String firstChar = suggest.substring(0, 1).toLowerCase(Locale.ENGLISH);
    suggest = firstChar + suggest.substring(1);
    return suggest;
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
