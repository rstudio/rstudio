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
package com.google.gwt.uibinder.parsers;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.javac.impl.MockResourceOracle;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.uibinder.rebind.AttributeParsers;
import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.W3cDomHelper;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.XMLElementProvider;
import com.google.gwt.uibinder.rebind.XMLElementProviderImpl;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.test.UiJavaResources;
import com.google.gwt.user.client.ui.DockLayoutPanel;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A unit test. Guess what of. 
 */
public class DockLayoutPanelParserTest extends TestCase {
  private static class MyUiBinderWriter extends UiBinderWriter {
    final List<String> statements = new ArrayList<String>();
    String died;

    public MyUiBinderWriter(JClassType baseClass, String implClassName,
        String templatePath, TypeOracle oracle, MortalLogger logger,
        FieldManager fieldManager, MessagesWriter messagesWriter)
        throws UnableToCompleteException {
      super(baseClass, implClassName, templatePath, oracle, logger,
          fieldManager, messagesWriter);
    }

    @Override
    public void addStatement(String format, Object... args) {
      statements.add(String.format(format, args));
    }

    @Override
    public String parseElementToField(XMLElement elem)
        throws UnableToCompleteException {
      return elem.consumeOpeningTag();
    }

    @Override
    public void die(String message, Object... params)
        throws UnableToCompleteException {
      noteDeath(String.format(message, params));
      super.die(message, params);
    }

    @Override
    public void die(String message) throws UnableToCompleteException {
      noteDeath(message);
      super.die(message);
    }
    
    /** Handy place to set a break point and inspect suicide notes. */
    void noteDeath(String s) {
      died = s;
    }
  }

  private static final W3cDomHelper docHelper = new W3cDomHelper();
  private static final String BINDER_URI = "binderUri";

  private static final String fieldName = "fieldName";

