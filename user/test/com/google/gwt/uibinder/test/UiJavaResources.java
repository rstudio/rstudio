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

  public static final MockJavaResource DIALOG_BOX = new MockJavaResource(
      "com.google.gwt.user.client.ui.DialogBox") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class DialogBox extends Widget {\n");
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
  public static final MockJavaResource LABEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.Label") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class Label extends Widget {\n");
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
      code.append("  public enum Unit { PX, PT, EM };\n");
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
    rtn.add(DIALOG_BOX);
    rtn.add(DOCK_LAYOUT_PANEL);
    rtn.add(HAS_HORIZONTAL_ALIGNMENT);
    rtn.add(HAS_VERTICAL_ALIGNMENT);
    rtn.add(LABEL);
    rtn.add(SPLIT_LAYOUT_PANEL);
    rtn.add(STACK_LAYOUT_PANEL);
    rtn.add(STYLE);
    rtn.add(TAB_LAYOUT_PANEL);
    rtn.add(UI_OBJECT);
    rtn.add(UI_BINDER);
    rtn.add(WIDGET);
    return rtn;
  }
}
