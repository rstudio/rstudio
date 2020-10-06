/*
 * HistoryPanel.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.AbstractPager;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.HasData;
import com.google.inject.Inject;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.HistoryBranchToolbarButton;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.CommitDetailDisplay;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.CommitListDisplay;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.Display;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.DisplayBuilder;

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

   public static class Builder implements DisplayBuilder
   {
      @Inject
      public Builder(HistoryBranchToolbarButton branchToolbarButton,
                     CommitFilterToolbarButton commitFilterToolbarButton,
                     Commands commands)
      {
         branchToolbarButton_ = branchToolbarButton;
         commitFilterToolbarButton_ = commitFilterToolbarButton;
         commands_ = commands;
      }

      public HistoryPanel build(HistoryStrategy strategy)
      {
         return new HistoryPanel(branchToolbarButton_,
                                 commitFilterToolbarButton_,
                                 commands_,
                                 strategy);
      }

      private final HistoryBranchToolbarButton branchToolbarButton_;
      private final CommitFilterToolbarButton commitFilterToolbarButton_;
      private final Commands commands_;
   }

   protected HistoryPanel(HistoryBranchToolbarButton branchToolbarButton,
                          CommitFilterToolbarButton commitFilterToolbarButton,
                          Commands commands,
                          HistoryStrategy strategy)
   {
      Styles styles = GWT.<Resources>create(Resources.class).styles();
      commitTable_ = new CommitListTable(styles, strategy.idColumnName());
      splitPanel_ = new SplitLayoutPanel(4);
      pager_ = strategy.getPager();
      branchToolbarButton_ = branchToolbarButton;
      commitFilterToolbarButton_ = commitFilterToolbarButton;
      topToolbar_ = new Toolbar("History");

      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      commitDetail_.setIdDesc(strategy.idColumnName());
      commitDetail_.setScrollPanel(detailScrollPanel_);

      topToolbar_.addStyleName(styles.toolbar());

      switchViewButton_ = new LeftRightToggleButton("Changes", "History", false);
      topToolbar_.addLeftWidget(switchViewButton_);
      topToolbar_.addLeftWidget(branchToolbarButton_);

      topToolbar_.addLeftWidget(commitFilterToolbarButton_);
      
      topToolbar_.addLeftSeparator();
      
      refreshButton_ = new ToolbarButton(
                                 ToolbarButton.NoText,
                                 commands.vcsRefresh().getTooltip(),
                                 commands.vcsRefresh().getImageResource(),
                                 (ClickHandler) null);
      topToolbar_.addLeftWidget(refreshButton_); 
      
      searchText_ = new SearchWidget("Search version control history", 
                                     new MultiWordSuggestOracle(),
                                     new TextBoxWithCue("Search"),
                                     null);
      topToolbar_.addRightWidget(searchText_);
      topToolbar_.addRightSeparator();

      topToolbar_.addRightWidget(commands.vcsPull().createToolbarButton());
      
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
   public HasValue<FileSystemItem> getFileFilter()
   {
      return commitFilterToolbarButton_;
   }

   @Override
   public void removeBranchToolbarButton()
   {
      topToolbar_.removeLeftWidget(branchToolbarButton_);
      commitFilterToolbarButton_.getElement().getStyle().setMarginLeft(10,
                                                                       Unit.PX);
   }

   @Override
   public void removeSearchTextBox()
   {
      searchText_.removeFromParent();
   }

   @Override
   public HasValue<String> getSearchTextBox()
   {
      return searchText_;
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
   @UiField(provided = true)
   Toolbar topToolbar_;
   @UiField(provided = true)
   CommitListTable commitTable_;
   @UiField
   CommitDetail commitDetail_;
   @UiField
   ScrollPanel detailScrollPanel_;
   @UiField(provided = true)
   AbstractPager pager_;

   SearchWidget searchText_;

   private LeftRightToggleButton switchViewButton_;

   static
   {
      GWT.<Resources>create(Resources.class).styles().ensureInjected();
   }

   private ToolbarButton refreshButton_;
   private final HistoryBranchToolbarButton branchToolbarButton_;
   private final CommitFilterToolbarButton commitFilterToolbarButton_;
}
