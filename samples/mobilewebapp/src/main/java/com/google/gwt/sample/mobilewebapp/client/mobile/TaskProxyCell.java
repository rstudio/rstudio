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
package com.google.gwt.sample.mobilewebapp.client.mobile;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.uibinder.client.UiRenderer;

import java.util.Date;

/**
 * A {@link com.google.gwt.cell.client.Cell} used to render a {@link TaskProxy}.
 * Uses a UiRenderer to generate the cell contents.
 */
class TaskProxyCell extends AbstractCell<TaskProxy> {

  /**
   * Use a UiBinder template to render this cell.
   */
  interface Renderer extends UiRenderer {
    /**
     * Retrieves the CSS resource defined in the template.
     */
    CellStyle getCellStyle();

    /**
     * Renders the cell contents into {@code sb}, filling in {@name},
     * {@code date}, and the CSS {@code dateStyle}.
     */
    void render(SafeHtmlBuilder sb, SafeHtml name, String date, String dateStyle);
  }

  interface CellStyle extends CssResource {
    String noDate();
    String onTime();
    String pastDue();
  }

  private final DateTimeFormat dateFormat = DateTimeFormat.getFormat(PredefinedFormat.DATE_LONG);

  private final Renderer renderer;

  public TaskProxyCell() {
    renderer = GWT.<Renderer>create(Renderer.class);
  }

  /**
   * Renders the cell contents. Delegates the actual rendering to the
   * UiRenderer decined in the template.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void render(com.google.gwt.cell.client.Cell.Context context, TaskProxy value,
      SafeHtmlBuilder sb) {
    if (value == null) {
      return;
    }

    SafeHtml name;
    if (value.getName() == null) {
      name = SafeHtmlUtils.fromSafeConstant("<i>Unnamed</i>");
    } else {
      name = SafeHtmlUtils.fromString(value.getName());
    }

    Date date = value.getDueDate();
    Date today = new Date();
    today.setHours(0);
    today.setMinutes(0);
    today.setSeconds(0);
    if (date == null) {
      renderer.render(sb, name, " ", renderer.getCellStyle().noDate());
    } else if (date.before(today)) {
      renderer.render(sb, name, dateFormat.format(date), renderer.getCellStyle().pastDue());
    } else {
      renderer.render(sb, name, dateFormat.format(date), renderer.getCellStyle().onTime());
    }
  }
}
