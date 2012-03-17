/*
 * RequestLogDetail.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application.ui;

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import org.rstudio.core.client.jsonrpc.RequestLogEntry;

public class RequestLogDetail extends Composite
{
   public RequestLogDetail(RequestLogEntry entry)
   {
      String req = entry.getRequestData();
      String resp = entry.getResponseData();

      final FlowPanel panel = new FlowPanel();
      panel.getElement().getStyle().setOverflow(Overflow.AUTO);

      HTML html = new HTML();
      html.setText("Request ID: " + entry.getRequestId() + "\n\n"
                   + "== REQUEST ======\n"
                   + req
                   + "\n\n"
                   + "== RESPONSE ======\n"
                   + resp
                   + "\n");
      html.getElement().getStyle().setProperty("whiteSpace", "pre-wrap");

      panel.add(html);

      initWidget(panel);
   }
}
