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
package com.google.gwt.uibinder.sample.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DListElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.OListElement;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.Shared;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.Widget;

/**
 * Sample use of a {@code UiBinder} with the com.google.gwt.user Widget set, and
 * custom widgets.
 */
public class WidgetBasedUi extends Composite {
  /**
   * This CssResource is a requirement of the WidgetBasedUi, to be provided by
   * its ui.xml template.
   */
  @Shared
  public interface Style extends CssResource {
    String menuBar();
  }
  
  interface Binder extends UiBinder<Widget, WidgetBasedUi> {
  }
  private static final Binder binder = GWT.create(Binder.class);

  private static boolean stylesInjected = false;

  @UiField(provided = true)
  final WidgetBasedUiExternalResources external;

  @UiField(provided = true)
  final Label bundledLabel;

  @UiField Style myStyle;
  @UiField ClickyLink customLinkWidget;
  @UiField PointlessRadioButtonSubclass emptyRadio;
  @UiField ClickyLink funnyCharsAttributeWidget;
  @UiField ParagraphElement funnyCharsDomAttributeParagraph;
  @UiField ClickyLink funnyCharsMessageAttributeWidget;
  @UiField ParagraphElement funnyCharsMessageDomAttributeParagraph;
  @UiField ParagraphElement funnyCharsMessageParagraph;
  @UiField SpanElement funnyCharsMessageChildSpan;
  @UiField ParagraphElement funnyCharsParagraph;
  @UiField ParagraphElement funnyCharsProtectedMessageParagraph;
  @UiField Label gwtFieldLabel;
  @UiField ParagraphElement main;
  @UiField Button myButton;
  @UiField RadioButton myRadioAble;
  @UiField RadioButton myRadioBaker;
  @UiField StackPanel myStackPanel;
  @UiField Widget myStackPanelItem;
  @UiField DisclosurePanel myDisclosurePanel;
  @UiField Widget myDisclosurePanelItem;
  @UiField Tree myTree;
  @UiField Element nonStandardElement;
  @UiField DockPanel root;
  @UiField DivElement sideBar;
  @UiField SpanElement spanInMsg;
  @UiField Element tmElement;
  @UiField Element tmElementJr;
  @UiField SpanElement trimmedMessage;
  @UiField NeedlesslyAnnotatedLabel needlessLabel;
  @UiField AnnotatedStrictLabel strictLabel;
  @UiField AnnotatedStrictLabel translatedStrictLabel;
  @UiField StrictLabel veryStrictLabel;
  @UiField StrictLabel translatedVeryStrictLabel;
  @UiField FooLabel theFoo;
  @UiField MenuBar dropdownMenuBar;
  @UiField MenuItem menuItemMop;
  @UiField MenuItem menuItemLegacy;
  @UiField SpanElement messageInMain;
  @UiField TableElement widgetCrazyTable;
  @UiField OListElement widgetCrazyOrderedList;
  @UiField DListElement widgetCrazyDefinitionList;
  @UiField HTMLPanel customTagHtmlPanel;
  @UiField ParagraphElement privateStyleParagraph;
  @UiField ParagraphElement reallyPrivateStyleParagraph;
  @UiField SpanElement totallyPrivateStyleSpan;
  @UiField ImageResource prettyImage;
  @UiField ImageResource prettyTilingImage;
  @UiField Image babyWidget;
  @UiField ParagraphElement simpleSpriteParagraph;
  @UiField DataResource heartCursorResource;
  @UiField CssImportScopeSample cssImportScopeSample;

  public WidgetBasedUi() {
    this.bundledLabel = new Label();
    this.external = GWT.create(WidgetBasedUiExternalResources.class);

    // Inject only once.
    if (!stylesInjected) {
      StyleInjector.injectStylesheet(external.style().getText());
      stylesInjected = true;
    }

    initWidget(binder.createAndBindUi(this));
  }

  @UiFactory
  StrictLabel createStrictLabel(String text) {
    return new StrictLabel(text);
  }
}
