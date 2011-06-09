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

package com.google.gwt.geolocation.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Real {@link JavaScriptObject} implementation of the {@link Position}.
 */
final class PositionImpl extends JavaScriptObject implements Position {

  protected PositionImpl() {
  }

  @Override
  public final native Coordinates getCoordinates() /*-{
    return this.coords;
  }-*/;

  @Override
  public final native double getTimestamp() /*-{
    return this.timestamp;
  }-*/;

  static final class CoordinatesImpl extends JavaScriptObject implements Coordinates {

    protected CoordinatesImpl() {
    }

    @Override
    public final native double getAccuracy() /*-{
      return this.accuracy;
    }-*/;

    @Override
    public final native Double getAltitude() /*-{
      return this.altitude || null;
    }-*/;

    @Override
    public final native Double getAltitudeAccuracy() /*-{
      return this.altitudeAccuracy || null;
    }-*/;

    @Override
    public final native Double getHeading() /*-{
      return this.heading || null;
    }-*/;

    @Override
    public final native double getLatitude() /*-{
      return this.latitude;
    }-*/;

    @Override
    public final native double getLongitude() /*-{
      return this.longitude;
    }-*/;

    @Override
    public final native Double getSpeed() /*-{
      return this.speed || null;
    }-*/;
  }
}
