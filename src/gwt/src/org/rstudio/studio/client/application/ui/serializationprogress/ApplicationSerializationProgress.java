/*
 * ApplicationSerializationProgress.java
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

package org.rstudio.studio.client.application.ui.serializationprogress;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;


public class ApplicationSerializationProgress extends PopupPanel 
{
   public ApplicationSerializationProgress(String message, 
                                           boolean modal, 
                                           int delayMs,
                                           boolean announce)
   {
      super(false, modal);
      addStyleName(RESOURCES.styles().panel());
      
      if (modal)
      {
         //setGlassEnabled(true);
         setGlassStyleName(RESOURCES.styles().glass());
      }

      ApplicationSerializationProgressLabel label =
            new ApplicationSerializationProgressLabel(announce);
      label.setText(message);

      // set widget
      setWidget(label);
      
      // show (after specified delay so long as we were not already hidden)
      if (delayMs > 0)
      {
         new Timer() { public void run()
         {
            if (!wasHidden_)
               showProgress();
            
         }}.schedule(delayMs);
      }
      else
      {
         showProgress();
      }
   } 
   
   @Override
   public void hide()
   {
      super.hide();
      wasHidden_ = true;
   }
   
   private void showProgress()
   {
      setPopupPositionAndShow( new PopupPanel.PositionCallback() 
      {
         public void setPosition(int width, int height) 
         {
            setPopupPosition((Window.getClientWidth()/2) - (width/2), 0);
         }
      });
   }
   
   private boolean wasHidden_ = false;
   
   public interface Styles extends CssResource
   {
      String panel();
      String glass();
      String spinner();
      String label();
      String left();
      String center();
      String right();
   }
  
   public interface Resources extends ClientBundle
   {
      @Source("ApplicationSerializationProgress.css")
      Styles styles();

      ImageResource spinnerManilla();
      ImageResource statusPopupLeft();
      ImageResource statusPopupRight();
      @ImageOptions(repeatStyle=RepeatStyle.Horizontal)
      ImageResource statusPopupTile();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
            StyleInjector.inject(
            "." + RESOURCES.styles().glass() +
            " {filter: alpha(opacity = 0) !important;}");
   }
 
}
