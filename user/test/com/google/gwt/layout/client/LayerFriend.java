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
package com.google.gwt.layout.client;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.layout.client.Layout.Layer;

/**
 * A helper class to gain access to the package protected internals of a
 * {@link Layout.Layer} from other packages. For testing purposes.
 * 
 */
public class LayerFriend {

  private Layer layer;

  public LayerFriend(Layer layer) {
    this.layer = layer;
  }
  
  public double getTop() {
    return layer.top;
  }
  
  public Unit getTopUnit() {
    return layer.topUnit;
  }
  
  public double getBottom() {
    return layer.bottom;
  }
  
  public Unit getBottomUnit() {
    return layer.bottomUnit;
  }
  
  public double getRight() {
    return layer.right;
  }
  
  public Unit getRightUnit() {
    return layer.rightUnit;
  }
  
  public double getLeft() {
    return layer.left;
  }
  
  public Unit getLeftUnit() {
    return layer.leftUnit;
  }
  
  public double getHeight() {
    return layer.height;
  }
  
  public Unit getHeightUnit() {
    return layer.heightUnit;
  }
  
  public double getWidth() {
    return layer.width;
  }
  
  public Unit getWidthUnit() {
    return layer.widthUnit;
  }

}
