/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.user.client.ui.HTMLPanelTest;
import com.google.gwt.user.client.ui.HTMLTest;
import com.google.gwt.user.client.ui.HeaderPanelTest;
import com.google.gwt.user.client.ui.HiddenTest;
import com.google.gwt.user.client.ui.HistoryTest;
import com.google.gwt.user.client.ui.HorizontalPanelTest;
import com.google.gwt.user.client.ui.HorizontalSplitPanelTest;
import com.google.gwt.user.client.ui.HyperlinkTest;
import com.google.gwt.user.client.ui.ImageTest;
import com.google.gwt.user.client.ui.InlineHTMLTest;
import com.google.gwt.user.client.ui.InlineHyperlinkTest;
import com.google.gwt.user.client.ui.IsWidgetTest;
import com.google.gwt.user.client.ui.LabelTest;
import com.google.gwt.user.client.ui.LazyPanelTest;
import com.google.gwt.user.client.ui.LinearPanelTest;
import com.google.gwt.user.client.ui.ListBoxTest;
import com.google.gwt.user.client.ui.MenuBarTest;
import com.google.gwt.user.client.ui.MenuItemTest;
import com.google.gwt.user.client.ui.NamedFrameTest;
import com.google.gwt.user.client.ui.NativeHorizontalScrollbarTest;
import com.google.gwt.user.client.ui.NativeVerticalScrollbarTest;
import com.google.gwt.user.client.ui.PopupTest;
import com.google.gwt.user.client.ui.PrefixTreeTest;
import com.google.gwt.user.client.ui.RadioButtonTest;
import com.google.gwt.user.client.ui.ResetButtonTest;
import com.google.gwt.user.client.ui.ResizeLayoutPanelTest;
import com.google.gwt.user.client.ui.RichTextAreaTest;
import com.google.gwt.user.client.ui.RootPanelTest;
import com.google.gwt.user.client.ui.ScrollPanelTest;
import com.google.gwt.user.client.ui.SimpleCheckBoxTest;
import com.google.gwt.user.client.ui.SimpleLayoutPanelTest;
import com.google.gwt.user.client.ui.SimplePanelTest;
import com.google.gwt.user.client.ui.SimpleRadioButtonTest;
import com.google.gwt.user.client.ui.SplitLayoutPanelTest;
import com.google.gwt.user.client.ui.StackLayoutPanelTest;
import com.google.gwt.user.client.ui.StackPanelTest;
import com.google.gwt.user.client.ui.SubmitButtonTest;
import com.google.gwt.user.client.ui.SuggestBoxTest;
import com.google.gwt.user.client.ui.TabBarTest;
import com.google.gwt.user.client.ui.TabLayoutPanelTest;
import com.google.gwt.user.client.ui.TabPanelTest;
import com.google.gwt.user.client.ui.TextAreaTest;
import com.google.gwt.user.client.ui.TreeItemTest;
import com.google.gwt.user.client.ui.TreeTest;
import com.google.gwt.user.client.ui.UIObjectTest;
import com.google.gwt.user.client.ui.ValueBoxBaseTest;
import com.google.gwt.user.client.ui.ValueListBoxTest;
import com.google.gwt.user.client.ui.VerticalPanelTest;
import com.google.gwt.user.client.ui.VerticalSplitPanelTest;
import com.google.gwt.user.client.ui.WidgetCollectionTest;
import com.google.gwt.user.client.ui.WidgetIteratorsTest;
import com.google.gwt.user.client.ui.WidgetOnLoadTest;
import com.google.gwt.user.client.ui.WidgetSubclassingTest;
import com.google.gwt.user.client.ui.WidgetTest;

import junit.framework.Test;

/**
 * Tests in the user.client.ui package that start with H-Z.
 * @see UiPart1Suite
 */
public class UiPart2Suite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test for suite for all user widgets");

    suite.addTestSuite(HeaderPanelTest.class);
    suite.addTestSuite(HiddenTest.class);
    suite.addTestSuite(HistoryTest.class);
    suite.addTestSuite(HorizontalPanelTest.class);
    suite.addTestSuite(HorizontalSplitPanelTest.class);
    suite.addTestSuite(HTMLPanelTest.class);
    suite.addTestSuite(HTMLTest.class);
    suite.addTestSuite(HyperlinkTest.class);
    suite.addTestSuite(ImageTest.class);
    suite.addTestSuite(InlineHTMLTest.class);
    suite.addTestSuite(InlineHyperlinkTest.class);
    suite.addTestSuite(IsWidgetTest.class);
    suite.addTestSuite(LabelTest.class);
    suite.addTestSuite(LazyPanelTest.class);
    suite.addTestSuite(LinearPanelTest.class);
    suite.addTestSuite(ListBoxTest.class);
    suite.addTestSuite(MenuBarTest.class);
    suite.addTestSuite(MenuItemTest.class);
    suite.addTestSuite(NamedFrameTest.class);
    suite.addTestSuite(NativeHorizontalScrollbarTest.class);
    suite.addTestSuite(NativeVerticalScrollbarTest.class);
    suite.addTestSuite(PopupTest.class);
    suite.addTestSuite(PrefixTreeTest.class);
    suite.addTestSuite(RadioButtonTest.class);
    suite.addTestSuite(ResetButtonTest.class);
    suite.addTestSuite(ResizeLayoutPanelTest.class);
    suite.addTestSuite(RichTextAreaTest.class);
    suite.addTestSuite(RootPanelTest.class);
    suite.addTestSuite(ScrollPanelTest.class);
    suite.addTestSuite(SimpleCheckBoxTest.class);
    suite.addTestSuite(SimpleRadioButtonTest.class);
    suite.addTestSuite(SimplePanelTest.class);
    suite.addTestSuite(SimpleLayoutPanelTest.class);
    suite.addTestSuite(SplitLayoutPanelTest.class);
    suite.addTestSuite(StackLayoutPanelTest.class);
    suite.addTestSuite(StackPanelTest.class);
    suite.addTestSuite(SubmitButtonTest.class);
    suite.addTestSuite(SuggestBoxTest.class);
    suite.addTestSuite(TabBarTest.class);
    suite.addTestSuite(TabLayoutPanelTest.class);
    suite.addTestSuite(TabPanelTest.class);
    suite.addTestSuite(TextAreaTest.class);
    suite.addTestSuite(TreeTest.class);
    suite.addTestSuite(TreeItemTest.class);
    suite.addTestSuite(UIObjectTest.class);
    suite.addTestSuite(ValueBoxBaseTest.class);
    suite.addTestSuite(ValueListBoxTest.class);
    suite.addTestSuite(VerticalPanelTest.class);
    suite.addTestSuite(VerticalSplitPanelTest.class);
    suite.addTestSuite(WidgetCollectionTest.class);
    suite.addTestSuite(WidgetIteratorsTest.class);
    suite.addTestSuite(WidgetOnLoadTest.class);
    suite.addTestSuite(WidgetSubclassingTest.class);
    suite.addTestSuite(WidgetTest.class);
    return suite;
  }
}
