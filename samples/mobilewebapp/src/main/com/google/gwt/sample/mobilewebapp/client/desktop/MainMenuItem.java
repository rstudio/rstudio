/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.sample.mobilewebapp.client.desktop;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.place.shared.Place;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * An item in the main menu that maps to a specific place.
 */
 class MainMenuItem {
  /**
   * The cell used to render a {@link MainMenuItem}.
   */
  static class Cell extends AbstractCell<MainMenuItem> {
    
    @Override
    public void render(com.google.gwt.cell.client.Cell.Context context, MainMenuItem value,
        SafeHtmlBuilder sb) {
      if (value == null) {
        return;
      }
      sb.appendEscaped(value.getName());
    }
  }
  private final String name;

  private final Place place;

  /**
   * Construct a new {@link MainMenuItem}.
   * 
   * @param name the display name
   * @param place the place to open when selected
   */
  public MainMenuItem(String name, Place place) {
    this.name = name;
    this.place = place;
  }

  public String getName() {
    return name;
  }

  public Place getPlace() {
    return place;
  }
  /**
   * Check whether or not this {@link MainMenuItem} maps to the specified
   * place.
   * 
   * @param p a {@link Place}
   * @return true if this menu item maps to the place, false if not
   */
  public boolean mapsToPlace(Place p) {
    return place == p;
  }
}