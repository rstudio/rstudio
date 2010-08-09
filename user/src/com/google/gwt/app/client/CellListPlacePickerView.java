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
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.List;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Hacky CellList based implementation of PlacePickerView, to be replaced by new
 * data widget, or at least something less ugly.
 * 
 * @param <P> the type of places listed
 */
public class CellListPlacePickerView<P extends Place> extends Composite
    implements PlacePickerView<P> {

  private class CellRenderer extends AbstractCell<P> {
    @Override
    public void render(P value, Object viewData, StringBuilder sb) {
      sb.append(renderer.render(value));
    }
  }

  private final CellList<P> cellList;
  private PlacePickerView.Listener<P> listener;
  private SingleSelectionModel<P> smodel = new SingleSelectionModel<P>();
  private Renderer<P> renderer;

  public CellListPlacePickerView() {
    this.cellList = new CellList<P>(new CellRenderer());
    initWidget(cellList);
    cellList.setSelectionModel(smodel);
    smodel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        if (listener != null) {
          listener.placePicked(smodel.getSelectedObject());
        }
      }
    });
  }

  /**
   * @return this view
   */
  public CellListPlacePickerView<P> asWidget() {
    return this;
  }

  /**
   * Set the listener.
   */
  public void setListener(final PlacePickerView.Listener<P> listener) {
    this.listener = listener;
  }

  public void setPageSize(int size) {
    cellList.setPageSize(size);
  }

  public void setValues(List<P> places, Renderer<P> renderer) {
    // Replace the current renderer.
    this.renderer = renderer;
    cellList.setData(0, places.size(), places);
    if (places.size() > 0) {
      smodel.setSelected(places.get(0), true);
    }
  }
}
