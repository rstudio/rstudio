package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Pair;
import org.rstudio.studio.client.common.vcs.VCSServerOperations.PatchMode;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.CommitDetailDisplay;
import org.rstudio.studio.client.workbench.views.vcs.diff.ChunkOrLine;
import org.rstudio.studio.client.workbench.views.vcs.diff.DiffChunk;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTableView;
import org.rstudio.studio.client.workbench.views.vcs.diff.UnifiedParser;

import java.util.ArrayList;

public class CommitDetail extends Composite implements CommitDetailDisplay
{
   interface Resources extends ClientBundle
   {
      Styles styles();
   }

   interface Styles extends CssResource
   {}

   interface Binder extends UiBinder<Widget, CommitDetail>
   {}

   public CommitDetail()
   {
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));
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
      detailPanel_.clear();
   }

   @Override
   public void setDetails(UnifiedParser unifiedParser)
   {
      Pair<String, String> filePair;
      while (null != (filePair = unifiedParser.nextFilePair()))
      {
         detailPanel_.add(new Label(filePair.first + ", " + filePair.second));
         LineTableView view = new LineTableView();
         view.setShowActions(false);
         ArrayList<ChunkOrLine> lines = new ArrayList<ChunkOrLine>();
         DiffChunk chunk;
         while (null != (chunk = unifiedParser.nextChunk()))
         {
            lines.addAll(ChunkOrLine.fromChunk(chunk));
         }
         view.setData(lines, PatchMode.Stage);
         view.setWidth("100%");

         DiffFrame diffFrame = new DiffFrame(null, filePair.first, null, view);
         diffFrame.setWidth("100%");
         detailPanel_.add(diffFrame);
      }
   }

   private void updateInfo()
   {
      labelId_.setText(commit_.getId());
      labelAuthor_.setText(commit_.getAuthor());
      labelDate_.setText(DateTimeFormat.getFormat(
            PredefinedFormat.DATE_SHORT).format(commit_.getDate()));
      labelSubject_.setText(commit_.getSubject());
      //labelParent_.setText(commit_.getParentId());
   }

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
   FlowPanel detailPanel_;
}
