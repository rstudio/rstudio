/*
 * HyperlinkLabel.java
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

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.Label;
import org.rstudio.core.client.HandlerRegistrations;

public class HyperlinkLabel extends Label 
{
   public HyperlinkLabel()
   {
      super();
      this.setStyleName("rstudio-HyperlinkLabel");
   }
   
   public HyperlinkLabel(String caption, ClickHandler clickHandler)
   {
      super(caption); 
      clickHandler_ = clickHandler ;
      this.setStyleName("rstudio-HyperlinkLabel");
   }
   
   public HyperlinkLabel(String caption)
   {
      this(caption, null);
   }
   
   // must call this before the element is loaded
   public void setClickHandler(ClickHandler clickHandler)
   {
      clickHandler_ = clickHandler; 
   }

   private class MouseHandlers implements MouseOverHandler,
                                          MouseOutHandler
   {
      public void onMouseOver(MouseOverEvent event)
      {
         if (!alwaysUnderline_)
            addStyleDependentName("Link");
      }

      public void onMouseOut(MouseOutEvent event)
      {
         if ( !alwaysUnderline_)
            removeStyleDependentName("Link");
      }
   }
   
   public void setAlwaysUnderline(boolean alwaysUnderline)
   {
      alwaysUnderline_ = alwaysUnderline;
      if (alwaysUnderline_)
         addStyleDependentName("Link");
      else
         removeStyleDependentName("Link");
   }
   
   public void setClearUnderlineOnClick(boolean clearOnClick)
   {
      clearUnderlineOnClick_ = clearOnClick;
   }
  
   
   @Override 
   protected void onLoad()
   {
      releaseOnUnload_.add(addMouseOverHandler(mouseHandlers_));
      releaseOnUnload_.add(addMouseOutHandler(mouseHandlers_));
      if (clickHandler_ != null)
         releaseOnUnload_.add(addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event)
            {
               if (clearUnderlineOnClick_)
                  removeStyleDependentName("Link");
               clickHandler_.onClick(event);        
            }
            
         }));
   }
  
   private MouseHandlers mouseHandlers_ = new MouseHandlers();
   private ClickHandler clickHandler_ ;
   private final HandlerRegistrations releaseOnUnload_ = new HandlerRegistrations();
  
   
   private boolean alwaysUnderline_ = false;
   private boolean clearUnderlineOnClick_ = false;
}
