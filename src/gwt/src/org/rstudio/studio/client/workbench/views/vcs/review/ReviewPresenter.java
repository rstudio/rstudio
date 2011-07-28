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
package org.rstudio.studio.client.workbench.views.vcs.review;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.HasAttachHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
import org.rstudio.core.client.ValueSink;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.common.vcs.VCSServerOperations.PatchMode;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.vcs.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.diff.*;
import org.rstudio.studio.client.workbench.views.vcs.events.*;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffChunkActionEvent.Action;
import org.rstudio.studio.client.workbench.views.vcs.history.CommitInfo;

import java.util.ArrayList;

public class ReviewPresenter implements IsWidget
{
   public interface Display extends IsWidget, HasAttachHandlers
   {
      ArrayList<String> getSelectedPaths();
      ArrayList<String> getSelectedDiscardablePaths();

      HasValue<Boolean> getStagedCheckBox();
      HasValue<Boolean> getUnstagedCheckBox();
      ValueSink<ArrayList<ChunkOrLine>> getGutter();
      LineTablePresenter.Display getLineTableDisplay();
      ChangelistTable getChangelistTable();
      HasValue<Integer> getContextLines();

      HasClickHandlers getStageAllFilesButton();
      HasClickHandlers getDiscardSelectedFiles();
      HasClickHandlers getDiscardAllFiles();
      HasClickHandlers getIgnoreButton();
      HasClickHandlers getRefreshButton();
      HasClickHandlers getPullButton();
      HasClickHandlers getPushButton();
      HasClickHandlers getStageAllButton();
      HasClickHandlers getDiscardAllButton();
      HasClickHandlers getUnstageAllButton();

      HasText getCommitMessage();
      HasClickHandlers getCommitButton();

      HasValue<Boolean> getCommitIsAmend();

      void setStageButtonLabel(String label);
      void setDiscardButtonLabel(String label);
      void setUnstageButtonLabel(String label);
   }

   private class ApplyPatchClickHandler implements ClickHandler
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
         ArrayList<Line> selectedLines = view_.getLineTableDisplay().getSelectedLines();
         if (selectedLines.size() == 0)
            selectedLines = view_.getLineTableDisplay().getAllLines();

         applyPatch(activeChunks_, selectedLines, reverse_, patchMode_);
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
                          EventBus events)
   {
      server_ = server;
      view_ = view;

      events.addHandler(
            view_,
            VcsRefreshEvent.TYPE,
            new VcsRefreshHandler()
            {
               @Override
               public void onVcsRefresh(
                     VcsRefreshEvent event)
               {
                  updateDiff(false);
               }
            });

      view_.getChangelistTable().addSelectionChangeHandler(new Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            updateDiff(true);
         }
      });

      view_.getStageAllFilesButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            server_.vcsFullStatus(new SimpleRequestCallback<JsArray<StatusAndPath>>() {
               @Override
               public void onResponseReceived(JsArray<StatusAndPath> response)
               {
                  super.onResponseReceived(response);

                  ArrayList<String> paths = new ArrayList<String>();
                  for (int i = 0; i < response.length(); i++)
                     paths.add(response.get(i).getPath());

                  server_.vcsStage(paths, new SimpleRequestCallback<Void>());
               }
            });
         }
      });

      view_.getDiscardSelectedFiles().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            ArrayList<String> selectedPaths = view_.getSelectedDiscardablePaths();

            if (selectedPaths.size() > 0)
            {
               server_.vcsDiscard(selectedPaths,
                                  new SimpleRequestCallback<Void>());
            }
         }
      });

      view_.getDiscardAllFiles().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            server_.vcsFullStatus(new SimpleRequestCallback<JsArray<StatusAndPath>>() {
               @Override
               public void onResponseReceived(JsArray<StatusAndPath> response)
               {
                  super.onResponseReceived(response);

                  ArrayList<String> paths = new ArrayList<String>();
                  for (int i = 0; i < response.length(); i++)
                     if (response.get(i).isDiscardable())
                        paths.add(response.get(i).getPath());

                  if (paths.size() > 0)
                     server_.vcsDiscard(paths, new SimpleRequestCallback<Void>());
               }
            });
         }
      });

      view_.getCommitIsAmend().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> booleanValueChangeEvent)
         {
            server_.vcsHistory("", 1, new ServerRequestCallback<RpcObjectList<CommitInfo>>() {
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
      view_.getDiscardAllButton().addClickHandler(
            new ApplyPatchClickHandler(PatchMode.Working, true));
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

      view_.getContextLines().addValueChangeHandler(new ValueChangeHandler<Integer>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Integer> event)
         {
            updateDiff(false);
         }
      });

      view_.getCommitButton().addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            server_.vcsCommitGit(view_.getCommitMessage().getText(),
                                 view_.getCommitIsAmend().getValue(),
                                 false,
                                 new SimpleRequestCallback<Void>() {
                                    @Override
                                    public void onResponseReceived(Void resp)
                                    {
                                       super.onResponseReceived(resp);
                                       view_.getCommitMessage().setText("");
                                    }
                                 });
         }
      });

      server_.vcsFullStatus(new SimpleRequestCallback<JsArray<StatusAndPath>>() {
         @Override
         public void onResponseReceived(JsArray<StatusAndPath> response)
         {
            ArrayList<StatusAndPath> items = new ArrayList<StatusAndPath>();
            for (int i = 0; i < response.length(); i++)
               items.add(response.get(i));
            view_.getChangelistTable().setItems(items);
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

      server_.vcsApplyPatch(patch, patchMode, new SimpleRequestCallback<Void>());
   }

   private void updateDiff(boolean allowModeSwitch)
   {
      view_.getLineTableDisplay().clear();
      ArrayList<String> paths = view_.getChangelistTable().getSelectedPaths();
      if (paths.size() != 1)
         return;

      if (allowModeSwitch)
      {
         StatusAndPath item = view_.getChangelistTable().getSelectedItems().get(0);
         if ((item.getStatus().charAt(0) == ' ' || item.getStatus().charAt(0) == '?')
             && view_.getStagedCheckBox().getValue())
         {
            view_.getUnstagedCheckBox().setValue(true, false);
         }
         else if (item.getStatus().charAt(1) == ' ' && view_.getUnstagedCheckBox().getValue())
         {
            view_.getStagedCheckBox().setValue(true, false);
         }
      }


      final Token token = diffInvalidation_.getInvalidationToken();

      final PatchMode patchMode = view_.getStagedCheckBox().getValue()
                                  ? PatchMode.Stage
                                  : PatchMode.Working;
      server_.vcsDiffFile(
            paths.get(0),
            patchMode,
            view_.getContextLines().getValue(),
            new SimpleRequestCallback<String>("Diff Error")
            {
               @Override
               public void onResponseReceived(String response)
               {
                  if (token.isInvalid())
                     return;

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

                  view_.getLineTableDisplay().setData(allLines, patchMode);
                  view_.getGutter().setValue(allLines);
               }
            });
   }

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }

   private final Invalidation diffInvalidation_ = new Invalidation();
   private final VCSServerOperations server_;
   private final Display view_;
   private ArrayList<DiffChunk> activeChunks_ = new ArrayList<DiffChunk>();
}
