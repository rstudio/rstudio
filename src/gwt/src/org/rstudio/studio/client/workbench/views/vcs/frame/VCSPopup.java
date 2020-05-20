/*
 * VCSPopup.java
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
package org.rstudio.studio.client.workbench.views.vcs.frame;

import java.util.ArrayList;

import com.google.gwt.dom.client.Style.Unit;

import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.views.vcs.common.events.SwitchViewEvent;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;

public class VCSPopup
{
   public interface Controller
   {
      void switchToHistory(FileSystemItem fileFilter);
      void switchToReview(ArrayList<StatusAndPath> selected);
   }
   
   public static Controller show(final LayoutPanel swapContainer,
                                 final ReviewPresenter rpres,
                                 final HistoryPresenter hpres,
                                 boolean showHistory)
   {
      final Widget review = rpres.asWidget();
      review.setSize("100%", "100%");

      final Widget history = hpres.asWidget();
      history.setSize("100%", "100%");

      swapContainer.setSize("100%", "100%");
      swapContainer.add(review);
      swapContainer.setWidgetLeftRight(review, 0, Unit.PX, 0, Unit.PX);
      swapContainer.setWidgetTopBottom(review, 0, Unit.PX, 0, Unit.PX);
      swapContainer.add(history);
      swapContainer.setWidgetLeftRight(history, 0, Unit.PX, 0, Unit.PX);
      swapContainer.setWidgetTopBottom(history, 0, Unit.PX, 0, Unit.PX);

      if (showHistory)
      {
         swapContainer.setWidgetVisible(review, false);
         hpres.onShow();
      }
      else
      {
         swapContainer.setWidgetVisible(history, false);
         rpres.onShow();
      }
      
      // create a controller used to implement switch view and to return 
      final Controller controller = new Controller() {
         @Override
         public void switchToHistory(FileSystemItem fileFilter)
         {
            if (fileFilter != null)
               hpres.setFileFilter(fileFilter);
            
            hpres.onShow();
            swapContainer.setWidgetVisible(history, true);
            swapContainer.setWidgetVisible(review, false);    
         }

         @Override
         public void switchToReview(ArrayList<StatusAndPath> selected)
         {
            if (selected != null)
               rpres.setSelectedPaths(selected);
            
            rpres.onShow();
            swapContainer.setWidgetVisible(review, true);
            swapContainer.setWidgetVisible(history, false);
         }
         
      };

      rpres.addSwitchViewHandler(new SwitchViewEvent.Handler() {
         @Override
         public void onSwitchView(SwitchViewEvent event)
         {
            controller.switchToHistory(null);
         }
      });
      hpres.addSwitchViewHandler(new SwitchViewEvent.Handler() {
         @Override
         public void onSwitchView(SwitchViewEvent event)
         {
            controller.switchToReview(null);
         }
      });
      
      return controller;
   }
   
   
}
