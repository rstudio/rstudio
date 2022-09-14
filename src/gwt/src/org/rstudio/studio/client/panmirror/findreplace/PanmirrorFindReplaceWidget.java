/*
 * PanmirrorFindReplaceWidget.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */



package org.rstudio.studio.client.panmirror.findreplace;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.widget.HasFindReplace;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.panmirror.PanmirrorConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;

import com.google.gwt.core.client.Scheduler;

public class PanmirrorFindReplaceWidget extends FindReplaceBar implements HasFindReplace
{
   public interface Container 
   {
      PanmirrorFindReplace getFindReplace();
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
      getCloseButton().addClickHandler((event) -> {
         container_.showFindReplace(false);
      });
      
      // re-execute search on changes
      addFindKeyUpHandler((event) -> {
         
         int keycode = event.getNativeKeyCode();
         if (KeyboardHelper.isNavigationalKeycode(keycode) ||
             KeyboardHelper.isControlKeycode(keycode))
         {
            return;
         }
         
         // perform incremental search
         performFind();
         
      });
      getCaseSensitive().addValueChangeHandler((e) -> {
         performFind();
      });
      getRegex().addValueChangeHandler((e) -> {
         performFind();
      });
      getWrapSearch().addValueChangeHandler((e) -> {
         performFind();
      });
      
      // perform find on text box focus
      addTextBoxFocusHandler((event) -> {
         performFind();
      });
      
      // hookup replace all
      getReplaceAll().addClickHandler((event) -> {
         PanmirrorFindReplace find = findWithResults();
         String text = getReplaceValue().getValue();
         int replaced = find.replaceAll(text);
         RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(
            GlobalDisplay.MSG_INFO,
            constants_.findReplaceTitle(),
            constants_.rStudioGinjectorErrorMessage(replaced)
         );
      });
     
      
   }
   
   public void performFind()
   {
      performFind(null);
   }
   
   public void performFind(String term)
   {
      if (term != null)
         getFindValue().setValue(term);
      timeBufferedFind_.nudge();
   }
   
   private TimeBufferedCommand timeBufferedFind_ = new TimeBufferedCommand(300) {
      @Override
      protected void performAction(boolean shouldReschedule)
      {
         PanmirrorFindReplace find = container_.getFindReplace();
         PanmirrorFindOptions options =  new PanmirrorFindOptions();
         options.caseSensitive = getCaseSensitive().getValue();
         options.regex = getRegex().getValue();
         options.wrap = getWrapSearch().getValue();
         find.find(getFindValue().getValue(), options);
         find.selectCurrent(); 
      }
   };
   
   
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
   public void hideFindReplace()
   {
      container_.showFindReplace(false);
   }
   
   @Override
   public void findFromSelection(String term)
   {
      showFindReplace(true);
      performFind(term);
   }


   @Override
   public void findNext()
   {
      PanmirrorFindReplace find = findWithResults();
      find.selectNext();
   }

   @Override
   public void findPrevious()
   {
      PanmirrorFindReplace find = findWithResults();
      find.selectPrevious();
      
   }

   @Override
   public void replaceAndFind()
   {
      PanmirrorFindReplace find = findWithResults();
      String text = getReplaceValue().getValue();
      find.replace(text);
      find.selectNext();
   }
   
   private PanmirrorFindReplace findWithResults()
   {
      PanmirrorFindReplace find = container_.getFindReplace();
      if (find.matches() == 0)
         performFind();
      return find;
   }
  
   
   private Container container_;

   private static final PanmirrorConstants constants_ = GWT.create(PanmirrorConstants.class);

}
