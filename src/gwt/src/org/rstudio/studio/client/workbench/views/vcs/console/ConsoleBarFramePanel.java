package org.rstudio.studio.client.workbench.views.vcs.console;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.events.EnsureHiddenEvent;
import org.rstudio.core.client.events.EnsureHiddenHandler;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.studio.client.workbench.views.vcs.console.ConsoleBarPresenter.OutputDisplay;

public class ConsoleBarFramePanel extends Composite
   implements AcceptsOneWidget
{
   @Inject
   public ConsoleBarFramePanel(ConsoleBarPresenter consoleBarPresenter)
   {
      consoleBarPresenter_ = consoleBarPresenter;
      layoutPanel_ = new LayoutPanel();
      layoutPanel_.getElement().getStyle().setOverflow(Overflow.HIDDEN);
   }

   @Override
   public void setWidget(IsWidget w)
   {
      Widget widget = w.asWidget();
      layoutPanel_.add(widget);
      layoutPanel_.setWidgetLeftRight(widget, 0, Unit.PX, 0, Unit.PX);
      layoutPanel_.setWidgetTopBottom(widget,
                                      0, Unit.PX,
                                      CONSOLE_BAR_HEIGHT, Unit.PX);

      finishInit();
   }

   private void finishInit()
   {
      outputView_ = consoleBarPresenter_.getOutputView();
      outputWidget_ = outputView_.asWidget();
      outputWidget_.setSize("100%", "100%");
      layoutPanel_.add(outputWidget_);
      layoutPanel_.setWidgetLeftRight(outputWidget_, 20, Unit.PX, 20, Unit.PX);
      layoutPanel_.setWidgetTopBottom(outputWidget_, outputTopMargin_, Unit.PX,
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

      initWidget(layoutPanel_);
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
               outputView_.onShow();
               layoutPanel_.setWidgetTopBottom(outputWidget_,
                                               outputTopMargin_, Unit.PX,
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
               outputView_.onBeforeHide();
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
      int height = getOffsetHeight() - CONSOLE_BAR_HEIGHT - outputTopMargin_;
      layoutPanel_.setWidgetTopHeight(outputWidget_,
                                      getOffsetHeight(), Unit.PX,
                                      height, Unit.PX);
   }

   private LayoutPanel layoutPanel_;
   private ConsoleBarPresenter consoleBarPresenter_;
   private Widget outputWidget_;
   private OutputDisplay outputView_;
   private int outputTopMargin_ = 20;

   private static final int CONSOLE_BAR_HEIGHT = 23;
}
