/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user;

import com.google.gwt.user.client.ui.AbsolutePanelTest;
import com.google.gwt.user.client.ui.CheckBoxTest;
import com.google.gwt.user.client.ui.CustomButtonTest;
import com.google.gwt.user.client.ui.DOMTest;
import com.google.gwt.user.client.ui.DialogBoxTest;
import com.google.gwt.user.client.ui.DisclosurePanelTest;
import com.google.gwt.user.client.ui.DockPanelTest;
import com.google.gwt.user.client.ui.FastStringMapTest;
import com.google.gwt.user.client.ui.FlexTableTest;
import com.google.gwt.user.client.ui.FlowPanelTest;
import com.google.gwt.user.client.ui.FocusPanelTest;
import com.google.gwt.user.client.ui.GridTest;
import com.google.gwt.user.client.ui.HTMLPanelTest;
import com.google.gwt.user.client.ui.HiddenTest;
import com.google.gwt.user.client.ui.HorizontalPanelTest;
import com.google.gwt.user.client.ui.HyperlinkTest;
import com.google.gwt.user.client.ui.ImageTest;
import com.google.gwt.user.client.ui.LinearPanelTest;
import com.google.gwt.user.client.ui.ListBoxTest;
import com.google.gwt.user.client.ui.MenuBarTest;
import com.google.gwt.user.client.ui.NamedFrameTest;
import com.google.gwt.user.client.ui.PanelTest;
import com.google.gwt.user.client.ui.PopupTest;
import com.google.gwt.user.client.ui.PrefixTreeTest;
import com.google.gwt.user.client.ui.RadioButtonTest;
import com.google.gwt.user.client.ui.ScrollPanelTest;
import com.google.gwt.user.client.ui.SplitPanelTest;
import com.google.gwt.user.client.ui.StackPanelTest;
import com.google.gwt.user.client.ui.TabBarTest;
import com.google.gwt.user.client.ui.TabPanelTest;
import com.google.gwt.user.client.ui.TextAreaTest;
import com.google.gwt.user.client.ui.TitledPanelTest;
import com.google.gwt.user.client.ui.TreeTest;
import com.google.gwt.user.client.ui.UIObjectTest;
import com.google.gwt.user.client.ui.VerticalPanelTest;
import com.google.gwt.user.client.ui.WidgetCollectionTest;
import com.google.gwt.user.client.ui.WidgetIteratorsTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TODO: document me.
 */
public class UISuite {
  public static Test suite() {
    TestSuite suite = new TestSuite(
        "Test for suite for the com.google.gwt.ui module");

    suite.addTestSuite(AbsolutePanelTest.class);
    suite.addTestSuite(CheckBoxTest.class);
    suite.addTestSuite(CustomButtonTest.class);
    suite.addTestSuite(DialogBoxTest.class);
    suite.addTestSuite(DisclosurePanelTest.class);
    suite.addTestSuite(DockPanelTest.class);
    suite.addTestSuite(DOMTest.class);
    suite.addTestSuite(FastStringMapTest.class);
    suite.addTestSuite(FlexTableTest.class);
    suite.addTestSuite(FlowPanelTest.class);
    suite.addTestSuite(FocusPanelTest.class);
    // suite.addTestSuite(FormPanelTest.class);
    suite.addTestSuite(GridTest.class);
    suite.addTestSuite(HiddenTest.class);
    // suite.addTestSuite(HistoryTest.class);
    suite.addTestSuite(HorizontalPanelTest.class);
    suite.addTestSuite(HTMLPanelTest.class);
    suite.addTestSuite(HyperlinkTest.class);
    suite.addTestSuite(ImageTest.class);
    suite.addTestSuite(LinearPanelTest.class);
    suite.addTestSuite(ListBoxTest.class);
    suite.addTestSuite(MenuBarTest.class);
    suite.addTestSuite(NamedFrameTest.class);
    suite.addTestSuite(PanelTest.class);
    suite.addTestSuite(PopupTest.class);
    suite.addTestSuite(PrefixTreeTest.class);
    suite.addTestSuite(RadioButtonTest.class);
    suite.addTestSuite(ScrollPanelTest.class);
    suite.addTestSuite(SplitPanelTest.class);
    suite.addTestSuite(StackPanelTest.class);
    suite.addTestSuite(TabBarTest.class);
    suite.addTestSuite(TabPanelTest.class);
    suite.addTestSuite(TextAreaTest.class);
    suite.addTestSuite(TitledPanelTest.class);
    suite.addTestSuite(TreeTest.class);
    suite.addTestSuite(UIObjectTest.class);
    suite.addTestSuite(VerticalPanelTest.class);
    suite.addTestSuite(WidgetCollectionTest.class);
    suite.addTestSuite(WidgetIteratorsTest.class);

    return suite;
  }
}
