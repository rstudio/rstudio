/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;

import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;

/**
 * A sample user interface dependent upon localized text resources.
 */
public class I18N implements EntryPoint {
  private static final String DETAILED_EXPLANATION_STYLE = "detailedExplanation";
  private static final String USER_TABLE_LABEL_STYLE = "userTableLabel";
  private static final String USER_TABLE_STYLE = "userTable";
  private static final String MESSAGES_TABLE_STYLE = "messagesTable";
  private static final String MESSAGES_ARGUMENT_TYPE_STYLE = "messagesArgumentType";
  /* Root node to start injecting code. */
  private static final String ROOT = "root";
  private static final int USER_TABLE_WIDTH = 1;

  /**
   * Creates a UI with each of four tabs showcasing each of the text
   * localization classes.
   * 
   * @see com.google.gwt.core.client.EntryPoint#onModuleLoad()
   */
  public void onModuleLoad() {
    // Use GWT.create to bind the localizable resources.
    MyConstants constants = (MyConstants) GWT.create(MyConstants.class);
    MyMessages msgs = (MyMessages) GWT.create(MyMessages.class);
    Colors colors = (Colors) GWT.create(Colors.class);

    // Set up the tab panel.
    final RootPanel root = RootPanel.get(ROOT);
    final TabPanel tabPanel = new TabPanel();
    root.add(tabPanel);
    tabPanel.setWidth("100%");

    // Constants
    FlowPanel constantsTab = new FlowPanel();
    tabPanel.add(constantsTab, "Constants");
    tabPanel.selectTab(0);
    createTabForConstants(constants, constantsTab);

    // ConstantsWithLookup
    FlowPanel constantsWithLookupTab = new FlowPanel();
    tabPanel.add(constantsWithLookupTab, "ConstantsWithLookup");
    createTabsForConstantsWithLookup(constants, colors, constantsWithLookupTab);

    // Messages
    FlowPanel messagesTab = new FlowPanel();
    tabPanel.add(messagesTab, "Messages");
    createTabForMessages(constants, msgs, messagesTab);

    // Dictionary
    FlowPanel dictionaryTab = new FlowPanel();
    tabPanel.add(dictionaryTab, "Dictionary");
    createTabsForDictionary(constants, dictionaryTab);
  }

  /**
   * Helper method to provide consistent handling of the detailed description of
   * each tab.
   * 
   * @param text detailed text
   * @param panel panel to add the text to
   */
  private void addDetailedMessage(String text, FlowPanel panel) {
    Label l = new HTML(text);
    l.setStyleName(DETAILED_EXPLANATION_STYLE);
    panel.add(l);
  }

  /**
   * Helper method to add an iterator to a list box.
   * 
   * @param box list box
   * @param iter iter to add
   */
  private void addItems(ListBox box, Iterator iter) {
    while (iter.hasNext()) {
      box.addItem((String) iter.next());
    }
  }

  /**
   * Helper method to add values from a map to a list box.
   * 
   * @param box
   * @param values
   */
  private void addItems(ListBox box, Map values) {
    Iterator elements = values.values().iterator();
    addItems(box, elements);
  }

  private TextBox addTextBox(String label, String defaultValue, HTMLTable t,
      int row, int column) {
    HorizontalPanel panel = new HorizontalPanel();
    final TextBox box = new TextBox();
    box.setText(defaultValue);
    panel.add(box);
    Label l2 = new Label(label);
    l2.setStyleName(MESSAGES_ARGUMENT_TYPE_STYLE);
    panel.add(l2);
    panel.setCellWidth(l2, "250");
    t.setWidget(row, column, panel);
    return box;
  }

  /**
   * Creates tab to show off <code>Constants</code>.
   * 
   * @param constants module constants
   * @param panel tab panel
   */
  private void createTabForConstants(MyConstants constants, FlowPanel panel) {
    addDetailedMessage(constants.constantsExample(), panel);
    FlexTable t = new FlexTable();
    CellFormatter formatter = t.getFlexCellFormatter();
    t.setStyleName(USER_TABLE_STYLE);
    t.setBorderWidth(USER_TABLE_WIDTH);

    // First Name
    t.setText(0, 0, constants.firstName());
    formatter.setStyleName(0, 0, USER_TABLE_LABEL_STYLE);
    t.setWidget(0, 1, new TextBox());

    // Last Name
    t.setText(1, 0, constants.lastName());
    formatter.setStyleName(1, 0, USER_TABLE_LABEL_STYLE);
    t.setWidget(1, 1, new TextBox());

    // Gender
    t.setText(2, 0, constants.gender());
    formatter.setStyleName(2, 0, USER_TABLE_LABEL_STYLE);
    ListBox gender = new ListBox();
    addItems(gender, constants.genderMap());
    t.setWidget(2, 1, gender);

    panel.add(t);
  }

