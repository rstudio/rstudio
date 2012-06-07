/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiRenderer;
import com.google.gwt.uibinder.test.client.UiRendererEventsTest.MockBarReceiver;
import com.google.gwt.uibinder.test.client.UiRendererEventsTest.MockBazReceiver;
import com.google.gwt.uibinder.test.client.UiRendererEventsTest.MockFooReceiver;

/**
 * Sample use of a {@code UiRenderer} with no dependency on com.google.gwt.user.
 */
public class UiRendererUi {

  /**
   * Resources for this template.
   */
  public interface Resources extends ClientBundle {
    @Source("UiRendererUi.css")
    Style style();
  }

  /**
   * CSS for this template.
   */
  public interface Style extends CssResource {
    String bodyColor();

    String bodyFont();
  }

  static class Foo {
    String bar;

    public Foo(String bar) {
      this.bar = bar;
    }

    String getBar() {
      return bar;
    }
  }

  static class Bar {
    Integer baz;

    public Bar(Integer bar) {
      this.baz = bar;
    }

    Integer getBar() {
      return baz;
    }
  }

  /**
   * A style defined within the UiBinder file.
   */
  public interface UiStyle extends CssResource {
    String enabled();
    String disabled();
  }

  /**
   * A UiRinder Cell renderer.
   */
  public interface HtmlRenderer extends UiRenderer {
    UiStyle getUiStyle();
    SpanElement getNameSpan(Element owner);
    TableColElement getNarrowColumn(Element owner);
    DivElement getRoot(Element owner);
    TableSectionElement getTbody(Element owner);
    TableCellElement getTh1(Element owner);
    TableCellElement getTh2(Element owner);
    TableCellElement getTh4(Element owner);
    Element getTmElement(Element owner);
    TableRowElement getTr(Element owner);

    void render(SafeHtmlBuilder sb, Foo aValue, Foo aValueTwice);
    void onBrowserEvent(MockFooReceiver o, NativeEvent e, Element p, Foo f, String s);
    void onBrowserEvent(MockBarReceiver o, NativeEvent e, Element p, Bar b, int i);
  }

  interface InheritedRenderer extends HtmlRenderer {
    TableCellElement getTh3(Element owner);
    void onBrowserEvent(MockBazReceiver o, NativeEvent e, Element p);
  }

  private static final HtmlRenderer renderer = GWT.create(HtmlRenderer.class);

  private static final InheritedRenderer inheritedRenderer = GWT.create(InheritedRenderer.class);

  public static HtmlRenderer getRenderer() {
    return renderer;
  }

  public static InheritedRenderer getInheritedRenderer() {
    return inheritedRenderer;
  }

  public UiRendererUi() {
  }

  public SafeHtml render(String value, String valueTwice) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    getRenderer().render(sb, new Foo(value), new Foo(valueTwice));
    return sb.toSafeHtml();
  }
}
