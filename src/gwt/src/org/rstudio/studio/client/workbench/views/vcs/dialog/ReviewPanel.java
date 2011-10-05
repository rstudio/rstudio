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
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.BranchToolbarButton;
import org.rstudio.studio.client.workbench.views.vcs.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.ChangelistTablePresenter;
import org.rstudio.studio.client.workbench.views.vcs.console.ConsoleBarFramePanel;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter.Display;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTablePresenter;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTableView;

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

      @Source("images/stage.png")
      ImageResource stage();

      @Source("images/splitterTileV.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource splitterTileV();

      @Source("images/splitterTileH.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource splitterTileH();

      @Source("images/blankFileIcon.png")
      ImageResource blankFileIcon();
   }

   interface Styles extends SharedStyles
   {
      String diffToolbar();

      String stagedLabel();
      String staged();

      String unstaged();

      String diffViewOptions();

      String commitMessage();
      String commitButton();

      String splitPanelCommit();
   }

   private static class ClickCommand implements HasClickHandlers, Command
   {
      @Override
      public void execute()
      {
         ClickEvent.fireNativeEvent(
               Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false),
               this);
      }

      @Override
      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return handlerManager_.addHandler(ClickEvent.getType(), handler);
      }

      @Override
      public void fireEvent(GwtEvent<?> event)
      {
         handlerManager_.fireEvent(event);
      }

      private final HandlerManager handlerManager_ = new HandlerManager(this);
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
   public ReviewPanel(ChangelistTablePresenter changelist,
                      LineTableView diffPane,
                      ConsoleBarFramePanel consoleBarFramePanel,
                      final Commands commands,
                      FileTypeRegistry fileTypeRegistry,
                      BranchToolbarButton branchToolbarButton)
   {
      fileTypeRegistry_ = fileTypeRegistry;
      splitPanel_ = new SplitLayoutPanel(4);
      splitPanelCommit_ = new SplitLayoutPanel(4);

      commitButton_ = new ThemedButton("Commit");
      commitButton_.addStyleName(RES.styles().commitButton());

      changelist_ = changelist.getView();
      lines_ = diffPane;
      lines_.getElement().setTabIndex(-1);

      Widget widget = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      consoleBarFramePanel.setWidget(widget);

      initWidget(consoleBarFramePanel);

      topToolbar_.addStyleName(RES.styles().toolbar());

      switchViewButton_ = new LeftRightToggleButton("Changes", "History", true);
      topToolbar_.addLeftWidget(switchViewButton_);

      topToolbar_.addLeftWidget(branchToolbarButton);

      stageAllFilesButton_ = topToolbar_.addLeftWidget(new ToolbarButton(
            "Stage All Files", RES.stageAllFiles(), (ClickHandler) null));

      topToolbar_.addLeftSeparator();

      ToolbarPopupMenu discardMenu = new ToolbarPopupMenu();
      discardSelectedFiles_ = new ClickCommand();
      discardAllFiles_ = new ClickCommand();
      discardMenu.addItem(new MenuItem("Discard Selected",
                                       discardSelectedFiles_));
      discardMenu.addItem(new MenuItem("Discard All Files", discardAllFiles_));
      topToolbar_.addLeftWidget(new ToolbarButton(
            "Discard", RES.discard(), discardMenu));

      topToolbar_.addLeftSeparator();

      ignoreButton_ = topToolbar_.addLeftWidget(new ToolbarButton(
            "Ignore", RES.ignore(), (ClickHandler) null));

      topToolbar_.addRightWidget(new ToolbarButton(
            "Refresh", commands.vcsRefresh().getImageResource(),
            new ClickHandler() {
               @Override
               public void onClick(ClickEvent event)
               {
                  changelist_.showProgress();
                  commands.vcsRefresh().execute();
               }
            }));

      topToolbar_.addRightSeparator();

      topToolbar_.addRightWidget(new ToolbarButton(
            "Pull", commands.vcsPull().getImageResource(),
            commands.vcsPull()));

      topToolbar_.addRightSeparator();

      topToolbar_.addRightWidget(new ToolbarButton(
            "Push", commands.vcsPush().getImageResource(),
            commands.vcsPush()));

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

      unstagedCheckBox_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> e)
         {
            ValueChangeEvent.fire(stagedCheckBox_, stagedCheckBox_.getValue());
         }
      });

      stagedCheckBox_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> e)
         {
            stageAllButton_.setVisible(!e.getValue());
            discardAllButton_.setVisible(!e.getValue());
            unstageAllButton_.setVisible(e.getValue());
            diffToolbar_.invalidateSeparators();
         }
      });

      listBoxAdapter_ = new ListBoxAdapter(contextLines_);

      FontSizer.applyNormalFontSize(commitMessage_);
   }

   @Override
   public HasClickHandlers getSwitchViewButton()
   {
      return switchViewButton_;
   }

   @Override
   public HasClickHandlers getStageAllFilesButton()
   {
      return stageAllFilesButton_;
   }

   @Override
   public HasClickHandlers getDiscardSelectedFiles()
   {
      return discardSelectedFiles_;
   }

   @Override
   public HasClickHandlers getDiscardAllFiles()
   {
      return discardAllFiles_;
   }

   @Override
   public HasClickHandlers getIgnoreButton()
   {
      return ignoreButton_;
   }

   @Override
   public HasClickHandlers getStageAllButton()
   {
      return stageAllButton_;
   }

   @Override
   public HasClickHandlers getDiscardAllButton()
   {
      return discardAllButton_;
   }

   @Override
   public HasClickHandlers getUnstageAllButton()
   {
      return unstageAllButton_;
   }

   @Override
   public void setStageButtonLabel(String label)
   {
      stageAllButton_.setText(label);
   }

   @Override
   public void setDiscardButtonLabel(String label)
   {
      discardAllButton_.setText(label);
   }

   @Override
   public void setUnstageButtonLabel(String label)
   {
      unstageAllButton_.setText(label);
   }

   @Override
   public HasText getCommitMessage()
   {
      return commitMessage_;
   }

   @Override
   public HasClickHandlers getCommitButton()
   {
      return commitButton_;
   }

   @Override
   public HasValue<Boolean> getCommitIsAmend()
   {
      return commitIsAmend_;
   }

   @Override
   public ArrayList<String> getSelectedPaths()
   {
      return changelist_.getSelectedPaths();
   }

   @Override
   public void setSelectedStatusAndPaths(ArrayList<StatusAndPath> selectedPaths)
   {
      changelist_.setSelectedStatusAndPaths(selectedPaths);
   }

   @Override
   public ArrayList<String> getSelectedDiscardablePaths()
   {
      return changelist_.getSelectedDiscardablePaths();
   }

   @Override
   public HasValue<Boolean> getStagedCheckBox()
   {
      return stagedCheckBox_;
   }

   @Override
   public HasValue<Boolean> getUnstagedCheckBox()
   {
      return unstagedCheckBox_;
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
   @UiField
   RadioButton unstagedCheckBox_;
   @UiField(provided = true)
   LineTableView lines_;
   @UiField
   ListBox contextLines_;
   @UiField
   Toolbar topToolbar_;
   @UiField
   Toolbar diffToolbar_;
   @UiField
   TextArea commitMessage_;
   @UiField
   CheckBox commitIsAmend_;

   private ListBoxAdapter listBoxAdapter_;

   private ToolbarButton stageAllFilesButton_;
   private ToolbarButton ignoreButton_;
   private ToolbarButton stageAllButton_;
   private ToolbarButton discardAllButton_;
   private ToolbarButton unstageAllButton_;
   private ClickCommand discardSelectedFiles_;
   private ClickCommand discardAllFiles_;
   private final FileTypeRegistry fileTypeRegistry_;
   private LeftRightToggleButton switchViewButton_;

   private static final Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
}
