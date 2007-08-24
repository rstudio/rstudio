/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.sample.i18n.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A sample module that demonstrates how to localize text resources.
 */
public class I18N implements EntryPoint {

  /**
   * Attaches logical controllers to various elements on the page. Note that
   * this module is logic only; it assumes the HTML handles all layout and
   * styling.
   * 
   * @see com.google.gwt.core.client.EntryPoint#onModuleLoad()
   */
  public void onModuleLoad() {
    // An example that demonstrates NumberFormat.
    NumberFormatExampleConstants numberFormatConstants = GWT.create(NumberFormatExampleConstants.class);
    NumberFormatExampleController numberFormatController = new NumberFormatExampleController(
        numberFormatConstants);
    initNumberFormatExample(numberFormatController);

    // An example that demonstrates DateTimeFormat.
    DateTimeFormatExampleConstants dateTimeFormatConstants = GWT.create(DateTimeFormatExampleConstants.class);
    DateTimeFormatExampleController dateTimeFormatController = new DateTimeFormatExampleController(
        dateTimeFormatConstants);
    initDateTimeFormatExample(dateTimeFormatController);

    // An example that demonstrates Constants.
    initConstantsExample();

    // An example that demonstrates ConstantsWithLookup.
    ConstantsWithLookupExampleConstants constantsWithLookupConstants = GWT.create(ConstantsWithLookupExampleConstants.class);
    ConstantsWithLookupExampleController constantsWithLookupController = new ConstantsWithLookupExampleController(
        constantsWithLookupConstants);
    initConstantsWithLookupExample(constantsWithLookupController);

    // An example that demonstrates Messages.
    MessagesExampleConstants messagesExampleConstants = GWT.create(MessagesExampleConstants.class);
    MessagesExampleController messagesController = new MessagesExampleController(
        messagesExampleConstants);
    initMessagesExample(messagesController);

    // An example that demonstrates Dictionary.
    initDictionaryExample();

    TextBox initiallyFocusedTextBox = numberFormatController.txtInput;
    initiallyFocusedTextBox.setFocus(true);
    initiallyFocusedTextBox.selectAll();
  }

  private void bindElement(String id, String text) {
    Element elem = DOM.getElementById(id);
    if (elem == null) {
      throw new NoSuchElementException(id);
    }
    DOM.setInnerText(elem, text);
  }

  private void bindElement(String id, Widget widget) {
    RootPanel rp = RootPanel.get(id);
    if (rp == null) {
      throw new NoSuchElementException(id);
    }
    rp.clear();
    rp.add(widget);
  }

  private void initConstantsExample() {
    ConstantsExampleConstants constants = GWT.create(ConstantsExampleConstants.class);
    ListBox colorChoices = new ListBox();

    for (String color : constants.colorMap().values()) {
      colorChoices.addItem(color);
    }

    TextBox txtFirstName = new TextBox();
    TextBox txtLastName = new TextBox();

    bindElement("constantsFirstNameCaption", constants.firstName());
    bindElement("constantsFirstNameText", txtFirstName);
    bindElement("constantsLastNameCaption", constants.lastName());
    bindElement("constantsLastNameText", txtLastName);
    bindElement("constantsFavoriteColorCaption", constants.favoriteColor());
    bindElement("constantsFavoriteColorList", colorChoices);

    txtFirstName.setText("Amelie");
    txtLastName.setText("Crutcher");
  }

  private void initConstantsWithLookupExample(
      ConstantsWithLookupExampleController controller) {
    ConstantsWithLookupExampleConstants constants = controller.getConstants();
    bindElement("constantsWithLookupInputCaption", constants.input());
    bindElement("constantsWithLookupInputText", controller.txtInput);
    bindElement("constantsWithLookupResultsCaption", constants.result());
    bindElement("constantsWithLookupResultsText", controller.txtResult);
  }

  private void initDateTimeFormatExample(
      DateTimeFormatExampleController controller) {
    DateTimeFormatExampleConstants constants = controller.getConstants();
    bindElement("dateTimeFormatPatternCaption", constants.pattern());
    bindElement("dateTimeFormatPatternList", controller.lstSamplePatterns);
    bindElement("dateTimeFormatPatternText", controller.txtCurrentPattern);
    bindElement("dateTimeFormatPatternError", controller.lblPatternError);
    bindElement("dateTimeFormatInputCaption", constants.inputValue());
    bindElement("dateTimeFormatInputText", controller.txtInput);
    bindElement("dateTimeFormatInputError", controller.lblParseError);
    bindElement("dateTimeFormatOutputCaption", constants.formattedOutput());
    bindElement("dateTimeFormatOutputText", controller.lblFormattedOutput);
  }

  private void initDictionaryExample() {
    FlexTable t = new FlexTable();
    t.setStyleName("i18n-dictionary");
    bindElement("dictionaryExample", t);

    Dictionary userInfo = Dictionary.getDictionary("userInfo");
    Iterator<String> s = userInfo.keySet().iterator();
    for (int i = 0; s.hasNext(); i++) {
      String key = s.next();
      t.setText(0, i, key);
      t.setText(1, i, userInfo.get(key));
    }
    t.getRowFormatter().setStyleName(0, "i18n-dictionary-header-row");
  }

  private void initMessagesExample(MessagesExampleController controller) {
    MessagesExampleConstants constants = controller.getConstants();
    bindElement("messagesTemplateCaption", constants.messageTemplate());
    bindElement("messagesTemplateText", controller.lblMessageTemplate);
    bindElement("messagesArg1Caption", constants.arg1());
    bindElement("messagesArg1Text", controller.txtArg1);
    bindElement("messagesArg2Caption", constants.arg2());
    bindElement("messagesArg2Text", controller.txtArg2);
    bindElement("messagesArg3Caption", constants.arg3());
    bindElement("messagesArg3Text", controller.txtArg3);
    bindElement("messagesFormattedOutputCaption", constants.formattedMessage());
    bindElement("messagesFormattedOutputText", controller.lblFormattedMessage);
  }

  private void initNumberFormatExample(NumberFormatExampleController controller) {
    NumberFormatExampleConstants constants = controller.getConstants();
    bindElement("numberFormatPatternCaption", constants.pattern());
    bindElement("numberFormatPatternList", controller.lstSamplePatterns);
    bindElement("numberFormatPatternText", controller.txtCurrentPattern);
    bindElement("numberFormatPatternError", controller.lblPatternError);
    bindElement("numberFormatInputCaption", constants.inputValue());
    bindElement("numberFormatInputText", controller.txtInput);
    bindElement("numberFormatInputError", controller.lblParseError);
    bindElement("numberFormatOutputCaption", constants.formattedOutput());
    bindElement("numberFormatOutputText", controller.lblFormattedOutput);
  }
}
