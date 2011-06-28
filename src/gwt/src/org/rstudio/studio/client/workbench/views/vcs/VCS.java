/*
 * VCS.java
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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTable;
import org.rstudio.studio.client.workbench.views.vcs.diff.UnifiedParser;
import org.rstudio.studio.client.workbench.views.vcs.diff.UnifiedParser.DiffChunk;

import java.util.ArrayList;

public class VCS extends BasePresenter implements IsWidget
{
   public interface Binder extends CommandBinder<Commands, VCS> {}

   public interface Display extends WorkbenchView, IsWidget
   {
      void setItems(ArrayList<StatusAndPath> items);

      ArrayList<String> getSelectedPaths();
   }

   public interface CommitDisplay
   {
      void showModal();
   }

   @Inject
   public VCS(Display view,
              Provider<CommitDisplay> pCommitView,
              Provider<LineTable> pLineTable,
              VCSServerOperations server,
              Commands commands,
              Binder commandBinder)
   {
      super(view);
      view_ = view;
      pCommitView_ = pCommitView;
      pLineTable_ = pLineTable;
      server_ = server;

      commandBinder.bind(commands, this);

      refresh();
   }

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }

   @Handler
   void onVcsDiff()
   {
      ArrayList<String> paths = view_.getSelectedPaths();
      if (paths.size() == 0)
         return;

      server_.vcsDiffFile(
            paths.get(0),
            new SimpleRequestCallback<String>("Diff")
            {
               @Override
               public void onResponseReceived(String diff)
               {
                  UnifiedParser parser = new UnifiedParser(diff);
                  parser.nextFilePair();
                  DiffChunk chunk = parser.nextChunk();
                  if (chunk != null)
                  {
                     final LineTable lineTable = pLineTable_.get();
                     lineTable.setSize("100%", "auto");
                     lineTable.setRowData(chunk.diffLines);
                     lineTable.setPageSize(chunk.diffLines.size());
                     new ModalDialogBase() {
                        @Override
                        protected Widget createMainWidget()
                        {
                           ScrollPanel scrollPanel = new ScrollPanel(lineTable);
                           scrollPanel.setSize("800px", "400px");
                           return scrollPanel;
                        }
                     }.showModal();
                  }
               }
            });
   }

   @Handler
   void onVcsStage()
   {
      ArrayList<String> paths = view_.getSelectedPaths();
      if (paths.size() == 0)
         return;

      server_.vcsAdd(paths, new SimpleRequestCallback<Void>("Stage Changes")
      {
         @Override
         public void onResponseReceived(Void response)
         {
            refresh();
         }
      });
   }

   @Handler
   void onVcsUnstage()
   {
      ArrayList<String> paths = view_.getSelectedPaths();
      if (paths.size() == 0)
         return;

      server_.vcsUnstage(paths,
                         new SimpleRequestCallback<Void>("Unstage Changes")
                         {
                            @Override
                            public void onResponseReceived(Void response)
                            {
                               refresh();
                            }
                         });
   }

   @Handler
   void onVcsRevert()
   {
      ArrayList<String> paths = view_.getSelectedPaths();
      if (paths.size() == 0)
         return;

      server_.vcsRevert(paths, new SimpleRequestCallback<Void>("Revert Changes")
      {
         @Override
         public void onResponseReceived(Void response)
         {
            refresh();
         }
      });
   }

   @Handler
   void onVcsCommit()
   {
      pCommitView_.get().showModal();
   }

   @Handler
   void onVcsRefresh()
   {
      refresh();
   }

   private void refresh()
   {
      server_.vcsFullStatus(new ServerRequestCallback<JsArray<StatusAndPath>>()
      {
         @Override
         public void onResponseReceived(JsArray<StatusAndPath> response)
         {
            ArrayList<StatusAndPath> list = new ArrayList<StatusAndPath>();
            for (int i = 0; i < response.length(); i++)
               list.add(response.get(i));
            view_.setItems(list);
         }

         @Override
         public void onError(ServerError error)
         {
            //To change body of implemented methods use File | Settings | File Templates.
         }
      });
   }

   private final Display view_;
   private final Provider<CommitDisplay> pCommitView_;
   private final Provider<LineTable> pLineTable_;
   private final VCSServerOperations server_;
}
