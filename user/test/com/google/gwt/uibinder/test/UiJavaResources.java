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
package com.google.gwt.uibinder.test;

import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;

import java.util.Arrays;
import java.util.Set;

/**
 * A pared down, very low fidelity set of GWT widget Java source files for code
 * generator testing.
 */
public class UiJavaResources {

  public static final MockJavaResource ABSOLUTE_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.AbsolutePanel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class AbsolutePanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource BUTTON = new MockJavaResource(
      "com.google.gwt.user.client.ui.Button") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("import com.google.gwt.event.dom.client.ClickEvent;\n");
      code.append("import com.google.gwt.event.dom.client.ClickHandler;\n");
      code.append("import com.google.gwt.event.dom.client.MouseOverEvent;\n");
      code.append("import com.google.gwt.event.dom.client.MouseOverHandler;\n");
      code.append("public class Button extends Widget");
      code.append("  implements ClickHandler, MouseOverHandler {\n");
      code.append("  public void onMouseOver(MouseOverEvent event){}\n");
      code.append("  public void onClick(ClickEvent event){}\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CLICK_EVENT = new MockJavaResource(
      "com.google.gwt.event.dom.client.ClickEvent") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.dom.client;\n");
      code.append("import com.google.gwt.event.shared.GwtEvent;\n");
      code.append("public class ClickEvent extends DomEvent<ClickHandler> {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CLICK_HANDLER = new MockJavaResource(
      "com.google.gwt.event.dom.client.ClickHandler") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.dom.client;\n");
      code.append("import com.google.gwt.event.shared.EventHandler;\n");
      code.append("public interface ClickHandler extends EventHandler {\n");
      code.append("  void onClick(ClickEvent event);\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource COMMAND = new MockJavaResource(
      "com.google.gwt.user.client.Command") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client;\n");
      code.append("public interface Command {\n");
      code.append("  void execute();\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CURRENCY_DATA = new MockJavaResource(
      "com.google.gwt.i18n.client.CurrencyData") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.client;\n");
      code.append("public class CurrencyData {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource DATE_LABEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.DateLabel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("import com.google.gwt.i18n.shared.DateTimeFormat;\n");
      code.append("import com.google.gwt.i18n.shared.TimeZone;\n");
      code.append("public class DateLabel extends ValueLabel {\n");
      code.append("  public DateLabel() { super(null); } ");
      code.append("  public DateLabel(DateTimeFormat format) { super(null); } ");
      code.append("  public DateLabel(DateTimeFormat format, TimeZone timeZone) { super(null); } ");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource DATE_TIME_FORMAT = new MockJavaResource(
      "com.google.gwt.i18n.shared.DateTimeFormat") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.shared;\n");
      code.append("public class DateTimeFormat {\n");
      code.append("  public static enum PredefinedFormat {\n");
      PredefinedFormat[] values = PredefinedFormat.values();
      for (int i = 0; i < values.length; i++) {
        code.append("    ").append(values[i].name());
        if (i < values.length - 1) {
          code.append(",\n");
        }
      }
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource DATE_TIME_FORMAT_OLD = new MockJavaResource(
      "com.google.gwt.i18n.client.DateTimeFormat") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.client;\n");
      code.append("public class DateTimeFormat extends com.google.gwt.i18n.shared.DateTimeFormat {\n");
      code.append("  public static enum PredefinedFormat {\n");
      PredefinedFormat[] values = PredefinedFormat.values();
      for (int i = 0; i < values.length; i++) {
        code.append("    ").append(values[i].name());
        if (i < values.length - 1) {
          code.append(",\n");
        }
      }
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource DIALOG_BOX = new MockJavaResource(
      "com.google.gwt.user.client.ui.DialogBox") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class DialogBox extends Widget {\n");
      code.append(" public interface Caption {} \n");
      code.append("  public DialogBox(boolean autoHide, boolean modal) {} \n");
      code.append("  public DialogBox(boolean autoHide, boolean modal, Caption caption) {} ");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource DISCLOSURE_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.DisclosurePanel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class DisclosurePanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource DOCK_LAYOUT_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.DockLayoutPanel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class DockLayoutPanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource DOM_EVENT = new MockJavaResource(
      "com.google.gwt.event.dom.client.DomEvent") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.dom.client;\n");
      code.append("import com.google.gwt.event.shared.EventHandler;\n");
      code.append("import com.google.gwt.event.shared.GwtEvent;\n");
      code.append("public abstract class DomEvent<H extends EventHandler> extends GwtEvent<H> {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource ELEMENT = new MockJavaResource(
      "com.google.gwt.dom.client.Element") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.dom.client;\n");
      code.append("public class Element {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource EVENT_HANDLER = new MockJavaResource(
      "com.google.gwt.event.shared.EventHandler") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.shared;\n");
      code.append("public interface EventHandler {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource GRID = new MockJavaResource(
      "com.google.gwt.user.client.ui.Grid") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class Grid extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource GWT_EVENT = new MockJavaResource(
      "com.google.gwt.event.shared.GwtEvent") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.shared;\n");
      code.append("public abstract class GwtEvent<H extends EventHandler> {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource HANDLER_REGISTRATION = new MockJavaResource(
      "com.google.gwt.event.shared.HandlerRegistration") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.shared;\n");
      code.append("public interface HandlerRegistration {\n");
      code.append("  void removeHandler();");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource HAS_CLICK_HANDLERS = new MockJavaResource(
      "com.google.gwt.event.dom.client.HasClickHandlers") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.dom.client;\n");
      code.append("import com.google.gwt.event.shared.HandlerRegistration;\n");
      code.append("public interface HasClickHandlers {\n");
      code.append("  HandlerRegistration addClickHandler(ClickHandler handler);");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource HAS_HORIZONTAL_ALIGNMENT = new MockJavaResource(
      "com.google.gwt.user.client.ui.HasHorizontalAlignment") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class HasHorizontalAlignment {\n");
      code.append("  public static class HorizontalAlignmentConstant {\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource HAS_VERTICAL_ALIGNMENT = new MockJavaResource(
      "com.google.gwt.user.client.ui.HasVerticalAlignment") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class HasVerticalAlignment {\n");
      code.append("  public static class VerticalAlignmentConstant {\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource HTML_PANEL = new MockJavaResource(
  "com.google.gwt.user.client.ui.HTMLPanel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class HTMLPanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource IMAGE = new MockJavaResource(
      "com.google.gwt.user.client.ui.Image") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("import com.google.gwt.resources.client.ImageResource;\n");
      code.append("public class Image extends Widget {\n");
      code.append("  public Image() {} ");
      code.append("  public Image(ImageResource r) {} ");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource IMAGE_RESOURCE = new MockJavaResource(
      "com.google.gwt.resources.client.ImageResource") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.resources.client;\n");
      code.append("public class ImageResource  {\n");
      code.append("  public ImageResource() {} ");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource IS_WIDGET = new MockJavaResource(
      "com.google.gwt.user.client.ui.IsWidget") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public interface IsWidget  {\n");
      code.append("  Widget asWidget();\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource LABEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.Label") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("import com.google.gwt.event.dom.client.ClickEvent;\n");
      code.append("import com.google.gwt.event.dom.client.ClickHandler;\n");
      code.append("import com.google.gwt.event.dom.client.MouseOverEvent;\n");
      code.append("import com.google.gwt.event.dom.client.MouseOverHandler;\n");
      code.append("public class Label extends Widget");
      code.append("  implements ClickHandler, MouseOverHandler {\n");
      code.append("  public void onMouseOver(MouseOverEvent event){}\n");
      code.append("  public void onClick(ClickEvent event){}\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource LAYOUT_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.LayoutPanel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class LayoutPanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource LIST_BOX = new MockJavaResource(
      "com.google.gwt.user.client.ui.ListBox") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class ListBox extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource MENU_BAR = new MockJavaResource(
      "com.google.gwt.user.client.ui.MenuBar") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class MenuBar extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource MENU_ITEM = new MockJavaResource(
      "com.google.gwt.user.client.ui.MenuItem") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("import com.google.gwt.user.client.Command;\n");
      code.append("public class MenuItem extends UIObject {\n");
      code.append("  public MenuItem(String text, Command command) {\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource MENU_ITEM_SEPARATOR = new MockJavaResource(
      "com.google.gwt.user.client.ui.MenuItemSeparator") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("import com.google.gwt.user.client.Command;\n");
      code.append("public class MenuItemSeparator extends UIObject {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource MOUSE_OVER_EVENT = new MockJavaResource(
      "com.google.gwt.event.dom.client.MouseOverEvent") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.dom.client;\n");
      code.append("import com.google.gwt.event.shared.GwtEvent;\n");
      code.append("public class MouseOverEvent extends GwtEvent<MouseOverHandler> {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource MOUSE_OVER_HANDLER = new MockJavaResource(
      "com.google.gwt.event.dom.client.MouseOverHandler") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.dom.client;\n");
      code.append("import com.google.gwt.event.shared.EventHandler;\n");
      code.append("public interface MouseOverHandler extends EventHandler {\n");
      code.append("  void onMouseOver(MouseOverEvent event);\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource NUMBER_LABEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.NumberLabel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("import com.google.gwt.i18n.client.NumberFormat;\n");
      code.append("public class NumberLabel extends ValueLabel {\n");
      code.append("  public NumberLabel() { super(null); } ");
      code.append("  public NumberLabel(NumberFormat format) { super(null); } ");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource NATIVE_EVENT = new MockJavaResource(
      "com.google.gwt.dom.client.NativeEvent") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.dom.client;\n");
      code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
      code.append("public class NativeEvent extends JavaScriptObject {\n");
      code.append("  protected NativeEvent() {\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource NUMBER_FORMAT = new MockJavaResource(
      "com.google.gwt.i18n.client.NumberFormat") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.client;\n");
      code.append("public class NumberFormat {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource RENDERER = new MockJavaResource(
      "com.google.gwt.text.shared.Renderer") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.text.shared;\n");
      code.append("public class Renderer<T> {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource SAFE_HTML = new MockJavaResource(
  "com.google.gwt.safehtml.shared.SafeHtml") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.safehtml.shared;\n");
      code.append("public interface SafeHtml {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource SAFE_HTML_BUILDER = new MockJavaResource(
      "com.google.gwt.safehtml.shared.SafeHtmlBuilder") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.safehtml.shared;\n");
      code.append("public class SafeHtmlBuilder {");
      code.append("}");
      return code;
    }
  };
  public static final MockJavaResource SAFE_URI = new MockJavaResource(
  "com.google.gwt.safehtml.shared.SafeUri") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.safehtml.shared;\n");
      code.append("public interface SafeUri{\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource SPLIT_LAYOUT_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.SplitLayoutPanel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class SplitLayoutPanel extends DockLayoutPanel {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource STACK_LAYOUT_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.StackLayoutPanel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class StackLayoutPanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource STACK_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.StackPanel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class StackPanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource STYLE = new MockJavaResource(
      "com.google.gwt.dom.client.Style") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.dom.client;\n");
      code.append("public class Style  {\n");
      code.append("  public enum Unit { PX, PCT, EM, EX, PT, PC, IN, CM, MM };\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource TAB_LAYOUT_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.TabLayoutPanel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class TabLayoutPanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource TAB_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.TabPanel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class TabPanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource TEXT_BOX_BASE = new MockJavaResource(
      "com.google.gwt.user.client.ui.TextBoxBase") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class TextBoxBase {\n");
      code.append("  public static class TextAlignConstant {\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource TREE = new MockJavaResource(
      "com.google.gwt.user.client.ui.Tree") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class Tree extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource TREE_ITEM = new MockJavaResource(
      "com.google.gwt.user.client.ui.TreeItem") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class TreeItem extends UIObject {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource TIME_ZONE = new MockJavaResource(
      "com.google.gwt.i18n.shared.TimeZone") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.shared;\n");
      code.append("public interface TimeZone {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource TIME_ZONE_OLD = new MockJavaResource(
      "com.google.gwt.i18n.client.TimeZone") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.client;\n");
      code.append("public class TimeZone implements com.google.gwt.i18n.shared.TimeZone {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource UI_BINDER = new MockJavaResource(
      "com.google.gwt.uibinder.client.UiBinder") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.uibinder.client;\n");
      code.append("public interface UiBinder<U, O> {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource UI_FACTORY = new MockJavaResource(
      "com.google.gwt.uibinder.client.UiFactory") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.uibinder.client;\n");
      code.append("import java.lang.annotation.Target;\n");
      // code.append("@Target(ElementType.METHOD)");
      code.append("public @interface UiFactory {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource UI_FIELD = new MockJavaResource(
      "com.google.gwt.uibinder.client.UiField") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.uibinder.client;");
      code.append("import java.lang.annotation.Documented;");
      code.append("import java.lang.annotation.ElementType;");
      code.append("import java.lang.annotation.Retention;");
      code.append("import java.lang.annotation.RetentionPolicy;");
      code.append("import java.lang.annotation.Target;");
      code.append("@Documented");
      code.append("@Retention(RetentionPolicy.RUNTIME)");
      code.append("@Target(ElementType.FIELD)");
      code.append("public @interface UiField {");
      code.append("  boolean provided() default false;");
      code.append("}");
      return code;
    }
  };
  public static final MockJavaResource UI_HANDLER = new MockJavaResource(
      "com.google.gwt.uibinder.client.UiHandler") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.uibinder.client;");
      code.append("import java.lang.annotation.Documented;");
      code.append("import java.lang.annotation.ElementType;");
      code.append("import java.lang.annotation.Retention;");
      code.append("import java.lang.annotation.RetentionPolicy;");
      code.append("import java.lang.annotation.Target;");
      code.append("@Documented");
      code.append("@Retention(RetentionPolicy.RUNTIME)");
      code.append("@Target(ElementType.METHOD)");
      code.append("public @interface UiHandler {");
      code.append("String[] value();");
      code.append("}");
      return code;
    }
  };
  public static final MockJavaResource UI_OBJECT = new MockJavaResource(
      "com.google.gwt.user.client.ui.UIObject") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class UIObject {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource UI_RENDERER = new MockJavaResource(
      "com.google.gwt.uibinder.client.UiRenderer") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.uibinder.client;\n");
      code.append("public interface UiRenderer {");
      code.append("}");
      return code;
    }
  };
  public static final MockJavaResource VALUE_LABEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.ValueLabel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("import com.google.gwt.text.shared.Renderer;\n");
      code.append("public class ValueLabel extends Widget {\n");
      code.append("  public ValueLabel(Renderer renderer) {} ");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource WIDGET = new MockJavaResource(
      "com.google.gwt.user.client.ui.Widget") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class Widget extends UIObject implements IsWidget {\n");
      code.append("  public Widget asWidget() { return this; }");
      code.append("}\n");
      return code;
    }
  };

