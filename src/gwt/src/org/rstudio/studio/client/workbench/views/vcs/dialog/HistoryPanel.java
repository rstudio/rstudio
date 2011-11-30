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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.HasData;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.HistoryBranchToolbarButton;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.CommitDetailDisplay;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.CommitListDisplay;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.Display;

public class HistoryPanel extends Composite implements Display
{
   public interface Resources extends ClientBundle
   {
      @Source("HistoryPanel.css")
      Styles styles();
   }

   interface SimplePagerResources extends SimplePager.Resources
   {
      @Override
      @Source("images/PageForwardButton.png")
      ImageResource simplePagerFastForward();

      @Override
      @Source("images/PageForwardButtonDisabled.png")
      ImageResource simplePagerFastForwardDisabled();

      @Override
      @Source("images/PageFirstButton.png")
      ImageResource simplePagerFirstPage();

      @Override
      @Source("images/PageFirstButtonDisabled.png")
      ImageResource simplePagerFirstPageDisabled();

      @Override
      @Source("images/PageLastButton.png")
      ImageResource simplePagerLastPage();

      @Override
      @Source("images/PageLastButtonDisabled.png")
      ImageResource simplePagerLastPageDisabled();

      @Override
      @Source("images/PageNextButton.png")
      ImageResource simplePagerNextPage();

      @Override
      @Source("images/PageNextButtonDisabled.png")
      ImageResource simplePagerNextPageDisabled();

      @Override
      @Source("images/PagePreviousButton.png")
      ImageResource simplePagerPreviousPage();

      @Override
      @Source("images/PagePreviousButtonDisabled.png")
      ImageResource simplePagerPreviousPageDisabled();

      @Override
      @Source({"com/google/gwt/user/cellview/client/SimplePager.css",
              "SimplePagerStyle.css"})
      SimplePagerStyle simplePagerStyle();
   }

   interface SimplePagerStyle extends SimplePager.Style
   {

   }

   public interface Styles extends SharedStyles
   {
      String commitDetail();
      String commitTableScrollPanel();

      String ref();
      String head();
      String branch();
      String remote();
      String tag();

      String graphLineImg();
   }

   interface Binder extends UiBinder<Widget, HistoryPanel>
   {}

   @Inject
   public HistoryPanel(HistoryBranchToolbarButton branchToolbarButton,
                       Commands commands)
   {
      Styles styles = GWT.<Resources>create(Resources.class).styles();
      commitTable_ = new CommitListTable(styles);
      splitPanel_ = new SplitLayoutPanel(4);
      pager_ = new SimplePager(
            TextLocation.CENTER,
            GWT.<SimplePagerResources>create(SimplePagerResources.class),
            true, 500, true);
      pager_.getElement().setAttribute("align", "center");
      branchToolbarButton_ = branchToolbarButton;

      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      commitDetail_.setScrollPanel(detailScrollPanel_);

      topToolbar_.addStyleName(styles.toolbar());

      switchViewButton_ = new LeftRightToggleButton("Changes", "History", false);
      topToolbar_.addLeftWidget(switchViewButton_);
      topToolbar_.addLeftWidget(branchToolbarButton_);


      filterText_ = new SearchWidget(new MultiWordSuggestOracle(),
                                     new TextBoxWithCue("Search"),
                                     null);
      topToolbar_.addRightWidget(filterText_);
      topToolbar_.addRightSeparator();

      refreshButton_ = new ToolbarButton(
            "Refresh", commands.vcsRefresh().getImageResource(),
            (ClickHandler) null);
      topToolbar_.addRightWidget(refreshButton_);

      topToolbar_.addRightSeparator();

      topToolbar_.addRightWidget(commands.vcsPull().createToolbarButton());
     
      topToolbar_.addRightSeparator();

      topToolbar_.addRightWidget(commands.vcsPush().createToolbarButton());

      pager_.setPageSize(100);
      pager_.setDisplay(commitTable_);

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

   @Override
   public HasClickHandlers getOverrideSizeWarningButton()
   {
      return commitDetail_.getOverrideSizeWarningButton();
   }

   @Override
   public HasClickHandlers getRefreshButton()
   {
      return refreshButton_;
   }

   @Override
   public HasData<CommitInfo> getDataDisplay()
   {
      return commitTable_;
   }

   @Override
   public HasValue<String> getFilterTextBox()
   {
      return filterText_;
   }
   
   @Override 
   public void setPageStart(int pageStart)
   {
      commitTable_.setPageStart(pageStart);
   }
   
   @Override
   public HandlerRegistration addBranchChangedHandler(
                                       ValueChangeHandler<String> handler)
   {
      return branchToolbarButton_.addValueChangeHandler(handler);
   }
  
   @Override
   public void showSizeWarning(long sizeInBytes)
   {
      commitDetail_.showSizeWarning(sizeInBytes);
   }

   @Override
   public void hideSizeWarning()
   {
      commitDetail_.hideSizeWarning();
   }

   @Override
   public void onShow()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            commitTable_.setFocus(true);
         }
      });
   }

   @UiField(provided = true)
   SplitLayoutPanel splitPanel_;
   @UiField
   Toolbar topToolbar_;
   @UiField(provided = true)
   CommitListTable commitTable_;
   @UiField
   CommitDetail commitDetail_;
   @UiField
   ScrollPanel detailScrollPanel_;
   @UiField(provided = true)
   SimplePager pager_;

   SearchWidget filterText_;

   private LeftRightToggleButton switchViewButton_;

   static
   {
      GWT.<Resources>create(Resources.class).styles().ensureInjected();
   }

   private ToolbarButton refreshButton_;
   private final HistoryBranchToolbarButton branchToolbarButton_;
}
