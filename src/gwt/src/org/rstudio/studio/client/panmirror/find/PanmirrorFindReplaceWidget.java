/*
 * PanmirrorFindReplaceWidget.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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



package org.rstudio.studio.client.panmirror.find;

import org.rstudio.core.client.widget.HasFindReplace;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;

import com.google.gwt.core.client.Scheduler;

public class PanmirrorFindReplaceWidget extends FindReplaceBar implements HasFindReplace
{
   public interface Container 
   {
      PanmirrorFind getPanmirrorFind();
      boolean isFindReplaceShowing();
      void showFindReplace(boolean show);
   }
  

   public PanmirrorFindReplaceWidget(Container container)
   {
      super(false, true, false, false, true);
      container_ = container;
      
      // set defaults
      getWrapSearch().setValue(true);
      
      // handle close
      getCloseButton().addClickHandler((value) -> {
         container_.showFindReplace(false);
      });
      
      // re-execute search on changes
      addFindKeyUpHandler((e) -> {
         performNewFind();
      });
      getCaseSensitive().addValueChangeHandler((e) -> {
         performNewFind();
      });
      getRegex().addValueChangeHandler((e) -> {
         performNewFind();
      });
      getWrapSearch().addValueChangeHandler((e) -> {
         performNewFind();
      });
         
      
   }
   
   
   @Override
   public boolean isFindReplaceShowing()
   {
      return container_.isFindReplaceShowing();
   }

   @Override
   public void showFindReplace(boolean show)
   {
      container_.showFindReplace(show);
      if (show) 
      {
         Scheduler.get().scheduleDeferred(() -> {
            activate(null, true, false);
         });
      }
     
   }

   @Override
   public void findNext()
   {
      PanmirrorFind find = container_.getPanmirrorFind();
      find.selectNext();
      
   }

   @Override
   public void findPrevious()
   {
      PanmirrorFind find = container_.getPanmirrorFind();
      find.selectPrevious();
      
   }

   @Override
   public void replaceAndFind()
   {
      // TODO Auto-generated method stub
      
   }
   
   private void performNewFind()
   {
      PanmirrorFind find = container_.getPanmirrorFind();
      PanmirrorFindOptions options =  new PanmirrorFindOptions();
      options.caseSensitive = getCaseSensitive().getValue();
      options.regex = getRegex().getValue();
      options.wrap = getWrapSearch().getValue();
      find.find(getFindValue().getValue(), options);
      find.selectNext();
   }
   
   private Container container_;

  

}
