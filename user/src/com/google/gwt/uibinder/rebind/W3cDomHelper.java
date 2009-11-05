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
package com.google.gwt.uibinder.rebind;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Simplifies instantiation of the w3c XML parser, in just the style
 * that UiBinder likes it. Used by both prod and test.
 */
public class W3cDomHelper {
  private final DocumentBuilderFactory factory;
  private final DocumentBuilder builder;

  public W3cDomHelper() {
    factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setExpandEntityReferences(true);
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    builder.setEntityResolver(new GwtResourceEntityResolver());
  }

  /**
   * Creates an XML document model with the given contents. Nice for testing.
   * 
   * @param string the document contents
   */
  public Document documentFor(String string) throws SAXException,
      IOException {
    return builder.parse(new ByteArrayInputStream(string.getBytes()));
  }

  public Document documentFor(URL url) throws SAXParseException {
    try {
      InputStream stream = url.openStream();
      InputSource input = new InputSource(stream);
      input.setSystemId(url.toExternalForm());

      return builder.parse(input);
    } catch (SAXParseException e) {
      // Let SAXParseExceptions through.
      throw e;
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
