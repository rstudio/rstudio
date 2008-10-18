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

import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;

/**
 * SuggestBox does not shrink to fit its contents when the number of suggestions
 * is reduced. This issue was reported against IE in standards mode.
 */
public class Issue2392 extends AbstractIssue {
  private MultiWordSuggestOracle oracle = null;

  @Override
  public Widget createIssue() {
    SuggestBox box = new SuggestBox(getSuggestOracle());

    // Enable animations to ensure that the suggestions are only animated when
    // they are opened the first time.
    box.setAnimationEnabled(true);

    return box;
  }

  @Override
  public String getInstructions() {
    return "Type in the word 'add' one letter at a time.  As the number of "
        + "suggestions is reduced, the red box around the SuggestPopup should "
        + "shrink to just fit the contents.";
  }

  @Override
  public String getSummary() {
    return "SuggestBox does not shrink when the number of suggestions is "
        + "reduced";
  }

  @Override
  public boolean hasCSS() {
    return true;
  }

  /**
   * Create a {@link SuggestOracle} that returns random words. Only one suggest
   * oracle is created.
   * 
   * @return a {@link SuggestOracle}
   */
  private SuggestOracle getSuggestOracle() {
    if (oracle == null) {
      oracle = new MultiWordSuggestOracle();
      for (char a = 'a'; a <= 'z'; a++) {
        for (char b = 'a'; b <= 'm'; b++) {
          for (char c = 'a'; c <= 'd'; c++) {
            oracle.add("" + a + b + c);
          }
        }
      }
    }
    return oracle;
  }

}
