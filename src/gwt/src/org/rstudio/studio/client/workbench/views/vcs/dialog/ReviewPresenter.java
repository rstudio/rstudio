/*
 * ReviewPresenter.java
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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasAttachHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.widget.DoubleClickState;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.common.vcs.VCSServerOperations.PatchMode;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.vcs.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.diff.*;
import org.rstudio.studio.client.workbench.views.vcs.events.*;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffChunkActionEvent.Action;
import org.rstudio.studio.client.workbench.views.vcs.events.VcsRefreshEvent.Reason;
import org.rstudio.studio.client.workbench.views.vcs.model.VcsState;

import java.util.ArrayList;

public class ReviewPresenter implements IsWidget
{
   public interface Display extends IsWidget, HasAttachHandlers
   {
      ArrayList<String> getSelectedPaths();
      ArrayList<String> getSelectedDiscardablePaths();
      void setSelectedStatusAndPaths(ArrayList<StatusAndPath> selectedPaths);

      HasValue<Boolean> getStagedCheckBox();
      HasValue<Boolean> getUnstagedCheckBox();
      LineTablePresenter.Display getLineTableDisplay();
      ChangelistTable getChangelistTable();
      HasValue<Integer> getContextLines();

      HasClickHandlers getSwitchViewButton();
      HasClickHandlers getStageFilesButton();
      HasClickHandlers getRevertFilesButton();
      void setFilesCommandsEnabled(boolean enabled);
      HasClickHandlers getIgnoreButton();
      HasClickHandlers getStageAllButton();
      HasClickHandlers getDiscardAllButton();
      HasClickHandlers getUnstageAllButton();

      HasText getCommitMessage();
      HasClickHandlers getCommitButton();

      HasValue<Boolean> getCommitIsAmend();

      void setStageButtonLabel(String label);
      void setDiscardButtonLabel(String label);
      void setUnstageButtonLabel(String label);

      void setData(ArrayList<ChunkOrLine> lines, PatchMode patchMode);
   }

   private class ApplyPatchClickHandler implements ClickHandler, Command
   {
      public ApplyPatchClickHandler(PatchMode patchMode,
                                    boolean reverse)
      {
         patchMode_ = patchMode;
         reverse_ = reverse;
      }

      @Override
      public void onClick(ClickEvent event)
      {
         execute();
      }

      @Override
      public void execute()
      {
         ArrayList<Line> selectedLines = view_.getLineTableDisplay().getSelectedLines();
         if (selectedLines.size() != 0)
         {
            applyPatch(activeChunks_, selectedLines, reverse_, patchMode_);
         }
         else
         {
            ArrayList<String> paths = view_.getSelectedPaths();

            if (patchMode_ == PatchMode.Stage && !reverse_)
               server_.vcsStage(paths, new SimpleRequestCallback<Void>("Stage"));
            else if (patchMode_ == PatchMode.Stage && reverse_)
               server_.vcsUnstage(paths, new SimpleRequestCallback<Void>("Unstage"));
            else if (patchMode_ == PatchMode.Working && reverse_)
               server_.vcsDiscard(paths, new SimpleRequestCallback<Void>("Discard"));
            else
               throw new RuntimeException("Unknown patchMode and reverse combo");

            view_.getChangelistTable().moveSelectionDown();
         }
      }

      private final PatchMode patchMode_;
      private final boolean reverse_;
   }

   private class ApplyPatchHandler implements DiffChunkActionHandler,
                                              DiffLineActionHandler
   {
      @Override
      public void onDiffChunkAction(DiffChunkActionEvent event)
      {
         ArrayList<DiffChunk> chunks = new ArrayList<DiffChunk>();
         chunks.add(event.getDiffChunk());
         doPatch(event.getAction(), event.getDiffChunk().diffLines, chunks);
      }

      @Override
      public void onDiffLineAction(DiffLineActionEvent event)
      {
         ArrayList<Line> lines = new ArrayList<Line>();
         lines.add(event.getLine());
         doPatch(event.getAction(), lines, activeChunks_);
      }

      private void doPatch(Action action,
                           ArrayList<Line> lines,
                           ArrayList<DiffChunk> chunks)
      {
         boolean reverse;
         PatchMode patchMode;
         switch (action)
         {
            case Stage:
               reverse = false;
               patchMode = VCSServerOperations.PatchMode.Stage;
               break;
            case Unstage:
               reverse = true;
               patchMode = VCSServerOperations.PatchMode.Stage;
               break;
            case Discard:
               reverse = true;
               patchMode = VCSServerOperations.PatchMode.Working;
               break;
            default:
               throw new IllegalArgumentException("Unhandled diff chunk action");
         }

         applyPatch(chunks, lines, reverse, patchMode);
      }

   }

   @Inject
   public ReviewPresenter(VCSServerOperations server,
                          Display view,
                          final EventBus events,
                          final VcsState vcsState,
                          final Session session,
                          final GlobalDisplay globalDisplay)
   {
      server_ = server;
      view_ = view;
      globalDisplay_ = globalDisplay;

      vcsState.bindRefreshHandler(view.asWidget(), new VcsRefreshHandler()
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
                     updateDiff(true);
                  }
               });
            }
         }
      });

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

                  StatusAndPath vcsStatus = event.getFileChange().getFile().getVCSStatus();
                  if (paths.get(0).getRawPath().equals(vcsStatus.getRawPath()))
                  {
                     vcsState.refresh(false);
                  }
               }
            });
         }
      };

      // Ensure that we're fresh
      vcsState.refresh();

      view_.getChangelistTable().addSelectionChangeHandler(new Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            view_.setFilesCommandsEnabled(view_.getSelectedPaths().size() > 0);
            updateDiff(true);
         }
      });
      view_.getChangelistTable().addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            // Space toggles the staged/unstaged state of the current selection.
            // Enter does the same plus moves the selection down.

            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER
                  || event.getNativeKeyCode() == ' ')
            {
               view_.getChangelistTable().toggleStaged(
                     event.getNativeKeyCode() == KeyCodes.KEY_ENTER);
            }
         }
      });
      view_.getChangelistTable().addClickHandler(new ClickHandler()
      {
         private DoubleClickState dblClick = new DoubleClickState();
         @Override
         public void onClick(ClickEvent event)
         {
            if (dblClick.checkForDoubleClick(event.getNativeEvent()))
            {
               event.preventDefault();
               event.stopPropagation();
               view_.getChangelistTable().toggleStaged(false);
            }
         }
      });

      view_.getStageFilesButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            ArrayList<String> paths = view_.getSelectedPaths();
            if (paths.size() == 0)
               return;
            server_.vcsStage(paths, new SimpleRequestCallback<Void>());
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

                        server_.vcsRevert(
                              paths,
                              new SimpleRequestCallback<Void>("Revert Changes"));
                     }
                  },
                  false);
         }
      });

      view_.getCommitIsAmend().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> booleanValueChangeEvent)
         {
            server_.vcsHistory("", 0, 1, null, new ServerRequestCallback<RpcObjectList<CommitInfo>>() {
               @Override
               public void onResponseReceived(RpcObjectList<CommitInfo> response)
               {
                  if (response.length() == 1)
                  {
                     String description = response.get(0).getDescription();

                     if (view_.getCommitIsAmend().getValue())
                     {
                        if (view_.getCommitMessage().getText().length() == 0)
                           view_.getCommitMessage().setText(description);
                     }
                     else
                     {
                        if (view_.getCommitMessage().getText().equals(description))
                           view_.getCommitMessage().setText("");
                     }
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
         }
      });

      view_.getStageAllButton().addClickHandler(
            new ApplyPatchClickHandler(PatchMode.Stage, false));
      view_.getDiscardAllButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            String which = view_.getLineTableDisplay().getSelectedLines().size() == 0
                           ? "All unstaged"
                           : "The selected";
            globalDisplay.showYesNoMessage(
                  GlobalDisplay.MSG_WARNING,
                  "Discard All",
                  which + " changes in this file will be " +
                  "lost.\n\nAre you sure you want to continue?",
                  new Operation() {
                     @Override
                     public void execute() {
                        new ApplyPatchClickHandler(PatchMode.Working, true).execute();
                     }
                  },
                  false);
         }
      });
      view_.getUnstageAllButton().addClickHandler(
            new ApplyPatchClickHandler(PatchMode.Stage, true));
      view_.getStagedCheckBox().addValueChangeHandler(
            new ValueChangeHandler<Boolean>()
            {
               @Override
               public void onValueChange(ValueChangeEvent<Boolean> event)
               {
                  updateDiff(false);
               }
            });
      view_.getLineTableDisplay().addDiffChunkActionHandler(new ApplyPatchHandler());
      view_.getLineTableDisplay().addDiffLineActionHandler(new ApplyPatchHandler());

      view_.getLineTableDisplay().addSelectionChangeHandler(new SelectionChangeEvent.Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            if (view_.getLineTableDisplay().getSelectedLines().size() > 0)
            {
               view_.setUnstageButtonLabel("Unstage Selection");
               view_.setStageButtonLabel("Stage Selection");
               view_.setDiscardButtonLabel("Discard Selection");
            }
            else
            {
               view_.setUnstageButtonLabel("Unstage All");
               view_.setStageButtonLabel("Stage All");
               view_.setDiscardButtonLabel("Discard All");
            }
         }
      });

      new IntStateValue(MODULE_VCS, KEY_CONTEXT_LINES, ClientState.PERSISTENT,
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
            updateDiff(false);
         }
      });

      view_.getCommitButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            server_.vcsCommitGit(
                  view_.getCommitMessage().getText(),
                  view_.getCommitIsAmend().getValue(),
                  false,
                  new SimpleRequestCallback<ConsoleProcess>()
                  {
                     @Override
                     public void onResponseReceived(ConsoleProcess proc)
                     {
                        proc.addProcessExitHandler(new ProcessExitEvent.Handler()
                        {
                           @Override
                           public void onProcessExit(ProcessExitEvent event)
                           {
                              if (event.getExitCode() == 0)
                                 view_.getCommitMessage().setText("");
                           }
                        });
                        new ConsoleProgressDialog("Commit", proc).showModal();
                     }
                  });
         }
      });
   }

   private void applyPatch(ArrayList<DiffChunk> chunks,
                           ArrayList<Line> lines,
                           boolean reverse,
                           PatchMode patchMode)
   {
      chunks = new ArrayList<DiffChunk>(chunks);

      if (reverse)
      {
         for (int i = 0; i < chunks.size(); i++)
            chunks.set(i, chunks.get(i).reverse());
         lines = Line.reverseLines(lines);
      }

      UnifiedEmitter emitter = new UnifiedEmitter(
            view_.getChangelistTable().getSelectedPaths().get(0));
      for (DiffChunk chunk : chunks)
         emitter.addContext(chunk);
      emitter.addDiffs(lines);
      String patch = emitter.createPatch();

      softModeSwitch_ = true;
      server_.vcsApplyPatch(patch, patchMode, new SimpleRequestCallback<Void>());
   }

   private void updateDiff(boolean allowModeSwitch)
   {
      final ArrayList<StatusAndPath> paths = view_.getChangelistTable().getSelectedItems();
      if (paths.size() != 1)
      {
         clearDiff();
         return;
      }

      final StatusAndPath item = paths.get(0);

      if (allowModeSwitch)
      {
         if (!softModeSwitch_)
         {
            boolean staged = item.getStatus().charAt(0) != ' ' &&
                             item.getStatus().charAt(1) == ' ';
            HasValue<Boolean> checkbox = staged ?
                                         view_.getStagedCheckBox() :
                                         view_.getUnstagedCheckBox();
            if (!checkbox.getValue())
            {
               clearDiff();
               checkbox.setValue(true, true);
            }
         }
         else
         {
            if (view_.getStagedCheckBox().getValue()
                && (item.getStatus().charAt(0) == ' ' || item.getStatus().charAt(0) == '?'))
            {
               clearDiff();
               view_.getUnstagedCheckBox().setValue(true, true);
            }
            else if (view_.getUnstagedCheckBox().getValue()
                     && item.getStatus().charAt(1) == ' ')
            {
               clearDiff();
               view_.getStagedCheckBox().setValue(true, true);
            }
         }
      }
      softModeSwitch_ = false;

      if (!item.getPath().equals(currentFilename_))
      {
         clearDiff();
         currentFilename_ = item.getPath();
      }

      diffInvalidation_.invalidate();
      final Token token = diffInvalidation_.getInvalidationToken();

      final PatchMode patchMode = view_.getStagedCheckBox().getValue()
                                  ? PatchMode.Stage
                                  : PatchMode.Working;
      server_.vcsDiffFile(
            item.getPath(),
            patchMode,
            view_.getContextLines().getValue(),
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
                       null != (chunk = parser.nextChunk()); )
                  {
                     activeChunks_.add(chunk);
                     allLines.add(new ChunkOrLine(chunk));
                     for (Line line : chunk.diffLines)
                        allLines.add(new ChunkOrLine(line));
                  }

                  view_.getLineTableDisplay().setShowActions(
                        item.isFineGrainedActionable());
                  view_.setData(allLines, patchMode);
               }

            });
   }

   private void clearDiff()
   {
      softModeSwitch_ = false;
      currentResponse_ = null;
      currentFilename_ = null;
      view_.getLineTableDisplay().clear();
   }

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
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

   public void setSelectedPaths(ArrayList<StatusAndPath> selectedPaths)
   {
      view_.setSelectedStatusAndPaths(selectedPaths);
   }

   public void onShow()
   {
   }

   private final Invalidation diffInvalidation_ = new Invalidation();
   private final VCSServerOperations server_;
   private final Display view_;
   private final GlobalDisplay globalDisplay_;
   private ArrayList<DiffChunk> activeChunks_ = new ArrayList<DiffChunk>();
   private String currentResponse_;
   private String currentFilename_;
   // Hack to prevent us flipping to unstaged view when a line is unstaged
   // from staged view
   private boolean softModeSwitch_;
   private static final String MODULE_VCS = "vcs";
   private static final String KEY_CONTEXT_LINES = "context_lines";
}
