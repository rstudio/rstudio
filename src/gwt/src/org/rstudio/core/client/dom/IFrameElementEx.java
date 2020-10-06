/*
 * IFrameElementEx.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.core.client.dom;

import org.rstudio.core.client.BrowseCap;

import com.google.gwt.dom.client.IFrameElement;

public class IFrameElementEx extends IFrameElement
{
   protected IFrameElementEx()
   {
   }

   public final native WindowEx getContentWindow() /*-{
      return this.contentWindow;
   }-*/;

   /**
    * Gets the URL currently displayed in the IFrame, or null if the URL cannot
    * be determined.
    *
    * @return The current URL displayed in the IFrame.
    */
   public final String getCurrentUrl()
   {
      String url = null;
      try
      {
         if (getContentWindow() != null)
         {
            url = getContentWindow().getLocationHref();
         }
      }
      catch (Exception e)
      {
         // attempting to get the URL can throw with a DOM security exception if
         // the current URL is on another domain--in this case we'll just want
         // to return null, so eat the exception.
      }
      return url;
   }

   public final void setFocus()
   {
      if (BrowseCap.isInternetExplorer())
         getContentWindow().focus();
      else
         focus();
   }
}
