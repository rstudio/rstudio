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
package com.google.gwt.cell.client;

/**
 * A {@link Cell} used to render text.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 *
 * Important TODO: This cell treats its value as HTML. We need to properly treat
 * its value as a raw string, so that it's safe to use with unsanitized data.
 * See the related comment in {@link EditTextCell}.
 */
public class TextCell extends AbstractCell<String> {

  @Override
  public void render(String value, Object key, StringBuilder sb) {
    if (value != null) {
      sb.append(value);
    }
  }
}
