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
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.workbench.views.vcs.diff.UnifiedParser;
import org.rstudio.studio.client.workbench.views.vcs.events.SwitchViewEvent;

import java.util.ArrayList;

public class HistoryPresenter
{
   public interface Display extends IsWidget
   {
      void setData(ArrayList<CommitInfo> commits);

      HasClickHandlers getSwitchViewButton();
      CommitListDisplay getCommitList();
      CommitDetailDisplay getCommitDetail();

      HasClickHandlers getRefreshButton();

      HasData<CommitInfo> getDataDisplay();
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
   public HistoryPresenter(VCSServerOperations server,
                           final Display view,
                           HistoryAsyncDataProvider provider)
   {
      server_ = server;
      view_ = view;
      provider_ = provider;

      view_.getCommitList().addSelectionChangeHandler(new SelectionChangeEvent.Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            CommitInfo commitInfo = view_.getCommitList().getSelectedCommit();
            view_.getCommitDetail().setSelectedCommit(commitInfo);
            view_.getCommitDetail().clearDetails();
            invalidation_.invalidate();

            if (commitInfo == null)
               return;

            final Token token = invalidation_.getInvalidationToken();

            server_.vcsShow(commitInfo.getId(),
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
                            });
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
   }

   private final VCSServerOperations server_;
   private final Display view_;
   private final HistoryAsyncDataProvider provider_;
   private final Invalidation invalidation_ = new Invalidation();
   private boolean initialized_;
}
