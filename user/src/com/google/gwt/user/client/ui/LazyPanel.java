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

package com.google.gwt.user.client.ui;

/**
 * Convenience class to help lazy loading. The bulk of a LazyPanel is not
 * instantiated until {@link #setVisible}(true) or {@link #ensureWidget} is
 * called.
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.LazyPanelExample}
 */
public abstract class LazyPanel extends SimplePanel {

  public LazyPanel() {
  }

  /**
   * Create the widget contained within the {@link LazyPanel}.
   * 
   * @return the lazy widget
   */
  protected abstract Widget createWidget();

  /**
   * Ensures that the widget has been created by calling {@link #createWidget}
   * if {@link #getWidget} returns <code>null</code>. Typically it is not
   * necessary to call this directly, as it is called as a side effect of a
   * <code>setVisible(true)</code> call.
   */
  public void ensureWidget() {
    Widget widget = getWidget();
    if (widget == null) {
      widget = createWidget();
      setWidget(widget);
    }
  }

  @Override
  /*
   * Sets whether this object is visible. If <code>visible</code> is
   * <code>true</code>, creates the sole child widget if necessary by calling
   * {@link #ensureWidget}.
   * 
   * @param visible <code>true</code> to show the object, <code>false</code> to
   * hide it
   */
  public void setVisible(boolean visible) {
    if (visible) {
      ensureWidget();
    }
    super.setVisible(visible);
  }
}
