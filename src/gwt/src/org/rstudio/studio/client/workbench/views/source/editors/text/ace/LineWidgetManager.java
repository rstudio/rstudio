/*
 * LineWidgetManager.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class LineWidgetManager extends JavaScriptObject
{
   protected LineWidgetManager() {}

   public native final void addLineWidget(LineWidget widget) /*-{
      this.addLineWidget(widget);
   }-*/;
   
   public native final void removeLineWidget(LineWidget widget) /*-{
      this.removeLineWidget(widget);
   }-*/;
   
   public native final void onWidgetChanged(LineWidget widget) /*-{
      this.onWidgetChanged(widget);
   }-*/;
   
   public native final JsArray<LineWidget> getLineWidgets() /*-{
      return this.session.lineWidgets || [];
   }-*/;
}
