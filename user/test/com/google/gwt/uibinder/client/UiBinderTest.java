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
package com.google.gwt.uibinder.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.uibinder.sample.client.ClickyLink;
import com.google.gwt.uibinder.sample.client.DomBasedUi;
import com.google.gwt.uibinder.sample.client.FakeBundle;
import com.google.gwt.uibinder.sample.client.Foo;
import com.google.gwt.uibinder.sample.client.WidgetBasedUi;
import com.google.gwt.uibinder.sample.client.WidgetBasedUiResources;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Functional test of UiBinder.
 */
public class UiBinderTest extends GWTTestCase {
  private WidgetBasedUi widgetUi;
  private DomBasedUi domUi;
  private DockPanel root;

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.UiBinderTestModule";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    RootPanel.get().clear();
    domUi = new DomBasedUi("Cherished User");
    Document.get().getBody().appendChild(domUi.getRoot());

    widgetUi = new WidgetBasedUi();
    root = widgetUi.getRoot();
    RootPanel.get().add(widgetUi);
  }

  @Override
  public void gwtTearDown() throws Exception {
    RootPanel.get().clear();
    super.gwtTearDown();
  }

  public void testAccessToNonStandardElement() {
    Element elm = widgetUi.getNonStandardElement();
    assertEquals("I", elm.getTagName());
  }

  // TODO(rjrjr) The direction stuff in these tests really belongs in
  // DockPanelParserTest

  public void testAllowIdOnDomElements() {
    Element elm = DOM.getElementById("shouldSayHTML");
    assertEquals("HTML", elm.getInnerHTML());
  }

  public void testBundle() {
    assertEquals(getCenter(), widgetUi.getBundledLabel().getParent());
    assertEquals(new FakeBundle().helloText(),
        widgetUi.getBundledLabel().getText());
    WidgetBasedUiResources resources = GWT.create(WidgetBasedUiResources.class);
    assertEquals("bundledLabel should have styleName",
        resources.style().prettyText(),
        widgetUi.getBundledLabel().getStyleName());

    Element pretty = DOM.getElementById("prettyPara");
    assertEquals(resources.style().prettyText(), pretty.getClassName());

    Foo f = new Foo();
    assertTrue("Expect " + f,
        widgetUi.getTheFoo().getText().contains(f.toString()));
  }

  public void testCenter() {
    // TODO(rjrjr) More of a test of HTMLPanelParser

    Widget center = getCenter();
    assertEquals(DockPanel.CENTER, root.getWidgetDirection(center));
    assertEquals(HTMLPanel.class, center.getClass());
    String html = center.getElement().getInnerHTML();
    assertTrue(html.contains("main area"));
    assertTrue(html.contains("Button with"));
    assertTrue(html.contains("Of course"));

    assertEquals(center, widgetUi.getMyButton().getParent());
  }

  public void testComputedAttributeInPlaceholderedElement() {
    WidgetBasedUiResources resources = GWT.create(WidgetBasedUiResources.class);
    assertEquals(resources.style().prettyText(),
        widgetUi.getSpanInMsg().getClassName());
  }

  public void testComputedStyleInAPlaceholder() {
    WidgetBasedUiResources resources = GWT.create(WidgetBasedUiResources.class);
    assertEquals(resources.style().tmText(),
        widgetUi.getTmElement().getClassName());
  }

  public void testDomAccessAndComputedAttributeOnPlaceholderedElement() {
    WidgetBasedUiResources resources = GWT.create(WidgetBasedUiResources.class);
    Element elem = DOM.getElementById("placeholdersSpan");
    assertEquals("bold", elem.getStyle().getProperty("fontWeight"));
    assertEquals(resources.style().prettyText(), elem.getClassName());
  }

  public void testDomAccessInHtml() {
    DivElement sideBar = widgetUi.getSideBar();
    assertTrue("sideBar should start: \"This could\"",
        sideBar.getInnerText().startsWith("This could"));
    assertTrue("sideBar should end: \"example:\"",
        sideBar.getInnerText().endsWith("like that..."));
    assertEquals("Should have no id", "", sideBar.getAttribute("id"));
  }

  public void testDomAccessInHtmlPanel() {
    SpanElement messageInMain = widgetUi.getMessageInMain();
    String text = messageInMain.getInnerText().trim();
    assertTrue("sideBar should start: \"This is the main area\"",
        text.startsWith("This is the main area"));
    assertTrue("sideBar should end: \"example.\"", text.endsWith("example."));
  }

  public void testDomAttributeMessageWithFunnyChars() {
    ParagraphElement p =
      widgetUi.getFunnyCharsMessageDomAttributeParagraph();
    String t = p.getAttribute("title");
    assertEquals("funny characters \" ' ' & < > > { }", t);
  }

  public void testDomAttributeNoMessageWithFunnyChars() {
    ParagraphElement p = widgetUi.getFunnyCharsDomAttributeParagraph();
    String t = p.getAttribute("title");
    assertEquals("funny characters \" ' ' & < > > { }", t);
  }

  public void testDomTextMessageWithFunnyChars() {
    String t = widgetUi.getFunnyCharsMessageParagraph().getInnerText();
    assertEquals("They might show up in body text that has been marked for "
        + "translation: funny characters \" \" ' ' & < > > { }",
        t);
  }

  public void suppressedForSafari3Fail_testDomTextNoMessageWithFunnyChars() {
    ParagraphElement p = widgetUi.getFunnyCharsParagraph();
    // WebKit does \n replace thing, so let's do it everywhere
    String t = p.getInnerHTML().replace("\n", " ").toLowerCase(); 
    String expected = "Templates can be marked up for <b>localization</b>, which presents alls "
        + "kinds of exciting opportunities for bugs related to character escaping. "
        + "Consider these funny characters \" \" ' ' &amp; &lt; &gt; &gt; { }, and "
        + "the various places they might make your life miserable, like this "
        + "untranslated paragraph.";
    expected = expected.toLowerCase();
    assertEquals(expected, t);
  }

  public void testFieldAttribute() {
    assertEquals(getCenter(), widgetUi.getGwtFieldLabel().getParent());
  }

  public void testFieldInPlaceholderedElement() {
    assertEquals("named portions", widgetUi.getSpanInMsg().getInnerText());
  }

  public void testMenuAttributes() {
    WidgetBasedUiResources resources = GWT.create(WidgetBasedUiResources.class);
    assertEquals(widgetUi.getDropdownMenuBar().getStyleName(),
        resources.style().menuBar());
  }

  public void testMenuItems() {
    // Has a legacy MenuItemHTML in its midst
    assertEquals("The pig's in a hurry",
        widgetUi.getMenuItemLegacy().getElement().getInnerText());
    assertTrue("Style should include \"moppy\"",
        widgetUi.getMenuItemMop().getStyleName().contains("moppy"));
  }

  public void testMessageTrimming() {
    assertEquals("Title area, specified largely in HTML.",
        widgetUi.getTrimmedMessage().getInnerHTML());
    assertEquals("Tommy can you hear me? Can you field me near you?",
        widgetUi.getGwtFieldLabel().getText());
  }
  
  public void testMinimalDom() {
    assertEquals("Expect no wrapper div around root", widgetUi.getElement(),
        root.getElement());
  }

  public void testNamedElementInAPlaceholder() {
    assertEquals("TM", widgetUi.getTmElement().getInnerText());
  }

  public void testNestedBundle() {
    DomBasedUi.Resources resources =
      GWT.create(DomBasedUi.Resources.class);
    assertEquals(resources.style().bodyColor()
        + " " + resources.style().bodyFont() ,
        domUi.getRoot().getClassName());
  }

  public void suppressedForIEfail_testNonXmlEntities() {
    // This fragment includes both translated and non-translated strings
    ParagraphElement mainParagraph = widgetUi.getMain();
    final String innerHTML = mainParagraph.getInnerHTML().trim();
    assertTrue(innerHTML.contains(" \u261E \u2022 XHTML \u2022 \u261C"));
    assertTrue(innerHTML.startsWith("\u261E&nbsp;<span>"));
    assertTrue(innerHTML.endsWith("</span>&nbsp;\u261C"));
  }

  public void testNorth() {
    Widget north = root.getWidget(0);
    assertEquals(DockPanel.NORTH, root.getWidgetDirection(north));
    assertEquals(HTML.class, north.getClass());
    assertTrue(((HTML) north).getHTML().contains("Title area"));
  }

  @DoNotRunWith(Platform.Htmlunit)
  public void testRadioButton() {
    RadioButton able = widgetUi.getMyRadioAble();
    RadioButton baker = widgetUi.getMyRadioBaker();
    assertTrue("able should be checked", able.getValue());
    assertFalse("baker should not be checked", baker.getValue());
    assertEquals("radios", able.getName());
    assertEquals("radios", baker.getName());
  }

  public void testStackPanel() {
    StackPanel p = widgetUi.getMyStackPanel();
    assertNotNull("Panel exists", p);
    Widget w = widgetUi.getMyStackPanelItem();
    assertNotNull("Widget exists", w);
    boolean containsWidget = false;
    for (int i = 0; i < p.getWidgetCount(); i++) {
      if (p.getWidget(i) == w) {
        containsWidget = true;
      }
    }
    assertTrue("Panel contains widget", containsWidget);
  }

  public void testDisclosurePanel() {
    DisclosurePanel p = widgetUi.getMyDisclosurePanel();
    assertNotNull("Panel exists", p);
    Widget w = widgetUi.getMyDisclosurePanelItem();
    assertNotNull("Widget exists", w);
    assertEquals("Panel contains widget", w, p.getContent());
  }

  public void testStringAttributeIgnoresStaticSetter() {
    // Assumes setPopupText() is overloaded such that there is a static
    // setPopupText(Foo, String) method.
    ClickyLink clicky = widgetUi.getCustomLinkWidget();
    assertEquals("overloaded setter should have been called",
                 "That tickles!", clicky.getPopupText());
  }

  public void testStringAttributeWithFormatChars() {
    assertEquals("100%", root.getElement().getStyle().getProperty("width"));
  }

  public void testWest() {
    Widget west = root.getWidget(1);
    assertEquals(DockPanel.WEST, root.getWidgetDirection(west));
    assertEquals(HTML.class, west.getClass());
    String html = ((HTML) west).getHTML();
    assertTrue(html.contains("side bar"));
  }

  public void testWidgetAttributeMessageWithFunnyChars() {
    ClickyLink clicky = widgetUi.getFunnyCharsMessageAttributeWidget();
    String t = clicky.getPopupText();
    assertEquals("funny characters \" ' ' & < > > { }", t);
  }

  public void testWidgetAttributeNoMessageWithFunnyChars() {
    ClickyLink clicky = widgetUi.getFunnyCharsAttributeWidget();
    String t = clicky.getPopupText();
    assertEquals("funny characters \" ' ' & < > > { }", t);
  }
  
  public void suppressForIEfail_testBizarrelyElementedWidgets() {
    assertInOrder(widgetUi.getWidgetCrazyTable().getInnerHTML().toLowerCase(),
        "<td>they have been known</td>", "<td>to write widgets</td>",
        "<td>that masquerade</td>", "<td>as table cells,</td>",
        "<td>just like these.</td>", "<td>burma shave</td>");

    assertInOrder(widgetUi.getWidgetCrazyOrderedList().getInnerHTML(),
        "<li>similar</li>", "<li>things</li>");

    assertInOrder(widgetUi.getWidgetCrazyDefinitionList().getInnerHTML(),
        "<dt>Being</dt>", "<dd>done</dd>", "<dd>with</dd>", "<dd>lists</dd>");
  }

  public void testCustomHtmlPanelTag() {
    assertInOrder(widgetUi.getCustomTagHtmlPanel().getElement().getInnerHTML(),
        "<td>Even HTMLPanel gets in on the game</td>",
        "<td>Lately, anyway.</td>");
  }

  /**
   * Assert that the expect strings are found in body, and in the order given.
   * WARNING: both body and expected are normalized to lower case, to get around
   * IE's habit of returning capitalized DOM elements.
   */
  private void assertInOrder(String body, String... expected) {
    body = body.toLowerCase();
    int lastIndex = 0;
    String lastExpected = "";
    
    for (String next : expected) {
      next = next.toLowerCase();
      int index = body.indexOf(next);
      assertTrue(body + " should contain " + next, index > -1);
      assertTrue("Expect " + next + " after " + lastExpected, index > lastIndex);
      lastIndex = index;
    }
  }

  private Widget getCenter() {
    Widget center = root.getWidget(2);
    return center;
  }
}
