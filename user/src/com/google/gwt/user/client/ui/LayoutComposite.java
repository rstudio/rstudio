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
 * A {@link Composite} implementation that implements {@link RequiresLayout} and
 * automatically delegates that interface's methods to its wrapped widget, which
 * must itself implement {@link RequiresLayout}.
 */
public abstract class LayoutComposite extends Composite implements RequiresLayout {

  @Override
  protected void initWidget(Widget widget) {
    assert widget instanceof RequiresLayout :
      "LayoutComposite requires that its wrapped widget implement HasLayout";
    super.initWidget(widget);
  }

  public void onLayout() {
    ((RequiresLayout) getWidget()).onLayout();
  }
}
