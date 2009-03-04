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

package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.museum.client.common.EventReporter;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Arrays;
import java.util.List;

/**
 * A simple test for suggest box events.
 */
@SuppressWarnings("deprecation")
public class VisualsForSuggestBoxEvents extends AbstractIssue {

  HorizontalPanel report = new HorizontalPanel();

  @Override
  public Widget createIssue() {
    VerticalPanel p = new VerticalPanel();

    p.add(createSuggestBox("suggest 1", p));
    p.add(createSuggestBox("suggest 2", p));
    report.setBorderWidth(3);

    report.setCellWidth(report.getWidget(0), "300px");
    report.setCellWidth(report.getWidget(1), "300px");

    p.add(report);
    return p;
  }

  @Override
  public String getInstructions() {
    return "Select suggestions from suggest box, check report for events being fired";
  }

  @Override
  public String getSummary() {
    return "suggest box event visual test";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  SuggestBox createSuggestBox(final String suggestBoxName, Panel p) {

    List<String> femaleNames = Arrays.asList(new String[] {
        "Jamie", "Jill", "Jackie", "Susan", "Helen", "Emily", "Karen",
        "Abigail", "Kaitlyn", "Laura", "Joanna", "Tasha"});
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.addAll(femaleNames);

    final SuggestBox b = new SuggestBox(oracle);
    b.setTitle(suggestBoxName);
    p.add(b);
    final CheckBox selectsFirst = new CheckBox("Selects first suggestion");
    selectsFirst.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        b.setAutoSelectEnabled(event.getValue());
      }
    });
    selectsFirst.setChecked(b.isAutoSelectEnabled());
    p.add(selectsFirst);
    final EventReporter<String, SuggestBox> handler = new EventReporter<String, SuggestBox>(
        report);

    handler.new CheckBoxEvent("KeyDown", p) {

      @Override
      public HandlerRegistration addHandler() {
        return b.addKeyDownHandler(handler);
      }

    };

    handler.new CheckBoxListener("ChangeListener", p) {

      @Override
      public void addListener() {
        b.addChangeListener(handler);
      }

      @Override
      public void removeListener() {
        b.removeChangeListener(handler);
      }
    };
    handler.new CheckBoxListener("Suggestion listener", p) {

      @Override
      public void addListener() {
        b.addEventHandler(handler);
      }

      @Override
      public void removeListener() {
        b.removeEventHandler(handler);
      }
    };

    b.addKeyUpHandler(handler);
    b.addKeyPressHandler(handler);
    b.addFocusListener(handler);
    b.addSelectionHandler(handler);
    b.addValueChangeHandler(handler);
    return b;
  }
}