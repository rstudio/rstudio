/*
 * WizardIntermediatePage.java
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

import java.util.ArrayList;

import org.rstudio.core.client.CommandWithArg;

import com.google.gwt.resources.client.ImageResource;

// An intermediate wizard page collects input but passes that input to a single
// next page rather than completing the wizard.
public abstract class WizardIntermediatePage<I,T> extends WizardPage<I,T>
{
   public WizardIntermediatePage(String title, String subTitle,
         String pageCaption, ImageResource image, ImageResource largeImage, 
         WizardPage<I, T> nextPage)
   {
      super(title, subTitle, pageCaption, image, largeImage);
      nextPage_ = nextPage;
   }
   
   public void setNextHandler(CommandWithArg<WizardPage<I, T>> command)
   {
      nextHandler_ = command;
   }
   
   public void collectIntermediateInput(ProgressIndicator indicator,
         OperationWithInput<T> result)
   {
      result.execute(intermediateResult_);
   }
   
   public void setIntermediateResult(T result) 
   {
      intermediateResult_ = result;
   }

   @Override
   public ArrayList<WizardPage<I, T>> getSubPages()
   {
      // we have a single subpage, which is the next page we were initialized
      // with
      ArrayList<WizardPage<I, T>> subPage = new ArrayList<WizardPage<I, T>>();
      subPage.add(nextPage_);
      return subPage;
   }
   
   protected void advance() 
   {
      if (nextHandler_ != null)
         nextHandler_.execute(nextPage_);
   }
   
   protected T getIntermediateResult()
   {
      return intermediateResult_;
   }
   
   private T intermediateResult_;
   
   private CommandWithArg<WizardPage<I, T>> nextHandler_;
   private WizardPage<I, T> nextPage_;
}
