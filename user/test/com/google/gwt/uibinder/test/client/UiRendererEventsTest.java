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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.test.client.UiRendererUi.Bar;
import com.google.gwt.uibinder.test.client.UiRendererUi.Foo;

/**
 * Functional test of UiRenderer event handling.
 */
public class UiRendererEventsTest extends GWTTestCase {

  /**
   * Receives events containing {@link UiRendererUi.Bar} objects dispatched from
   * {@link UiRendererUi.HtmlRenderer#onBrowserEvent(MockBarReceiver, NativeEvent, Element,
   * UiRendererUi.Bar, int)}.
   */
  public class MockBarReceiver extends AbstractCell<Integer> {

    public DomEvent<?> event;
    public Bar firstExtraParam;
    public int handlerCalled = 0;
    public Element root;
    public int secondExtraParam;

    @Override
    public void render(com.google.gwt.cell.client.Cell.Context context, Integer value,
        SafeHtmlBuilder sb) {
      UiRendererUi.getRenderer().render(sb, new Foo(value.toString()), new Foo(value.toString()));
    }

    @UiHandler({"nameSpan"})
    void handler1(ClickEvent clickEvent, Element theRoot, Bar e, int f) {
      handlerCalled = 1;
      this.event = clickEvent;
      this.root = theRoot;
      this.firstExtraParam = e;
      this.secondExtraParam = f;
    }
  }

  /**
   * Implements all methods needed by {@link MockBazReceiver}.
   */
  public class MockBaseReceiver extends AbstractCell<Integer> {

    public DomEvent<?> event;
    public int handlerCalled = 0;

    @Override
    public void render(com.google.gwt.cell.client.Cell.Context context, Integer value,
        SafeHtmlBuilder sb) {
      UiRendererUi.getInheritedRenderer().render(sb, new Foo(value.toString()),
          new Foo(value.toString()));
    }

    @UiHandler({"th3"})
    void handler1(ClickEvent clickEvent) {
      handlerCalled = 1;
      this.event = clickEvent;
    }
  }

  /**
   * Receives events dispatched from
   * {@link UiRendererUi.InheritedRenderer#onBrowserEvent(MockBazReceiver, NativeEvent, Element, 
   * boolean)}.
   */
  public class MockBazReceiver extends MockBaseReceiver {
  }

  /**
   * Receives events containing {@link UiRendererUi.Foo} objects dispatched from
   * {@link UiRendererUi.HtmlRenderer#onBrowserEvent(MockFooReceiver, NativeEvent, Element,
   * UiRendererUi.Foo, String)}.
   */
  public class MockFooReceiver extends AbstractCell<String> {

    public DomEvent<?> event;
    public Foo firstExtraParam;
    public int handlerCalled = 0;
    public Element root;
    public String secondExtraParam;

    @Override
    public void render(com.google.gwt.cell.client.Cell.Context context, String value,
        SafeHtmlBuilder sb) {
      UiRendererUi.getRenderer().render(sb, new Foo(value), new Foo(value));
    }

    @UiHandler({"root", "tmElement"})
    void handler1(ClickEvent clickEvent, Element theRoot, Foo a, String b) {
      handlerCalled = 1;
      this.event = clickEvent;
      this.root = theRoot;
      this.firstExtraParam = a;
      this.secondExtraParam = b;
    }

    @UiHandler({"th1"})
    void handler2(KeyPressEvent keyEvent, Element aRoot, Foo c, String d) {
      handlerCalled = 2;
      this.event = keyEvent;
      this.root = aRoot;
      this.firstExtraParam = c;
      this.secondExtraParam = d;
    }

    @UiHandler({"tr2"})
    void handler3(ClickEvent clickEvent, Element aRoot, Foo e, String f) {
      handlerCalled = 3;
      this.event = clickEvent;
      this.root = aRoot;
      this.firstExtraParam = e;
      this.secondExtraParam = f;
    }
  }
  private MockBarReceiver barReceiver;
  private MockBazReceiver bazReceiver;
  private MockFooReceiver fooReceiver;