  /**
   * Returns a pale reflection of com.google.gwt.user.ui, plus
   * {@link JavaResourceBase#getStandardResources}.
   */
  public static Set<Resource> getUiResources() {
    Set<Resource> rtn = new HashSet<Resource>(
        Arrays.asList(JavaResourceBase.getStandardResources()));
    rtn.add(ABSOLUTE_PANEL);
    rtn.add(BUTTON);
    rtn.add(CLICK_EVENT);
    rtn.add(CLICK_HANDLER);
    rtn.add(COMMAND);
    rtn.add(CURRENCY_DATA);
    rtn.add(DATE_LABEL);
    rtn.add(DATE_TIME_FORMAT);
    rtn.add(DATE_TIME_FORMAT_OLD);
    rtn.add(DIALOG_BOX);
    rtn.add(DISCLOSURE_PANEL);
    rtn.add(DOM_EVENT);
    rtn.add(DOCK_LAYOUT_PANEL);
    rtn.add(ELEMENT);
    rtn.add(EVENT_HANDLER);
    rtn.add(GRID);
    rtn.add(GWT_EVENT);
    rtn.add(IMAGE);
    rtn.add(IMAGE_RESOURCE);
    rtn.add(IS_WIDGET);
    rtn.add(HANDLER_REGISTRATION);
    rtn.add(HAS_CLICK_HANDLERS);
    rtn.add(HAS_HORIZONTAL_ALIGNMENT);
    rtn.add(HAS_VERTICAL_ALIGNMENT);
    rtn.add(HTML_PANEL);
    rtn.add(LABEL);
    rtn.add(LAYOUT_PANEL);
    rtn.add(LIST_BOX);
    rtn.add(MENU_BAR);
    rtn.add(MENU_ITEM);
    rtn.add(MENU_ITEM_SEPARATOR);
    rtn.add(MOUSE_OVER_EVENT);
    rtn.add(MOUSE_OVER_HANDLER);
    rtn.add(NATIVE_EVENT);
    rtn.add(NUMBER_LABEL);
    rtn.add(NUMBER_FORMAT);
    rtn.add(RENDERER);
    rtn.add(SAFE_HTML);
    rtn.add(SAFE_HTML_BUILDER);
    rtn.add(SAFE_URI);
    rtn.add(SPLIT_LAYOUT_PANEL);
    rtn.add(STACK_LAYOUT_PANEL);
    rtn.add(STACK_PANEL);
    rtn.add(STYLE);
    rtn.add(TAB_LAYOUT_PANEL);
    rtn.add(TAB_PANEL);
    rtn.add(TEXT_BOX_BASE);
    rtn.add(TIME_ZONE);
    rtn.add(TIME_ZONE_OLD);
    rtn.add(TREE);
    rtn.add(TREE_ITEM);
    rtn.add(UI_OBJECT);
    rtn.add(UI_BINDER);
    rtn.add(UI_FACTORY);
    rtn.add(UI_FIELD);
    rtn.add(UI_HANDLER);
    rtn.add(UI_RENDERER);
    rtn.add(VALUE_LABEL);
    rtn.add(WIDGET);
    return rtn;
  }
}
