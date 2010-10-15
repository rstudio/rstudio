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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.sample.expenses.client.style.Styles;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 * An enum describing the approval status.
 */
public enum Approval {
  BLANK("", "inherit", Styles.resources().blankIcon()), APPROVED("Approved",
      "#00aa00", Styles.resources().approvedIcon()), DENIED("Denied",
      "#ff0000", Styles.resources().deniedIcon());

  /**
   * Get the {@link Approval} from the specified string.
   * 
   * @param approval the approval string
   * @return the {@link Approval}
   */
  public static Approval from(String approval) {
    if (APPROVED.is(approval)) {
      return APPROVED;
    } else if (DENIED.is(approval)) {
      return DENIED;
    }
    return BLANK;
  }

  private final String color;
  private final SafeHtml iconHtml;
  private final String text;

  private Approval(String text, String color, ImageResource res) {
    this.text = text;
    this.color = color;
    this.iconHtml = SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(res).getHTML());
  }

  public String getColor() {
    return color;
  }

  public SafeHtml getIconHtml() {
    return iconHtml;
  }

  public String getText() {
    return text;
  }

  public boolean is(String compare) {
    return text.equals(compare);
  }
}