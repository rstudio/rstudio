/*
 * RStudioFrame.java
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

package org.rstudio.core.client.widget;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.NodeWebkit;

import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Frame;

public class RStudioFrame extends Frame
{
   public RStudioFrame()
   {
      this(null);
   }
   
   public RStudioFrame(String url)
   {
      super();
      if (url != null)
         setUrl(url);
      
      if (NodeWebkit.isNodeWebkit())
      {
         addLoadHandler(new LoadHandler() {
            @Override
            public void onLoad(LoadEvent event)
            {
               try
               {
                  NodeWebkit.fixupIFrameLinks(getWindow().getDocument());
               }
               catch(Exception e)
               {
                  Debug.log("Error fixing up iframe links: " + e.getMessage()); 
               }
            }
         });
      }
   }
   
   public WindowEx getWindow()
   {
      return getIFrame().getContentWindow();
   }
   
   public IFrameElementEx getIFrame()
   {
      return getElement().cast();
   }

}