  private SafeHtml renderedUi;

  private DivElement uiParent;

  private UiRendererUi uiRendererUi;

  private Element uiRoot;

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.LazyWidgetBuilderSuite";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    UiRendererTestApp app = UiRendererTestApp.getInstance();
    uiRendererUi = app.getUiRendererUi();
    renderedUi = uiRendererUi.render("once", "twice");

    fooReceiver = new MockFooReceiver();
    barReceiver = new MockBarReceiver();
    bazReceiver = new MockBazReceiver();

    uiParent = Document.get().createDivElement();
    uiParent.setInnerHTML(renderedUi.asString());
    Document.get().getBody().appendChild(uiParent);

    uiRoot = Element.as(uiParent.getChild(0));
  }

  public void testDispatchEventAtRoot() {

    NativeEvent eventAtRoot = createMockNativeEvent(uiRoot, "click");

    UiRendererUi.getRenderer().onBrowserEvent(fooReceiver, eventAtRoot, uiRoot, new Foo("one"), "two");

    assertEquals(1, fooReceiver.handlerCalled);
    assertEquals("click", fooReceiver.event.getAssociatedType().getName());
    assertEquals(uiRoot, fooReceiver.root);
    assertEquals("one", fooReceiver.firstExtraParam.bar);
    assertEquals("two", fooReceiver.secondExtraParam);

    // Dispatch also works when the parent is passed
    fooReceiver = new MockFooReceiver();
    UiRendererUi.getRenderer().onBrowserEvent(fooReceiver, eventAtRoot, uiParent, new Foo("three"), "four");

    assertEquals(1, fooReceiver.handlerCalled);
    assertEquals("click", fooReceiver.event.getAssociatedType().getName());
    assertEquals(uiRoot, fooReceiver.root);
    assertEquals("three", fooReceiver.firstExtraParam.bar);
    assertEquals("four", fooReceiver.secondExtraParam);
  }

  public void testDispatchWithInheritedRenderer() {

    TableCellElement th3 = UiRendererUi.getInheritedRenderer().getTh3(uiRoot);
    NativeEvent eventAtTh3 = createMockNativeEvent(th3, "click");

    UiRendererUi.getInheritedRenderer().onBrowserEvent(bazReceiver, eventAtTh3, uiRoot);

    assertEquals(1, bazReceiver.handlerCalled);
    assertEquals("click", bazReceiver.event.getAssociatedType().getName());
  }

  public void testDispatchWithinNestedElement() {
    Element th4 = UiRendererUi.getRenderer().getTh4(uiRoot);
    NativeEvent eventAtTh4 = createMockNativeEvent(th4, "click");
    UiRendererUi.getRenderer().onBrowserEvent(fooReceiver, eventAtTh4, uiRoot, new Foo("one"), "two");

    assertEquals(3, fooReceiver.handlerCalled);
    assertEquals("click", fooReceiver.event.getAssociatedType().getName());
    assertEquals(uiRoot, fooReceiver.root);
    assertEquals("one", fooReceiver.firstExtraParam.bar);
    assertEquals("two", fooReceiver.secondExtraParam);
  }

  public void testDispatchWithinRoot() {
    Element tmElement = UiRendererUi.getRenderer().getTmElement(uiRoot);
    NativeEvent eventAtTm = createMockNativeEvent(tmElement, "click");
    UiRendererUi.getRenderer().onBrowserEvent(fooReceiver, eventAtTm, uiRoot, new Foo("one"), "two");

    assertEquals(1, fooReceiver.handlerCalled);
    assertEquals("click", fooReceiver.event.getAssociatedType().getName());
    assertEquals(uiRoot, fooReceiver.root);
    assertEquals("one", fooReceiver.firstExtraParam.bar);
    assertEquals("two", fooReceiver.secondExtraParam);

    Element th1 = UiRendererUi.getRenderer().getTh1(uiRoot);
    NativeEvent eventAtTh1 = createMockNativeEvent(th1, "keypress");
    fooReceiver = new MockFooReceiver();
    UiRendererUi.getRenderer().onBrowserEvent(fooReceiver, eventAtTh1, uiRoot, new Foo("three"), "four");

    assertEquals(2, fooReceiver.handlerCalled);
    assertEquals("keypress", fooReceiver.event.getAssociatedType().getName());
    assertEquals(uiRoot, fooReceiver.root);
    assertEquals("three", fooReceiver.firstExtraParam.bar);
    assertEquals("four", fooReceiver.secondExtraParam);

    Element nameSpan = UiRendererUi.getRenderer().getNameSpan(uiRoot);
    NativeEvent eventAtNameSpan = createMockNativeEvent(nameSpan, "click");
    fooReceiver = new MockFooReceiver();
    UiRendererUi.getRenderer().onBrowserEvent(barReceiver, eventAtNameSpan, uiRoot, new Bar(5), 6);

    assertEquals(1, barReceiver.handlerCalled);
    assertEquals("click", barReceiver.event.getAssociatedType().getName());
    assertEquals(uiRoot, barReceiver.root);
    assertEquals(5, barReceiver.firstExtraParam.baz.intValue());
    assertEquals(6, barReceiver.secondExtraParam);
  }

  public void testNoDispatchOnNullRoot() {
    NativeEvent eventAtBody = createMockNativeEvent(Document.get().getBody(), "click");
    try {
      UiRendererUi.getRenderer().onBrowserEvent(fooReceiver, eventAtBody, null, new Foo("one"), "two");
      fail("NPE expected");
    } catch (NullPointerException e) {
      // Expected case
      assertEquals(0, fooReceiver.handlerCalled);
    }
  }

  public void testNoDispatchOnNullEvent() {
    try {
      UiRendererUi.getRenderer().onBrowserEvent(fooReceiver, null, uiRoot, new Foo("one"), "two");
      fail("NPE expected");
    } catch (NullPointerException e) {
      // Expected case
      assertEquals(0, fooReceiver.handlerCalled);
    }
  }

  public void testNoDispatchOnNullReceiver() {
    NativeEvent eventAtBody = createMockNativeEvent(Document.get().getBody(), "click");
    try {
      UiRendererUi.getRenderer().onBrowserEvent(null, eventAtBody, uiRoot, new Foo("one"), "two");
      fail("NPE expected");
    } catch (NullPointerException e) {
      // Expected case
      assertEquals(0, fooReceiver.handlerCalled);
    }
  }

  public void testNoDispatchOutsideRendered() {
    // An event at the body is not dispatched
    NativeEvent eventAtBody = createMockNativeEvent(Document.get().getBody(), "click");
    UiRendererUi.getRenderer().onBrowserEvent(fooReceiver, eventAtBody, uiRoot, new Foo("one"), "two");
    assertEquals(0, fooReceiver.handlerCalled);

    // An event at the parent is not dispatched
    NativeEvent eventAtParent = createMockNativeEvent(uiParent, "click");
    fooReceiver = new MockFooReceiver();
    UiRendererUi.getRenderer().onBrowserEvent(fooReceiver, eventAtParent, uiRoot, new Foo("one"), "two");
    assertEquals(0, fooReceiver.handlerCalled);

    // At an element besides the parent does not fire either
    DivElement externalDiv = Document.get().createDivElement();
    Document.get().getBody().appendChild(externalDiv);
    NativeEvent eventAtDiv = createMockNativeEvent(externalDiv, "click");
    fooReceiver = new MockFooReceiver();
    UiRendererUi.getRenderer().onBrowserEvent(fooReceiver, eventAtDiv, uiRoot, new Foo("one"), "two");
    assertEquals(0, fooReceiver.handlerCalled);
  }

  @Override
  protected void gwtTearDown() {
    uiParent.removeFromParent();
    uiParent = null;
  }

  private native NativeEvent createMockNativeEvent(Element target, String type) /*-{
    var event = {};

    event.target = target;
    // For IE < 9
    event.srcElement = target;

    event.type = type;
    return event;
  }-*/;
}
