/*
 * WizardNavigationPage.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import java.util.ArrayList;

import org.rstudio.core.client.CommandWithArg;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;

public class WizardNavigationPage<I,T> extends WizardPage<I,T>
{
   public WizardNavigationPage(String title, 
                               String subTitle, 
                               String pageCaption, 
                               ImageResource image,
                               ImageResource largeImage,
                               ArrayList<WizardPage<I,T>> pages)
   {
      super(title, 
            subTitle, 
            pageCaption, 
            image, 
            largeImage,
            new WizardPageSelector<I,T>(pages));
      
      pages_ = pages;
   }
   
   @SuppressWarnings("unchecked")
   public void setSelectionHandler(CommandWithArg<WizardPage<I,T>> onSelected)
   {
      ((WizardPageSelector<I,T>)getWidget()).setSelectionHandler(
                                                                  onSelected);
   }

   
   public ArrayList<WizardPage<I,T>> getPages()
   {
      return pages_;
   }
  
   @Override
   public void focus()
   {
      getWidget().getElement().focus();
   }

   @Override
   protected Widget createWidget()
   {
      // handled in the constructor
      return null;
   }

   @Override
   protected void initialize(I initData)
   {  
   }
   
   @Override
   protected T collectInput()
   {
      return null;
   }

   @Override
   protected boolean validate(T input)
   {
      return false;
   }
   
   private ArrayList<WizardPage<I,T>> pages_;
}
