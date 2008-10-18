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
package com.google.gwt.user.client.ui;

/**
 * Test cases for {@link DecoratedPopupPanel}.
 */
public class DecoratedPopupTest extends PopupTest {
  @Override
  public void testDependantPopupPanel() {
    // Create the dependent popup
    final PopupPanel dependantPopup = createPopupPanel();
    dependantPopup.setAnimationEnabled(true);

    // Create the primary popup
    final DecoratedPopupPanel primaryPopup = new DecoratedPopupPanel(false,
        false) {
      @Override
      protected void onAttach() {
        dependantPopup.show();
        super.onAttach();
      }

      @Override
      protected void onDetach() {
        dependantPopup.hide();
        super.onDetach();
      }
    };
    primaryPopup.setAnimationEnabled(true);

    testDependantPopupPanel(primaryPopup);
  }

  @Override
  protected PopupPanel createPopupPanel() {
    return new DecoratedPopupPanel();
  }
}