  /**
   * Creates tab to show of <code>Messages</code>.
   * 
   * @param constants module constants
   * @param messages module messages
   * @param panel tab panel
   */
  private void createTabForMessages(MyConstants constants,
      final MyMessages messages, FlowPanel panel) {
    addDetailedMessage(constants.messagesExample(), panel);
    Grid table = new Grid(4, 4);
    panel.add(table);
    table.setStyleName(MESSAGES_TABLE_STYLE);
    table.setBorderWidth(USER_TABLE_WIDTH);
    table.getRowFormatter().setStyleName(0, USER_TABLE_LABEL_STYLE);
    table.setText(0, 0, constants.messageTemplates());
    table.setText(0, 1, constants.messageArgumentOne());
    table.setText(0, 2, constants.messageArgumentTwo());

    // Security permission error message
    String perm = messages.permission("{0}", "{1}");
    table.setText(1, 0, perm);
    final TextBox perm1 = addTextBox(constants.enterString(),
        constants.defaultResource(), table, 1, 1);
    final TextBox perm2 = addTextBox(constants.enterString(),
        constants.defaultSecurity(), table, 1, 2);

    Button b = new Button(constants.showMessage());
    b.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        Window.alert(messages.permission(perm1.getText(), perm2.getText()));
      }
    });
    table.setWidget(1, 3, b);

    // Remaining space info message
    // As messages.info takes in a int, cannot use the same trick as above to
    // recreate the original error message, so must store it in MyConstants
    // instead.
    String info = constants.infoMessage();
    table.setText(2, 0, info);
    final TextBox info1 = addTextBox(constants.enterInt(), "123", table, 2, 1);
    Button b2 = new Button(constants.showMessage());
    b2.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        String text = info1.getText();
        try {
          int numMB = Integer.parseInt(text);
          Window.alert(messages.info(numMB));
        } catch (NumberFormatException e) {
          Window.alert(messages.intParseError(text));
        }
      }
    });
    table.setWidget(2, 3, b2);

    // Required field error message
    String required = messages.requiredField("{0}");
    table.setText(3, 0, required);
    final TextBox required1 = addTextBox(constants.enterString(),
        constants.defaultRequired(), table, 3, 1);
    Button b3 = new Button(constants.showMessage());
    b3.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        Window.alert(messages.requiredField(required1.getText()));
      }
    });

    table.setWidget(3, 3, b3);
  }

  /**
   * Creates tab to show off <code>ConstantsWithLookup</code>.
   * 
   * @param constants module constants
   * @param colorConstants colors
   * @param panel tab panel
   */
  private void createTabsForConstantsWithLookup(final MyConstants constants,
      final Colors colorConstants, FlowPanel panel) {
    // Set up tab.
    String details = constants.constantsWithLookupExample();
    addDetailedMessage(details, panel);
    final FlexTable t = new FlexTable();
    CellFormatter cellFormat = t.getCellFormatter();
    t.setBorderWidth(USER_TABLE_WIDTH);
    t.setStyleName(USER_TABLE_STYLE);
    panel.add(t);
    t.setText(0, 0, constants.typeColorHere());
    cellFormat.setStyleName(0, 0, USER_TABLE_LABEL_STYLE);
    final TextBox colorBox = new TextBox();
    t.setWidget(0, 1, colorBox);
    t.setText(1, 0, constants.result());
    cellFormat.setStyleName(1, 0, USER_TABLE_LABEL_STYLE);
    t.setHTML(1, 1, constants.noResult());
    Button b = new Button(constants.translate());
    panel.add(b);

    // Usage of ConstantsWithLookup.
    b.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        String color = colorBox.getText().trim();
        if (color.equals("")) {
          t.setHTML(1, 1, constants.noResult());
          return;
        }
        color = color.trim();
        try {
          String value = colorConstants.getString(color);
          t.setHTML(1, 1, "<b>" + value + "</b>");
        } catch (MissingResourceException e) {
          t.setHTML(1, 1, constants.noResult());
        }
      }
    });
  }

  /**
   * Creates tab to show off <code>Dictionary</code>.
   * 
   * @param constants module constants
   * @param panel tab panel
   */
  private void createTabsForDictionary(MyConstants constants, FlowPanel panel) {
    String details = constants.dictionaryExample() + constants.dictionaryHTML();

    addDetailedMessage(details, panel);

    Dictionary userInfo = Dictionary.getDictionary("userInfo");
    FlexTable t = new FlexTable();
    t.setBorderWidth(USER_TABLE_WIDTH);
    panel.add(t);
    t.setStyleName(USER_TABLE_STYLE);
    t.getRowFormatter().setStyleName(0, USER_TABLE_LABEL_STYLE);

    Iterator s = userInfo.keySet().iterator();
    for (int i = 0; s.hasNext(); i++) {
      String key = (String) s.next();
      t.setText(0, i, key);
      t.setText(1, i, userInfo.get(key));
    }
  }
}
