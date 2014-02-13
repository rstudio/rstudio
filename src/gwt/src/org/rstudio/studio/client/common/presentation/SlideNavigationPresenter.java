/*
 * SlideNavigationPresenter.java
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

package org.rstudio.studio.client.common.presentation;

import org.rstudio.studio.client.workbench.views.presentation.model.SlideNavigation;
import org.rstudio.studio.client.workbench.views.presentation.model.SlideNavigationItem;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;

public class SlideNavigationPresenter
{
   public SlideNavigationPresenter(SlideView view)
   {
      view_ = view;
   }
   
   public void setSlideNavigation(SlideNavigation navigation)
   {
      slideNavigation_ = navigation;
      JsArray<SlideNavigationItem> items = slideNavigation_.getItems();
      
      // reset the slides menu
      SlideNavigationMenu slideMenu = view_.getSlideMenu();
      slideMenu.clear(); 
      for (int i=0; i<items.length(); i++)
      {
         // get slide
         final SlideNavigationItem item = items.get(i);
          
         // build html
         SafeHtmlBuilder menuHtml = new SafeHtmlBuilder();
         for (int j=0; j<item.getIndent(); j++)
            menuHtml.appendHtmlConstant("&nbsp;&nbsp;&nbsp;");
         menuHtml.appendEscaped(item.getTitle());
         
      
         slideMenu.addItem(new MenuItem(menuHtml.toSafeHtml(),
                                        new Command() {
            @Override
            public void execute()
            {
               view_.slide(item.getIndex()); 
            }
         })); 
      }  
      
      slideMenu.setDropDownVisible(slideNavigation_.getItems().length() > 1);
   }
   
   public void setSlideIndex(int index)
   {
      // find the first navigation item that is <= to the index
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
               
               
               view_.getSlideMenu().setCaption(caption);
               break;
            }
         }
      }
   }
   
   
   private final SlideView view_;
   private SlideNavigation slideNavigation_ = null;
   
}
