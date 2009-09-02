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
package com.google.gwt.user;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.user.client.AsyncProxyTest;
import com.google.gwt.user.client.CommandExecutorTest;
import com.google.gwt.user.client.CookieTest;
import com.google.gwt.user.client.EventTest;
import com.google.gwt.user.client.WindowTest;
import com.google.gwt.user.client.ui.AbsolutePanelTest;
import com.google.gwt.user.client.ui.AnchorTest;
import com.google.gwt.user.client.ui.ButtonTest;
import com.google.gwt.user.client.ui.CaptionPanelTest;
import com.google.gwt.user.client.ui.CheckBoxTest;
import com.google.gwt.user.client.ui.CompositeTest;
import com.google.gwt.user.client.ui.CreateEventTest;
import com.google.gwt.user.client.ui.CustomButtonTest;
import com.google.gwt.user.client.ui.DOMRtlTest;
import com.google.gwt.user.client.ui.DOMTest;
import com.google.gwt.user.client.ui.DateBoxTest;
import com.google.gwt.user.client.ui.DatePickerTest;
import com.google.gwt.user.client.ui.DeckPanelTest;
import com.google.gwt.user.client.ui.DecoratedPopupTest;
import com.google.gwt.user.client.ui.DecoratedStackPanelTest;
import com.google.gwt.user.client.ui.DecoratedTabBarTest;
import com.google.gwt.user.client.ui.DecoratedTabPanelTest;
import com.google.gwt.user.client.ui.DecoratorPanelTest;
import com.google.gwt.user.client.ui.DelegatingKeyboardListenerCollectionTest;
import com.google.gwt.user.client.ui.DialogBoxTest;
import com.google.gwt.user.client.ui.DisclosurePanelTest;
import com.google.gwt.user.client.ui.DockPanelTest;
import com.google.gwt.user.client.ui.ElementWrappingTest;
import com.google.gwt.user.client.ui.FastStringMapTest;
import com.google.gwt.user.client.ui.FileUploadTest;
import com.google.gwt.user.client.ui.FlexTableTest;
import com.google.gwt.user.client.ui.FlowPanelTest;
import com.google.gwt.user.client.ui.FocusPanelTest;
import com.google.gwt.user.client.ui.GridTest;
import com.google.gwt.user.client.ui.HTMLPanelTest;
import com.google.gwt.user.client.ui.HiddenTest;
import com.google.gwt.user.client.ui.HistoryTest;
import com.google.gwt.user.client.ui.HorizontalPanelTest;
import com.google.gwt.user.client.ui.HyperlinkTest;
import com.google.gwt.user.client.ui.ImageTest;
import com.google.gwt.user.client.ui.LazyPanelTest;
import com.google.gwt.user.client.ui.LinearPanelTest;
import com.google.gwt.user.client.ui.ListBoxTest;
import com.google.gwt.user.client.ui.MenuBarTest;
import com.google.gwt.user.client.ui.NamedFrameTest;
import com.google.gwt.user.client.ui.PanelTest;
import com.google.gwt.user.client.ui.PopupTest;
import com.google.gwt.user.client.ui.PrefixTreeTest;
import com.google.gwt.user.client.ui.RadioButtonTest;
import com.google.gwt.user.client.ui.RichTextAreaTest;
import com.google.gwt.user.client.ui.RootPanelTest;
import com.google.gwt.user.client.ui.ScrollPanelTest;
import com.google.gwt.user.client.ui.SimpleCheckBoxTest;
import com.google.gwt.user.client.ui.SimpleRadioButtonTest;
import com.google.gwt.user.client.ui.SplitPanelTest;
import com.google.gwt.user.client.ui.StackPanelTest;
import com.google.gwt.user.client.ui.SuggestBoxTest;
import com.google.gwt.user.client.ui.TabBarTest;
import com.google.gwt.user.client.ui.TabPanelTest;
import com.google.gwt.user.client.ui.TextAreaTest;
import com.google.gwt.user.client.ui.TreeItemTest;
import com.google.gwt.user.client.ui.TreeTest;
import com.google.gwt.user.client.ui.UIObjectTest;
import com.google.gwt.user.client.ui.VerticalPanelTest;
import com.google.gwt.user.client.ui.WidgetCollectionTest;
import com.google.gwt.user.client.ui.WidgetIteratorsTest;
import com.google.gwt.user.client.ui.WidgetOnLoadTest;
import com.google.gwt.user.client.ui.WidgetSubclassingTest;
import com.google.gwt.user.client.ui.WidgetTest;
import com.google.gwt.user.client.ui.impl.ClippedImagePrototypeTest;
import com.google.gwt.user.datepicker.client.CalendarUtilTest;
import com.google.gwt.user.datepicker.client.DateChangeEventTest;
import com.google.gwt.user.rebind.ui.ImageBundleGeneratorTest;
import com.google.gwt.xml.client.XMLTest;

