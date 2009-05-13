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

package com.google.gwt.user.client.ui;

/**
 * {@link ChangeListenerCollection} used to correctly hook up listeners which
 * need to listen to events from another source.
 * <p>
 * For example, {@link Composite} widgets often need to listen to events
 * generated on their wrapped widget. Upon the firing of a wrapped widget's
 * event, the composite widget must fire its own listeners with itself as the
 * source of the event. To use a {@link DelegatingChangeListenerCollection},
 * simply use the {@link DelegatingChangeListenerCollection} instead of a
 * {@link ChangeListenerCollection}. For example, in {@link SuggestBox}, the
 * following code is used to listen to change events on the {@link SuggestBox}'s
 * underlying widget.
 * </p>
 * 
 * <pre>
 *  public void addChangeListener(ChangeListener listener) {
 *    if (changeListeners == null) {
 *      changeListeners = new DelegatingChangeListenerCollection(this, box);
 *    }
 *    changeListeners.add(listener);
 *  }
 *</pre>
 * 
 * @deprecated Use {@link Widget#delegateEvent Widget.delegateEvent} instead
 */
@Deprecated
public class DelegatingChangeListenerCollection extends
    ChangeListenerCollection implements ChangeListener {

  private final Widget owner;

  /**
   * Constructor for {@link DelegatingChangeListenerCollection}.
   * 
   * @param owner owner of listeners
   * @param delegatedTo source of events
   */
  public DelegatingChangeListenerCollection(Widget owner,
      SourcesChangeEvents delegatedTo) {
    this.owner = owner;
    delegatedTo.addChangeListener(this);
  }

  public void onChange(Widget sender) {
    fireChange(owner);
  }
}
