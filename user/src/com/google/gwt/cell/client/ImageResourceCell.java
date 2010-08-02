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

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 * A {@link AbstractCell} used to render an {@link ImageResource}.
 */
public class ImageResourceCell extends AbstractCell<ImageResource> {

  @Override
  public void render(ImageResource value, Object key, StringBuilder sb) {
    if (value != null) {
      sb.append(AbstractImagePrototype.create(value).getHTML());
    }
  }
}
