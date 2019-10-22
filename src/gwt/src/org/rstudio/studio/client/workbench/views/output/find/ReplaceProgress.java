/*
 * ReplaceProgress.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.find;

import java.util.Date;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ProgressBar;
import org.rstudio.studio.client.workbench.views.output.find.FindOutputPresenter;
import org.rstudio.studio.client.workbench.views.output.find.model.LocalReplaceProgress;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ReplaceProgress extends Composite
                         //implements ReplaceProgressPresenter.Display
{

   private static ReplaceProgressUiBinder uiBinder = GWT.create(ReplaceProgressUiBinder.class);

   interface ReplaceProgressUiBinder extends UiBinder<Widget, ReplaceProgress>
   {
   }

   public ReplaceProgress()
   {
      initWidget(uiBinder.createAndBindUi(this));
      complete_ = false;
      
      progress_.setHeight("10px");
   }
   
   //@Override
   public void showProgress(LocalReplaceProgress progress)
   {
      progress_.setProgress(progress.units(), progress.max());
      replaceProgress_ = progress;
   }
   
   /*
   @Override
   public void showReplace(Replace replace)
   {
      String status = ReplaceConstants.stateDescription(replace.state);
      if (replace.completed > 0)
      {
         // Replace is not running; show its completion status and time
         progress_.setVisible(false);
         status += " " + StringUtil.friendlyDateTime(new Date(replace.completed * 1000));
         elapsed_.setText(StringUtil.conciseElaspedTime(replace.completed - replace.started));
         status_.setVisible(true);
      }
      else if (replace.max > 0)
      {
         // Replace is running and has a progress bar; show it and hide the status indicator
         progress_.setVisible(true);
         progress_.setProgress(replace.progress, replace.max);
         status_.setVisible(false);
      }
      else
      {
         // Replace is running but does not have progress; show the status field
         progress_.setVisible(false);
         status_.setVisible(true);
         if (!StringUtil.isNullOrEmpty(replace.status))
         {
            // Still running; show its status
            status = replace.status;
         }
      }
      status_.setText(status);
      replaceProgress_ = new LocalReplaceProgress(replace);
      complete_ = replace.completed > 0;
   }
   */

   //@Override
   public void updateElapsed(int timestamp)
   {
      if (replaceProgress_ == null || complete_)
         return;
      
      int delta = timestamp - replaceProgress_.received();
      elapsed_.setText(StringUtil.conciseElaspedTime(replaceProgress_.elapsed() + delta));
   }

   @UiField ProgressBar progress_;
   @UiField Label elapsed_;
   @UiField Label status_;
   
   private LocalReplaceProgress replaceProgress_;
   private boolean complete_;
}
