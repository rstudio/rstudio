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
 * Simple extension of {@link AbstractComposite} that doesn't require type
 * parameters. This originally was the only implementation of Composite, before
 * {@link AbstractComposite} was introduced to allow strong typing and deferred
 * construction.
 *
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.CompositeExample}
 * </p>
 */
public abstract class Composite extends AbstractComposite<Widget> {
  
  /**
   * Provided for compatibility with legacy unit tests that mocked
   * this specific method.
   */
  @Deprecated
  protected void initWidget(Widget widget) { 
    super.initWidget(widget);
  }

  /**
   * This method was for initializing the Widget to be wrapped by this
   * Composite, but has been deprecated in favor of {@link #initWidget(Widget)}.
   * 
   * @deprecated Use {@link #initWidget(Widget)} instead
   */
  @Deprecated
  protected void setWidget(Widget widget) {
    initWidget(widget);
  }
}
