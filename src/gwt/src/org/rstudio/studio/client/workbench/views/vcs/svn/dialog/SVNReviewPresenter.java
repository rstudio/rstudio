/*
 * SVNReviewPresenter.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn.dialog;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasAttachHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.common.ProcessCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.*;
import org.rstudio.studio.client.workbench.views.vcs.common.events.*;
import org.rstudio.studio.client.workbench.views.vcs.common.events.DiffChunkActionEvent.Action;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent.Reason;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;
import org.rstudio.studio.client.workbench.views.vcs.svn.model.SVNState;

import java.util.ArrayList;

public class SVNReviewPresenter implements ReviewPresenter
{
   public interface Display extends IsWidget, HasAttachHandlers
   {
      ArrayList<String> getSelectedPaths();
      void setSelectedStatusAndPaths(ArrayList<StatusAndPath> selectedPaths);

      LineTablePresenter.Display getLineTableDisplay();
      ChangelistTable getChangelistTable();
      HasValue<Integer> getContextLines();

      HasClickHandlers getSwitchViewButton();
      HasClickHandlers getRevertFilesButton();
      void setFilesCommandsEnabled(boolean enabled);
      HasClickHandlers getIgnoreButton();
      HasClickHandlers getDiscardAllButton();
      HasClickHandlers getRefreshButton();

      void setData(ArrayList<ChunkOrLine> lines);

      HasClickHandlers getOverrideSizeWarningButton();
      void showSizeWarning(long sizeInBytes);
      void hideSizeWarning();

      void showContextMenu(int clientX, int clientY);

      void onShow();
   }

   private class DiscardClickHandler implements ClickHandler, Command
   {
      public DiscardClickHandler()
      {
      }

      @Override
      public void onClick(ClickEvent event)
      {
         execute();
      }

      @Override
      public void execute()
      {
         ArrayList<String> paths = view_.getSelectedPaths();

         server_.svnRevert(paths,
                           new ProcessCallback("Revert"));

         view_.getChangelistTable().moveSelectionDown();
      }
   }

   private class ApplyPatchHandler implements DiffChunkActionHandler,
                                              DiffLinesActionHandler
   {
      @Override
      public void onDiffChunkAction(DiffChunkActionEvent event)
      {
         ArrayList<DiffChunk> chunks = new ArrayList<DiffChunk>();
         chunks.add(event.getDiffChunk());
         doPatch(event.getAction(), event.getDiffChunk().getLines(), chunks);
      }

      @Override
      public void onDiffLinesAction(DiffLinesActionEvent event)
      {
         ArrayList<Line> lines = view_.getLineTableDisplay().getSelectedLines();
         doPatch(event.getAction(), lines, activeChunks_);
      }

      private void doPatch(Action action,
                           ArrayList<Line> lines,
                           ArrayList<DiffChunk> chunks)
      {
         if (action != Action.Discard)
            throw new IllegalArgumentException("Unhandled diff chunk action");

         applyPatch(chunks, lines, true);
      }

   }

   @Inject
   public SVNReviewPresenter(SVNServerOperations server,
                             Display view,
                             final EventBus events,
                             final SVNState svnState,
                             final Session session,
                             final GlobalDisplay globalDisplay)
   {
      server_ = server;
      view_ = view;
      globalDisplay_ = globalDisplay;
      svnState_ = svnState;

      new WidgetHandlerRegistration(view.asWidget())
      {
         @Override
         protected HandlerRegistration doRegister()
         {
            return svnState_.addVcsRefreshHandler(new VcsRefreshHandler()
            {
               @Override
               public void onVcsRefresh(VcsRefreshEvent event)
               {
                  if (event.getReason() == Reason.VcsOperation)
                  {
                     Scheduler.get().scheduleDeferred(new ScheduledCommand()
                     {
                        @Override
                        public void execute()
                        {
                           updateDiff();

                           initialized_ = true;
                        }
                     });
                  }
               }
            }, false);
         }
      };

      new WidgetHandlerRegistration(view.asWidget())
      {
         @Override
         protected HandlerRegistration doRegister()
         {
            return events.addHandler(FileChangeEvent.TYPE, new FileChangeHandler()
            {
               @Override
               public void onFileChange(FileChangeEvent event)
               {
                  ArrayList<StatusAndPath> paths = view_.getChangelistTable()
                        .getSelectedItems();
                  if (paths.size() != 1)
                  {
                     clearDiff();
                     return;
                  }

                  StatusAndPath vcsStatus = StatusAndPath.fromInfo(
                        event.getFileChange().getFile().getGitStatus());
                  if (paths.get(0).getRawPath().equals(vcsStatus.getRawPath()))
                  {
                     svnState.refresh(false);
                  }
               }
            });
         }
      };

      view_.getChangelistTable().addSelectionChangeHandler(new com.google.gwt.view.client.SelectionChangeEvent.Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            overrideSizeWarning_ = false;
            view_.setFilesCommandsEnabled(view_.getSelectedPaths().size() > 0);
            if (initialized_)
               updateDiff();
         }
      });

      view_.getChangelistTable().addContextMenuHandler(new ContextMenuHandler()
      {
         @Override
         public void onContextMenu(ContextMenuEvent event)
         {
            NativeEvent nativeEvent = event.getNativeEvent();
            view_.showContextMenu(nativeEvent.getClientX(),
                                  nativeEvent.getClientY());

         }
      });

      view_.getRevertFilesButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            final ArrayList<String> paths = view_.getSelectedPaths();
            if (paths.size() == 0)
               return;
            String noun = paths.size() == 1 ? "file" : "files";
            globalDisplay_.showYesNoMessage(
                  GlobalDisplay.MSG_WARNING,
                  "Revert Changes",
                  "Changes to the selected " + noun + " will be lost, including " +
                  "staged changes.\n\nAre you sure you want to continue?",
                  new Operation()
                  {
                     @Override
                     public void execute()
                     {
                        view_.getChangelistTable().selectNextUnselectedItem();

                        server_.svnRevert(
                              paths,
                              new ProcessCallback("Revert Changes"));

                        view_.getChangelistTable().focus();
                     }
                  },
                  false);
         }
      });

      view_.getDiscardAllButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            String which = view_.getLineTableDisplay()
                                 .getSelectedLines()
                                 .size() == 0
                           ? "All unstaged"
                           : "The selected";
            globalDisplay.showYesNoMessage(
                  GlobalDisplay.MSG_WARNING,
                  "Discard All",
                  which + " changes in this file will be " +
                  "lost.\n\nAre you sure you want to continue?",
                  new Operation()
                  {
                     @Override
                     public void execute()
                     {
                        new DiscardClickHandler().execute();
                     }
                  },
                  false);
         }
      });

      view_.getRefreshButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            view_.getChangelistTable().showProgress();
            svnState_.refresh(true);
         }
      });

      view_.getLineTableDisplay().addDiffChunkActionHandler(new ApplyPatchHandler());
      view_.getLineTableDisplay().addDiffLineActionHandler(new ApplyPatchHandler());

      new IntStateValue(MODULE_SVN, KEY_CONTEXT_LINES, ClientState.PERSISTENT,
                        session.getSessionInfo().getClientState())
      {
         @Override
         protected void onInit(Integer value)
         {
            if (value != null)
               view_.getContextLines().setValue(value);
         }

         @Override
         protected Integer getValue()
         {
            return view_.getContextLines().getValue();
         }
      };

      view_.getContextLines().addValueChangeHandler(new ValueChangeHandler<Integer>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Integer> event)
         {
            updateDiff();
         }
      });

      view_.getOverrideSizeWarningButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            overrideSizeWarning_ = true;
            updateDiff();
         }
      });
   }

   private void applyPatch(ArrayList<DiffChunk> chunks,
                           ArrayList<Line> lines,
                           boolean reverse)
   {
      chunks = new ArrayList<DiffChunk>(chunks);

      if (reverse)
      {
         for (int i = 0; i < chunks.size(); i++)
            chunks.set(i, chunks.get(i).reverse());
         lines = Line.reverseLines(lines);
      }

      String path = view_.getChangelistTable().getSelectedPaths().get(0);

      // TODO: Verify that this is not possible in SVN, and remove
      if (path.indexOf(" -> ") >= 0)
         path = path.substring(path.indexOf(" -> ") + " -> ".length());

      UnifiedEmitter emitter = new UnifiedEmitter(path);
      for (DiffChunk chunk : chunks)
         emitter.addContext(chunk);
      emitter.addDiffs(lines);
      String patch = emitter.createPatch(false);

      server_.svnApplyPatch(path, patch, new SimpleRequestCallback<Void>());
   }

   private void updateDiff()
   {
      view_.hideSizeWarning();

      final ArrayList<StatusAndPath> paths = view_.getChangelistTable().getSelectedItems();
      if (paths.size() != 1)
      {
         clearDiff();
         return;
      }

      final StatusAndPath item = paths.get(0);

      if (!item.getPath().equals(currentFilename_))
      {
         clearDiff();
         currentFilename_ = item.getPath();
      }

      diffInvalidation_.invalidate();
      final Token token = diffInvalidation_.getInvalidationToken();

      server_.svnDiffFile(
            item.getPath(),
            view_.getContextLines().getValue(),
            overrideSizeWarning_,
            new SimpleRequestCallback<String>("Diff Error")
            {
               @Override
               public void onResponseReceived(String response)
               {
                  if (token.isInvalid())
                     return;

                  // Use lastResponse_ to prevent unnecessary flicker
                  if (response.equals(currentResponse_))
                     return;
                  currentResponse_ = response;

                  UnifiedParser parser = new UnifiedParser(response);
                  parser.nextFilePair();

                  ArrayList<ChunkOrLine> allLines = new ArrayList<ChunkOrLine>();

                  activeChunks_.clear();
                  for (DiffChunk chunk;
                       null != (chunk = parser.nextChunk());)
                  {
                     activeChunks_.add(chunk);
                     allLines.add(new ChunkOrLine(chunk));
                     for (Line line : chunk.getLines())
                        allLines.add(new ChunkOrLine(line));
                  }

                  view_.getLineTableDisplay().setShowActions(
                        item.isFineGrainedActionable());
                  view_.setData(allLines);
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

   private void clearDiff()
   {
      currentResponse_ = null;
      currentFilename_ = null;
      view_.getLineTableDisplay().clear();
   }

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }

   @Override
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

   @Override
   public void setSelectedPaths(ArrayList<StatusAndPath> selectedPaths)
   {
      view_.setSelectedStatusAndPaths(selectedPaths);
   }

   public void onShow()
   {
      // Ensure that we're fresh
      svnState_.refresh();

      view_.onShow();
   }

   private final Invalidation diffInvalidation_ = new Invalidation();
   private final SVNServerOperations server_;
   private final Display view_;
   private final GlobalDisplay globalDisplay_;
   private ArrayList<DiffChunk> activeChunks_ = new ArrayList<DiffChunk>();
   private String currentResponse_;
   private String currentFilename_;
   private SVNState svnState_;
   private boolean initialized_;
   private static final String MODULE_SVN = "vcs_svn";
   private static final String KEY_CONTEXT_LINES = "context_lines";

   private boolean overrideSizeWarning_ = false;

}
