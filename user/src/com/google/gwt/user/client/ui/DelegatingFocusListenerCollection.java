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
 * <p>
 * For example, {@link Composite} widgets often need to listen to events
 * generated on their wrapped widget. Upon the firing of a wrapped widget's
 * event, the composite widget must fire its own listeners with itself as the
 * source of the event. To use a {@link DelegatingFocusListenerCollection},
 * simply use the {@link DelegatingFocusListenerCollection} instead of a
 * {@link FocusListenerCollection}. For example, in {@link SuggestBox}, the
 * following code is used to listen to focus events on the {@link SuggestBox}'s
 * underlying widget.
 * </p>
 * 
 * <pre>
 *  public void addFocusListener(FocusListener listener) {
 *    if (focusListeners == null) {
 *      focusListeners = new DelegatingFocusListenerCollection(this, box);
 *    }
 *    focusListeners.add(listener);
 *  }
 *</pre>
 * 
 * @deprecated Use {@link Widget#delegateEvent} instead
 */
@Deprecated
public class DelegatingFocusListenerCollection extends FocusListenerCollection
    implements FocusListener {

  private final Widget owner;

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
    fireFocus(owner);
  }

  public void onLostFocus(Widget sender) {
    fireLostFocus(owner);
  }
}
