/*
 * AnchorableFrame.java
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

package org.rstudio.core.client.widget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

public class AnchorableFrame extends RStudioFrame
{
   public AnchorableFrame(String title)
   {
      this(title, true);
   }
   
   public AnchorableFrame(String title, boolean autoFocus)
   {
      super(title);
      autoFocus_ = autoFocus;
      setStylePrimaryName("rstudio-HelpFrame");
      getElement().getStyle().setBackgroundColor("white");
   }
       
   public void navigate(final String url)
   {
      RepeatingCommand navigateCommand = new RepeatingCommand() {
         @Override
         public boolean execute()
         {
            if (getIFrame() != null && getWindow() != null)
            {
               // if this is the same page but without an anchor qualification
               // then reload (so we preserve the anchor location)
               if (isBasePageOfCurrentAnchor(url))
               {
                  getWindow().reload();
               }
               // if it's the same page then set the anchor and force a reload
               else if (isSamePage(url))
               {
                  getWindow().replaceLocationHref(url);
                  getWindow().reload();
               }
               // otherwise a new url, merely replacing will force a reload
               else
               {
                  getWindow().replaceLocationHref(url);
               }
               
               if (autoFocus_)
                  getWindow().focus();
               
               return false;
            }
            else
            {
               return true;
            }
         }
      };

      if (navigateCommand.execute())
         Scheduler.get().scheduleFixedDelay(navigateCommand, 50);      
   }
   
   private boolean isBasePageOfCurrentAnchor(String newUrl)
   {
      // make sure there is an existing url to compare to
      String existingUrl = getWindow().getLocationHref();
      if (existingUrl == null)
         return false;      
     
      return newUrl == stripAnchor(existingUrl);
   }
   
   private boolean isSamePage(String newUrl)
   {
      // make sure there is an existing url to compare to
      String existingUrl = getWindow().getLocationHref();
      if (existingUrl == null)
         return false;      
      
      return stripAnchor(newUrl) == stripAnchor(existingUrl);
   }
   
   private String stripAnchor(String url)
   {
      int hashPos = url.lastIndexOf('#');
      if (hashPos != -1)
         return url.substring(0, hashPos);
      else
         return url;
   }
   
   private final boolean autoFocus_;

}
