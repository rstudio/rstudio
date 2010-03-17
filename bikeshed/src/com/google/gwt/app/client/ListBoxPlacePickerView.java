/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.app.client;

import com.google.gwt.app.place.Place;
import com.google.gwt.app.place.PlacePickerView;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ValueListBox;

/**
 * Hacky ValueListBox based implementation of PlacePickerView, to be replaced by
 * new data widget, or at least something less ugly.
 * 
 * @param <P> the type of places listed
 */
public class ListBoxPlacePickerView<P extends Place> extends ValueListBox<P>
    implements PlacePickerView<P> {
  private HandlerRegistration handlerRegistration;

  /**
   * Set the listener.
   */
  public void setListener(final PlacePickerView.Listener<P> listener) {
    if (handlerRegistration != null) {
      handlerRegistration.removeHandler();
      handlerRegistration = null;
    }

    handlerRegistration = addValueChangeHandler(new ValueChangeHandler<P>() {
      public void onValueChange(ValueChangeEvent<P> event) {
        listener.placePicked(event.getValue());
      }
    });
  }
}
