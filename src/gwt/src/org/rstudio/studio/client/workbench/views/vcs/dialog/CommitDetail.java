/*
 * CommitDetail.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ProgressPanel;
import org.rstudio.core.client.widget.images.ProgressImages;
import org.rstudio.studio.client.common.vcs.GitServerOperations.PatchMode;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.*;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ViewFileRevisionEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ViewFileRevisionHandler;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.CommitDetailDisplay;

import java.util.ArrayList;

public class CommitDetail extends Composite implements CommitDetailDisplay
{
   interface Binder extends UiBinder<Widget, CommitDetail>
   {}

   public CommitDetail()
   {
      sizeWarning_ = new SizeWarningWidget("commit");
      sizeWarning_.setVisible(false);
      progressPanel_ = new ProgressPanel(ProgressImages.createLargeGray());
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));
      
      ThemeStyles styles = ThemeStyles.INSTANCE;
      labelId_.addStyleName(styles.selectableText());
      labelParent_.addStyleName(styles.selectableText());
   }
   
   public void setIdDesc(String idDesc)
   {
      labelIdDesc_.setText(idDesc);
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

      commitViewPanel_.setVisible(commit_ != null);
      emptySelectionLabel_.setVisible(!(commit_ != null));
   }

   @Override
   public void clearDetails()
   {
      invalidation_.invalidate();
      tocPanel_.clear();
      detailPanel_.clear();

      setProgressVisible(false);
   }

   @Override
   public void showDetailProgress()
   {
      clearDetails();

      setProgressVisible(true);
   }

   @Override
   public void setDetails(final DiffParser unifiedParser,
                          final boolean suppressViewLink)
   {
      setProgressVisible(false);

      invalidation_.invalidate();
      final Token token = invalidation_.getInvalidationToken();

      Scheduler.get().scheduleIncremental(new RepeatingCommand() {
         @Override
         public boolean execute()
         {
            if (token.isInvalid())
               return false;

            final DiffFileHeader fileHeader = unifiedParser.nextFilePair();
            if (fileHeader == null)
               return false;

            int filesCompared = 2;
            ArrayList<ChunkOrLine> lines = new ArrayList<ChunkOrLine>();
            DiffChunk chunk;
            while (null != (chunk = unifiedParser.nextChunk()))
            {
               if (!chunk.shouldIgnore())
                  filesCompared = chunk.getRanges().length;
               lines.addAll(ChunkOrLine.fromChunk(chunk));
            }

            LineTableView view = new LineTableView(filesCompared);
            view.setUseStartBorder(true);
            view.setUseEndBorder(false);
            view.setShowActions(false);
            view.setData(lines, PatchMode.Stage);
            view.setWidth("100%");
            
            final DiffFrame diffFrame = new DiffFrame(
                           null, 
                           fileHeader.getDescription(), 
                           null, 
                           commit_.getId(), 
                           view,
                           new Command() {
                              @Override
                              public void execute()
                              { 
                                 fireEvent(new ViewFileRevisionEvent(
                                          commit_.getId(), 
                                          fileHeader.getDescription().trim()));
                                 
                              }
                           },
                           suppressViewLink);
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

   @Override
   public void setCommitListIsLoading(boolean isLoading)
   {
      emptySelectionLabel_.setText(isLoading ? "" : "(No commit selected)");
   }

   @Override
   public HandlerRegistration addViewFileRevisionHandler(
                                             ViewFileRevisionHandler handler)
   {
      return addHandler(handler, ViewFileRevisionEvent.TYPE);
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

      parentTableRow_.getStyle().setProperty(
            "display",
            StringUtil.isNullOrEmpty(commit_.getParent()) ? "none"
                                                          : "table-row");
      labelParent_.setText(commit_.getParent());
   }

   public void showSizeWarning(long sizeInBytes)
   {
      tocPanel_.setVisible(false);
      detailPanel_.setVisible(false);
      setProgressVisible(false);
      sizeWarning_.setSize(sizeInBytes);
      sizeWarning_.setVisible(true);
   }

   private void setProgressVisible(boolean visible)
   {
      if (visible)
      {
         progressPanel_.setVisible(true);
         progressPanel_.beginProgressOperation(100);
      }
      else
      {
         progressPanel_.setVisible(false);
         progressPanel_.endProgressOperation();
      }
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
   Label labelIdDesc_;
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
   VerticalPanel tocPanel_;
   @UiField(provided = true)
   ProgressPanel progressPanel_;
   @UiField
   VerticalPanel detailPanel_;
   @UiField(provided = true)
   SizeWarningWidget sizeWarning_;
   @UiField
   TableRowElement parentTableRow_;
   @UiField
   Label emptySelectionLabel_;
   @UiField
   HTMLPanel commitViewPanel_;

   private ScrollPanel container_;
   
}
