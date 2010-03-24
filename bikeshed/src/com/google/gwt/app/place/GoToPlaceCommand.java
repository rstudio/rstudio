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

import com.google.gwt.user.client.Command;

/**
 * Command to change the app location.
 * 
 * @param <P> the type of place managed by the {@link PlaceController}
 */
public class GoToPlaceCommand<P extends Place> implements Command {
  private final P place;
  private final PlaceController<? super P> placeController;

  /**
   * @param place
   * @param placeController
   */
  public GoToPlaceCommand(P place, PlaceController<? super P> placeController) {
    this.place = place;
    this.placeController = placeController;
  }

  public void execute() {
    placeController.goTo(place);
  }
}
