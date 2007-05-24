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

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;
import java.util.Map;

/**
 * Abstract base class used to implement {@link NumberFormatExampleController}
 * and {@link DateTimeFormatExampleController}.
 */
public abstract class AbstractFormatExampleController {

  private static final String PATTERN_KEY_CUSTOM = "custom";
  public final TextBox txtCurrentPattern = new TextBox();
  public final Label lblFormattedOutput = new Label();
  public final Label lblPatternError = new Label();
  public final Label lblParseError = new Label();
  public final ListBox lstSamplePatterns = new ListBox();
  public final TextBox txtInput = new TextBox();
  private String prevPattern;
  private String prevInput;

  protected AbstractFormatExampleController(String defaultText, Map patterns) {
    initWidgetsForPattern(patterns);
    initWidgetsForInput();
    txtInput.setText(defaultText);
    tryToParseInput(false);
  }

  protected abstract String doGetPattern(String patternKey);

  /**
   * Parses the specified pattern and remembers it for formatting input later.
   * 
   * @param pattern
   * @throws IllegalArgumentException if the pattern could not be parsed
   */
  protected abstract void doParseAndRememberPattern(String pattern);

  protected abstract void doParseInput(String toParse, HasText output,
      HasText error);

  private void initWidgetsForInput() {
    txtInput.addKeyboardListener(new KeyboardListenerAdapter() {
      public void onKeyUp(Widget sender, char keyCode, int modifiers) {
        tryToParseInput(false);
      }
    });
  }

  private void initWidgetsForPattern(Map patternMap) {
    txtCurrentPattern.addKeyboardListener(new KeyboardListenerAdapter() {
      public void onKeyUp(Widget sender, char keyCode, int modifiers) {
        String pattern = txtCurrentPattern.getText();

        // Update the active pattern.
        tryToActivatePattern(pattern);
      }
    });

    // Load pattern choices.
    for (Iterator iter = patternMap.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      String patternKey = (String) entry.getKey();
      String caption = (String) entry.getValue();

      lstSamplePatterns.addItem(caption, patternKey);
    }

    lstSamplePatterns.addChangeListener(new ChangeListener() {
      public void onChange(Widget sender) {
        syncPatternToList();
      }
    });

    lstSamplePatterns.setSelectedIndex(0);

    syncPatternToList();
  }

  private void syncPatternToList() {
    int sel = lstSamplePatterns.getSelectedIndex();
    assert (sel >= 0) && (sel < lstSamplePatterns.getItemCount());

    // Update the current pattern.
    String patternKey = lstSamplePatterns.getValue(sel);
    String pattern;
    if (PATTERN_KEY_CUSTOM.equals(patternKey)) {
      // Make the pattern text box editable.
      txtCurrentPattern.setReadOnly(false);
      pattern = txtCurrentPattern.getText();
      txtCurrentPattern.setText(pattern);
      txtCurrentPattern.selectAll();
      txtCurrentPattern.setFocus(true);
    } else {
      // Make the pattern text box read only.
      txtCurrentPattern.setReadOnly(true);
      pattern = doGetPattern(patternKey);
      txtCurrentPattern.setText(pattern);
    }

    // Make the new pattern active.
    tryToActivatePattern(pattern);
  }

  private void tryToActivatePattern(String pattern) {
    if (!pattern.equals(prevPattern)) {
      prevPattern = pattern;
      lblPatternError.setText("");
      try {
        // Allow the subclass to parse the pattern.
        doParseAndRememberPattern(pattern);

        // Parse and format the input again since the pattern changed.
        tryToParseInput(true);
      } catch (IllegalArgumentException e) {
        lblPatternError.setText(e.getMessage());
      }
    }
  }

  private void tryToParseInput(boolean forceReparse) {
    String toParse = txtInput.getText();
    if (forceReparse || !toParse.equals(prevInput)) {
      prevInput = toParse;
      lblParseError.setText("");
      doParseInput(toParse, lblFormattedOutput, lblParseError);
    }
  }
}
