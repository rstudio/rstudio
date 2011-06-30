/*
 * ReviewPanel.java
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.vcs.ReviewPresenter.Display;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTablePresenter;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTableView;

public class ReviewPanel extends Composite implements Display
{
   interface Binder extends UiBinder<Widget, ReviewPanel>
   {
   }

   @Inject
   public ReviewPanel(ChangelistTable changelist,
                      LineTableView diffPane)
   {
      stageButton_ = new ThemedButton("Stage");
      discardButton_ = new ThemedButton("Discard");
      changelist_ = changelist;
      lines_ = diffPane;

      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));
   }

   @Override
   public HasClickHandlers getStageButton()
   {
      return stageButton_;
   }

   @Override
   public HasClickHandlers getDiscardButton()
   {
      return discardButton_;
   }

   @Override
   public LineTablePresenter.Display getLineTableDisplay()
   {
      return lines_;
   }

   @Override
   public ChangelistTable getChangelistTable()
   {
      return changelist_;
   }

   @UiField(provided = true)
   ThemedButton stageButton_;
   @UiField(provided = true)
   ThemedButton discardButton_;
   @UiField(provided = true)
   ChangelistTable changelist_;
   @UiField(provided = true)
   LineTableView lines_;
}
