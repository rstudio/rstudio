/*
 * VCSPopup.java
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
package org.rstudio.studio.client.workbench.views.vcs.frame;

import com.google.gwt.dom.client.Style.Unit;

import com.google.gwt.user.client.ui.*;
import org.rstudio.studio.client.workbench.views.vcs.common.events.SwitchViewEvent;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;

public class VCSPopup
{
   public static void show(final LayoutPanel swapContainer,
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

      rpres.addSwitchViewHandler(new SwitchViewEvent.Handler() {
         @Override
         public void onSwitchView(SwitchViewEvent event)
         {
            hpres.onShow();
            swapContainer.setWidgetVisible(history, true);
            swapContainer.setWidgetVisible(review, false);
         }
      });
      hpres.addSwitchViewHandler(new SwitchViewEvent.Handler() {
         @Override
         public void onSwitchView(SwitchViewEvent event)
         {
            rpres.onShow();
            swapContainer.setWidgetVisible(review, true);
            swapContainer.setWidgetVisible(history, false);
         }
      });
   }
}