import junit.framework.Test;

/**
 * Tests of the ui package.
 */
public class UISuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test for suite for all user widgets");

    suite.addTestSuite(AbsolutePanelTest.class);
    suite.addTestSuite(AnchorTest.class);
    suite.addTestSuite(AsyncProxyTest.class);
    suite.addTestSuite(ButtonTest.class);
    suite.addTestSuite(CaptionPanelTest.class);
    suite.addTestSuite(CheckBoxTest.class);
    suite.addTestSuite(ClippedImagePrototypeTest.class);
    suite.addTestSuite(CommandExecutorTest.class);
    suite.addTestSuite(CompositeTest.class);
    suite.addTestSuite(CookieTest.class);
    suite.addTestSuite(CustomButtonTest.class);
    suite.addTestSuite(CalendarUtilTest.class);
    suite.addTestSuite(DateBoxTest.class);
    suite.addTestSuite(DatePickerTest.class);
    suite.addTestSuite(DeckPanelTest.class);
    suite.addTestSuite(DecoratedPopupTest.class);
    suite.addTestSuite(DecoratedStackPanelTest.class);
    suite.addTestSuite(DecoratedTabBarTest.class);
    suite.addTestSuite(DecoratedTabPanelTest.class);
    suite.addTestSuite(DecoratorPanelTest.class);
    suite.addTestSuite(DelegatingKeyboardListenerCollectionTest.class);
    suite.addTestSuite(DialogBoxTest.class);
    suite.addTestSuite(DisclosurePanelTest.class);
    suite.addTestSuite(DockPanelTest.class);
    suite.addTestSuite(DOMTest.class);
    suite.addTestSuite(DOMRtlTest.class);
    suite.addTestSuite(ElementWrappingTest.class);
    suite.addTestSuite(EventTest.class);
    suite.addTestSuite(FastStringMapTest.class);
    suite.addTestSuite(FileUploadTest.class);
    suite.addTestSuite(FlexTableTest.class);
    suite.addTestSuite(FlowPanelTest.class);
    suite.addTestSuite(FocusPanelTest.class);
    // Old Mozilla complains about the cross-site forms in FormPanelTest.
    // suite.addTestSuite(FormPanelTest.class);
    suite.addTestSuite(GridTest.class);
    suite.addTestSuite(HiddenTest.class);
    suite.addTestSuite(HistoryTest.class);
    suite.addTestSuite(HorizontalPanelTest.class);
    suite.addTestSuite(HTMLPanelTest.class);
    suite.addTestSuite(HyperlinkTest.class);
    suite.addTestSuite(ImageBundleGeneratorTest.class);
    suite.addTestSuite(ImageTest.class);
    suite.addTestSuite(LazyPanelTest.class);
    suite.addTestSuite(LinearPanelTest.class);
    suite.addTestSuite(ListBoxTest.class);
    suite.addTestSuite(MenuBarTest.class);
    suite.addTestSuite(NamedFrameTest.class);
    suite.addTestSuite(PanelTest.class);
    suite.addTestSuite(PopupTest.class);
    suite.addTestSuite(PrefixTreeTest.class);
    suite.addTestSuite(RadioButtonTest.class);
    suite.addTestSuite(RichTextAreaTest.class);
    suite.addTestSuite(ScrollPanelTest.class);
    suite.addTestSuite(SimpleCheckBoxTest.class);
    suite.addTestSuite(SimpleRadioButtonTest.class);
    suite.addTestSuite(SplitPanelTest.class);
    suite.addTestSuite(StackPanelTest.class);
    suite.addTestSuite(SuggestBoxTest.class);
    suite.addTestSuite(TabBarTest.class);
    suite.addTestSuite(TabPanelTest.class);
    suite.addTestSuite(TextAreaTest.class);
    suite.addTestSuite(TreeTest.class);
    suite.addTestSuite(TreeItemTest.class);
    suite.addTestSuite(UIObjectTest.class);
    suite.addTestSuite(VerticalPanelTest.class);
    suite.addTestSuite(WidgetCollectionTest.class);
    suite.addTestSuite(WidgetIteratorsTest.class);
    suite.addTestSuite(WidgetOnLoadTest.class);
    suite.addTestSuite(WidgetSubclassingTest.class);
    suite.addTestSuite(WindowTest.class);
    suite.addTestSuite(XMLTest.class);
    suite.addTestSuite(ClassInitTest.class);
    suite.addTestSuite(DateChangeEventTest.class);
    suite.addTestSuite(CreateEventTest.class);
    suite.addTestSuite(WidgetTest.class);
    suite.addTestSuite(RootPanelTest.class);
    return suite;
  }
}
