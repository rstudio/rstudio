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

import com.google.gwt.dev.javac.impl.JavaResourceBase;
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.collect.HashSet;

import java.util.Arrays;
import java.util.Set;

/**
 * A pared down, very low fidelity set of GWT widget Java source files for code
 * generator testing.
 */
public class UiJavaResources {

  public static final MockJavaResource BUTTON = new MockJavaResource(
      "com.google.gwt.user.client.ui.Button") {
    @Override
    protected CharSequence getContent() {
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
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.dom.client;\n");
      code.append("import com.google.gwt.event.shared.GwtEvent;\n");
      code.append("public class ClickEvent extends GwtEvent<ClickHandler> {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource CLICK_HANDLER = new MockJavaResource(
      "com.google.gwt.event.dom.client.ClickHandler") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.dom.client;\n");
      code.append("import com.google.gwt.event.shared.EventHandler;\n");
      code.append("public interface ClickHandler extends EventHandler {\n");
      code.append("  void onClick(ClickEvent event);\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource DIALOG_BOX = new MockJavaResource(
      "com.google.gwt.user.client.ui.DialogBox") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class DialogBox extends Widget {\n");
      code.append("  public DialogBox(boolean autoHide, boolean modal) {} ");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource DOCK_LAYOUT_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.DockLayoutPanel") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class DockLayoutPanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource EVENT_HANDLER = new MockJavaResource(
      "com.google.gwt.event.shared.EventHandler") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.shared;\n");
      code.append("public interface EventHandler {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource GWT_EVENT = new MockJavaResource(
      "com.google.gwt.event.shared.GwtEvent") {
    @Override
    protected CharSequence getContent() {
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
    protected CharSequence getContent() {
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
    protected CharSequence getContent() {
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
    protected CharSequence getContent() {
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
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class HasVerticalAlignment {\n");
      code.append("  public static class VerticalAlignmentConstant {\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource IMAGE = new MockJavaResource(
      "com.google.gwt.user.client.ui.Image") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class Image extends Widget {\n");
      code.append("  public Image() {} ");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource IMAGE_RESOURCE = new MockJavaResource(
      "com.google.gwt.resources.client.ImageResource") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.resources.client;\n");
      code.append("public class ImageResource  {\n");
      code.append("  public ImageResource() {} ");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource LABEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.Label") {
    @Override
    protected CharSequence getContent() {
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
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class LayoutPanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource MOUSE_OVER_EVENT = new MockJavaResource(
      "com.google.gwt.event.dom.client.MouseOverEvent") {
    @Override
    protected CharSequence getContent() {
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
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.event.dom.client;\n");
      code.append("import com.google.gwt.event.shared.EventHandler;\n");
      code.append("public interface MouseOverHandler extends EventHandler {\n");
      code.append("  void onMouseOver(MouseOverEvent event);\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource SPLIT_LAYOUT_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.SplitLayoutPanel") {
    @Override
    protected CharSequence getContent() {
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
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class StackLayoutPanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource STYLE = new MockJavaResource(
      "com.google.gwt.dom.client.Style") {
    @Override
    protected CharSequence getContent() {
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
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class TabLayoutPanel extends Widget {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource UI_BINDER = new MockJavaResource(
      "com.google.gwt.uibinder.client.UiBinder") {
    @Override
    protected CharSequence getContent() {
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
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.uibinder.client;\n");
      code.append("import java.lang.annotation.Target;\n");
      // code.append("@Target(ElementType.METHOD)");
      code.append("public @interface UiFactory {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource UI_OBJECT = new MockJavaResource(
      "com.google.gwt.user.client.ui.UIObject") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class UIObject {\n");
      code.append("}\n");
      return code;
    }
  };
  public static final MockJavaResource WIDGET = new MockJavaResource(
      "com.google.gwt.user.client.ui.Widget") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class Widget extends UIObject {\n");
      code.append("}\n");
      return code;
    }
  };

  /**
   * @return a pale reflection of com.google.gwt.user.ui, plus
   *         {@link JavaResourceBase#getStandardResources}
   */
  public static Set<Resource> getUiResources() {
    Set<Resource> rtn = new HashSet<Resource>(
        Arrays.asList(JavaResourceBase.getStandardResources()));
    rtn.add(BUTTON);
    rtn.add(CLICK_EVENT);
    rtn.add(CLICK_HANDLER);
    rtn.add(DIALOG_BOX);
    rtn.add(DOCK_LAYOUT_PANEL);
    rtn.add(EVENT_HANDLER);
    rtn.add(GWT_EVENT);
    rtn.add(IMAGE);
    rtn.add(IMAGE_RESOURCE);
    rtn.add(HANDLER_REGISTRATION);
    rtn.add(HAS_CLICK_HANDLERS);
    rtn.add(HAS_HORIZONTAL_ALIGNMENT);
    rtn.add(HAS_VERTICAL_ALIGNMENT);
    rtn.add(LABEL);
    rtn.add(LAYOUT_PANEL);
    rtn.add(MOUSE_OVER_EVENT);
    rtn.add(MOUSE_OVER_HANDLER);
    rtn.add(SPLIT_LAYOUT_PANEL);
    rtn.add(STACK_LAYOUT_PANEL);
    rtn.add(STYLE);
    rtn.add(TAB_LAYOUT_PANEL);
    rtn.add(UI_OBJECT);
    rtn.add(UI_BINDER);
    rtn.add(UI_FACTORY);
    rtn.add(WIDGET);
    return rtn;
  }
}
