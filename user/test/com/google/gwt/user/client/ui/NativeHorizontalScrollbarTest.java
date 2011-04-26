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
package com.google.gwt.user.client.ui;

/**
 * Tests the {@link NativeHorizontalScrollbar} widget.
 */
public class NativeHorizontalScrollbarTest extends
    NativeScrollbarTestBase<NativeHorizontalScrollbar> {

  @Override
  protected NativeHorizontalScrollbar createScrollbar() {
    return new NativeHorizontalScrollbar();
  }

  @Override
  protected int getMaximumScrollPosition(NativeHorizontalScrollbar scrollbar) {
    return scrollbar.getMaximumHorizontalScrollPosition();
  }

  @Override
  protected int getMinimumScrollPosition(NativeHorizontalScrollbar scrollbar) {
    return scrollbar.getMinimumHorizontalScrollPosition();
  }

  @Override
  protected int getScrollPosition(NativeHorizontalScrollbar scrollbar) {
    return scrollbar.getHorizontalScrollPosition();
  }

  @Override
  protected int getScrollSize(NativeHorizontalScrollbar scrollbar) {
    return scrollbar.getScrollWidth();
  }

  @Override
  protected void setScrollbarSize(NativeHorizontalScrollbar scrollbar, String size) {
    scrollbar.setWidth(size);
  }

  @Override
  protected void setScrollPosition(NativeHorizontalScrollbar scrollbar, int position) {
    scrollbar.setHorizontalScrollPosition(position);
  }

  @Override
  protected void setScrollSize(NativeHorizontalScrollbar scrollbar, int size) {
    scrollbar.setScrollWidth(size);
  }
}
