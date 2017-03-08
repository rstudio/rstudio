/*
 * ChunkOutputWidget.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.rstudio.core.client.ColorUtil;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.ProgressSpinner;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.InterruptStatusEvent;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.rmarkdown.model.NotebookFrameMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookHtmlMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookPlotMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookQueueUnit;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutput;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutputUnit;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorHandler;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputHost;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class ChunkOutputWidget extends Composite
                               implements ConsoleWriteOutputHandler,
                                          ConsoleWriteErrorHandler,
                                          RestartStatusEvent.Handler,
                                          InterruptStatusEvent.Handler,
                                          ChunkOutputPresenter.Host
{

   private static ChunkOutputWidgetUiBinder uiBinder = GWT
         .create(ChunkOutputWidgetUiBinder.class);

   interface ChunkOutputWidgetUiBinder
         extends UiBinder<Widget, ChunkOutputWidget>
   {
      
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("ExpandChunkIcon_2x.png")
      ImageResource expandChunkIcon2x();
      
      @Source("CollapseChunkIcon_2x.png")
      ImageResource collapseChunkIcon2x();

      @Source("RemoveChunkIcon_2x.png")
      ImageResource removeChunkIcon2x();

      @Source("PopoutChunkIcon_2x.png")
      ImageResource popoutIcon2x();
   }
   
   public interface ChunkStyle extends CssResource
   {
      String overflowY();
      String collapsed();
      String spinner();
      String pendingResize();
      String fullsize();
      String baresize();
      String noclear();
   }

   public ChunkOutputWidget(String documentId, String chunkId, 
         RmdChunkOptions options, int expansionState, boolean canClose, 
         ChunkOutputHost host, ChunkOutputSize chunkOutputSize)
   {
      documentId_ = documentId;
      chunkId_ = chunkId;
      host_ = host;
      options_ = options;
      chunkOutputSize_ = chunkOutputSize;
      initWidget(uiBinder.createAndBindUi(this));
      expansionState_ = new Value<Integer>(expansionState);
      applyCachedEditorStyle();
      if (expansionState_.getValue() == COLLAPSED)
         setCollapsedStyles();

      ChunkDataWidget.injectPagedTableResources();
      
      if (chunkOutputSize_ == ChunkOutputSize.Default)
      {
         frame_.getElement().getStyle().setHeight(
               expansionState_.getValue() == COLLAPSED ? 
                     ChunkOutputUi.CHUNK_COLLAPSED_HEIGHT :
                     ChunkOutputUi.MIN_CHUNK_HEIGHT, Unit.PX);
      }
      else if (chunkOutputSize_ == ChunkOutputSize.Full)
      {
         addStyleName(style.fullsize());
      }
      else if (chunkOutputSize_ == ChunkOutputSize.Bare)
      {
         addStyleName(style.baresize());
      }
      if (!canClose)
         addStyleName(style.noclear());

      // create the initial output stream and attach it to the frame
      attachPresenter(new ChunkOutputStream(this, chunkOutputSize_));
      
      DOM.sinkEvents(clear_.getElement(), Event.ONCLICK);
      DOM.setEventListener(clear_.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event evt)
         {
            switch(DOM.eventGetType(evt))
            {
            case Event.ONCLICK:
               host_.onOutputRemoved(ChunkOutputWidget.this);
               break;
            };
         }
      });
      
      EventListener toggleExpansion = new EventListener()
      {
         @Override
         public void onBrowserEvent(Event evt)
         {
            switch(DOM.eventGetType(evt))
            {
            case Event.ONCLICK:
               toggleExpansionState(true);
               break;
            };
         }
      };

      EventListener popoutChunkEvent = new EventListener()
      {
         @Override
         public void onBrowserEvent(Event evt)
         {
            switch(DOM.eventGetType(evt))
            {
            case Event.ONCLICK:
               popoutChunk();
               break;
            };
         }
      };

      DOM.sinkEvents(expander_.getElement(), Event.ONCLICK);
      DOM.setEventListener(expander_.getElement(), toggleExpansion);
      
      DOM.sinkEvents(expand_.getElement(), Event.ONCLICK);
      DOM.setEventListener(expand_.getElement(), toggleExpansion);

      DOM.sinkEvents(popout_.getElement(), Event.ONCLICK);
      DOM.setEventListener(popout_.getElement(), popoutChunkEvent);
      
      EventBus events = RStudioGinjector.INSTANCE.getEventBus();
      events.addHandler(RestartStatusEvent.TYPE, this);
      events.addHandler(InterruptStatusEvent.TYPE, this);

      chunkWindowManager_ = RStudioGinjector.INSTANCE.getChunkWindowManager();
   }
   
   // Public methods ----------------------------------------------------------

   public int getExpansionState()
   {
      return expansionState_.getValue();
   }
   
   public void setExpansionState(int state)
   {
      setExpansionState(state, null);
   }
   
   public void setExpansionState(int state, CommandWithArg<Boolean> onTransitionCompleted)
   {
      if (state == expansionState_.getValue())
      {
         if (onTransitionCompleted != null)
            onTransitionCompleted.execute(false);
         return;
      }
      
      toggleExpansionState(false, onTransitionCompleted);
   }
   
   public int getState()
   {
      return state_;
   }

   public void setOptions(RmdChunkOptions options)
   {
      boolean needsSync = options_.include() != options.include();
      options_ = options;
      
      if (needsSync)
         syncHeight(false, false);
   }
   
   public HandlerRegistration addExpansionStateChangeHandler(
         ValueChangeHandler<Integer> handler)
   {
      return expansionState_.addValueChangeHandler(handler);
   }
    
   public void showChunkOutput(RmdChunkOutput output, int mode, int scope,
         boolean complete, boolean ensureVisible)
   {
      if (output.getType() == RmdChunkOutput.TYPE_MULTIPLE_UNIT)
      {
         JsArray<RmdChunkOutputUnit> units = output.getUnits();
      
         // prepare chunk for output on replay
         if (output.isReplay() && state_ == CHUNK_EMPTY && units.length() > 0)
            state_ = CHUNK_PRE_OUTPUT;

         // loop over the output units and emit the appropriate contents for
         // each
         for (int i = 0; i < units.length(); i++)
         {
            showChunkOutputUnit(units.get(i), mode, output.isReplay(), 
                  ensureVisible);
         }

         // if complete, wrap everything up; if not (could happen for partial
         // replay) just sync up the height
         if (complete)
            onOutputFinished(ensureVisible, scope);
         else
            syncHeight(true, ensureVisible);
      }
      else if (output.getType() == RmdChunkOutput.TYPE_SINGLE_UNIT)
      {
         showChunkOutputUnit(output.getUnit(), mode, output.isReplay(), 
               ensureVisible);
      }
   }

   @Override
   public void notifyHeightChanged()
   {
      syncHeight(true, false);
   }
   
   public void syncHeight(final boolean scrollToBottom, 
                          final boolean ensureVisible)
   {
      // special behavior for chunks which don't have output included by 
      // default: hide unless chunk includes errors or is not being run as 
      // a unit
      if (!options_.include() && 
          !hasErrors_ && 
          execScope_ == NotebookQueueUnit.EXEC_SCOPE_CHUNK)
      {
         if (isVisible())
         {
            setVisible(false);
            host_.onOutputHeightChanged(this, 0, ensureVisible);
         }
         return;
      }
      
      // don't sync if not visible and no output yet
      if (!isVisible() && (state_ == CHUNK_EMPTY || state_ == CHUNK_PRE_OUTPUT))
         return;

      setVisible(true);
      
      // clamp chunk height to min/max (the +19 is the sum of the vertical
      // padding on the element)
      int height = ChunkOutputUi.CHUNK_COLLAPSED_HEIGHT;
      if (expansionState_.getValue() == EXPANDED)
      {
         int contentHeight = root_.getElement().getOffsetHeight() + 19;
         height = Math.max(ChunkOutputUi.MIN_CHUNK_HEIGHT, contentHeight);

         // if we have renders pending, don't shrink until they're loaded 
         if (pendingRenders_ > 0 && height < renderedHeight_)
            return;
      }

      // don't report height if it hasn't changed (unless we also need to ensure
      // visibility)
      if (height == renderedHeight_ && !ensureVisible)
         return;

      // cache last reported render size
      renderedHeight_ = height;
      if (scrollToBottom)
         root_.getElement().setScrollTop(root_.getElement().getScrollHeight());
      
      if (chunkOutputSize_ != ChunkOutputSize.Full)
         frame_.getElement().getStyle().setHeight(height, Unit.PX);
         
      // allocate some extra space so the cursor doesn't touch the output frame
      host_.onOutputHeightChanged(this, height + 7, ensureVisible);
   }
   
   public static boolean isEditorStyleCached()
   {
      return s_colors != null;
   }
   
   public static EditorThemeListener.Colors getEditorColors()
   {
      return s_colors;
   }
   
   public void onOutputFinished(boolean ensureVisible, int execScope)
   {
      presenter_.completeOutput();
      
      if (state_ != CHUNK_PRE_OUTPUT)
      {
         // if we got some output, synchronize the chunk's height to accommodate
         // it
         syncHeight(true, ensureVisible);
      }
      else if (execScope == NotebookQueueUnit.EXEC_SCOPE_CHUNK)
      {
         // if executing the whole chunk but no output was received, clean up
         // any prior output and hide the output
         presenter_.clearOutput();
         renderedHeight_ = 0;
         setVisible(false);
         host_.onOutputHeightChanged(this, 0, ensureVisible);
      }

      state_ = presenter_.hasOutput() ? CHUNK_READY : CHUNK_EMPTY;
      setOverflowStyle();
      showReadyState();
      unregisterConsoleEvents();
   }

   public void setCodeExecuting(int mode, int scope)
   {
      // expand if currently collapsed
      if (expansionState_.getValue() == COLLAPSED)
         toggleExpansionState(false);

      // do nothing if code is already executing
      if (state_ == CHUNK_PRE_OUTPUT || 
          state_ == CHUNK_POST_OUTPUT)
      {
         return;
      }

      // clean error state
      hasErrors_ = false;

      // if we already had output, clear it
      if (state_ == CHUNK_READY)
      {
         presenter_.clearOutput();
         attachPresenter(new ChunkOutputStream(this, chunkOutputSize_));
      }

      registerConsoleEvents();
      state_ = CHUNK_PRE_OUTPUT;

      execScope_ = scope;
      showBusyState();
   }
   
   public static void cacheEditorStyle(
      String foregroundColor,
      String backgroundColor,
      String aceEditorColor)
   {      
      // use a muted version of the text color for the outline
      ColorUtil.RGBColor text = ColorUtil.RGBColor.fromCss(aceEditorColor);
      
      // dark themes require a slightly more pronounced color
      ColorUtil.RGBColor outline = new ColorUtil.RGBColor(
            text.red(), text.green(), text.blue(),
            text.isDark() ? 0.12: 0.18);

      String border = outline.asRgb();
      
      // highlight color used in data chunks
      ColorUtil.RGBColor highlight = new ColorUtil.RGBColor(
            text.red(), text.green(), text.blue(), 0.02);
      
      // synthesize a surface color by blending the keyword color with the 
      // background
      JsArrayString classes = JsArrayString.createArray().cast();
      classes.push("ace_editor");
      classes.push("ace_keyword");
      ColorUtil.RGBColor surface = ColorUtil.RGBColor.fromCss(
            DomUtils.extractCssValue(classes, "color"));
      surface = surface.mixedWith(
            ColorUtil.RGBColor.fromCss(backgroundColor), 0.02, 1);
      
      s_colors = new EditorThemeListener.Colors(foregroundColor, backgroundColor, border,
            highlight.asRgb(), surface.asRgb());
   }
   
   public void showServerError(ServerError error)
   {
      // consider: less obtrusive error message 
      RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
            "Chunk Execution Error", error.getMessage());
      
      // treat as an interrupt (don't clear output)
      completeInterrupt();
   }
   
   public void applyCachedEditorStyle()
   {
      if (!isEditorStyleCached())
         return;
      Style frameStyle = frame_.getElement().getStyle();
      frameStyle.setBorderColor(s_colors.border);

      getElement().getStyle().setBackgroundColor(s_colors.background);
      frame_.getElement().getStyle().setBackgroundColor(s_colors.background);

      if (presenter_ != null)
         presenter_.onEditorThemeChanged(s_colors);
   }
   
   public boolean hasErrors()
   {
      return hasErrors_;
   }
   
   public boolean hasPlots()
   {
      return presenter_.hasPlots();
   }
   
   public void updatePlot(String url)
   {
      presenter_.updatePlot(url, style.pendingResize());
   }

   public void setPlotPending(boolean pending)
   {
      presenter_.setPlotPending(pending, style.pendingResize());
   }
   
   public void setHost(ChunkOutputHost host)
   {
      host_ = host;
   }

   public void onResize()
   {
      presenter_.onResize();
   }
   
   // Event handlers ----------------------------------------------------------

   @Override
   public void onConsoleWriteOutput(ConsoleWriteOutputEvent event)
   {
      if (event.getConsole() != chunkId_)
         return;

      initializeOutput(RmdChunkOutputUnit.TYPE_TEXT);
      presenter_.showConsoleText(event.getOutput());
   }
   
   @Override
   public void onConsoleWriteError(ConsoleWriteErrorEvent event)
   {
      if (event.getConsole() != chunkId_)
         return;
      
      initializeOutput(RmdChunkOutputUnit.TYPE_TEXT);
      presenter_.showConsoleError(event.getError());
   }
   
   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      if (event.getStatus() != RestartStatusEvent.RESTART_COMPLETED)
         return;
      
      // when R is restarted, we're not going to get any more output, so act
      // as though the server told us it's done
      if (state_ != CHUNK_READY)
      {
         onOutputFinished(false, NotebookQueueUnit.EXEC_SCOPE_PARTIAL);
      }
   }

   @Override
   public void onInterruptStatus(InterruptStatusEvent event)
   {
      if (event.getStatus() != InterruptStatusEvent.INTERRUPT_COMPLETED)
         return;
      
      completeInterrupt();
   }
   
   public void setRootWidget(Widget widget)
   {
      root_.setWidget(widget);
   }

   public void hideSatellitePopup()
   {
      popout_.setVisible(false);
      hideSatellitePopup_ = true;
   }
   
   public HTMLPanel getFrame()
   {
      return frame_;
   }

   // Private methods ---------------------------------------------------------

   private void showChunkOutputUnit(RmdChunkOutputUnit unit, int mode,
         boolean replay, boolean ensureVisible)
   {
      // no-op for empty console objects (avoid initializing output when we have
      // nothing to show)
      if (unit.getType() == RmdChunkOutputUnit.TYPE_TEXT && 
          unit.getArray().length() < 1)
         return;
      
      // prepare for rendering this output type (except for ordinals, which
      // are not visible)
      if (unit.getType() != RmdChunkOutputUnit.TYPE_ORDINAL)
         initializeOutput(unit.getType());

      switch(unit.getType())
      {
      case RmdChunkOutputUnit.TYPE_TEXT:
         presenter_.showConsoleOutput(unit.getArray());
         break;
      case RmdChunkOutputUnit.TYPE_HTML:
         final RenderTimer widgetTimer = new RenderTimer();
         presenter_.showHtmlOutput(unit.getString(), 
               (NotebookHtmlMetadata)unit.getMetadata().cast(), 
               unit.getOrdinal(),
               new Command() {
                  @Override
                  public void execute()
                  {
                     widgetTimer.cancel();
                  }
               });
         break;
      case RmdChunkOutputUnit.TYPE_PLOT:
         final RenderTimer plotTimer = new RenderTimer();
         presenter_.showPlotOutput(unit.getString(), 
               (NotebookPlotMetadata)unit.getMetadata().cast(), 
               unit.getOrdinal(), 
               new Command() {
                  @Override
                  public void execute()
                  {
                     plotTimer.cancel();
                  }
               });
         break;
      case RmdChunkOutputUnit.TYPE_ERROR:
         // override visibility flag when there's an error in batch mode
         if (!replay && !options_.error() && 
             mode == NotebookQueueUnit.EXEC_MODE_BATCH)
            ensureVisible = true;
         hasErrors_ = true;
         presenter_.showErrorOutput(unit.getUnhandledError());
         break;
      case RmdChunkOutputUnit.TYPE_ORDINAL:
         // used to reserve a plot placeholder 
         presenter_.showOrdinalOutput(unit.getOrdinal());
         break;
      case RmdChunkOutputUnit.TYPE_DATA:
         presenter_.showDataOutput(unit.getOuputObject(), 
               (NotebookFrameMetadata)unit.getMetadata().cast(),
               unit.getOrdinal());
         break;
      }
   }
   
   private class RenderTimer extends Timer
   {
      public RenderTimer()
      {
         pendingRenders_++;

         // ensure we decrement the counter eventually even if content never
         // renders
         schedule(15000);
      }

      @Override
      public void cancel()
      {
         if (isRunning())
            pendingRenders_--;
         super.cancel();
      }

      @Override
      public void run()
      {
         pendingRenders_--;
      }
   };
   
   private void registerConsoleEvents()
   {
      EventBus events = RStudioGinjector.INSTANCE.getEventBus();
      events.addHandler(ConsoleWriteOutputEvent.TYPE, this);
      events.addHandler(ConsoleWriteErrorEvent.TYPE, this);
   }

   private void unregisterConsoleEvents()
   {
      EventBus events = RStudioGinjector.INSTANCE.getEventBus();
      events.removeHandler(ConsoleWriteOutputEvent.TYPE, this);
      events.removeHandler(ConsoleWriteErrorEvent.TYPE, this);
   }
   
   private void showBusyState()
   {
      if (spinner_ != null)
      {
         spinner_.removeFromParent();
         spinner_.detach();
         spinner_ = null;
      }
      // create a black or white spinner as appropriate
      ColorUtil.RGBColor bgColor = 
            ColorUtil.RGBColor.fromCss(s_colors.background);
      spinner_ = new ProgressSpinner(
            bgColor.isDark() ? ProgressSpinner.COLOR_WHITE :
                               ProgressSpinner.COLOR_BLACK);

      spinner_.getElement().addClassName(style.spinner());
      frame_.add(spinner_);
      spinner_.getElement().getStyle().setOpacity(1);
      root_.getElement().getStyle().setOpacity(0.2);

      clear_.setVisible(false);
      expand_.setVisible(false);
      popout_.setVisible(false);
   }

   private void showReadyState()
   {
      if (getElement() != null && getElement().getStyle() != null && s_colors != null)
      {
         getElement().getStyle().setBackgroundColor(s_colors.background);
      }

      if (spinner_ != null)
      {
         spinner_.removeFromParent();
         spinner_.detach();
         spinner_ = null;
      }

      if (expansionState_.getValue() == EXPANDED)
         root_.getElement().getStyle().setOpacity(1);

      if (chunkOutputSize_ != ChunkOutputSize.Full && !hideSatellitePopup_)
      {
         clear_.setVisible(true);
         expand_.setVisible(true);
         popout_.setVisible(true);
      }
   }
   
   private void setOverflowStyle()
   {
      Element ele = root_.getElement();
      boolean hasOverflow = ele.getScrollHeight() > ele.getOffsetHeight();
      if (hasOverflow && !root_.getElement().hasClassName(style.overflowY()))
      {
         frame_.getElement().addClassName(style.overflowY());
      }
      else if (!hasOverflow && 
               root_.getElement().hasClassName(style.overflowY()))
      {
         frame_.getElement().removeClassName(style.overflowY());
      }
   }
   
   private void completeInterrupt()
   {
      if (state_ == CHUNK_PRE_OUTPUT ||
          state_ == CHUNK_POST_OUTPUT)
      {
         state_ = CHUNK_READY;
      }
      else
      {
         return;
      }
      showReadyState();
   }

   private void popoutChunk()
   {
      chunkWindowManager_.openChunkWindow(
         documentId_,
         chunkId_,
         new Size(getElement().getOffsetWidth(), getElement().getOffsetHeight())
      );
   }
   
   private void toggleExpansionState(final boolean ensureVisible)
   {
      toggleExpansionState(ensureVisible, null);
   }
   
   private void toggleExpansionState(final boolean ensureVisible,
                                     final CommandWithArg<Boolean> onTransitionCompleted)
   {
      // don't permit toggling state while we're animating a new state
      // (no simple way to gracefully reverse direction) 
      if (collapseTimer_ != null && collapseTimer_.isRunning())
         return;

      if (expansionState_.getValue() == EXPANDED)
      {
         // remove scrollbars
         frame_.getElement().getStyle().setProperty("transition", 
               "height " + ANIMATION_DUR + "ms ease");
         setCollapsedStyles();
         collapseTimer_ = new Timer()
         {
            @Override
            public void run()
            {
               renderedHeight_ = ChunkOutputUi.CHUNK_COLLAPSED_HEIGHT;
               host_.onOutputHeightChanged(ChunkOutputWidget.this, renderedHeight_, ensureVisible);
               if (onTransitionCompleted != null)
                  onTransitionCompleted.execute(true);
            }
            
         };
         expansionState_.setValue(COLLAPSED, true);
      }
      else
      {
         clearCollapsedStyles();
         expansionState_.setValue(EXPANDED, true);
         syncHeight(true, ensureVisible);
         collapseTimer_ = new Timer()
         {
            @Override
            public void run()
            {
               syncHeight(true, ensureVisible);
               frame_.getElement().getStyle().clearProperty("transition");
               if (onTransitionCompleted != null)
                  onTransitionCompleted.execute(true);
            }
         };
      }
      collapseTimer_.schedule(ANIMATION_DUR);
   }
   
   private void setCollapsedStyles()
   {
      getElement().addClassName(style.collapsed());
      root_.getElement().getStyle().setOverflow(Overflow.HIDDEN);
      root_.getElement().getStyle().setOpacity(0);
      frame_.getElement().getStyle().setHeight(
            ChunkOutputUi.CHUNK_COLLAPSED_HEIGHT, Unit.PX);
   }
   
   private void clearCollapsedStyles()
   {
      getElement().removeClassName(style.collapsed());
      root_.getElement().getStyle().clearOverflow();
      root_.getElement().getStyle().clearOpacity();
   }
   
   private void attachPresenter(ChunkOutputPresenter presenter)
   {
      if (root_.getWidget() != null)
         root_.remove(root_.getWidget());
      presenter_ = presenter;
      root_.add(presenter.asWidget());
   }
   
   private void initializeOutput(int type)
   {
      if (state_ == CHUNK_PRE_OUTPUT)
      {
         hasErrors_ = false;
         state_ = CHUNK_POST_OUTPUT;
      }
      else if (state_ == CHUNK_POST_OUTPUT &&
               presenter_ instanceof ChunkOutputStream &&
               (isBlockType(type) || 
                isBlockType(type) != isBlockType(lastOutputType_)))
      {
         // we switch to gallery mode when we have either two block-type
         // outputs (e.g. two plots), or a block-type output combined with 
         // non-block output (e.g. a plot and some text)
         final ChunkOutputStream stream = (ChunkOutputStream)presenter_;
         final ChunkOutputGallery gallery = new ChunkOutputGallery(this, 
               chunkOutputSize_);

         attachPresenter(gallery);
         
         // extract all the pages from the stream and populate the gallery
         List<ChunkOutputPage> pages = stream.extractPages();
         int ordinal = stream.getContentOrdinal();
         if (ordinal > 0)
         {
            // add the stream itself if there's still anything left in it
            pages.add(new ChunkConsolePage(ordinal, stream, chunkOutputSize_));
         }
         
         // ensure page ordering is correct
         Collections.sort(pages, new Comparator<ChunkOutputPage>()
         {
            @Override
            public int compare(ChunkOutputPage o1, ChunkOutputPage o2)
            {
               return o1.ordinal() - o2.ordinal();
            }
         });
         
         for (ChunkOutputPage page: pages)
         {
            gallery.addPage(page);
         }
         
         syncHeight(false, false);
      }
      
      lastOutputType_ = type;
   }
   
   private boolean isBlockType(int type)
   {
      switch (type)
      {
      case RmdChunkOutputUnit.TYPE_PLOT:
      case RmdChunkOutputUnit.TYPE_DATA:
      case RmdChunkOutputUnit.TYPE_HTML:
          return true;
      case RmdChunkOutputUnit.TYPE_TEXT:
      case RmdChunkOutputUnit.TYPE_ERROR:
      case RmdChunkOutputUnit.TYPE_ORDINAL:
         return false;
      }
      return true;
   }
   
   @UiField Image clear_;
   @UiField Image expand_;
   @UiField Image popout_;
   @UiField SimplePanel root_;
   @UiField ChunkStyle style;
   @UiField HTMLPanel frame_;
   @UiField HTMLPanel expander_;

   private ProgressSpinner spinner_;
   private RmdChunkOptions options_;
   private ChunkOutputHost host_;
   private ChunkOutputPresenter presenter_;
   private ChunkWindowManager chunkWindowManager_;
   private ChunkOutputSize chunkOutputSize_;
   
   private int state_ = CHUNK_EMPTY;
   private int execScope_ = NotebookQueueUnit.EXEC_SCOPE_CHUNK;
   private int renderedHeight_ = 0;
   private int pendingRenders_ = 0;
   private int lastOutputType_ = RmdChunkOutputUnit.TYPE_NONE;
   private boolean hasErrors_ = false;
   private boolean hideSatellitePopup_ = false;
   
   private Timer collapseTimer_ = null;
   private final String documentId_;
   private final String chunkId_;
   private final Value<Integer> expansionState_;

   private static EditorThemeListener.Colors s_colors;

   public final static int EXPANDED   = 0;
   public final static int COLLAPSED  = 1;

   private final static int ANIMATION_DUR = 400;
   
   public final static int CHUNK_EMPTY       = 1;
   public final static int CHUNK_READY       = 2;
   public final static int CHUNK_PRE_OUTPUT  = 3;
   public final static int CHUNK_POST_OUTPUT = 4;
}
