/*
 * HistoryPresenter.java
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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.UnifiedParser;
import org.rstudio.studio.client.workbench.views.vcs.common.events.SwitchViewEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent.Reason;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.git.model.VcsState;

public class HistoryPresenter
{
   public interface Display extends IsWidget
   {
      HasClickHandlers getSwitchViewButton();
      CommitListDisplay getCommitList();
      CommitDetailDisplay getCommitDetail();

      HasClickHandlers getOverrideSizeWarningButton();

      HasClickHandlers getRefreshButton();

      HasData<CommitInfo> getDataDisplay();

      HasValue<String> getFilterTextBox();

      void showSizeWarning(long sizeInBytes);
      void hideSizeWarning();

      void onShow();
   }

   public interface CommitListDisplay
   {
      HandlerRegistration addSelectionChangeHandler(
            SelectionChangeEvent.Handler handler);

      CommitInfo getSelectedCommit();
   }

   public interface CommitDetailDisplay
   {
      void setSelectedCommit(CommitInfo commit);
      void clearDetails();
      void setDetails(UnifiedParser unifiedParser);
   }

   @Inject
   public HistoryPresenter(GitServerOperations server,
                           final Display view,
                           HistoryAsyncDataProvider provider,
                           final VcsState vcsState)
   {
      server_ = server;
      view_ = view;
      provider_ = provider;

      view_.getCommitList().addSelectionChangeHandler(new SelectionChangeEvent.Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            showCommitDetail(false);
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

      provider_.setFilter(view_.getFilterTextBox());
      view_.getFilterTextBox().addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent)
         {
            refreshHistoryCommand_.nudge();
         }
      });

      new WidgetHandlerRegistration(view_.asWidget())
      {
         @Override
         protected HandlerRegistration doRegister()
         {
            return vcsState.addVcsRefreshHandler(new VcsRefreshHandler()
            {
               @Override
               public void onVcsRefresh(VcsRefreshEvent event)
               {
                  if (event.getReason() == Reason.VcsOperation)
                  {
                     if (view_.asWidget().isVisible())
                        refreshHistory();
                  }
               }
            }, false);
         }
      };
   }

   private void showCommitDetail(boolean noSizeWarning)
   {
      view_.hideSizeWarning();

      CommitInfo commitInfo = view_.getCommitList().getSelectedCommit();
      view_.getCommitDetail().setSelectedCommit(commitInfo);
      view_.getCommitDetail().clearDetails();
      invalidation_.invalidate();

      if (commitInfo == null)
         return;

      final Token token = invalidation_.getInvalidationToken();

      server_.gitShow(
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

                  UnifiedParser parser = new UnifiedParser(
                        response);
                  view_.getCommitDetail().setDetails(parser);
               }

               @Override
               public void onError(ServerError error)
               {
                  JSONNumber size = error.getClientInfo().isNumber();
                  if (size != null)
                     view_.showSizeWarning((long) size.doubleValue());
                  else
                     super.onError(error);
               }
            });
   }

   private void refreshHistory()
   {
      provider_.refreshCount();
      provider_.onRangeChanged(view_.getDataDisplay());
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

   public void onShow()
   {
      if (!initialized_)
      {
         initialized_ = true;
         provider_.addDataDisplay(view_.getDataDisplay());
         provider_.refreshCount();
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

   private final GitServerOperations server_;
   private final Display view_;
   private final HistoryAsyncDataProvider provider_;
   private final Invalidation invalidation_ = new Invalidation();
   private boolean initialized_;
}
