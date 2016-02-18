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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.test.client.EnumeratedLabel.Suffix;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DateLabel;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.NamedFrame;
import com.google.gwt.user.client.ui.NumberLabel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.ValueLabel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Locale;

/**
 * Functional test of UiBinder.
 */
public class UiBinderTest extends GWTTestCase {
  private WidgetBasedUi widgetUi;
  private DomBasedUi domUi;
  private com.google.gwt.user.client.ui.DockPanel root;

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.UiBinderSuite";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    UiBinderTestApp app = UiBinderTestApp.getInstance();
    widgetUi = app.getWidgetUi();
    domUi = app.getDomUi();
    root = widgetUi.root;
  }

  /**
   * Tests that the provided value takes precedence over a custom parser,
   * {@code UiFactory} or {@code UiConstructor}.
   * <p>
   * The fields are {@code final} and we test that they've correctly been
   * modified by the template.
   *
   * @see "http://code.google.com/p/google-web-toolkit/issues/detail?id=7740"
   */
  public void testProvidedWidgetWithCustomInitializer() {
    // Custom parser: should use the provided header, as the one from the
    // template is passed to the (ignored) custom initializer. But the child
    // widget should have been set.
    assertEquals("Provided header text", widgetUi.myProvidedDisclosurePanel.getHeaderTextAccessor().getText());
    assertNotNull(widgetUi.myProvidedDisclosurePanelItem);
    assertSame(widgetUi.myProvidedDisclosurePanelItem, widgetUi.myProvidedDisclosurePanel.getContent());
    // UiConstructor
    assertEquals("ditto", widgetUi.providedAnnotatedStrictLabel.getText());
    // UiFactory
    assertEquals("from template", widgetUi.providedStrictLabel.getText());
  }

  public void testTableWithColumns() {
    assertEquals("col", domUi.narrowColumn.getTagName().toLowerCase(Locale.ROOT));
    assertEquals("tr", domUi.tr.getTagName().toLowerCase(Locale.ROOT));
    assertEquals("th", domUi.th1.getTagName().toLowerCase(Locale.ROOT));
    assertEquals("th", domUi.th2.getTagName().toLowerCase(Locale.ROOT));
  }

  public void testTableWithExplicitTbody() {
    assertEquals("tbody", domUi.tbody.getTagName().toLowerCase(Locale.ROOT));
    assertEquals("th", domUi.th4.getTagName().toLowerCase(Locale.ROOT));
  }

  public void testAutoboxingFieldRef() {
    FakeBundle fakeBundle = new FakeBundle();

    assertEquals(new Integer(fakeBundle.anInt()),
        widgetUi.primitiveIntoObject.getObjectInteger());
    assertEquals(fakeBundle.anIntegerObject().intValue(),
        widgetUi.objectIntoPrimitive.getRawInt());
    assertEquals(fakeBundle.anIntegerObject(),
        widgetUi.allObject.getObjectInteger());
    assertEquals(fakeBundle.anInt(), widgetUi.allPrimitive.getRawInt());

    assertEquals(new Integer((int) fakeBundle.aDouble()),
        widgetUi.mismatchPrimitiveIntoObject.getObjectInteger());
    assertEquals((int) fakeBundle.aDouble(),
        widgetUi.allMismatchPrimitive.getRawInt());

    assertEquals(new Boolean(fakeBundle.aBoolean()),
        widgetUi.primitiveBooleanIntoObject.getObjectBoolean());
    assertEquals(fakeBundle.aBooleanObject().booleanValue(),
        widgetUi.objectBooleanIntoPrimitive.getRawBoolean());
    assertEquals(fakeBundle.aBooleanObject(),
        widgetUi.allObjectBoolean.getObjectBoolean());
    assertEquals(fakeBundle.aBoolean(),
        widgetUi.allPrimitiveBoolean.getRawBoolean());
  }

  public void testAccessToNonStandardElement() {
    Element elm = widgetUi.nonStandardElement;
    assertEquals("I", elm.getTagName());
  }

  public void testElementWithTagName() {
    Element elem = widgetUi.myElementWithTagName;
    assertEquals("ELEMENT-WITH-TAGNAME", elem.getTagName());
  }

  public void testAddStyleNamesAndDebugId() {
    Label l = widgetUi.lblDebugId;
    assertEquals("gwt-debug-joe", l.getElement().getId());

    assertEquals("styleName", l.getStylePrimaryName());

    WidgetBasedUiExternalResources resources = GWT.create(WidgetBasedUiExternalResources.class);
    assertTrue(l.getStyleName().contains("newStyle"));
    assertTrue(l.getStyleName().contains("anotherStyle"));
    assertTrue(l.getStyleName().contains("styleName-dependentStyle"));
    assertTrue(l.getStyleName().contains("styleName-anotherDependentStyle"));
    assertTrue(l.getStyleName().contains(
        "styleName-" + resources.style().prettyText()));
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
    assertEquals("{{blah in blah}}", widgetUi.bracedParagraph.getAttribute("doubleMustache"));
  }

  public void testBundle() {
    assertEquals(getCenter(), widgetUi.bundledLabel.getParent());
    assertEquals(new FakeBundle().helloText(), widgetUi.bundledLabel.getText());
    WidgetBasedUiExternalResources resources = GWT.create(WidgetBasedUiExternalResources.class);
    assertEquals(resources.style().prettyText(),
        widgetUi.bundledLabel.getStyleName());

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
    assertEquals(com.google.gwt.user.client.ui.DockPanel.CENTER,
        root.getWidgetDirection(center));
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

  public void testCustomButtonBody() {
    assertEquals("Hi mom", widgetUi.toggle.getText());
  }

  public void testCustomDialogBox() {
    assertEquals("Custom dialog am I", widgetUi.fooDialog.getText());
    Widget body = widgetUi.fooDialog.iterator().next();
    assertTrue(body instanceof Label);
    assertEquals("body", ((Label) body).getText());
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
    assertEquals("funny characters \\ \" ' ' & < > > { }", t);
  }

  public void testDomAttributeNoMessageWithFunnyChars() {
    ParagraphElement p = widgetUi.funnyCharsDomAttributeParagraph;
    String t = p.getAttribute("title");
    assertEquals("funny characters \\ \" ' ' & < > > { }", t);
  }

  public void testDomTextMessageWithFunnyChars() {
    String t = widgetUi.funnyCharsMessageParagraph.getInnerText();
    assertEquals("They might show up in body text that has been marked for "
        + "translation: funny characters \\ \" \" ' ' & < > > { }", t);
  }

  public void testEmptyAttributesOkay() {
    assertEquals("", widgetUi.styleLess.getStyleName());
  }

  public void testMixOfWidgetsAndElementsInUiMsg() {
    assertEquals("single translatable message",
        widgetUi.mixedMessageWidget.getText());
    assertEquals("exciting and subtle",
        widgetUi.mixedMessageSpan.getInnerText());
  }

  public void testEnums() {
    Suffix expected = EnumeratedLabel.Suffix.tail;
    assertTrue("Should end with suffix \"" + expected + "\"",
        widgetUi.enumLabel.getText().endsWith(expected.toString()));
  }

  public void testCustomButtonParser() {
    // .toLowerCase normalization to keep IE happy
    assertEquals("<b>click me</b>",
        widgetUi.pushButton.getUpFace().getHTML().toLowerCase(Locale.ROOT));
    assertTrue(widgetUi.pushButton.getUpHoveringFace().getHTML().contains(
        ">Click ME!<"));
    assertEquals("<b>click me!</b>",
        widgetUi.pushButton.getUpHoveringFace().getHTML().toLowerCase(Locale.ROOT));
    // Can't test the images at all :-P
  }

  public void testProtectedDomTextMessageWithFunnyChars() {
    String t = widgetUi.funnyCharsProtectedMessageParagraph.getInnerText();
    assertEquals("Don't forget about protected untranslatable blocks: "
        + "funny characters \\ \" \" ' ' & < > > { }", t);
  }

  public void testDomTextInNamedElementMessageWithFunnyChars() {
    String t = widgetUi.funnyCharsMessageChildSpan.getInnerText();
    assertEquals("funny characters \\ \" \" ' ' & < > > { }", t);
  }

  public void suppressedForSafari3Fail_testDomTextNoMessageWithFunnyChars() {
    ParagraphElement p = widgetUi.funnyCharsParagraph;
    // WebKit does \n replace thing, so let's do it everywhere
    String t = p.getInnerHTML().replace("\n", " ").toLowerCase(Locale.ROOT);
    String expected = "Templates can be marked up for <b>localization</b>, which presents alls "
        + "kinds of exciting opportunities for bugs related to character escaping. "
        + "Consider these funny characters \\ \" \" ' ' &amp; &lt; &gt; &gt; { }, and "
        + "the various places they might make your life miserable, like this "
        + "untranslated paragraph.";
    expected = expected.toLowerCase(Locale.ROOT);
    assertEquals(expected, t);
  }

  public void testFieldAttribute() {
    assertEquals(getCenter(), widgetUi.gwtFieldLabel.getParent());
  }

  public void testFieldInPlaceholderedElement() {
    assertEquals("named portions", widgetUi.spanInMsg.getInnerText());
  }

  public void testGrid() {
    assertTrue(widgetUi.fooGrid.getWidget(0, 0) instanceof Label);
    assertTrue(widgetUi.fooGrid.getWidget(0, 1) instanceof Button);
    assertEquals(2, widgetUi.fooGrid.getColumnCount());
    assertEquals(1, widgetUi.fooGrid.getRowCount());
  }

  public void testListBox() {
    assertEquals(2, widgetUi.fooListBox.getItemCount());
    assertEquals("bar", widgetUi.fooListBox.getItemText(0));
    assertEquals("bar", widgetUi.fooListBox.getValue(0));
    assertEquals("bar 2", widgetUi.fooListBox.getItemText(1));
    assertEquals("bar2", widgetUi.fooListBox.getValue(1));
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
    @Source(value = {"WidgetBasedUi.gss", "Menu.gss"})
    @NotStrict
    WidgetBasedUi.Style style();
  }

  public void testNoOverrideInheritedSharedCssClasses() {
    Bundle bundle = GWT.create(Bundle.class);
    String publicStyle = bundle.style().menuBar();
    String privateStyle = widgetUi.myStyle.menuBar();
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
    assertEquals(com.google.gwt.user.client.ui.DockPanel.NORTH,
        root.getWidgetDirection(north));
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

  public void testAbsolutePanel() {
    AbsolutePanel p = widgetUi.myAbsolutePanel;
    assertNotNull("Panel exists", p);

    assertEquals("Panel contains exactly 3 widgets", 3, p.getWidgetCount());
    assertEquals("Panel contains expected itemA",
        widgetUi.myAbsolutePanelItemA, p.getWidget(0));
    assertEquals("Panel contains expected itemB",
        widgetUi.myAbsolutePanelItemB, p.getWidget(1));
    assertEquals("Panel contains expected itemC",
        widgetUi.myAbsolutePanelItemC, p.getWidget(2));

    /*
     * The following fails on Safari 3, off by a few pixels. The coverage in
     * AbsolutePanelParserTest and AbsolutePanelTest are enough to make up for
     * the lack. Leaving this here as a warning to the next guy.
     */
    // {
    // Widget w = widgetUi.myAbsolutePanelItemA;
    // assertNotNull("Widget exists", w);
    // assertEquals("Widget has left", 1, p.getWidgetLeft(w));
    // assertEquals("Widget has top", 2, p.getWidgetTop(w));
    // }
    //
    // {
    // Widget w = widgetUi.myAbsolutePanelItemC;
    // assertNotNull("Widget exists", w);
    // assertEquals("Widget has left", 10, p.getWidgetLeft(w));
    // assertEquals("Widget has top", 20, p.getWidgetTop(w));
    // }
  }

  public void testNamedFrame() {
    NamedFrame p = widgetUi.myNamedFrame;
    assertNotNull("NamedFrame exists", p);
  }

  public void testTree() {
    Tree tree = widgetUi.myTree;
    TreeItem complexItem = widgetUi.myTreeItemC;
    // top level items
    assertEquals(3, tree.getItemCount());
    assertSame(widgetUi.myTreeItemA, tree.getItem(0));
    assertSame(widgetUi.myTreeWidgetB, tree.getItem(1).getWidget());
    assertSame(complexItem, tree.getItem(2));
    // complex item
    assertSame(2, complexItem.getChildCount());
    assertSame(widgetUi.myTreeItemCA, complexItem.getChild(0));
    assertSame(widgetUi.myTreeWidgetCB, complexItem.getChild(1).getWidget());
  }

  public void testDateLabel() {
    DateLabel p = widgetUi.myDateLabel;
    assertNotNull("DateLabel exists", p);
    p = widgetUi.myDateLabel2;
    assertNotNull("DateLabel exists", p);
    p = widgetUi.myDateLabel3;
    assertNotNull("DateLabel exists", p);
  }

  public void testNumberLabel() {
    NumberLabel<Float> p = widgetUi.myNumberLabel;
    assertNotNull("NumberLabel exists", p);
  }

  public void testValueLabel() {
    ValueLabel<Double> p = widgetUi.myValueLabel;
    assertNotNull("ValueLabel exists", p);
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
    assertEquals(com.google.gwt.user.client.ui.DockPanel.WEST,
        root.getWidgetDirection(west));
    assertEquals(HTML.class, west.getClass());
    String html = ((HTML) west).getHTML();
    assertTrue(html.contains("side bar"));
  }

  public void testWidgetAttributeMessageWithFunnyChars() {
    ClickyLink clicky = widgetUi.funnyCharsMessageAttributeWidget;
    String t = clicky.getPopupText();
    assertEquals("funny characters \\ \" ' ' & < > > { }", t);
  }

  public void testWidgetAttributeNoMessageWithFunnyChars() {
    ClickyLink clicky = widgetUi.funnyCharsAttributeWidget;
    String t = clicky.getPopupText();
    assertEquals("funny characters \\ \" ' ' & < > > { }", t);
  }

  /**
   * Fails in all modes due to an HtmlUnit bug: offsetWidth always returns 1256.
   * TODO(t.broyer): file a new HtmlUnit bug.
   * Similar to http://sourceforge.net/p/htmlunit/bugs/1447/
   */
  @DoNotRunWith(Platform.HtmlUnitBug)
  public void testCustomImageClass() {
    ImageResource resource = widgetUi.prettyImage;
    Image widget = widgetUi.fooImage;
    assertEquals(resource.getWidth(), widget.getOffsetWidth());
    assertEquals(resource.getHeight(), widget.getOffsetHeight());
    assertEquals(resource.getTop(), widget.getOriginTop());
    assertEquals(resource.getLeft(), widget.getOriginLeft());
  }

  /**
   * Fails in all modes due to an HtmlUnit bug: offsetWidth always returns 1256.
   * TODO(t.broyer): file a new HtmlUnit bug.
   * Similar to http://sourceforge.net/p/htmlunit/bugs/1447/
   */
  @DoNotRunWith(Platform.HtmlUnitBug)
  public void testImageResourceInImageWidget() {
    ImageResource resource = widgetUi.prettyImage;
    Image widget = widgetUi.babyWidget;
    assertEquals(resource.getWidth(), widget.getOffsetWidth());
    assertEquals(resource.getHeight(), widget.getOffsetHeight());
    assertEquals(resource.getTop(), widget.getOriginTop());
    assertEquals(resource.getLeft(), widget.getOriginLeft());

    assertEquals("expected alt text", widget.getAltText());
    assertEquals("expected style name", widget.getStyleName());
  }

  public void testIntPair() {
    assertEquals(100, widgetUi.sideBarWidget.getOffsetWidth());
    assertEquals(150, widgetUi.sideBarWidget.getOffsetHeight());
  }

  public void testDataResource() {
    assertNotNull(widgetUi.heartCursorResource.getSafeUri());
  }

  public void testCssImportedScopes() {
    assertEquals(100, widgetUi.cssImportScopeSample.inner.getOffsetWidth());
  }

  public void testSpritedElement() {
    assertEquals(widgetUi.prettyImage.getWidth(),
        widgetUi.simpleSpriteParagraph.getOffsetWidth());
    assertEquals(widgetUi.prettyImage.getHeight(),
        widgetUi.simpleSpriteParagraph.getOffsetHeight());
  }

  public void testStaticImport() {
    assertEquals(Constants.CONST_FOO,
        widgetUi.bracedParagraph.getAttribute("foo"));
    assertEquals(Constants.Inner.CONST_BAR + " " + Constants.Inner.CONST_BAZ,
        widgetUi.bracedParagraph.getAttribute("bar"));
    assertEquals(Constants.MyEnum.ENUM_1.name() + " "
        + Constants.MyEnum.ENUM_2.name(),
        widgetUi.bracedParagraph.getAttribute("enum"));
  }

  public void suppressForIEfail_testBizarrelyElementedWidgets() {
    assertInOrder(widgetUi.widgetCrazyTable.getInnerHTML().toLowerCase(Locale.ROOT),
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

  public void testAlignmentAttributes() {
    assertEquals("left",
      widgetUi.myHorizontalPanel.getHorizontalAlignment().getTextAlignString());
    assertEquals("middle",
      widgetUi.myHorizontalPanel.getVerticalAlignment().getVerticalAlignString());

    final String innerHtml =
      widgetUi.myHorizontalPanel.getElement().getInnerHTML();
    assertInOrder(innerHtml, "vertical-align: middle",
        "a stackpanel");

    final String innerHtml2 = innerHtml.replace("\"", "");
    assertInOrder(innerHtml2, "align=left", "a stackpanel");
  }

  public void testUrlResource() {
    assertEquals(new FakeBundle().aUrl(), widgetUi.myImage.getSrc());
  }

  public void testUiTextWithSafeHtml() {
    assertEquals("<b>this text should be bold!</b>",
        widgetUi.htmlWithComputedSafeHtml.getHTML().toLowerCase(Locale.ROOT));
    assertEquals("&lt;b&gt;this text won't be bold!&lt;/b&gt;",
        widgetUi.htmlWithComputedText.getHTML().toLowerCase(Locale.ROOT).replaceAll(">", "&gt;"));
    assertEquals("<b>this text won't be bold!</b>",
        widgetUi.labelWithComputedText.getText().toLowerCase(Locale.ROOT));
  }

  public void testFlowPanelWithTag() {
    assertEquals("P", widgetUi.flowPanelWithTag.getElement().getTagName());
  }

  public void testEmbeddedSvgMimeType() {
    String url = widgetUi.embeddedSvgData.getSafeUri().asString();
    if (url.startsWith("data:")) {
      assertTrue(url.startsWith("data:image/svg+xml"));
    }
  }

  public void testLinkedSvgNotEmbedded() {
    String url = widgetUi.linkedSvgData.getSafeUri().asString();
    assertFalse(url.startsWith("data:"));
    assertTrue(url, url.endsWith(".svg"));
  }

  public void testIsWidget() {
    FooIsWidget isWidget = widgetUi.fooIsWidget;
    assertEquals("gwt-Label " + widgetUi.myStyle.menuBar(), isWidget.asWidget().getStyleName());
    assertEquals(false, isWidget.isVisible());
  }

  /**
   * Assert that the expect strings are found in body, and in the order given.
   * WARNING: both body and expected are normalized to lower case, to get around
   * IE's habit of returning capitalized DOM elements.
   */
  private void assertInOrder(String body, String... expected) {
    body = body.toLowerCase(Locale.ROOT);
    int lastIndex = 0;
    String lastExpected = "";

    for (String next : expected) {
      next = next.toLowerCase(Locale.ROOT);
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
