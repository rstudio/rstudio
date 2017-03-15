/*
 * SVNReviewPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
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
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.inject.Inject;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.LeftRightToggleButton;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.vcs.GitServerOperations.PatchMode;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.ChunkOrLine;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.LineTablePresenter;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.LineTableView;
import org.rstudio.studio.client.workbench.views.vcs.dialog.SharedStyles;
import org.rstudio.studio.client.workbench.views.vcs.dialog.SizeWarningWidget;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNChangelistTablePresenter;
import org.rstudio.studio.client.workbench.views.vcs.svn.dialog.SVNReviewPresenter.Display;

import java.util.ArrayList;

public class SVNReviewPanel extends ResizeComposite implements Display
{
   interface Resources extends ClientBundle
   {
      @Source("SVNReviewPanel.css")
      Styles styles();

      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      @Source("../../dialog/images/toolbarTile.png")
      ImageResource toolbarTile();

      @Source("../../dialog/images/stageAllFiles_2x.png")
      ImageResource stageAllFiles2x();

      @Source("../../dialog/images/discard_2x.png")
      ImageResource discard2x();

      @Source("../../dialog/images/stage_2x.png")
      ImageResource stage2x();

      @Source("../../dialog/images/splitterTileV.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource splitterTileV();

      @Source("../../dialog/images/splitterTileH.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource splitterTileH();

      @Source("../../dialog/images/blankFileIcon_2x.png")
      ImageResource blankFileIcon2x();
   }

   interface Styles extends SharedStyles
   {
      String contextLabel();
      String diffToolbar();
      String diffViewOptions();
   }

   @SuppressWarnings("unused")
   private static class ClickCommand implements HasClickHandlers, Command
   {
      @Override
      public void execute()
      {
         ClickEvent.fireNativeEvent(
               Document.get()
                     .createClickEvent(0,
                                       0,
                                       0,
                                       0,
                                       0,
                                       false,
                                       false,
                                       false,
                                       false),
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


   interface Binder extends UiBinder<Widget, SVNReviewPanel>
   {
   }

   @Inject
   public SVNReviewPanel(SVNChangelistTablePresenter changelist,
                         LineTableView diffPane,
                         Commands commands)
   {
      commands_ = commands;
      splitPanel_ = new SplitLayoutPanel(4);

      changelist_ = changelist.getView();
      lines_ = diffPane;
      lines_.getElement().setTabIndex(-1);
      lines_.hideStageCommands();

      overrideSizeWarning_ = new SizeWarningWidget("diff");

      changelist.setSelectFirstItemByDefault(true);

      Widget widget = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      initWidget(widget);

      topToolbar_.addStyleName(RES.styles().toolbar());

      switchViewButton_ = new LeftRightToggleButton("Changes", "History", true);
      switchViewButton_.getElement().getStyle().setMarginRight(8, Unit.PX);
      topToolbar_.addLeftWidget(switchViewButton_);
      
      topToolbar_.addLeftSeparator();
      
      topToolbar_.addLeftWidget(new ToolbarButton(
            "Refresh", commands.vcsRefresh().getImageResource(),
            new ClickHandler() {
               @Override
               public void onClick(ClickEvent event)
               {
                  changelist_.showProgress();
                  commands_.vcsRefresh().execute();
               }
            }));
      
      topToolbar_.addLeftSeparator();
      
      topToolbar_.addLeftWidget(commands.vcsAddFiles().createToolbarButton());
      topToolbar_.addLeftWidget(commands.vcsRemoveFiles().createToolbarButton());
      topToolbar_.addLeftSeparator();
      topToolbar_.addLeftWidget(commands.vcsRevert().createToolbarButton());
      topToolbar_.addLeftWidget(commands.vcsIgnore().createToolbarButton());
      topToolbar_.addLeftSeparator();
      topToolbar_.addLeftWidget(commands.vcsResolve().createToolbarButton());
      topToolbar_.addLeftSeparator();
      topToolbar_.addLeftWidget(commands.vcsCommit().createToolbarButton());
      
      
      commands.vcsPull().setButtonLabel("Update");
      commands.vcsPull().setMenuLabel("Update");
      topToolbar_.addRightWidget(commands.vcsPull().createToolbarButton());

      diffToolbar_.addStyleName(RES.styles().toolbar());
      diffToolbar_.addStyleName(RES.styles().diffToolbar());

      diffToolbar_.addLeftSeparator();
      discardAllButton_ = diffToolbar_.addLeftWidget(new ToolbarButton(
            "Discard All", new ImageResource2x(RES.discard2x()), (ClickHandler) null));

      listBoxAdapter_ = new ListBoxAdapter(contextLines_);

      new WidgetHandlerRegistration(this)
      {
         @Override
         protected HandlerRegistration doRegister()
         {
            return Event.addNativePreviewHandler(new NativePreviewHandler()
            {
               @Override
               public void onPreviewNativeEvent(NativePreviewEvent event)
               {
                  NativeEvent nativeEvent = event.getNativeEvent();
                  if (event.getTypeInt() == Event.ONKEYDOWN
                      && KeyboardShortcut.getModifierValue(nativeEvent) == KeyboardShortcut.CTRL)
                  {
                     switch (nativeEvent.getKeyCode())
                     {
                        case KeyCodes.KEY_DOWN:
                           nativeEvent.preventDefault();
                           scrollBy(diffScroll_, getLineScroll(diffScroll_), 0);
                           break;
                        case KeyCodes.KEY_UP:
                           nativeEvent.preventDefault();
                           scrollBy(diffScroll_,
                                    -getLineScroll(diffScroll_),
                                    0);
                           break;
                        case KeyCodes.KEY_PAGEDOWN:
                           nativeEvent.preventDefault();
                           scrollBy(diffScroll_, getPageScroll(diffScroll_), 0);
                           break;
                        case KeyCodes.KEY_PAGEUP:
                           nativeEvent.preventDefault();
                           scrollBy(diffScroll_,
                                    -getPageScroll(diffScroll_),
                                    0);
                           break;
                     }
                  }
               }
            });
         }
      };
   }

   private void scrollBy(ScrollPanel scrollPanel, int vscroll, int hscroll)
   {
      if (vscroll != 0)
      {
         scrollPanel.setVerticalScrollPosition(
               Math.max(0, scrollPanel.getVerticalScrollPosition() + vscroll));
      }

      if (hscroll != 0)
      {
         scrollPanel.setHorizontalScrollPosition(
               Math.max(0, scrollPanel.getHorizontalScrollPosition() + hscroll));
      }
   }

   private int getLineScroll(ScrollPanel panel)
   {
      return 30;
   }

   private int getPageScroll(ScrollPanel panel)
   {
      // Return slightly less than the client height (so there's overlap between
      // one screen and the next) but never less than the line scoll height.
      return Math.max(
            getLineScroll(panel),
            panel.getElement().getClientHeight() - getLineScroll(panel));
   }

   @Override
   public HasClickHandlers getSwitchViewButton()
   {
      return switchViewButton_;
   }

   @Override
   public HasClickHandlers getDiscardAllButton()
   {
      return discardAllButton_;
   }


   @Override
   public void setData(ArrayList<ChunkOrLine> lines)
   {
      int vscroll = diffScroll_.getVerticalScrollPosition();
      int hscroll = diffScroll_.getHorizontalScrollPosition();

      getLineTableDisplay().setData(lines, PatchMode.Working);

      diffScroll_.setVerticalScrollPosition(vscroll);
      diffScroll_.setHorizontalScrollPosition(hscroll);
   }

   @Override
   public ArrayList<String> getSelectedPaths()
   {
      return changelist_.getSelectedPaths();
   }
   
   @Override
   public ArrayList<StatusAndPath> getSelectedItems()
   {
      return changelist_.getSelectedItems();
   }

   @Override
   public void setSelectedStatusAndPaths(ArrayList<StatusAndPath> selectedPaths)
   {
      changelist_.setSelectedStatusAndPaths(selectedPaths);
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

   @Override
   public HasClickHandlers getOverrideSizeWarningButton()
   {
      return overrideSizeWarning_;
   }

   @Override
   public void showSizeWarning(long sizeInBytes)
   {
      overrideSizeWarning_.setSize(sizeInBytes);
      diffScroll_.setWidget(overrideSizeWarning_);
   }

   @Override
   public void hideSizeWarning()
   {
      diffScroll_.setWidget(lines_);
   }

   @Override
   public void showContextMenu(final int clientX, 
                               final int clientY)
   {
      final ToolbarPopupMenu menu = new ToolbarPopupMenu();
      
      menu.addItem(commands_.vcsAddFiles().createMenuItem(false));
      menu.addItem(commands_.vcsRemoveFiles().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.vcsRevert().createMenuItem(false));
      menu.addItem(commands_.vcsIgnore().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.vcsResolve().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.vcsOpen().createMenuItem(false));
    
      menu.setPopupPositionAndShow(new PositionCallback() {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            menu.setPopupPosition(clientX, clientY);     
         }
      });
   }

   @Override
   public void onShow()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            changelist_.focus();
         }
      });
   }

   @UiField(provided = true)
   SplitLayoutPanel splitPanel_;
   @UiField(provided = true)
   ChangelistTable changelist_;
   @UiField(provided = true)
   LineTableView lines_;
   @UiField
   ListBox contextLines_;
   @UiField
   Toolbar topToolbar_;
   @UiField
   Toolbar diffToolbar_;
   @UiField
   ScrollPanel diffScroll_;

   private final Commands commands_;
   
   private ListBoxAdapter listBoxAdapter_;

   private ToolbarButton discardAllButton_;

   private LeftRightToggleButton switchViewButton_;

   private SizeWarningWidget overrideSizeWarning_;

   private static final Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
}
