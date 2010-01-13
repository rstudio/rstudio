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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

/**
 * Tests for {@link LayoutPanel}. Note that this only tests LayoutPanel-specific
 * behavior, not general layout correctness, which is covered by
 * {@link com.google.gwt.layout.client.LayoutTest}.
 */
public class LayoutPanelTest extends WidgetTestBase {

  /**
   * Tests for a bug in LayoutCommand, which caused an animate() call, just
   * before an unnecessary forceLayout(), to get stuck. See issue 4360.
   */
  public void testRedundantForceLayout() {
    final LayoutPanel p = new LayoutPanel();
    Label l = new Label("foo");
    p.add(l);

    p.setWidgetTopHeight(l, 0, Unit.PX, 10, Unit.PX);
    p.forceLayout();

    delayTestFinish(5000);
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        p.animate(100, new AnimationCallback() {
          public void onLayout(Layer layer, double progress) {
          }

          public void onAnimationComplete() {
            // If LayoutCommand is broken, this will never happen.
            finishTest();
          }
        });
      }
    });
  }
}
