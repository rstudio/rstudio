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

package com.google.gwt.user.client.ui;

/**
 * {@link FocusListenerCollection} used to correctly hook up listeners which
 * need to listen to events from another source.
 * 
 */
public class DelegatingFocusListenerCollection extends FocusListenerCollection
    implements FocusListener {

  private Widget owner;

  /**
   * Constructor for {@link DelegatingFocusListenerCollection}. *
   * 
   * @param owner owner of listeners
   * @param delegatedTo source of events
   */
  public DelegatingFocusListenerCollection(Widget owner,
      SourcesFocusEvents delegatedTo) {
    this.owner = owner;
    delegatedTo.addFocusListener(this);
  }

  public void onFocus(Widget sender) {
    super.fireFocus(owner);
  }

  public void onLostFocus(Widget sender) {
    super.fireLostFocus(owner);
  }
}
