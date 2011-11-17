/*
 * CommitDetail.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.common.vcs.GitServerOperations.PatchMode;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.CommitDetailDisplay;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.*;

import java.util.ArrayList;

public class CommitDetail extends Composite implements CommitDetailDisplay
{
   interface Binder extends UiBinder<Widget, CommitDetail>
   {}

   public CommitDetail()
   {
      sizeWarning_ = new SizeWarningWidget("commit");
      sizeWarning_.setVisible(false);
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));
   }

   public void setScrollPanel(ScrollPanel container)
   {
    container_ = container;
   }

   @Override
   public void setSelectedCommit(CommitInfo commit)
   {
      commit_ = commit;
      if (commit_ != null)
         updateInfo();
      setVisible(commit_ != null);
   }

   @Override
   public void clearDetails()
   {
      invalidation_.invalidate();
      tocPanel_.clear();
      detailPanel_.clear();
   }

   @Override
   public void setDetails(final UnifiedParser unifiedParser)
   {
      invalidation_.invalidate();
      final Token token = invalidation_.getInvalidationToken();

      Scheduler.get().scheduleIncremental(new RepeatingCommand() {
         @Override
         public boolean execute()
         {
            if (token.isInvalid())
               return false;

            DiffFileHeader fileHeader = unifiedParser.nextFilePair();
            if (fileHeader == null)
               return false;

            int filesCompared = 2;
            ArrayList<ChunkOrLine> lines = new ArrayList<ChunkOrLine>();
            DiffChunk chunk;
            while (null != (chunk = unifiedParser.nextChunk()))
            {
               filesCompared = chunk.getRanges().length;
               lines.addAll(ChunkOrLine.fromChunk(chunk));
            }

            LineTableView view = new LineTableView(filesCompared);
            view.setShowActions(false);
            view.setData(lines, PatchMode.Stage);
            view.setWidth("100%");

            final DiffFrame diffFrame = new DiffFrame(
                  null, fileHeader.getDescription(), null, view);
            diffFrame.setWidth("100%");
            detailPanel_.add(diffFrame);

            CommitTocRow tocAnchor = new CommitTocRow(fileHeader.getDescription());
            tocAnchor.addClickHandler(new ClickHandler()
            {
               @Override
               public void onClick(ClickEvent event)
               {
                  Point relativePosition = DomUtils.getRelativePosition(
                        container_.getElement(),
                        diffFrame.getElement());
                  container_.setVerticalScrollPosition(relativePosition.getY());
               }
            });
            tocPanel_.add(tocAnchor);

            return true;
         }
      });
   }

   private void updateInfo()
   {
      labelId_.setText(commit_.getId());
      labelAuthor_.setText(commit_.getAuthor());
      labelDate_.setText(
            DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT).format(commit_.getDate()) +
            " " +
            DateTimeFormat.getFormat(PredefinedFormat.TIME_SHORT).format(commit_.getDate())
      );
      labelSubject_.setText(commit_.getSubject());
      labelParent_.setText(commit_.getParent());
   }

   public void showSizeWarning(long sizeInBytes)
   {
      tocPanel_.setVisible(false);
      detailPanel_.setVisible(false);
      sizeWarning_.setSize(sizeInBytes);
      sizeWarning_.setVisible(true);
   }

   public void hideSizeWarning()
   {
      tocPanel_.setVisible(true);
      detailPanel_.setVisible(true);
      sizeWarning_.setVisible(false);
   }

   public HasClickHandlers getOverrideSizeWarningButton()
   {
      return sizeWarning_;
   }

   private final Invalidation invalidation_ = new Invalidation();
   private CommitInfo commit_;
   @UiField
   Label labelId_;
   @UiField
   Label labelAuthor_;
   @UiField
   Label labelDate_;
   @UiField
   Label labelSubject_;
   @UiField
   Label labelParent_;
   @UiField
   VerticalPanel detailPanel_;
   @UiField
   VerticalPanel tocPanel_;
   @UiField(provided = true)
   SizeWarningWidget sizeWarning_;
   private ScrollPanel container_;
}
