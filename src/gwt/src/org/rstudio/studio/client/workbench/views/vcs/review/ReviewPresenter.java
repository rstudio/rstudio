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
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.inject.Inject;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.ValueSink;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.common.vcs.VCSServerOperations.PatchMode;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.vcs.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.diff.*;

import java.util.ArrayList;

public class ReviewPresenter implements IsWidget
{
   public interface Display extends IsWidget
   {
      HasClickHandlers getStageButton();
      HasClickHandlers getDiscardButton();
      HasClickHandlers getUnstageButton();
      HasValue<Boolean> getStagedCheckBox();
      ValueSink<ArrayList<Line>> getGutter();
      LineTablePresenter.Display getLineTableDisplay();
      ChangelistTable getChangelistTable();
      HasValue<Integer> getContextLines();
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
         ArrayList<DiffChunk> chunks = new ArrayList<DiffChunk>(activeChunks_);
         ArrayList<Line> selectedLines =
               view_.getLineTableDisplay().getSelectedLines();

         if (reverse_)
         {
            for (int i = 0; i < chunks.size(); i++)
               chunks.set(i, chunks.get(i).reverse());
            selectedLines = Line.reverseLines(selectedLines);
         }

         UnifiedEmitter emitter = new UnifiedEmitter(
               view_.getChangelistTable().getSelectedPaths().get(0));
         for (DiffChunk chunk : chunks)
            emitter.addContext(chunk);
         emitter.addDiffs(selectedLines);
         String patch = emitter.createPatch();

         server_.vcsApplyPatch(patch, patchMode_,
                               new SimpleRequestCallback<Void>() {
            @Override
            public void onResponseReceived(Void response)
            {
               updateDiff();
            }

            @Override
            public void onError(ServerError error)
            {
               super.onError(error);
               updateDiff();
            }
         });
      }

      private final PatchMode patchMode_;
      private final boolean reverse_;
   }

   @Inject
   public ReviewPresenter(VCSServerOperations server,
                          Display view)
   {
      server_ = server;
      view_ = view;

      view_.getChangelistTable().addSelectionChangeHandler(new Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            updateDiff();
         }
      });

      view_.getStageButton().addClickHandler(
            new ApplyPatchClickHandler(PatchMode.Stage, false));
      view_.getDiscardButton().addClickHandler(
            new ApplyPatchClickHandler(PatchMode.Working, true));
      view_.getUnstageButton().addClickHandler(
            new ApplyPatchClickHandler(PatchMode.Stage, true));
      view_.getStagedCheckBox().addValueChangeHandler(
            new ValueChangeHandler<Boolean>()
            {
               @Override
               public void onValueChange(ValueChangeEvent<Boolean> event)
               {
                  updateDiff();
               }
            }
      );

      view_.getContextLines().addValueChangeHandler(new ValueChangeHandler<Integer>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Integer> event)
         {
            updateDiff();
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

   private void updateDiff()
   {
      view_.getLineTableDisplay().clear();
      ArrayList<String> paths = view_.getChangelistTable()
            .getSelectedPaths();
      if (paths.size() != 1)
         return;

      final Token token = diffInvalidation_.getInvalidationToken();

      server_.vcsDiffFile(
            paths.get(0),
            view_.getStagedCheckBox().getValue() ? PatchMode.Stage : PatchMode.Working,
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

                  ArrayList<Line> allLines = new ArrayList<Line>();

                  activeChunks_.clear();
                  for (DiffChunk chunk;
                       null != (chunk = parser.nextChunk()); )
                  {
                     activeChunks_.add(chunk);
                     allLines.addAll(chunk.diffLines);
                  }

                  view_.getLineTableDisplay().setData(allLines);
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
