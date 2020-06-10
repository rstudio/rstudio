/*
 * SlideNavigationPresenter.java
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

package org.rstudio.studio.client.common.presentation;


import org.rstudio.studio.client.common.presentation.events.SlideIndexChangedEvent;
import org.rstudio.studio.client.common.presentation.events.SlideNavigationChangedEvent;
import org.rstudio.studio.client.common.presentation.model.SlideNavigation;
import org.rstudio.studio.client.common.presentation.model.SlideNavigationItem;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;

public class SlideNavigationPresenter implements 
                                 SlideNavigationChangedEvent.Handler,
                                 SlideIndexChangedEvent.Handler
{
   public interface Display
   {     
      void navigate(int index);
      
      void editCurrentSlide();
      
      SlideNavigationMenu getNavigationMenu();
      
      HandlerRegistration addSlideNavigationChangedHandler(
                              SlideNavigationChangedEvent.Handler handler);
      
      HandlerRegistration addSlideIndexChangedHandler(
                              SlideIndexChangedEvent.Handler handler);
   }
   
   public SlideNavigationPresenter(Display view)
   {
      view_ = view;
      
      view_.addSlideNavigationChangedHandler(this);
      view_.addSlideIndexChangedHandler(this);
      
      view_.getNavigationMenu().getHomeButton().addClickHandler(
            new ClickHandler() {
               @Override
               public void onClick(ClickEvent event)
               {
                  view_.navigate(0);                                   
               }
      });
      
      view_.getNavigationMenu().getEditButton().addClickHandler(
            new ClickHandler() {
               @Override
               public void onClick(ClickEvent event)
               {
                  view_.editCurrentSlide();
               }
      });
   }
  
 
   @Override
   public void onSlideNavigationChanged(SlideNavigationChangedEvent event)
   {  
      slideNavigation_ = event.getNavigation();
      
      SlideNavigationMenu navigationMenu = view_.getNavigationMenu();
      navigationMenu.clear(); 
      
      if (slideNavigation_ != null)
      {  
         JsArray<SlideNavigationItem> items = slideNavigation_.getItems();
         for (int i=0; i<items.length(); i++)
         {
            // get slide
            final SlideNavigationItem item = items.get(i);
             
            // build html
            SafeHtmlBuilder menuHtml = new SafeHtmlBuilder();
            for (int j=0; j<item.getIndent(); j++)
               menuHtml.appendHtmlConstant("&nbsp;&nbsp;&nbsp;");
            menuHtml.appendEscaped(item.getTitle());
         
            navigationMenu.addItem(new MenuItem(menuHtml.toSafeHtml(),
                                           new Command() {
               @Override
               public void execute()
               {
                  view_.navigate(item.getIndex()); 
               }
            })); 
         }  
         
         navigationMenu.setVisible(true);
         
         navigationMenu.setDropDownVisible(
                                 slideNavigation_.getItems().length() > 1);
      }
      else
      {
         navigationMenu.setVisible(false);
      }
      
   }
   
   @Override
   public void onSlideIndexChanged(SlideIndexChangedEvent event)
   {
      // find the first navigation item that is <= to the index
      int index = event.getIndex();
      if (slideNavigation_ != null)
      {
         JsArray<SlideNavigationItem> items = slideNavigation_.getItems();
         for (int i=(items.length()-1); i>=0; i--)
         {
            if (items.get(i).getIndex() <= index)
            {
               String caption = items.get(i).getTitle();
               caption += " (" + (index+1) + "/" + 
                          slideNavigation_.getTotalSlides() + ")";
               
               
               view_.getNavigationMenu().setCaption(caption);
               break;
            }
         }
      }
      
   }
   
   private final Display view_;
   private SlideNavigation slideNavigation_ = null;
}
