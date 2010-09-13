/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.user.client.ui;

/**
 * A {@link Composite} implementation that implements {@link RequiresResize} and
 * automatically delegates that interface's methods to its wrapped widget, which
 * must itself implement {@link RequiresResize}.
 */
public abstract class ResizeComposite extends Composite implements
    RequiresResize {

  @Override
  protected void initWidget(Widget widget) {
    assert widget instanceof RequiresResize :
      "LayoutComposite requires that its wrapped widget implement RequiresResize";
    super.initWidget(widget);
  }

  public void onResize() {
    ((RequiresResize) getWidget()).onResize();
  }
}
