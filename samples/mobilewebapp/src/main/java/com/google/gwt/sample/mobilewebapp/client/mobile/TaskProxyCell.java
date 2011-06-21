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
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;

import java.util.Date;

/**
 * A {@link com.google.gwt.cell.client.Cell} used to render a {@link TaskProxy}.
 */
class TaskProxyCell extends AbstractCell<TaskProxy> {

  /**
   * The template used by this cell.
   * 
   */
  interface Template extends SafeHtmlTemplates {
    @SafeHtmlTemplates.Template("{0}<div style=\"font-size:80%;\">&nbsp;</div>")
    SafeHtml noDate(SafeHtml name);

    @SafeHtmlTemplates.Template("{0}<div style=\"font-size:80%;color:#999;\">Due: {1}</div>")
    SafeHtml onTime(SafeHtml name, String date);

    @SafeHtmlTemplates.Template("{0}<div style=\"font-size:80%;color:red;\">Due: {1}</div>")
    SafeHtml pastDue(SafeHtml name, String date);
  }

  private static Template template;
  private final DateTimeFormat dateFormat = DateTimeFormat.getFormat(PredefinedFormat.DATE_LONG);

  public TaskProxyCell() {
    if (template == null) {
      template = GWT.create(Template.class);
    }
  }

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
      sb.append(template.noDate(name));
    } else if (date.before(today)) {
      sb.append(template.pastDue(name, dateFormat.format(date)));
    } else {
      sb.append(template.onTime(name, dateFormat.format(date)));
    }
  }
}
