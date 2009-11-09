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
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.javac.impl.MockResourceOracle;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.uibinder.attributeparsers.AttributeParsers;
import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.MockMortalLogger;
import com.google.gwt.uibinder.rebind.W3cDomHelper;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.XMLElementProvider;
import com.google.gwt.uibinder.rebind.XMLElementProviderImpl;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

class ElementParserTester {
   static final MockJavaResource BINDER_OWNER_JAVA = new MockJavaResource(
      "my.Ui") {
    @Override
    protected CharSequence getContent() {
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

  final JClassType parsedType;
  final MockMortalLogger logger = new MockMortalLogger();
  final W3cDomHelper docHelper = new W3cDomHelper(TreeLogger.NULL);

  final TypeOracle types;
  final XMLElementProvider elemProvider;
  final MockUiBinderWriter writer;
  final FieldManager fieldManager;
  final ElementParser parser;

  ElementParserTester(String parsedTypeName, ElementParser parser)
      throws UnableToCompleteException {
    this.parser = parser;
    String templatePath = "TemplatePath.ui.xml";
    String implName = "ImplClass";
    MockResourceOracle resources = new MockResourceOracle(getUiResources());
    CompilationState state = new CompilationState(TreeLogger.NULL, resources);
    types = state.getTypeOracle();

    elemProvider = new XMLElementProviderImpl(new AttributeParsers(), null,
        types, logger);

    fieldManager = new FieldManager(logger);
    JClassType baseType = types.findType("my.Ui.BaseClass");
    MessagesWriter messages = new MessagesWriter(BINDER_URI, logger,
        templatePath, baseType.getPackage().getName(), implName);

    writer = new MockUiBinderWriter(baseType, implName, templatePath, types,
        logger, fieldManager, messages);
    fieldManager.registerField(types.findType(parsedTypeName), FIELD_NAME);
    parsedType = types.findType(parsedTypeName);
  }

  public FieldWriter parse(String xml) throws UnableToCompleteException,
      SAXException, IOException {

    StringBuffer b = new StringBuffer();
    b.append("<ui:UiBinder xmlns:ui='" + BINDER_URI + "'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append(xml);
    b.append("</ui:UiBinder>");

    parser.parse(getElem(b.toString()), FIELD_NAME, parsedType, writer);
    return fieldManager.lookup(FIELD_NAME);
  }

  private XMLElement getElem(String string) throws SAXException, IOException {
    Document doc = docHelper.documentFor(string);
    Element w3cElem = (Element) doc.getDocumentElement().getElementsByTagName(
        "g:" + parsedType.getName()).item(0);
    XMLElement elem = elemProvider.get(w3cElem);
    return elem;
  }

  private MockJavaResource[] getUiResources() {
    List<MockJavaResource> rtn = Lists.create(UiJavaResources.getUiResources());
    rtn.add(BINDER_OWNER_JAVA);
    return rtn.toArray(new MockJavaResource[rtn.size()]);
  }
}