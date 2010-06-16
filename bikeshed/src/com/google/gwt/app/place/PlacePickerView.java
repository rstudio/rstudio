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
package com.google.gwt.app.place;

import com.google.gwt.input.shared.Renderer;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * View for a {@link PlacePicker}.
 * 
 * @param <P> the type of place displayed
 */
public interface PlacePickerView<P extends Place> extends IsWidget {

  /**
   * Implemented by the presenter currently using this view.
   */
  interface Listener<P> {
    void placePicked(P place);
  }

  void setListener(Listener<P> listener);

  /**
   * May throw {@link UnsupportedOperationException}, or return null.
   * 
   * @return the receiver as a Widget
   */
  Widget asWidget();

  /**
   * Renders a List of places.
   * 
   * @param places
   */
  void setValues(List<P> places, Renderer<P> render);
}
