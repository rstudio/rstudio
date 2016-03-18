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
package com.google.gwt.uibinder;

import com.google.gwt.uibinder.attributeparsers.CssNameConverterTest;
import com.google.gwt.uibinder.attributeparsers.FieldReferenceConverterTest;
import com.google.gwt.uibinder.attributeparsers.HorizontalAlignmentConstantParserTest;
import com.google.gwt.uibinder.attributeparsers.IntAttributeParserTest;
import com.google.gwt.uibinder.attributeparsers.IntPairAttributeParserTest;
import com.google.gwt.uibinder.attributeparsers.LengthAttributeParserTest;
import com.google.gwt.uibinder.attributeparsers.SafeUriAttributeParserTest;
import com.google.gwt.uibinder.attributeparsers.StrictAttributeParserTest;
import com.google.gwt.uibinder.attributeparsers.StringAttributeParserTest;
import com.google.gwt.uibinder.attributeparsers.TextAlignConstantParserTest;
import com.google.gwt.uibinder.attributeparsers.VerticalAlignmentConstantParserTest;
import com.google.gwt.uibinder.elementparsers.AbsolutePanelParserTest;
import com.google.gwt.uibinder.elementparsers.DateLabelParserTest;
import com.google.gwt.uibinder.elementparsers.DialogBoxParserTest;
import com.google.gwt.uibinder.elementparsers.DisclosurePanelParserTest;
import com.google.gwt.uibinder.elementparsers.DockLayoutPanelParserTest;
import com.google.gwt.uibinder.elementparsers.GridParserTest;
import com.google.gwt.uibinder.elementparsers.HasTreeItemsParserTest;
import com.google.gwt.uibinder.elementparsers.ImageParserTest;
import com.google.gwt.uibinder.elementparsers.IsEmptyParserTest;
import com.google.gwt.uibinder.elementparsers.LayoutPanelParserTest;
import com.google.gwt.uibinder.elementparsers.ListBoxParserTest;
import com.google.gwt.uibinder.elementparsers.MenuBarParserTest;
import com.google.gwt.uibinder.elementparsers.MenuItemParserTest;
import com.google.gwt.uibinder.elementparsers.NumberLabelParserTest;
import com.google.gwt.uibinder.elementparsers.StackLayoutPanelParserTest;
import com.google.gwt.uibinder.elementparsers.StackPanelParserTest;
import com.google.gwt.uibinder.elementparsers.TabLayoutPanelParserTest;
import com.google.gwt.uibinder.elementparsers.TabPanelParserTest;
import com.google.gwt.uibinder.elementparsers.UIObjectParserTest;
import com.google.gwt.uibinder.elementparsers.UiChildParserTest;
import com.google.gwt.uibinder.rebind.DesignTimeUtilsTest;
import com.google.gwt.uibinder.rebind.FieldWriterOfExistingTypeTest;
import com.google.gwt.uibinder.rebind.FieldWriterOfGeneratedCssResourceTest;
import com.google.gwt.uibinder.rebind.FieldWriterOfLazyDomElementTest;
import com.google.gwt.uibinder.rebind.GwtResourceEntityResolverTest;
import com.google.gwt.uibinder.rebind.HandlerEvaluatorTest;
import com.google.gwt.uibinder.rebind.TokenatorTest;
import com.google.gwt.uibinder.rebind.TypeOracleUtilsTest;
import com.google.gwt.uibinder.rebind.UiBinderParserUiImportTest;
import com.google.gwt.uibinder.rebind.UiBinderParserUiWithTest;
import com.google.gwt.uibinder.rebind.UiRendererEventValidationTest;
import com.google.gwt.uibinder.rebind.UiRendererValidationTest;
import com.google.gwt.uibinder.rebind.XMLElementTest;
import com.google.gwt.uibinder.rebind.model.HtmlTemplatesTest;
import com.google.gwt.uibinder.rebind.model.OwnerClassTest;
import com.google.gwt.uibinder.rebind.model.OwnerFieldClassTest;
import com.google.gwt.uibinder.rebind.model.OwnerFieldTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Suite of UiBinder tests that require the JRE.
 */
public class UiBinderJreSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite("UiBinder tests that require the JRE");

    // rebind
    suite.addTestSuite(FieldWriterOfExistingTypeTest.class);
    suite.addTestSuite(FieldWriterOfGeneratedCssResourceTest.class);
    suite.addTestSuite(FieldWriterOfLazyDomElementTest.class);
    suite.addTestSuite(GwtResourceEntityResolverTest.class);
    suite.addTestSuite(HandlerEvaluatorTest.class);
    suite.addTestSuite(TokenatorTest.class);
    suite.addTestSuite(XMLElementTest.class);
    suite.addTestSuite(DesignTimeUtilsTest.class);
    suite.addTestSuite(TypeOracleUtilsTest.class);
    suite.addTestSuite(UiBinderParserUiWithTest.class);
    suite.addTestSuite(UiBinderParserUiImportTest.class);
    suite.addTestSuite(UiRendererEventValidationTest.class);
    suite.addTestSuite(UiRendererValidationTest.class);
    suite.addTestSuite(HtmlTemplatesTest.class);

    // model
    suite.addTestSuite(OwnerClassTest.class);
    suite.addTestSuite(OwnerFieldClassTest.class);
    suite.addTestSuite(OwnerFieldTest.class);

    // attributeparsers
    suite.addTestSuite(CssNameConverterTest.class);
    suite.addTestSuite(FieldReferenceConverterTest.class);
    suite.addTestSuite(IntAttributeParserTest.class);
    suite.addTestSuite(IntPairAttributeParserTest.class);
    suite.addTestSuite(HorizontalAlignmentConstantParserTest.class);
    suite.addTestSuite(LengthAttributeParserTest.class);
    suite.addTestSuite(SafeUriAttributeParserTest.class);
    suite.addTestSuite(StrictAttributeParserTest.class);
    suite.addTestSuite(StringAttributeParserTest.class);
    suite.addTestSuite(TextAlignConstantParserTest.class);
    suite.addTestSuite(VerticalAlignmentConstantParserTest.class);

    // elementparsers
    suite.addTestSuite(AbsolutePanelParserTest.class);
    suite.addTestSuite(DateLabelParserTest.class);
    suite.addTestSuite(DialogBoxParserTest.class);
    suite.addTestSuite(DisclosurePanelParserTest.class);
    suite.addTestSuite(DockLayoutPanelParserTest.class);
    suite.addTestSuite(GridParserTest.class);
    suite.addTestSuite(HasTreeItemsParserTest.class);
    suite.addTestSuite(ImageParserTest.class);
    suite.addTestSuite(IsEmptyParserTest.class);
    suite.addTestSuite(LayoutPanelParserTest.class);
    suite.addTestSuite(ListBoxParserTest.class);
    suite.addTestSuite(MenuBarParserTest.class);
    suite.addTestSuite(MenuItemParserTest.class);
    suite.addTestSuite(NumberLabelParserTest.class);
    suite.addTestSuite(StackLayoutPanelParserTest.class);
    suite.addTestSuite(StackPanelParserTest.class);
    suite.addTestSuite(TabLayoutPanelParserTest.class);
    suite.addTestSuite(TabPanelParserTest.class);
    suite.addTestSuite(UiChildParserTest.class);
    suite.addTestSuite(UIObjectParserTest.class);

    return suite;
  }

  private UiBinderJreSuite() {
  }
}
