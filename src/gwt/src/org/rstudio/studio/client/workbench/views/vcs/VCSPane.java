/*
 * VCSPane.java
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.events.EnsureHiddenEvent;
import org.rstudio.core.client.events.EnsureHiddenHandler;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.vcs.VCS.Display;
import org.rstudio.studio.client.workbench.views.vcs.console.ConsoleBarPresenter;
import org.rstudio.studio.client.workbench.views.vcs.console.ConsoleBarPresenter.OutputDisplay;

import java.util.ArrayList;

public class VCSPane extends WorkbenchPane implements Display
{
   @Inject
   public VCSPane(Provider<ConsoleBarPresenter> pConsoleBar,
                  Session session,
                  Commands commands)
   {
      super(session.getSessionInfo().getVcsName());
      pConsoleBar_ = pConsoleBar;
      commands_ = commands;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();
      moreMenu.addItem(commands_.vcsIgnore().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands_.vcsPull().createMenuItem(false));
      moreMenu.addItem(commands_.vcsPush().createMenuItem(false));
      moreMenu.addSeparator();
      moreMenu.addItem(commands_.vcsShowHistory().createMenuItem(false));

      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(commands_.vcsDiff().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.vcsRevert().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.vcsCommit().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(new ToolbarButton(
            "More",
            StandardIcons.INSTANCE.more_actions(),
            moreMenu));

      toolbar.addRightWidget(commands_.vcsRefresh().createToolbarButton());
      return toolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
      table_ = new ChangelistTable();

      layoutPanel_ = new LayoutPanel();
      layoutPanel_.add(table_);
      layoutPanel_.setWidgetLeftRight(table_, 0, Unit.PX, 0, Unit.PX);
      layoutPanel_.setWidgetTopBottom(table_, 0, Unit.PX, CONSOLE_BAR_HEIGHT, Unit.PX);

      consoleBarPresenter_ = pConsoleBar_.get();

      outputView_ = consoleBarPresenter_.getOutputView();
      outputWidget_ = outputView_.asWidget();
      outputWidget_.setSize("100%", "100%");
      layoutPanel_.add(outputWidget_);
      layoutPanel_.setWidgetLeftRight(outputWidget_, 20, Unit.PX, 20, Unit.PX);
      layoutPanel_.setWidgetTopBottom(outputWidget_, OUTPUT_TOP_MARGIN, Unit.PX,
                                      CONSOLE_BAR_HEIGHT, Unit.PX);
      layoutPanel_.setWidgetVisible(outputWidget_, false);

      outputView_.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         @Override
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            setOutputPaneVisible(true);
         }
      });
      outputView_.addEnsureHiddenHandler(new EnsureHiddenHandler()
      {
         @Override
         public void onEnsureHidden(EnsureHiddenEvent event)
         {
            setOutputPaneVisible(false);
         }
      });

      ConsoleBarPresenter.Display consoleBarView =
            consoleBarPresenter_.getConsoleBarView();

      layoutPanel_.add(consoleBarView);
      layoutPanel_.setWidgetLeftRight(consoleBarView, 0, Unit.PX, 0, Unit.PX);
      layoutPanel_.setWidgetBottomHeight(consoleBarView, 0, Unit.PX, CONSOLE_BAR_HEIGHT, Unit.PX);

      consoleBarView.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            setOutputPaneVisible(!outputWidget_.isVisible());
         }
      });

      return layoutPanel_;
   }

   private void setOutputPaneVisible(boolean visible)
   {
      if (outputWidget_.isVisible() == visible)
         return;

      if (visible)
      {
         positionOutputToBottom();
         layoutPanel_.forceLayout();
         layoutPanel_.setWidgetVisible(outputWidget_, true);
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               layoutPanel_.setWidgetTopBottom(outputWidget_,
                                               OUTPUT_TOP_MARGIN, Unit.PX,
                                               CONSOLE_BAR_HEIGHT, Unit.PX);
               layoutPanel_.animate(300, new AnimationCallback()
               {
                  @Override
                  public void onAnimationComplete()
                  {
                     consoleBarPresenter_.setOutputVisible(true);
                  }

                  @Override
                  public void onLayout(Layer layer, double progress)
                  {
                  }
               });
            }
         });
      }
      else
      {
         positionOutputToBottom();
         layoutPanel_.animate(300, new AnimationCallback()
         {
            @Override
            public void onAnimationComplete()
            {
               layoutPanel_.setWidgetVisible(outputWidget_, false);
               consoleBarPresenter_.setOutputVisible(false);
            }

            @Override
            public void onLayout(Layer layer, double progress)
            {
            }
         });

      }
   }

   private void positionOutputToBottom()
   {
      int height = getOffsetHeight() - CONSOLE_BAR_HEIGHT - OUTPUT_TOP_MARGIN;
      layoutPanel_.setWidgetTopHeight(outputWidget_,
                                      getOffsetHeight(), Unit.PX,
                                      height, Unit.PX);
   }

   @Override
   public void setItems(ArrayList<StatusAndPath> items)
   {
      table_.setItems(items);
   }

   @Override
   public ArrayList<String> getSelectedPaths()
   {
      return table_.getSelectedPaths();
   }

   private final Provider<ConsoleBarPresenter> pConsoleBar_;
   private final Commands commands_;
   private ChangelistTable table_;
   private LayoutPanel layoutPanel_;
   private ConsoleBarPresenter consoleBarPresenter_;
   private Widget outputWidget_;
   private OutputDisplay outputView_;

   private static final int CONSOLE_BAR_HEIGHT = 23;
   private static final int OUTPUT_TOP_MARGIN = 20;
}
