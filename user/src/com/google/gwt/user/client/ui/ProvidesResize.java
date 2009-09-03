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
 * This tag interface specifies that the implementing widget will call
 * {@link RequiresResize#onResize()} on its children whenever their size may
 * have changed.
 * 
 * <p>
 * With limited exceptions (such as {@link RootLayoutPanel}), widgets that
 * implement this interface will also implement {@link RequiresResize}. A typical
 * widget will implement {@link RequiresResize#onResize()} like this:
 * 
 * <code>
 * public void onResize() {
 *   for (Widget child : getChildren()) {
 *     if (child instanceof RequiresResize) {
 *       ((RequiresResize) child).onResize();
 *     }
 *   }
 * }
 * </code>
 * </p>
 */
public interface ProvidesResize {
}
