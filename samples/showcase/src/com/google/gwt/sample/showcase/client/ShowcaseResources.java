/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.sample.showcase.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.ImageResource;

/**
 * The images and styles used throughout the Showcase.
 */
public interface ShowcaseResources extends ClientBundle {
  ImageResource catI18N();

  ImageResource catLists();

  ImageResource catOther();

  ImageResource catPanels();

  ImageResource catPopups();

  ImageResource catTables();

  ImageResource catTextInput();

  ImageResource catWidgets();

  /**
   * The styles used in LTR mode.
   */
  @NotStrict
  @Source("Showcase.css")
  CssResource css();

  ImageResource gwtLogo();

  ImageResource gwtLogoThumb();

  ImageResource jimmy();

  ImageResource jimmyThumb();

  ImageResource loading();

  /**
   * Indicates the locale selection box.
   */
  ImageResource locale();
}