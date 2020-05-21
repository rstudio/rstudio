/*
 * SVNReviewPresenter.java
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
import com.google.gwt.view.client.RowCountChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.DiffResult;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.common.ProcessCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.VCSFileOpener;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.*;
import org.rstudio.studio.client.workbench.views.vcs.common.events.*;
import org.rstudio.studio.client.workbench.views.vcs.common.events.DiffChunkActionEvent.Action;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent.Reason;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNCommandHandler;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNDiffParser;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNPresenterDisplay;
import org.rstudio.studio.client.workbench.views.vcs.svn.model.SVNState;

import java.util.ArrayList;
import java.util.HashSet;

public class SVNReviewPresenter implements ReviewPresenter
{
   public interface Binder extends CommandBinder<Commands, SVNReviewPresenter> {}
   
   public interface Display extends IsWidget, HasAttachHandlers, SVNPresenterDisplay
   {
      ArrayList<String> getSelectedPaths();
      void setSelectedStatusAndPaths(ArrayList<StatusAndPath> selectedPaths);

      LineTablePresenter.Display getLineTableDisplay();
      ChangelistTable getChangelistTable();
      HasValue<Integer> getContextLines();

      HasClickHandlers getSwitchViewButton();

      HasClickHandlers getDiscardAllButton();

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
                             Binder binder,
                             Commands commands,
                             final EventBus events,
                             final SVNState svnState,
                             final Session session,
                             final GlobalDisplay globalDisplay,
                             VCSFileOpener vcsFileOpener)
   {
      server_ = server;
      view_ = view;
      svnState_ = svnState;
      
      binder.bind(commands, this);
      
      undiffableStatuses_.add("?");
      undiffableStatuses_.add("!");
      undiffableStatuses_.add("X");
      
      commandHandler_ = new SVNCommandHandler(view, 
                                              globalDisplay, 
                                              commands, 
                                              server, 
                                              svnState, 
                                              vcsFileOpener);
      
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
                        event.getFileChange().getFile().getSVNStatus());
                  if (paths.get(0).getRawPath() == vcsStatus.getRawPath())
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
            commandHandler_.setFilesCommandsEnabled(view_.getSelectedPaths().size() > 0);
            if (initialized_)
               updateDiff();
         }
      });
      view_.getChangelistTable().addRowCountChangeHandler(new RowCountChangeEvent.Handler()
      {
         @Override
         public void onRowCountChange(RowCountChangeEvent event)
         {
            // This is necessary because during initial load, the selection
            // model has its selection set before any items are loaded into
            // the table (so therefore view_.getSelectedPaths().size() is always
            // 0, and the files commands are not enabled until selection changes
            // again). By updating the files commands' enabled state on row
            // count change as well, we can make sure they get enabled.
            commandHandler_.setFilesCommandsEnabled(view_.getSelectedPaths().size() > 0);
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

      view_.getDiscardAllButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            String which = view_.getLineTableDisplay()
                                 .getSelectedLines()
                                 .size() == 0
                           ? "All "
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

      UnifiedEmitter emitter = new UnifiedEmitter(path);
      for (DiffChunk chunk : chunks)
         emitter.addContext(chunk);
      emitter.addDiffs(lines);
      String patch = emitter.createPatch(false);

      server_.svnApplyPatch(path, patch,
                            StringUtil.notNull(currentEncoding_),
                            new SimpleRequestCallback<Void>());
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

      if (item.getPath() != currentFilename_)
      {
         clearDiff();
         currentFilename_ = item.getPath();
      }
      
      // bail if this is an undiffable status
      if (undiffableStatuses_.contains(item.getStatus()))
         return;

      diffInvalidation_.invalidate();
      final Token token = diffInvalidation_.getInvalidationToken();

      server_.svnDiffFile(
            item.getPath(),
            view_.getContextLines().getValue(),
            overrideSizeWarning_,
            new SimpleRequestCallback<DiffResult>("Diff Error")
            {
               @Override
               public void onResponseReceived(DiffResult diffResult)
               {
                  if (token.isInvalid())
                     return;

                  String response = diffResult.getDecodedValue();

                  // Use lastResponse_ to prevent unnecessary flicker
                  if (response == currentResponse_)
                     return;
                  currentResponse_ = response;
                  currentEncoding_ = diffResult.getSourceEncoding();

                  SVNDiffParser parser = new SVNDiffParser(response);
                  parser.nextFilePair();

                  ArrayList<ChunkOrLine> allLines = new ArrayList<ChunkOrLine>();

                  activeChunks_.clear();
                  for (DiffChunk chunk;
                       null != (chunk = parser.nextChunk());)
                  {
                     if (!chunk.shouldIgnore())
                     {
                        activeChunks_.add(chunk);
                        allLines.add(new ChunkOrLine(chunk));
                     }

                     for (Line line : chunk.getLines())
                        allLines.add(new ChunkOrLine(line));
                  }

                  view_.getLineTableDisplay().setShowActions(
                        !"?".equals(item.getStatus()));
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
   
   @Handler
   public void onVcsCommit()
   {
      commandHandler_.onVcsCommit();
   }
   
   @Handler
   public void onVcsPull()
   {
      commandHandler_.onVcsPull();
   }
   
   @Handler
   public void onVcsIgnore()
   {
      commandHandler_.onVcsIgnore();
   }

   private final Invalidation diffInvalidation_ = new Invalidation();
   private final SVNServerOperations server_;
   private final SVNCommandHandler commandHandler_;
   private final Display view_;
   private ArrayList<DiffChunk> activeChunks_ = new ArrayList<DiffChunk>();
   private String currentResponse_;
   private String currentEncoding_;
   private String currentFilename_;
   private SVNState svnState_;
   private boolean initialized_;
   private static final String MODULE_SVN = "vcs_svn";
   private static final String KEY_CONTEXT_LINES = "context_lines";
   
   private final HashSet<String> undiffableStatuses_ = new HashSet<String>();

   private boolean overrideSizeWarning_ = false;

}
