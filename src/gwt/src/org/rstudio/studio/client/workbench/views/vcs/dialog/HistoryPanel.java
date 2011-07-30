/*
 * HistoryPanel.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import org.rstudio.core.client.widget.LeftRightToggleButton;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.CommitDetailDisplay;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.CommitListDisplay;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.Display;

import java.util.ArrayList;

public class HistoryPanel extends Composite implements Display
{
   public interface Resources extends ClientBundle
   {
      @Source("HistoryPanel.css")
      Styles styles();
   }

   public interface Styles extends SharedStyles
   {
      String commitDetail();
   }

   interface Binder extends UiBinder<Widget, HistoryPanel>
   {}

   public HistoryPanel()
   {
      splitPanel_ = new SplitLayoutPanel(4);
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));
      Styles styles = GWT.<Resources>create(Resources.class).styles();

      topToolbar_.addStyleName(styles.toolbar());

      switchViewButton_ = new LeftRightToggleButton("Changes", "History", false);
      topToolbar_.addLeftWidget(switchViewButton_);
   }

   @Override
   public void setData(ArrayList<CommitInfo> commits)
   {
      commitTable_.setData(commits);
   }

   @Override
   public HasClickHandlers getSwitchViewButton()
   {
      return switchViewButton_;
   }

   @Override
   public CommitListDisplay getCommitList()
   {
      return commitTable_;
   }

   @Override
   public CommitDetailDisplay getCommitDetail()
   {
      return commitDetail_;
   }

   @UiField(provided = true)
   SplitLayoutPanel splitPanel_;
   @UiField
   Toolbar topToolbar_;
   @UiField
   CommitListTable commitTable_;
   @UiField
   CommitDetail commitDetail_;

   private LeftRightToggleButton switchViewButton_;

   static
   {
      GWT.<Resources>create(Resources.class).styles().ensureInjected();
   }
}
