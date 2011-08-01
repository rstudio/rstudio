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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.uibinder.attributeparsers.AttributeParsers;
import com.google.gwt.uibinder.rebind.DesignTimeUtilsStub;
import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.MockMortalLogger;
import com.google.gwt.uibinder.rebind.W3cDomHelper;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.XMLElementProvider;
import com.google.gwt.uibinder.rebind.XMLElementProviderImpl;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.test.UiJavaResources;

import junit.framework.Assert;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;

/**
 * Utility for testing {@link ElementParser} implementations in isolation. For a
 * new test you'll probably need to extend the mock class hierarchy in
 * {@link UiJavaResources}.
 */
class ElementParserTester {
  static final MockJavaResource BINDER_OWNER_JAVA = new MockJavaResource(
      "my.Ui") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("import com.google.gwt.user.client.ui.Widget;\n");
      code.append("public class Ui {\n");
      code.append("  public interface BaseClass extends "
          + "com.google.gwt.uibinder.client.UiBinder<Widget, BaseClass> {}\n");
      code.append("}\n");
      return code;
    }
  };

  static final String FIELD_NAME = "fieldName";
  static final String BINDER_URI = "binderUri";

  private static TreeLogger createLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  final JClassType parsedType;
  final MockMortalLogger logger = new MockMortalLogger();

  final W3cDomHelper docHelper = new W3cDomHelper(createLogger(),
      new MockResourceOracle());
  final TypeOracle types;
  final XMLElementProvider elemProvider;
  final MockUiBinderWriter writer;
  final FieldManager fieldManager;

  final ElementParser parser;

  ElementParserTester(String parsedTypeName, ElementParser parser,
      Resource... moreJava) throws UnableToCompleteException {
    this.parser = parser;
    String templatePath = "TemplatePath.ui.xml";
    String implName = "ImplClass";
    Set<Resource> uiResources = getUiResources();
    uiResources.addAll(Arrays.asList(moreJava));
    CompilationState state = CompilationStateBuilder.buildFrom(createLogger(),
        uiResources);
    types = state.getTypeOracle();

    elemProvider = new XMLElementProviderImpl(new AttributeParsers(types, null,
        logger), types, logger, DesignTimeUtilsStub.EMPTY);

    fieldManager = new FieldManager(types, logger, false);
    JClassType baseType = types.findType("my.Ui.BaseClass");
    MessagesWriter messages = new MessagesWriter(types, BINDER_URI, logger,
        templatePath, baseType.getPackage().getName(), implName);

    writer = new MockUiBinderWriter(baseType, implName, templatePath, types,
        logger, fieldManager, messages, BINDER_URI);
    fieldManager.registerField(types.findType(parsedTypeName), FIELD_NAME);
    parsedType = types.findType(parsedTypeName);
  }

  public XMLElement getElem(String string, String tag) throws SAXParseException {
    Document doc = docHelper.documentFor(string, null);
    Element w3cElem = (Element) doc.getDocumentElement().getElementsByTagName(
        tag).item(0);
    Assert.assertNotNull(
        String.format("Expected to find <%s> element in test DOM", tag),
        w3cElem);
    XMLElement elem = elemProvider.get(w3cElem);
    return elem;
  }

  public FieldWriter parse(String xml) throws UnableToCompleteException,
      SAXParseException {

    StringBuffer b = wrapXML(xml);

    // CHECKSTYLE_OFF
    String tag = "g:" + parsedType.getName();
    // CHECKSTYLE_ON
    parser.parse(getElem(b.toString(), tag), FIELD_NAME, parsedType, writer);
    return fieldManager.lookup(FIELD_NAME);
  }

  private Set<Resource> getUiResources() {
    Set<Resource> rtn = UiJavaResources.getUiResources();
    rtn.add(BINDER_OWNER_JAVA);
    return rtn;
  }
  
  public StringBuffer wrapXML(String xml) {
    StringBuffer b = new StringBuffer();
    b.append("<ui:UiBinder xmlns:ui='" + BINDER_URI + "'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append(xml);
    b.append("</ui:UiBinder>");
    return b;
  }
}
