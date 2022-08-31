/*
 * RStudioWebview.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.workbench.views.viewer.ViewerPane;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

public class RStudioWebview extends Widget implements ViewerPane.Display
{
   public RStudioWebview(String title)
   {
      WebviewElement view = Document.get().createElement("webview").cast();
      view.setTitle(title);
      setElement(view);
   }
   
   @Override
   public String getUrl()
   {
      return getElement().getAttribute("src");
   }

   @Override
   public void setUrl(String url)
   {
      getElement().setAttribute("src", url);
   }

   @Override
   public String getCurrentUrl()
   {
      WebviewElement view = getElement().cast();
      return view.getIFrameElement().getCurrentUrl();
   }

   @Override
   public void reload()
   {
      setUrl(getUrl());
   }

   @Override
   public WindowEx getContentWindow()
   {
      WebviewElement view = getElement().cast();
      return view.getIFrameElement().getContentWindow();
   }

   @Override
   public Document getContentDocument()
   {
      WebviewElement view = getElement().cast();
      return view.getIFrameElement().getContentDocument();
   }

   @Override
   public Widget getWidget()
   {
      return this;
   }

   @Override
   public HandlerRegistration addLoadHandler(LoadHandler handler)
   {
      return this.addLoadHandler(handler);
   }
   
}
