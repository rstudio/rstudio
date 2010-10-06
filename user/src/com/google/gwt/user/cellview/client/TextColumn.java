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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.TextCell;

/**
 * A column that displays its contents with a {@link TextCell} and does not make
 * use of view data.
 *
 * @param <T> the row type
 */
public abstract class TextColumn<T> extends Column<T, String> {

  /**
   * Construct a new TextColumn.
   */
  public TextColumn() {
    super(new TextCell());
  }
}
