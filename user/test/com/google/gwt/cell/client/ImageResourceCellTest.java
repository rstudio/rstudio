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

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 * Tests for {@link ImageResourceCell}.
 */
public class ImageResourceCellTest extends CellTestBase<ImageResource> {

  /**
   * The images used for this test.
   */
  static interface Images extends ClientBundle {
    ImageResource prettyPiccy();
  }

  private Images images;

  @Override
  protected Cell<ImageResource> createCell() {
    return new ImageResourceCell();
  }

  @Override
  protected ImageResource createCellValue() {
    return getImages().prettyPiccy();
  }

  @Override
  protected boolean dependsOnSelection() {
    return false;
  }

  @Override
  protected String[] getConsumedEvents() {
    return null;
  }

  @Override
  protected String getExpectedInnerHtml() {
    return AbstractImagePrototype.create(getImages().prettyPiccy()).getHTML();
  }

  @Override
  protected String getExpectedInnerHtmlNull() {
    return "";
  }

  private Images getImages() {
    if (images == null) {
      images = GWT.create(Images.class);
    }
    return images;
  }
}
