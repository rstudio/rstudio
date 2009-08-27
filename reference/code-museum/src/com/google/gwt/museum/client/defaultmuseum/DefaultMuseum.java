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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.museum.client.viewer.Museum;

/**
 * Default bug museum. By default, shows all visual tests defined in GWT ui to
 * date. Modify the code to add all bugs in as well.
 */
public class DefaultMuseum extends Museum implements EntryPoint {

  public DefaultMuseum() {
    addVisuals();
    addBugs();
  }

  public void addBugs() {
    addIssue(new Issue1245());
    addIssue(new Issue1772());
    addIssue(new Issue1897());
    addIssue(new Issue1932());
    addIssue(new Issue2261());
    addIssue(new Issue2290());
    addIssue(new Issue2307());
    addIssue(new Issue2318());
    addIssue(new Issue2321());
    addIssue(new Issue2331());
    addIssue(new Issue2338());
    addIssue(new Issue2339());
    addIssue(new Issue2390());
    addIssue(new Issue1169());
    addIssue(new Issue2392());
    addIssue(new Issue2443());
    addIssue(new Issue2553());
    addIssue(new Issue2855());
    addIssue(new Issue3172());
    addIssue(new Issue3962());
    addIssue(new Issue3973());
  }

  public void addVisuals() {
    addIssue(new VisualsForDateBox());
    addIssue(new VisualsForDatePicker());
    addIssue(new VisualsForDisclosurePanelEvents());
    addIssue(new VisualsForEventsFiring());
    addIssue(new VisualsForPopupEvents());
    addIssue(new VisualsForTextEvents());
    addIssue(new VisualsForSuggestBoxEvents());
    addIssue(new VisualsForTableEvents());
    addIssue(new VisualsForTree());
    addIssue(new VisualsForTreeEvents());
    addIssue(new VisualsForWindowEvents());
    addIssue(new VisualsForDialogBox());
    addIssue(new VisualsForSuggestBox());
    addIssue(new VisualsForCheckBoxAndRadioButtonEvents());
  }
}
