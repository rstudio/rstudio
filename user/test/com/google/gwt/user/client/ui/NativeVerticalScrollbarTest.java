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
 * Tests the {@link NativeVerticalScrollbar} widget.
 */
public class NativeVerticalScrollbarTest extends NativeScrollbarTestBase<NativeVerticalScrollbar> {

  @Override
  protected NativeVerticalScrollbar createScrollbar() {
    return new NativeVerticalScrollbar();
  }

  @Override
  protected int getMaximumScrollPosition(NativeVerticalScrollbar scrollbar) {
    return scrollbar.getMaximumVerticalScrollPosition();
  }

  @Override
  protected int getMinimumScrollPosition(NativeVerticalScrollbar scrollbar) {
    return scrollbar.getMinimumVerticalScrollPosition();
  }

  @Override
  protected int getScrollPosition(NativeVerticalScrollbar scrollbar) {
    return scrollbar.getVerticalScrollPosition();
  }

  @Override
  protected int getScrollSize(NativeVerticalScrollbar scrollbar) {
    return scrollbar.getScrollHeight();
  }

  @Override
  protected void setScrollbarSize(NativeVerticalScrollbar scrollbar, String size) {
    scrollbar.setHeight(size);
  }

  @Override
  protected void setScrollPosition(NativeVerticalScrollbar scrollbar, int position) {
    scrollbar.setVerticalScrollPosition(position);
  }

  @Override
  protected void setScrollSize(NativeVerticalScrollbar scrollbar, int size) {
    scrollbar.setScrollHeight(size);
  }
}
