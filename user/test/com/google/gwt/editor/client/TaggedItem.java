/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.editor.client;

/**
 * Simple parameterized data object used in 
 * {@link SimpleBeanEditorTest#testEditorWithParameterizedModels()}.
 *
 * @param <T> type of the item
 */
public class TaggedItem<T> {
  private T item;
  private String tag;

  public T getItem() {
    return item;
  }

  public String getTag() {
    return tag;
  }

  public void setItem(T item) {
    this.item = item;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }
}

