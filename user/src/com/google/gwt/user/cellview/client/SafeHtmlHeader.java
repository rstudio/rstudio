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

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * A Header containing safe HTML data rendered by a SafeHtmlCell.
 */
public class SafeHtmlHeader extends Header<SafeHtml> {

  private SafeHtml text;

  /**
   * Construct a Header with a given {@link SafeHtml} text value.
   *
   * @param text the header text, as safe HTML
   */
  public SafeHtmlHeader(SafeHtml text) {
    super(new SafeHtmlCell());
    this.text = text;
  }

  /**
   * Return the {@link SafeHtml} text value.
   */
  @Override
  public SafeHtml getValue() {
    return text;
  }
}
