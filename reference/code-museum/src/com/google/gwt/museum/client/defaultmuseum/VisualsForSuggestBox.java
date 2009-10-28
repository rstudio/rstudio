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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Arrays;
import java.util.List;

/**
 * Visual test for suggest box.
 */
public class VisualsForSuggestBox extends AbstractIssue {
  MultiWordSuggestOracle girlsNames = new MultiWordSuggestOracle();
  MultiWordSuggestOracle girlsNamesWithDefault = new MultiWordSuggestOracle();

  Element textBoxToWrap;

  VisualsForSuggestBox() {
    List<String> femaleNames = Arrays.asList("Jamie", "Jill", "Jackie",
        "Susan", "Helen", "Emily", "Karen", "Emily", "Isabella", "Emma", "Ava",
        "Madison", "Sophia", "Olivia", "Abigail", "Hannah", "Elizabeth",
        "Addison", "Samantha", "Ashley", "Alyssa", "Mia", "Chloe", "Natalie",
        "Sarah", "Alexis", "Grace", "Ella", "Brianna", "Hailey", "Taylor",
        "Anna", "Kayla", "Lily", "Lauren", "Victoria", "Savannah", "Nevaeh",
        "Jasmine", "Lillian", "Julia", "Sofia", "Kaylee", "Sydney",
        "Gabriella", "Katherine", "Alexa", "Destiny", "Jessica", "Morgan",
        "Kaitlyn", "Brooke", "Allison", "Makayla", "Avery", "Alexandra",
        "Jocelyn");
    girlsNames.addAll(femaleNames);
    girlsNamesWithDefault.addAll(femaleNames);
    girlsNamesWithDefault.setDefaultSuggestionsFromText(femaleNames);
  }

  /**
   * This is the entry point method.
   */
  @Override
  public Widget createIssue() {
    VerticalPanel panel = new VerticalPanel();
    HTML wrapperText = new HTML(
        "Make sure this wrapped suggest box works as normal.");
    panel.add(wrapperText);
    textBoxToWrap = Document.get().createTextInputElement();
    wrapperText.getElement().appendChild(textBoxToWrap);

    panel.add(new HTML(
        "Select the show button, nothing should open <br/> Type 'e' and select the button again, now a suggestion list should open and close."));
    panel.add(manuallyShowAndHide());
    panel.add(new Label(
        "Click on suggest box, it should open automatically. Check that First item is not selected"));
    panel.add(suggestBoxWithDefault());
    return panel;
  }

  @Override
  public String getInstructions() {
    return "Follow the directions above each suggest box";
  }

  @Override
  public String getSummary() {
    return "suggest box visuals";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  private Widget manuallyShowAndHide() {
    FlexTable t = new FlexTable();
    t.getFlexCellFormatter().setColSpan(0, 0, 20);
    final SuggestBox box = simpleSuggestBox();
    box.setAnimationEnabled(true);
    t.setWidget(0, 0, box);

    Button showSuggestions = new Button(
        "show suggestions, then hide after 2  seconds", new ClickHandler() {
          public void onClick(ClickEvent event) {
            box.showSuggestionList();
            new Timer() {

              @Override
              public void run() {
                box.hideSuggestionList();
              }

            }.schedule(2000);
          }
        });
    t.setWidget(1, 0, showSuggestions);
    return t;
  }

  private SuggestBox simpleSuggestBox() {
    SuggestBox b = new SuggestBox(girlsNames);
    return b;
  }

  private SuggestBox suggestBoxWithDefault() {
    final SuggestBox b = new SuggestBox(girlsNamesWithDefault);
    b.setAutoSelectEnabled(false);
    b.getTextBox().addMouseDownHandler(new MouseDownHandler() {

      public void onMouseDown(MouseDownEvent event) {
        b.showSuggestionList();
      }

    });
    return b;
  }
}