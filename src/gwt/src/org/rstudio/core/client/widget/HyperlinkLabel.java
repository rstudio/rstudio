/*
 * HyperlinkLabel.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.Label;

public class HyperlinkLabel extends Label 
{
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
      addMouseOverHandler(mouseHandlers_);
      addMouseOutHandler(mouseHandlers_);
      if (clickHandler_ != null)
         addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event)
            {
               if (clearUnderlineOnClick_)
                  removeStyleDependentName("Link");
               clickHandler_.onClick(event);        
            }
            
         });
   }
  
  
   private MouseHandlers mouseHandlers_ = new MouseHandlers();
   private ClickHandler clickHandler_ ;
  
   
   private boolean alwaysUnderline_ = false;
   private boolean clearUnderlineOnClick_ = false;
}
