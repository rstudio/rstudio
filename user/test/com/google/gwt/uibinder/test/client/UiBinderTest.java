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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.uibinder.test.client.EnumeratedLabel.Suffix;
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
    return "com.google.gwt.uibinder.test.UiBinderTestApp";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    RootPanel.get().clear();
    domUi = new DomBasedUi("Cherished User");
    Document.get().getBody().appendChild(domUi.root);

    widgetUi = new WidgetBasedUi();
    root = widgetUi.root;
    RootPanel.get().add(widgetUi);
  }

  @Override
  public void gwtTearDown() throws Exception {
    RootPanel.get().clear();
    super.gwtTearDown();
  }

  public void testAccessToNonStandardElement() {
    Element elm = widgetUi.nonStandardElement;
    assertEquals("I", elm.getTagName());
  }

  // TODO(rjrjr) The direction stuff in these tests really belongs in
  // DockPanelParserTest

  public void testAllowIdOnDomElements() {
    Element elm = DOM.getElementById("shouldSayHTML");
    assertEquals("HTML", elm.getInnerHTML());
  }

  public void testBraceEscaping() {
    assertEquals("blah di blah {foo: \"bar\"} di blah",
        widgetUi.bracedParagraph.getAttribute("fnord"));
  }

  public void testBundle() {
    assertEquals(getCenter(), widgetUi.bundledLabel.getParent());
    assertEquals(new FakeBundle().helloText(), widgetUi.bundledLabel.getText());
    WidgetBasedUiExternalResources resources = GWT.create(WidgetBasedUiExternalResources.class);
    assertEquals("bundledLabel should have styleName",
        resources.style().prettyText(), widgetUi.bundledLabel.getStyleName());

    Element pretty = DOM.getElementById("prettyPara");
    assertEquals(resources.style().prettyText(), pretty.getClassName());

    ArbitraryPojo pojo = new ArbitraryPojo();
    FooLabel foo = new FooLabel();
    foo.setPojo(pojo);
    assertEquals(foo.getText(), widgetUi.theFoo.getText());
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

    assertEquals(center, widgetUi.myButton.getParent());
  }

  public void testComputedAttributeInPlaceholderedElement() {
    WidgetBasedUiExternalResources resources = GWT.create(WidgetBasedUiExternalResources.class);
    assertEquals(resources.style().prettyText(),
        widgetUi.spanInMsg.getClassName());
  }

  public void testComputedStyleInAPlaceholder() {
    WidgetBasedUiExternalResources resources = GWT.create(WidgetBasedUiExternalResources.class);
    assertEquals(resources.style().tmText(), widgetUi.tmElement.getClassName());
  }

  public void testDomAccessAndComputedAttributeOnPlaceholderedElement() {
    WidgetBasedUiExternalResources resources = GWT.create(WidgetBasedUiExternalResources.class);
    Element elem = DOM.getElementById("placeholdersSpan");
    assertEquals("bold", elem.getStyle().getProperty("fontWeight"));
    assertEquals(resources.style().prettyText(), elem.getClassName());
  }

  public void testDomAccessInHtml() {
    DivElement sideBar = widgetUi.sideBar;
    assertTrue("sideBar should start: \"This could\"",
        sideBar.getInnerText().startsWith("This could"));
    assertTrue("sideBar should end: \"example:\"",
        sideBar.getInnerText().endsWith("like that..."));
    assertEquals("Should have no id", "", sideBar.getAttribute("id"));
  }

  public void testDomAccessInHtmlPanel() {
    SpanElement messageInMain = widgetUi.messageInMain;
    String text = messageInMain.getInnerText().trim();
    assertTrue("sideBar should start: \"This is the main area\"",
        text.startsWith("This is the main area"));
    assertTrue("sideBar should end: \"example.\"", text.endsWith("example."));
  }

  public void testDomAttributeMessageWithFunnyChars() {
    ParagraphElement p = widgetUi.funnyCharsMessageDomAttributeParagraph;
    String t = p.getAttribute("title");
    assertEquals("funny characters \" ' ' & < > > { }", t);
  }

  public void testDomAttributeNoMessageWithFunnyChars() {
    ParagraphElement p = widgetUi.funnyCharsDomAttributeParagraph;
    String t = p.getAttribute("title");
    assertEquals("funny characters \" ' ' & < > > { }", t);
  }

  public void testDomTextMessageWithFunnyChars() {
    String t = widgetUi.funnyCharsMessageParagraph.getInnerText();
    assertEquals("They might show up in body text that has been marked for "
        + "translation: funny characters \" \" ' ' & < > > { }", t);
  }

  public void testEnums() {
    Suffix expected = EnumeratedLabel.Suffix.tail;
    assertTrue("Should end with suffix \"" + expected + "\"",
        widgetUi.enumLabel.getText().endsWith(expected.toString()));
  }

  public void testProtectedDomTextMessageWithFunnyChars() {
    String t = widgetUi.funnyCharsProtectedMessageParagraph.getInnerText();
    assertEquals("Don't forget about protected untranslatable blocks: "
        + "funny characters \" \" ' ' & < > > { }", t);
  }

  public void testDomTextInNamedElementMessageWithFunnyChars() {
    String t = widgetUi.funnyCharsMessageChildSpan.getInnerText();
    assertEquals("funny characters \" \" ' ' & < > > { }", t);
  }

  public void suppressedForSafari3Fail_testDomTextNoMessageWithFunnyChars() {
    ParagraphElement p = widgetUi.funnyCharsParagraph;
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
    assertEquals(getCenter(), widgetUi.gwtFieldLabel.getParent());
  }

  public void testFieldInPlaceholderedElement() {
    assertEquals("named portions", widgetUi.spanInMsg.getInnerText());
  }

  public void testMenuAttributes() {
    assertEquals(widgetUi.dropdownMenuBar.getStyleName(),
        widgetUi.myStyle.menuBar());
  }

  public void testMenuItems() {
    // Has a legacy MenuItemHTML in its midst
    assertEquals("The pig's in a hurry",
        widgetUi.menuItemLegacy.getElement().getInnerText());
    assertTrue("Style should include \"moppy\"",
        widgetUi.menuItemMop.getStyleName().contains("moppy"));
  }

  public void testMessageTrimming() {
    assertEquals("Title area, specified largely in HTML.",
        widgetUi.trimmedMessage.getInnerHTML());
    assertEquals("Tommy can you hear me? Can you field me near you?",
        widgetUi.gwtFieldLabel.getText());
  }

  public void testMinimalDom() {
    assertEquals("Expect no wrapper div around root", widgetUi.getElement(),
        root.getElement());
  }

  public void testNamedElementInAPlaceholder() {
    assertEquals("TM", widgetUi.tmElement.getInnerText());
  }

  public void testNestedBundle() {
    DomBasedUi.Resources resources = GWT.create(DomBasedUi.Resources.class);
    assertEquals(resources.style().bodyColor() + " "
        + resources.style().bodyFont(), domUi.root.getClassName());
  }

  interface Bundle extends ClientBundle {
    @Source(value = {"WidgetBasedUi.css", "Menu.css"})
    @NotStrict
    WidgetBasedUi.Style style();
  }

  @DoNotRunWith(Platform.HtmlUnit)
  public void testNoOverrideInheritedSharedCssClasses() {
    Bundle bundle = GWT.create(Bundle.class);
    WidgetBasedUi ui = GWT.create(WidgetBasedUi.class);
    String publicStyle = bundle.style().menuBar();
    String privateStyle = ui.myStyle.menuBar();
    assertEquals(publicStyle, privateStyle);
  }

  public void suppressedForIEfail_testNonXmlEntities() {
    // This fragment includes both translated and non-translated strings
    ParagraphElement mainParagraph = widgetUi.main;
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

  public void testPrivateStyleFromExternalCss() {
    ParagraphElement p = widgetUi.privateStyleParagraph;
    assertTrue("Some kind of class should be set",
        p.getClassName().length() > 0);
  }

  public void testPrivateStylesFromInlineCss() {
    ParagraphElement p = widgetUi.reallyPrivateStyleParagraph;
    assertTrue("Some kind of class should be set",
        p.getClassName().length() > 0);
    assertFalse("Should be a different style than privateStyleParagraph's",
        widgetUi.privateStyleParagraph.getClassName().equals(p.getClassName()));

    assertTrue("Some kind of class should be set",
        widgetUi.totallyPrivateStyleSpan.getClassName().length() > 0);
  }

  public void testRadioButton() {
    RadioButton able = widgetUi.myRadioAble;
    RadioButton baker = widgetUi.myRadioBaker;
    assertTrue("able should be checked", able.getValue());
    assertFalse("baker should not be checked", baker.getValue());
    assertEquals("radios", able.getName());
    assertEquals("radios", baker.getName());
  }

  public void testStackPanel() {
    StackPanel p = widgetUi.myStackPanel;
    assertNotNull("Panel exists", p);
    Widget w = widgetUi.myStackPanelItem;
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
    DisclosurePanel p = widgetUi.myDisclosurePanel;
    assertNotNull("Panel exists", p);
    Widget w = widgetUi.myDisclosurePanelItem;
    assertNotNull("Widget exists", w);
    assertEquals("Panel contains widget", w, p.getContent());
  }

  public void testStringAttributeIgnoresStaticSetter() {
    // Assumes setPopupText() is overloaded such that there is a static
    // setPopupText(Foo, String) method.
    ClickyLink clicky = widgetUi.customLinkWidget;
    assertEquals("overloaded setter should have been called", "That tickles!",
        clicky.getPopupText());
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
    ClickyLink clicky = widgetUi.funnyCharsMessageAttributeWidget;
    String t = clicky.getPopupText();
    assertEquals("funny characters \" ' ' & < > > { }", t);
  }

  public void testWidgetAttributeNoMessageWithFunnyChars() {
    ClickyLink clicky = widgetUi.funnyCharsAttributeWidget;
    String t = clicky.getPopupText();
    assertEquals("funny characters \" ' ' & < > > { }", t);
  }

  public void testImageResourceInImageWidget() {
    assertEquals(widgetUi.prettyImage.getWidth(),
        widgetUi.babyWidget.getOffsetWidth());
    assertEquals(widgetUi.prettyImage.getHeight(),
        widgetUi.babyWidget.getOffsetHeight());
    assertEquals(widgetUi.prettyImage.getTop(),
        widgetUi.babyWidget.getOriginTop());
    assertEquals(widgetUi.prettyImage.getLeft(),
        widgetUi.babyWidget.getOriginLeft());
  }

  public void testDataResource() {
    assertNotNull(widgetUi.heartCursorResource.getUrl());
  }

  @DoNotRunWith(Platform.HtmlUnit)
  public void testCssImportedScopes() {
    assertEquals(100, widgetUi.cssImportScopeSample.inner.getOffsetWidth());
  }

  public void testSpritedElement() {
    assertEquals(widgetUi.prettyImage.getWidth(),
        widgetUi.simpleSpriteParagraph.getOffsetWidth());
    assertEquals(widgetUi.prettyImage.getHeight(),
        widgetUi.simpleSpriteParagraph.getOffsetHeight());
  }

  public void suppressForIEfail_testBizarrelyElementedWidgets() {
    assertInOrder(widgetUi.widgetCrazyTable.getInnerHTML().toLowerCase(),
        "<td>they have been known</td>", "<td>to write widgets</td>",
        "<td>that masquerade</td>", "<td>as table cells,</td>",
        "<td>just like these.</td>", "<td>burma shave</td>");

    assertInOrder(widgetUi.widgetCrazyOrderedList.getInnerHTML(),
        "<li>similar</li>", "<li>things</li>");

    assertInOrder(widgetUi.widgetCrazyDefinitionList.getInnerHTML(),
        "<dt>Being</dt>", "<dd>done</dd>", "<dd>with</dd>", "<dd>lists</dd>");
  }

  public void testCustomHtmlPanelTag() {
    assertInOrder(widgetUi.customTagHtmlPanel.getElement().getInnerHTML(),
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
