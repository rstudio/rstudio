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
package com.google.gwt.cell.client;

/**
 * <p>
 * A {@link AbstractCell} used to render an image. The String value is the url
 * of the image.
 * </p>
 * <p>
 * If the images being displayed are static or available at compile time, using
 * {@link ImageResourceCell} will usually be more efficient.
 * </p>
 *
 * @see ImageResourceCell
 */
public class ImageCell extends AbstractCell<String> {

  @Override
  public void render(String value, Object key, StringBuilder sb) {
    if (value != null) {
      sb.append("<img src='").append(value).append("'/>");
    }
  }
}