  // TODO(rjrjr) Move this to JavaResourceBase. Have to do it atomically for
  // all other tests that define their own Enum.
  private static final MockJavaResource ENUM = new MockJavaResource(
  "java.lang.Enum") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package java.lang;\n");
      code.append("public abstract class Enum<E extends Enum<E>> {\n");
      code.append("  protected Enum(String name, int ordinal) {}\n");
      code.append("}\n");
      return code;
    }
  };
  private static final MockJavaResource MY_UI_JAVA = new MockJavaResource(
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

  private TypeOracle types;
  private XMLElementProvider elemProvider;
  private Document doc;
  private MyUiBinderWriter writer;
  private DockLayoutPanelParser parser;
  private JClassType dockLayoutPanelType;
  private FieldManager fieldManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    MockResourceOracle resources = new MockResourceOracle(getUiResources());
    CompilationState state = new CompilationState(TreeLogger.NULL, resources);
    types = state.getTypeOracle();
    dockLayoutPanelType = types.findType("com.google.gwt.user.client.ui.DockLayoutPanel");

    elemProvider = new XMLElementProviderImpl(new AttributeParsers(), null,
        types, MortalLogger.NULL);

    fieldManager = new FieldManager(MortalLogger.NULL);
    fieldManager.registerField(
        types.findType(DockLayoutPanel.class.getCanonicalName()), fieldName);

    String templatePath = "TemplatePath.ui.xml";
    String implName = "ImplClass";
    JClassType baseType = types.findType("my.Ui.BaseClass");
    MessagesWriter messages = new MessagesWriter(BINDER_URI, MortalLogger.NULL,
        templatePath, baseType.getPackage().getName(), implName);

    writer = new MyUiBinderWriter(baseType, implName, templatePath, types,
        MortalLogger.NULL, fieldManager, messages);
    parser = new DockLayoutPanelParser();
  }

  public void testHappy() throws UnableToCompleteException, SAXException,
      IOException {
    StringBuffer b = new StringBuffer();
    b.append("<ui:UiBinder xmlns:ui='" + BINDER_URI + "'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <g:DockLayoutPanel unit='EM'>");
    b.append("    <g:north size='5'>");
    b.append("      <g:Label id='north'>north</g:Label>");
    b.append("    </g:north>");
    b.append("    <g:center>");
    b.append("      <g:Label id='center'>center</g:Label>");
    b.append("    </g:center>");
    b.append("  </g:DockLayoutPanel>");
    b.append("</ui:UiBinder>");

    String[] expected = {
        "fieldName.addNorth(<g:Label id='north'>, 5);",
        "fieldName.add(<g:Label id='center'>);",};

    parser.parse(getElem(b.toString()), fieldName, dockLayoutPanelType, writer);
    FieldWriter w = fieldManager.lookup(fieldName);
    assertEquals(
        "new com.google.gwt.user.client.ui.DockLayoutPanel(com.google.gwt.dom.client.Style.Unit.EM)",
        w.getInitializer());

    Iterator<String> i = writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(writer.died);
  }

  public void testNiceCenter() throws UnableToCompleteException, SAXException,
      IOException {
    StringBuffer b = new StringBuffer();
    b.append("<ui:UiBinder xmlns:ui='" + BINDER_URI + "'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <g:DockLayoutPanel unit='EM'>");
    b.append("    <g:center>");
    b.append("      <g:Label id='center'>center</g:Label>");
    b.append("    </g:center>");
    b.append("    <g:north size='5'>");
    b.append("      <g:Label id='north'>north</g:Label>");
    b.append("    </g:north>");
    b.append("  </g:DockLayoutPanel>");
    b.append("</ui:UiBinder>");

    String[] expected = {
        "fieldName.addNorth(<g:Label id='north'>, 5);",
        "fieldName.add(<g:Label id='center'>);",};

    parser.parse(getElem(b.toString()), fieldName, dockLayoutPanelType, writer);
    FieldWriter w = fieldManager.lookup(fieldName);
    assertEquals(
        "new com.google.gwt.user.client.ui.DockLayoutPanel(com.google.gwt.dom.client.Style.Unit.EM)",
        w.getInitializer());

    Iterator<String> i = writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
  }

  public void testTooManyCenters() throws SAXException, IOException {
    StringBuffer b = new StringBuffer();
    b.append("<ui:UiBinder xmlns:ui='" + BINDER_URI + "'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <g:DockLayoutPanel unit='EM'>");
    b.append("    <g:center>");
    b.append("      <g:Label id='center'>center</g:Label>");
    b.append("    </g:center>");
    b.append("    <g:center>");
    b.append("      <g:Label id='centerAlso'>centaur</g:Label>");
    b.append("    </g:center>");
    b.append("  </g:DockLayoutPanel>");
    b.append("</ui:UiBinder>");

    try {
      parser.parse(getElem(b.toString()), fieldName, dockLayoutPanelType,
          writer);
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(writer.died);
    }
  }

  public void testBadChild() throws SAXException, IOException {
    StringBuffer b = new StringBuffer();
    b.append("<ui:UiBinder xmlns:ui='" + BINDER_URI + "'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <g:DockLayoutPanel unit='EM'>");
    b.append("    <g:west><foo/></g:west>");
    b.append("  </g:DockLayoutPanel>");
    b.append("</ui:UiBinder>");

    try {
      parser.parse(getElem(b.toString()), fieldName, dockLayoutPanelType,
          writer);
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(writer.died);
    }
  }

  MockJavaResource[] getUiResources() {
    List<MockJavaResource> rtn = Lists.create(UiJavaResources.getUiResources());
    rtn.add(MY_UI_JAVA);
    rtn.add(ENUM);
    return rtn.toArray(new MockJavaResource[rtn.size()]);
  }

  private XMLElement getElem(String string) throws SAXException, IOException {
    doc = docHelper.documentFor(string);
    Element w3cElem = (Element) doc.getDocumentElement().getElementsByTagName(
        "g:DockLayoutPanel").item(0);
    XMLElement elem = elemProvider.get(w3cElem);
    return elem;
  }
}
