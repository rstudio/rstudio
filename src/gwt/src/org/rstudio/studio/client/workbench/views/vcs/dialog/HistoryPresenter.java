/*
 * HistoryPresenter.java
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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RangeChangeEvent.Handler;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.viewfile.ViewFilePanel;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.DiffParser;
import org.rstudio.studio.client.workbench.views.vcs.common.events.SwitchViewEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ViewFileRevisionEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ViewFileRevisionHandler;
import org.rstudio.studio.client.workbench.views.vcs.git.dialog.GitHistoryStrategy;
import org.rstudio.studio.client.workbench.views.vcs.svn.dialog.SVNHistoryStrategy;

public class HistoryPresenter
{
   public interface DisplayBuilder
   {
      Display build(HistoryStrategy strategy);
   }

   public interface Display extends IsWidget
   {
      HasClickHandlers getSwitchViewButton();
      CommitListDisplay getCommitList();
      CommitDetailDisplay getCommitDetail();

      HasClickHandlers getOverrideSizeWarningButton();

      HasClickHandlers getRefreshButton();

      HasData<CommitInfo> getDataDisplay();

      HasValue<String> getSearchTextBox();

      void setPageStart(int pageStart);
      
      HandlerRegistration addBranchChangedHandler(
                                       ValueChangeHandler<String> handler);
      
      HasValue<FileSystemItem> getFileFilter();

      void removeBranchToolbarButton();
      void removeSearchTextBox();
      
      void showSizeWarning(long sizeInBytes);
      void hideSizeWarning();

      void onShow();
   }

   public interface CommitListDisplay
   {
      HandlerRegistration addSelectionChangeHandler(
            SelectionChangeEvent.Handler handler);

      HandlerRegistration addRangeChangeHandler(
            RangeChangeEvent.Handler handler);

      HandlerRegistration addLoadingStateChangeHandler(
            LoadingStateChangeEvent.Handler handler);

      CommitInfo getSelectedCommit();

      void clearSelection();

      void setAutoSelectFirstRow(boolean autoSelect);
   }

   public interface CommitDetailDisplay extends HasHandlers
   {
      void setSelectedCommit(CommitInfo commit);
      void clearDetails();
      void showDetailProgress();
      void setDetails(DiffParser unifiedParser, boolean suppressViewLink);
      void setCommitListIsLoading(boolean isLoading);
      
      HandlerRegistration addViewFileRevisionHandler(
                                          ViewFileRevisionHandler handler);
   }

   @Inject
   public HistoryPresenter(final Commands commands,
                           final GlobalDisplay globalDisplay,
                           final Provider<ViewFilePanel> pViewFilePanel,
                           final DisplayBuilder viewBuilder,
                           final Session session,
                           final Provider<GitHistoryStrategy> pGitStrategy,
                           final Provider<SVNHistoryStrategy> pSvnStrategy)
   {
      String vcsName = session.getSessionInfo().getVcsName();
      if (vcsName.equalsIgnoreCase("git"))
         strategy_ = pGitStrategy.get();
      else if (vcsName.equalsIgnoreCase("svn"))
         strategy_ = pSvnStrategy.get();
      else
         throw new IllegalStateException("Unknown vcs name: " + vcsName);

      view_ = viewBuilder.build(strategy_);

      view_.getCommitList().setAutoSelectFirstRow(
                                             strategy_.getAutoSelectFirstRow());

      if (strategy_.isBranchingSupported())
      {
         view_.addBranchChangedHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event)
            {
               strategy_.setRev(event.getValue());
               refreshHistory();
               view_.setPageStart(0);
            }
         });
      }
      else
      {
         view_.removeBranchToolbarButton();
      }
      
      view_.getCommitList().addSelectionChangeHandler(new SelectionChangeEvent.Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            showCommitDetail(false);
         }
      });
      view_.getCommitList().addRangeChangeHandler(new Handler()
      {
         @Override
         public void onRangeChange(RangeChangeEvent event)
         {
            view_.getCommitList().clearSelection();
         }
      });
      view_.getCommitList().addLoadingStateChangeHandler(new LoadingStateChangeEvent.Handler()
      {
         @Override
         public void onLoadingStateChanged(LoadingStateChangeEvent event)
         {
            view_.getCommitDetail().setCommitListIsLoading(
                  event.getLoadingState() == LoadingState.LOADING);
         }
      });

      view_.getRefreshButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            refreshHistory();
         }
      });

      view_.getOverrideSizeWarningButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            showCommitDetail(true);
         }
      });

      if (strategy_.isSearchSupported())
      {
         strategy_.setSearchText(view_.getSearchTextBox());
         view_.getSearchTextBox().addValueChangeHandler(new ValueChangeHandler<String>()
         {
            @Override
            public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent)
            {
               refreshHistoryCommand_.nudge();
            }
         });
      }
      else
      {
         view_.removeSearchTextBox();
      }
      
      strategy_.setFileFilter(view_.getFileFilter());
      view_.getFileFilter().addValueChangeHandler(new ValueChangeHandler<FileSystemItem>() {

         @Override
         public void onValueChange(ValueChangeEvent<FileSystemItem> event)
         {
            view_.getCommitDetail().clearDetails();
            view_.getCommitDetail().setSelectedCommit(null);
            refreshHistory();
            view_.setPageStart(0);
         }
      });
      
      view_.getCommitDetail().addViewFileRevisionHandler(
                                          new ViewFileRevisionHandler() {
         @Override
         public void onViewFileRevision(final ViewFileRevisionEvent event)
         {
            final ProgressIndicator indicator = 
                  new GlobalProgressDelayer(globalDisplay, 
                                            500,
                                            "Reading file...").getIndicator();
            
            strategy_.showFile(
                  event.getRevision(),
                  event.getFilename(),
                  new ServerRequestCallback<String>()
                  {

                     @Override
                     public void onResponseReceived(String contents)
                     {
                        indicator.onCompleted();

                        final ViewFilePanel viewFilePanel = pViewFilePanel.get();
                        
                        viewFilePanel.setSaveFileAsHandler(
                                          new ViewFilePanel.SaveFileAsHandler()
                        {
                           
                           @Override
                           public void onSaveFileAs(FileSystemItem source, 
                                                    FileSystemItem destination,
                                                    ProgressIndicator indicator)
                           {
                              strategy_.saveFileAs(event.getRevision(),
                                                   source.getPath(),
                                                   destination.getPath(),
                                                   indicator);
                           }
                        });
                        
                        viewFilePanel.getToolbar().addRightWidget(
                                                         new ToolbarButton(
                              "Show History",
                              ToolbarButton.NoTitle,
                              commands.goToWorkingDir().getImageResource(),
                              new ClickHandler() {

                               @Override
                               public void onClick(ClickEvent event)
                               {
                                  view_.getFileFilter().setValue(
                                              viewFilePanel.getTargetFile());
                                  viewFilePanel.close();
                                 
                               }
                                 
                              }));
                        
                        viewFilePanel.showFile(
                              event.getFilename() + " @ " + event.getRevision(),
                              FileSystemItem.createFile(event.getFilename()),
                              contents);
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        if (strategy_.getShowHistoryErrors())
                        {
                           indicator.onError(error.getUserMessage());
                        }
                        else
                        {
                           indicator.onCompleted();
                           Debug.logError(error);
                        }
                     }

                  });
         }
         
      });
   }

   private void showCommitDetail(boolean noSizeWarning)
   {
      final CommitInfo commitInfo = view_.getCommitList().getSelectedCommit();

      if (!noSizeWarning
          && commitInfo != null
          && commitInfo.getId() == commitShowing_)
      {
         return;
      }

      commitShowing_ = null;

      view_.hideSizeWarning();

      view_.getCommitDetail().setSelectedCommit(commitInfo);
      view_.getCommitDetail().showDetailProgress();
      invalidation_.invalidate();

      if (commitInfo == null)
         return;

      final Token token = invalidation_.getInvalidationToken();

      strategy_.showCommit(
            commitInfo.getId(),
            noSizeWarning,
            new SimpleRequestCallback<String>()
            {
               @Override
               public void onResponseReceived(String response)
               {
                  super.onResponseReceived(response);
                  if (token.isInvalid())
                     return;

                  DiffParser parser = strategy_.createParserForCommit(response);
                  view_.getCommitDetail().setDetails(
                                      parser, !strategy_.isShowFileSupported());
                  commitShowing_ = commitInfo.getId();
               }

               @Override
               public void onError(ServerError error)
               {
                  commitShowing_ = null;

                  JSONNumber size = error.getClientInfo().isNumber();
                  if (size != null)
                     view_.showSizeWarning((long) size.doubleValue());
                  else if (strategy_.getShowHistoryErrors())
                     super.onError(error);
                  else
                     Debug.logError(error);
               }
            });
   }

   private void refreshHistory()
   {
      strategy_.refreshCount();
      view_.getDataDisplay().setVisibleRangeAndClearData(new Range(0, 100), true);
   }

   public HandlerRegistration addSwitchViewHandler(
         final SwitchViewEvent.Handler h)
   {
      return view_.getSwitchViewButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            h.onSwitchView(new SwitchViewEvent());
         }
      });
   }


   public Widget asWidget()
   {
      return view_.asWidget();
   }
   
   public void setFileFilter(FileSystemItem fileFilter)
   {
      if (fileFilter != null)
         view_.getFileFilter().setValue(fileFilter);
   }

   public void onShow()
   {
      if (!initialized_)
      {
         initialized_ = true;
         strategy_.initializeHistory(view_.getDataDisplay());
      }
      view_.onShow();
   }

   private final TimeBufferedCommand refreshHistoryCommand_ = new TimeBufferedCommand(1000)
   {
      @Override
      protected void performAction(boolean shouldSchedulePassive)
      {
         refreshHistory();
      }
   };

   private final Display view_;
   private final HistoryStrategy strategy_;
   private final Invalidation invalidation_ = new Invalidation();
   private boolean initialized_;
   private String commitShowing_;
}
