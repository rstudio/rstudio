/*
 * ChunkOutputFrame.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.widget.DynamicIFrame;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;

public class ChunkOutputFrame extends DynamicIFrame
{
   /**
    * @param title a11y title
    */
   public ChunkOutputFrame(String title)
   {
      super(title);
      timer_ = new Timer() 
      {
         @Override
         public void run()
         {
            loadUrl(url_, onCompleted_);
         }
      };
   }

   void loadUrl(String url, Command onCompleted)
   {
      onCompleted_ = onCompleted;
      super.setUrl(url);
   }
   
   /**
    * Loads a URL after a fixed amount of time.
    * 
    * @param url The URL to load.
    * @param delayMs The number of milliseconds to delay before loading the URL.
    * @param onCompleted The command to run after the time has passed *and* the
    *   URL had been loaded.
    */
   void loadUrlDelayed(String url, int delayMs, 
         Command onCompleted)
   {
      // prevent stacking timers
      if (timer_.isRunning())
         timer_.cancel();
      
      onCompleted_ = onCompleted;
      loaded_ = false;
      url_ = url;

      timer_.schedule(delayMs);
   }
   
   public void cancelPendingLoad()
   {
      if (timer_.isRunning())
         timer_.cancel();
   }

   @Override
   public String getUrl()
   {
      // return the pending URL if we haven't loaded one yet
      if (timer_.isRunning())
         return url_;
      else
         return super.getUrl();
   }
   
   @Override
   protected void onFrameLoaded()
   {
      loaded_ = true;
      if (onCompleted_ != null)
         onCompleted_.execute();
   }

   public void runAfterRender(final Command command)
   {
      if (command == null) return;

      Timer onRenderedTimer = new Timer() {
         private int retryCount_ = 0;

         @Override
         public void run()
         {
            if (loaded_) {
               Element body = getDocument().getBody();

               if (body != null && body.getChildCount() > 0) {
                  command.execute();
               } else if (retryCount_ < 50) {
                  retryCount_++;
                  schedule(100);
               }
            }
         }
      };
      
      onRenderedTimer.schedule(100);
   }
   
   private final Timer timer_;
   private String url_;
   private Command onCompleted_;
   private boolean loaded_ = false;
}
