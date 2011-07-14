/*
 * ReviewPanel.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.*;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.ValueSink;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.views.vcs.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.review.ReviewPresenter.Display;
import org.rstudio.studio.client.workbench.views.vcs.diff.Line;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTablePresenter;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTableView;
import org.rstudio.studio.client.workbench.views.vcs.diff.NavGutter;

import java.util.ArrayList;

public class ReviewPanel extends Composite implements Display
{
   interface Resources extends ClientBundle
   {
      @Source("ReviewPanel.css")
      Styles styles();

      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      @Source("images/toolbarTile.png")
      ImageResource toolbarTile();

      @Source("images/stageAllFiles.png")
      ImageResource stageAllFiles();

      @Source("images/discard.png")
      ImageResource discard();

      @Source("images/ignore.png")
      ImageResource ignore();

      @Source("images/pull.png")
      ImageResource pull();

      @Source("images/push.png")
      ImageResource push();

      @Source("images/stage.png")
      ImageResource stage();

      @Source("images/splitterTileV.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource splitterTileV();

      @Source("images/splitterTileH.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource splitterTileH();
   }

   interface Styles extends CssResource
   {
      String splitPanel();
      String whitebg();

      String toolbar();
      String toolbarWrapper();
      String diffToolbar();

      String stagedLabel();
      String staged();

      String unstaged();

      String diffViewOptions();

      String commitMessage();
      String commitButton();

      String splitPanelCommit();

      String filenameLabel();

      String fileInfoWrapper();

      String fileIcon();
   }

   private static class ListBoxAdapter implements HasValue<Integer>
   {
      private ListBoxAdapter(ListBox listBox)
      {
         listBox_ = listBox;
         listBox_.addChangeHandler(new ChangeHandler()
         {
            @Override
            public void onChange(ChangeEvent event)
            {
               ValueChangeEvent.fire(ListBoxAdapter.this, getValue());
            }
         });
      }

      @Override
      public Integer getValue()
      {
         return Integer.parseInt(
               listBox_.getValue(listBox_.getSelectedIndex()));
      }

      @Override
      public void setValue(Integer value)
      {
         setValue(value, true);
      }

      @Override
      public void setValue(Integer value, boolean fireEvents)
      {
         String valueStr = value.toString();
         for (int i = 0; i < listBox_.getItemCount(); i++)
         {
            if (listBox_.getValue(i).equals(valueStr))
            {
               listBox_.setSelectedIndex(i);
               break;
            }
         }
      }

      @Override
      public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Integer> handler)
      {
         return handlers_.addHandler(ValueChangeEvent.getType(), handler);
      }

      @Override
      public void fireEvent(GwtEvent<?> event)
      {
         handlers_.fireEvent(event);
      }

      private final ListBox listBox_;
      private final HandlerManager handlers_ = new HandlerManager(this);
   }


   interface Binder extends UiBinder<Widget, ReviewPanel>
   {
   }

   @Inject
   public ReviewPanel(ChangelistTable changelist,
                      LineTableView diffPane)
   {
      splitPanel_ = new SplitLayoutPanel(4);
      splitPanelCommit_ = new SplitLayoutPanel(4);

      commitButton_ = new ThemedButton("Commit");
      commitButton_.addStyleName(RES.styles().commitButton());

      changelist_ = changelist;
      lines_ = diffPane;

      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      fileIcon_.setResource(FileTypeRegistry.R.getDefaultIcon());
      fileIcon_.addStyleName(RES.styles().fileIcon());

      topToolbar_.addStyleName(RES.styles().toolbar());

      stageAllFilesButton_ = topToolbar_.addLeftWidget(new ToolbarButton(
            "Stage All Files", RES.stageAllFiles(), (ClickHandler) null));

      topToolbar_.addLeftSeparator();

      ToolbarPopupMenu discardMenu = new ToolbarPopupMenu();
      topToolbar_.addLeftWidget(new ToolbarButton(
            "Discard", RES.discard(), discardMenu));

      topToolbar_.addLeftSeparator();

      ignoreButton_ = topToolbar_.addLeftWidget(new ToolbarButton(
            "Ignore", RES.ignore(), (ClickHandler) null));

      pullButton_ = topToolbar_.addRightWidget(new ToolbarButton(
            "Pull", RES.pull(), (ClickHandler) null));

      topToolbar_.addRightSeparator();

      pushButton_ = topToolbar_.addRightWidget(new ToolbarButton(
            "Push", RES.push(), (ClickHandler) null));

      diffToolbar_.addStyleName(RES.styles().toolbar());
      diffToolbar_.addStyleName(RES.styles().diffToolbar());

      stageAllButton_ = diffToolbar_.addLeftWidget(new ToolbarButton(
            "Stage All", RES.stage(), (ClickHandler) null));
      diffToolbar_.addLeftSeparator();
      discardAllButton_ = diffToolbar_.addLeftWidget(new ToolbarButton(
            "Discard All", RES.discard(), (ClickHandler) null));

      unstageAllButton_ = diffToolbar_.addLeftWidget(new ToolbarButton(
            "Unstage All", RES.discard(), (ClickHandler) null));
      unstageAllButton_.setVisible(false);

      listBoxAdapter_ = new ListBoxAdapter(contextLines_);
   }

   @Override
   public ToolbarButton getStageAllFilesButton()
   {
      return stageAllFilesButton_;
   }

   @Override
   public ToolbarButton getIgnoreButton()
   {
      return ignoreButton_;
   }

   @Override
   public ToolbarButton getPullButton()
   {
      return pullButton_;
   }

   @Override
   public ToolbarButton getPushButton()
   {
      return pushButton_;
   }

   @Override
   public ToolbarButton getStageAllButton()
   {
      return stageAllButton_;
   }

   @Override
   public ToolbarButton getDiscardAllButton()
   {
      return discardAllButton_;
   }

   @Override
   public ToolbarButton getUnstageAllButton()
   {
      return unstageAllButton_;
   }

   @Override
   public HasValue<Boolean> getStagedCheckBox()
   {
      return stagedCheckBox_;
   }

   @Override
   public LineTablePresenter.Display getLineTableDisplay()
   {
      return lines_;
   }

   @Override
   public ChangelistTable getChangelistTable()
   {
      return changelist_;
   }

   @Override
   public ValueSink<ArrayList<Line>> getGutter()
   {
      return gutter_;
   }

   @Override
   public HasValue<Integer> getContextLines()
   {
      return listBoxAdapter_;
   }

   @UiField(provided = true)
   SplitLayoutPanel splitPanel_;
   @UiField(provided = true)
   SplitLayoutPanel splitPanelCommit_;
   @UiField(provided = true)
   ChangelistTable changelist_;
   @UiField(provided = true)
   ThemedButton commitButton_;
   @UiField
   RadioButton stagedCheckBox_;
   @UiField(provided = true)
   LineTableView lines_;
   @UiField
   NavGutter gutter_;
   @UiField
   ListBox contextLines_;
   @UiField
   Toolbar topToolbar_;
   @UiField
   Toolbar diffToolbar_;
   @UiField
   Image fileIcon_;
   @UiField
   Label filenameLabel_;

   private ListBoxAdapter listBoxAdapter_;

   private ToolbarButton stageAllFilesButton_;
   private ToolbarButton ignoreButton_;
   private ToolbarButton pullButton_;
   private ToolbarButton pushButton_;
   private ToolbarButton stageAllButton_;
   private ToolbarButton discardAllButton_;
   private ToolbarButton unstageAllButton_;

   private static final Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
}
