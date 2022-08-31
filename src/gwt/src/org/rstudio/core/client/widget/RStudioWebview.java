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

import org.rstudio.core.client.Debug;
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
      view_ = Document.get().createElement("webview").cast();
      view_.setAttribute("plugins", "");
      view_.setTitle(title);
      
      setElement(view_);
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
      Debug.breakpoint();
      return "NYI";
   }

   @Override
   public void reload()
   {
      Debug.breakpoint();
   }

   @Override
   public WindowEx getContentWindow()
   {
      Debug.breakpoint();
      return null;
   }

   @Override
   public Document getContentDocument()
   {
      Debug.breakpoint();
      return null;
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
   
   private final WebviewElement view_;
   
}
