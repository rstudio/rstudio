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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DListElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.OListElement;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Shared;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.text.client.DoubleRenderer;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DateLabel;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.DoubleBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.NamedFrame;
import com.google.gwt.user.client.ui.NumberLabel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.ValueLabel;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

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
  static class FakeBundle2 extends FakeBundle {
  }

  static class FakeBundle3 extends FakeBundle {
  }
  private static final Binder binder = GWT.create(Binder.class);

  @UiField(provided = true)
  final WidgetBasedUiExternalResources external = GWT.create(WidgetBasedUiExternalResources.class);

  public static final DateTimeFormat MY_DATE_FORMAT = DateTimeFormat.getFormat(PredefinedFormat.DATE_FULL);
  public static final NumberFormat MY_NUMBER_FORMAT = NumberFormat.getDecimalFormat();

  @UiField(provided = true)
  final Label bundledLabel =  new Label();

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
  @UiField HorizontalPanel myHorizontalPanel;
  @UiField Widget myStackPanelItem;
  @UiField DisclosurePanel myDisclosurePanel;
  @UiField Widget myDisclosurePanelItem;
  @UiField(provided = true)
  final DisclosurePanel myProvidedDisclosurePanel = new DisclosurePanel("Provided header text");
  @UiField Widget myProvidedDisclosurePanelItem;
  @UiField Tree myTree;
  @UiField TreeItem myTreeItemA;
  @UiField Widget myTreeWidgetB;
  @UiField TreeItem myTreeItemC;
  @UiField TreeItem myTreeItemCA;
  @UiField Widget myTreeWidgetCB;
  @UiField Element nonStandardElement;
  @UiField DockPanel root;
  @UiField Widget sideBarWidget;
  @UiField DivElement sideBar;
  @UiField SpanElement spanInMsg;
  @UiField Element tmElement;
  @UiField Element tmElementJr;
  @UiField SpanElement trimmedMessage;
  @UiField NeedlesslyAnnotatedLabel needlessLabel;
  @UiField AnnotatedStrictLabel strictLabel;
  @UiField(provided = true)
  final AnnotatedStrictLabel providedAnnotatedStrictLabel = new AnnotatedStrictLabel(
      "likewise");
  @UiField AnnotatedStrictLabel translatedStrictLabel;
  @UiField StrictLabel veryStrictLabel;
  @UiField StrictLabel translatedVeryStrictLabel;
  @UiField(provided = true) final StrictLabel providedStrictLabel = new StrictLabel("provided");
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
  @UiField FooImage fooImage;
  @UiField Image babyWidget;
  @UiField ParagraphElement simpleSpriteParagraph;
  @UiField DataResource heartCursorResource;
  @UiField CssImportScopeSample cssImportScopeSample;
  @UiField ParagraphElement bracedParagraph;
  @UiField EnumeratedLabel enumLabel;
  @UiField PushButton pushButton;
  @UiField Label lblDebugId;
  @UiField HasHTML mixedMessageWidget;
  @UiField SpanElement mixedMessageSpan;
  @UiField FooLabel primitiveIntoObject;
  @UiField FooLabel objectIntoPrimitive;
  @UiField FooLabel allObject;
  @UiField FooLabel allPrimitive;
  @UiField FooLabel mismatchPrimitiveIntoObject;
  @UiField FooLabel allMismatchPrimitive;
  @UiField FooLabel primitiveBooleanIntoObject;
  @UiField FooLabel objectBooleanIntoPrimitive;
  @UiField FooLabel allObjectBoolean;
  @UiField FooLabel allPrimitiveBoolean;
  @UiField ToggleButton toggle;
  @UiField HTML styleLess;
  @UiField FooDialog fooDialog;
  @UiField ListBox fooListBox;
  @UiField Grid fooGrid;
  @UiField AbsolutePanel myAbsolutePanel;
  @UiField Widget myAbsolutePanelItemA;
  @UiField Widget myAbsolutePanelItemB;
  @UiField Widget myAbsolutePanelItemC;
  @UiField NamedFrame myNamedFrame;
  @UiField DateLabel myDateLabel;
  @UiField DateLabel myDateLabel2;
  @UiField DateLabel myDateLabel3;
  @UiField NumberLabel<Float> myNumberLabel;
  @UiField NumberLabel<Float> myNumberLabel2;
  @UiField(provided = true) @SuppressWarnings("rawtypes")
  Renderer doubleRenderer = DoubleRenderer.instance();
  @UiField ValueLabel<Double> myValueLabel;
  @UiField IntegerBox myIntegerBox;
  @UiField DoubleBox myDoubleBox;
  @SuppressWarnings("rawtypes")
  @UiField ValueChangeWidget<List> myValueChangeWidget;
  @SuppressWarnings("rawtypes")
  @UiField ValueChangeWidget myValueChangeWidget_raw;
  @SuppressWarnings("rawtypes")
  @UiField ExtendsValueChangeWidget<List> myValueChangeWidget_extends;
  @UiField ImageElement myImage;
  @UiField HTML htmlWithComputedSafeHtml;
  @UiField HTML htmlWithComputedText;
  @UiField Label labelWithComputedText;
  @UiField FlowPanel flowPanelWithTag;
  @UiField Element myElementWithTagName;
  @UiField DataResource embeddedSvgData;
  @UiField DataResource linkedSvgData;
  @UiField(provided = true) FooIsWidget fooIsWidget = new FooIsWidgetImpl();

  ValueChangeEvent<Double> doubleValueChangeEvent;

  @UiHandler("myDoubleBox")
  void onValueChange(ValueChangeEvent<Double> event) {
    this.doubleValueChangeEvent = event;
  }

  @UiHandler({"myIntegerBox", "myDoubleBox"})
  void onWildcardValueChange_Multi(ValueChangeEvent<?> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget")
  void onWildcardValueChange(ValueChangeEvent<?> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget")
  void onStringValueChange(ValueChangeEvent<String> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget")
  void onListRawValueChange(ValueChangeEvent<List> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget")
  void onListValueChange(ValueChangeEvent<List<List>> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget")
  void onListWildcardValueChange(ValueChangeEvent<List<?>> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget_extends")
  void onWildcardValueChange_extends(ValueChangeEvent<?> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget_extends")
  void onStringValueChange_extends(ValueChangeEvent<String> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget_extends")
  void onListRawValueChange_extends(ValueChangeEvent<List> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget_extends")
  void onListValueChange_extends(ValueChangeEvent<List<List>> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget_extends")
  void onListWildcardValueChange_extends(ValueChangeEvent<List<?>> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget_raw")
  void onWildcardValueChange_raw(ValueChangeEvent<?> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget_raw")
  void onStringValueChange_raw(ValueChangeEvent<String> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget_raw")
  void onListValueChange_raw(ValueChangeEvent<List> event) { /* EMPTY */}

  @UiHandler("myValueChangeWidget_raw")
  void onSelection_raw(SelectionEvent<List> event) { /* EMPTY */}

  public WidgetBasedUi() {
    init();
  }

  protected void init() {
    external.style().ensureInjected();
    initWidget(binder.createAndBindUi(this));
  }

  @UiFactory
  StrictLabel createStrictLabel(String text) {
    return new StrictLabel(text);
  }
}
