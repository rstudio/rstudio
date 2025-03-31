/*
 * ChunkOutputStream.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;

import org.rstudio.core.client.AnsiCode;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.MutationObserver;
import org.rstudio.core.client.js.JsArrayEx;
import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.common.debugging.ui.ConsoleError;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.rmarkdown.model.NotebookFrameMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookHtmlMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookPlotMetadata;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutputUnit;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class ChunkOutputStream extends FlowPanel
                               implements ChunkOutputPresenter,
                                          ConsoleError.Observer
{
   public ChunkOutputStream(ChunkOutputPresenter.Host host)
   {
      this(host, ChunkOutputSize.Default);
   }
   
   public ChunkOutputStream(ChunkOutputPresenter.Host host, ChunkOutputSize chunkOutputSize)
   {
      host_ = host;
      chunkOutputSize_ = chunkOutputSize;
      metadata_ = new HashMap<>();

      if (chunkOutputSize_ == ChunkOutputSize.Full)
      {
         getElement().getStyle().setWidth(100, Unit.PCT);

         getElement().getStyle().setProperty("display", "-ms-flexbox");
         getElement().getStyle().setProperty("display", "-webkit-flex");
         getElement().getStyle().setProperty("display", "flex");

         getElement().getStyle().setProperty("msFlexDirection", "column");
         getElement().getStyle().setProperty("webkitFlexDirection", "column");
         getElement().getStyle().setProperty("flexDirection", "column");

         getElement().getStyle().setOverflow(Overflow.AUTO);
      }
   }

   @Override
   public void showConsoleText(String text)
   {
      // flush any queued errors 
      flushQueuedErrors();

      renderConsoleOutput(text, ChunkConsolePage.CONSOLE_OUTPUT);
   }
   
   @Override
   public void showConsoleError(String error)
   {
      // queue the error -- we don't emit errors right away since a more 
      // detailed error event may be forthcoming
      queuedError_ += error;
   }

   @Override
   public void showConsoleOutput(JsArray<JsArrayEx> output)
   {
      initializeOutput(RmdChunkOutputUnit.TYPE_TEXT);
      
      // track number of newlines in output
      int newlineCount = 0;
      int maxCount = Satellite.isCurrentWindowSatellite() ? 10000 : 500;
      
      for (int i = 0; i < output.length(); i++)
      {
         // the first element is the output, and the second is the text; if we
         // don't have at least 2 elements, it's not a valid entry
         if (output.get(i).length() < 2)
            continue;

         int outputType = output.get(i).getInt(0);
         String outputText = output.get(i).getString(1);
         
         // we don't currently render input as output
         if (outputType == ChunkConsolePage.CONSOLE_INPUT)
            continue;
         
         if (outputType == ChunkConsolePage.CONSOLE_ERROR)
         {
            queuedError_ += outputText;
         }
         else
         {
            // release any queued errors
            if (!queuedError_.isEmpty())
            {
               vconsole_.submit(queuedError_, VirtualConsole.Type.STDERR);
               queuedError_ = "";
            }
            
            submit(outputText, outputType);
         }
         
         // avoid hanging the IDE by displaying too much output
         // https://github.com/rstudio/rstudio/issues/5518
         newlineCount += StringUtil.countMatches(outputText, '\n');
         if (newlineCount >= maxCount)
         {
            vconsole_.submit("\n[Output truncated]", VirtualConsole.Type.STDERR);
            break;
         }
      }
   }
   
   @Override
   public void showPlotOutput(String url, NotebookPlotMetadata metadata, 
         int ordinal, final Command onRenderComplete)
   {
      // flush any queued errors
      initializeOutput(RmdChunkOutputUnit.TYPE_PLOT);
      flushQueuedErrors();
      
      // persist metadata
      metadata_.put(ordinal, metadata);
      
      final ChunkPlotWidget plot = new ChunkPlotWidget(url, metadata, 
            new Command()
            {
               @Override
               public void execute()
               {
                  onRenderComplete.execute();
                  onHeightChanged();
               }
            }, chunkOutputSize_);
      
      // check to see if the given ordinal matches one of the existing
      // placeholder elements
      boolean placed = false;
      for (int i = 0; i < getWidgetCount(); i++) 
      {
         Widget w = getWidget(i);
         int ordAttr = getOrdinal(w.getElement());
         if (ordAttr == ordinal)
         {
            // insert the plot widget after the ordinal 
            plot.getElement().setAttribute(
                  ORDINAL_ATTRIBUTE, "" + ordinal);
            if (i < getWidgetCount() - 1)
               insert(plot, i + 1);
            else
               add(plot);
            placed = true;
            break;
         }
      }

      // if we haven't placed the plot yet, add it at the end of the output
      if (!placed)
         addWithOrdinal(plot, ordinal);
   }

   @Override
   public void showHtmlOutput(String url, NotebookHtmlMetadata metadata, 
         int ordinal, final Command onRenderComplete)
   {
      // flush any queued errors
      initializeOutput(RmdChunkOutputUnit.TYPE_HTML);
      flushQueuedErrors();
      
      // persist metadata
      metadata_.put(ordinal, metadata);
      
      final boolean knitrFigure = metadata.getSizingPolicyKnitrFigure();
      
      // amend the URL to cause any contained widget to use the RStudio viewer
      // sizing policy
      if (url.indexOf('?') > 0)
         url += "&";
      else
         url += "?";

      if (knitrFigure) {
         url += "viewer_pane=1&capabilities=1";
      }

      final ChunkOutputFrame frame = new ChunkOutputFrame(constants_.chunkHtmlOutputFrame());

      if (chunkOutputSize_ == ChunkOutputSize.Default || 
          chunkOutputSize_ == ChunkOutputSize.Natural)
      {
         if (knitrFigure)
         {
            final FixedRatioWidget fixedFrame = new FixedRatioWidget(frame, 
                        ChunkOutputUi.OUTPUT_ASPECT, 
                        ChunkOutputUi.MAX_HTMLWIDGET_WIDTH);

            addWithOrdinal(fixedFrame, ordinal);
         }
         else
         {
            // reduce size of html widget as much as possible and add scroll,
            // once it loads, we will adjust the height appropriately.
            frame.getElement().getStyle().setHeight(25, Unit.PX);
            frame.getElement().getStyle().setOverflow(Overflow.SCROLL);

            frame.getElement().getStyle().setWidth(100, Unit.PCT);
            addWithOrdinal(frame, ordinal);
         }
      }
      else if (chunkOutputSize_ == ChunkOutputSize.Full)
      {
         frame.getElement().getStyle().setPosition(Position.ABSOLUTE);
         frame.getElement().getStyle().setWidth(100, Unit.PCT);
         frame.getElement().getStyle().setHeight(100, Unit.PCT);

         addWithOrdinal(frame, ordinal);
      }

      Element body = frame.getDocument().getBody();
      Style bodyStyle = body.getStyle();
            
      bodyStyle.setPadding(0, Unit.PX);
      bodyStyle.setMargin(0, Unit.PX);

      frame.loadUrlDelayed(url, 250, new Command() 
      {
         @Override
         public void execute()
         {
            onRenderComplete.execute();
            
            if (!knitrFigure) {
               int contentHeight = frame.getWindow().getDocument().getBody().getOffsetHeight();
               frame.getElement().getStyle().setHeight(contentHeight, Unit.PX);

               frame.getElement().getStyle().setOverflow(Overflow.HIDDEN);
               frame.getWindow().getDocument().getBody().getStyle().setOverflow(Overflow.HIDDEN);
            }
            
            onHeightChanged();
         }
      });

      themeColors_ = ChunkOutputWidget.getEditorColors();
      
      afterRender_ = () -> 
      {
         ChunkHtmlPage.syncThemeTextColor(themeColors_, frame.getDocument().getBody());
      };

      // when the frame loads, sync its text color -- note that a frame may load
      // more than once as it's reloaded if gets moved around in the DOM
      Event.sinkEvents(frame.getElement(), Event.ONLOAD);
      Event.setEventListener(frame.getElement(), e ->
      {
         if (Event.ONLOAD == e.getTypeInt())
         {
            afterRender_.execute();
         }
      });
   }

   @Override
   public void showErrorOutput(UnhandledError err)
   {
      hasErrors_ = true;
      
      // if there's only one error frame, it's not worth showing dedicated 
      // error UX
      if (err.getErrorFrames() != null &&
          err.getErrorFrames().length() < 2)
      {
         flushQueuedErrors();
         return;
      }

      int idx = queuedError_.indexOf(err.getErrorMessage());
      if (idx >= 0)
      {
         // emit any messages queued prior to the error
         if (idx > 0)
         {
            renderConsoleOutput(
                  StringUtil.substring(queuedError_, 0, idx), 
                  ChunkConsolePage.CONSOLE_ERROR);
            initializeOutput(RmdChunkOutputUnit.TYPE_ERROR);
         }
         
         // leave messages following the error in the queue
         queuedError_ = StringUtil.substring(
               queuedError_, 
               idx + err.getErrorMessage().length());
      }
      else
      {
         // flush any irrelevant messages from the stream
         flushQueuedErrors();
      }
      
      // TODO: Use Ace theme when appropriate.
      Element errorEl = DomUtils.querySelector(getElement(), "." + VirtualConsole.RES.styles().groupError());
      ConsoleError error = new ConsoleError(err, null, null, this, errorEl);
      UserPrefs prefs =  RStudioGinjector.INSTANCE.getUserPrefs();
      error.setTracebackVisible(prefs.autoExpandErrorTracebacks().getValue());
      add(error);

      flushQueuedErrors();
      onHeightChanged();
   }

   @Override
   public void showOrdinalOutput(int ordinal)
   {
      // ordinals are placeholder elements which can be replaced with content
      // later
      addWithOrdinal(new ChunkOrdinalWidget(), ordinal);
   }
   
   @Override
   public void showDataOutput(JavaScriptObject data, 
         NotebookFrameMetadata metadata, int ordinal)
   {
      metadata_.put(ordinal, metadata);
      addWithOrdinal(new ChunkDataWidget(data, metadata, chunkOutputSize_), ordinal);
   }

   @Override
   public void onErrorBoxResize()
   {
      onHeightChanged();
   }

   @Override
   public void runCommandWithDebug(String command)
   {
      // not implemented (this is is only useful in the console)
   }

   @Override
   public void clearOutput()
   {
      clear();
      if (vconsole_ != null)
         vconsole_.clear();
      lastOutputType_ = RmdChunkOutputUnit.TYPE_NONE;
   }

   @Override
   public void completeOutput()
   {
      // flush any remaining queued errors
      flushQueuedErrors();
      lastOutputType_ = RmdChunkOutputUnit.TYPE_NONE;
   }
   
   @Override
   public void setPlotPending(boolean pending, String pendingStyle)
   {
      for (Widget w: this)
      {
         if (w instanceof FixedRatioWidget && 
             ((FixedRatioWidget)w).getWidget() instanceof Image)
         {
            if (pending)
               w.addStyleName(pendingStyle);
            else
               w.removeStyleName(pendingStyle);
         }
      }
   }

   @Override
   public void updatePlot(String plotUrl, String pendingStyle)
   {
      for (Widget w: this)
      {
         if (w instanceof ChunkPlotWidget)
         {
            // ask the plot to sync this URL (it contains the logic for 
            // determining whether it matches the URL)
            ChunkPlotWidget plot = (ChunkPlotWidget)w;
            plot.updateImageUrl(plotUrl, pendingStyle);
         }
      }
   }

   @Override
   public void showCallbackHtml(String htmlOutput, Element parentElement)
   {
      // flush any queued errors
      initializeOutput(RmdChunkOutputUnit.TYPE_HTML);
      flushQueuedErrors();

      if (StringUtil.isNullOrEmpty(htmlOutput))
         return;
      final ChunkOutputFrame frame = new ChunkOutputFrame(constants_.chunkFeedback());
      add(frame);

      Element body = frame.getDocument().getBody();
      Style bodyStyle = body.getStyle();
      bodyStyle.setPadding(0, Unit.PX);
      bodyStyle.setMargin(0, Unit.PX);

      frame.loadUrlDelayed(htmlOutput, 250, new Command()
      {
         @Override
         public void execute()
         {
            DomUtils.fillIFrame(frame.getIFrame(), htmlOutput);
            DomUtils.forwardWheelEvent(frame.getIFrame().getContentDocument(), parentElement);
            
            int contentHeight = frame.getWindow().getDocument().getDocumentElement().getOffsetHeight();
            frame.getElement().getStyle().setHeight(contentHeight, Unit.PX);
            frame.getElement().getStyle().setWidth(100, Unit.PCT);
            onHeightChanged();

            Command handler = () -> {
               // reset height so we can shrink it if necessary
               frame.getElement().getStyle().setHeight(0, Unit.PX);
  
               // delay calculating the height so any images can load
               new Timer()
               {
                  @Override
                  public void run()
                  {
                     int newHeight = frame.getWindow().getDocument().getDocumentElement().getOffsetHeight();
                     frame.getElement().getStyle().setHeight(newHeight, Unit.PX);
                     onHeightChanged();
                  }
               }.schedule(50);
            };
            
            MutationObserver.Builder builder = new MutationObserver.Builder(handler);
            builder.attributes(true);
            builder.characterData(true);
            builder.childList(true);
            builder.subtree(true);
            MutationObserver observer = builder.get();
            observer.observe(frame.getIFrame().getContentDocument().getBody());
         }
      });
   }

   @Override
   public void onEditorThemeChanged(EditorThemeListener.Colors colors)
   {
      themeColors_ = colors;
      
      // apply the style to any frames in the output
      for (Widget w: this)
      {
         if (w instanceof ChunkOutputFrame)
         {
            ChunkOutputFrame frame = (ChunkOutputFrame)w;
            frame.runAfterRender(afterRender_);
         }
         else if (w instanceof FixedRatioWidget)
         {
            FixedRatioWidget fixedRatioWidget = (FixedRatioWidget)w;
            Widget innerWidget = fixedRatioWidget.getWidget();
            if (innerWidget instanceof ChunkOutputFrame)
            {
               ChunkOutputFrame frame = (ChunkOutputFrame)innerWidget;
               frame.runAfterRender(afterRender_);
            }
         }
         else if (w instanceof EditorThemeListener)
         {
            ((EditorThemeListener)w).onEditorThemeChanged(colors);
         }
      }
   }

   @Override
   public boolean hasOutput()
   {
      return getWidgetCount() > 0;
   }

   @Override
   public boolean hasPlots()
   {
      for (Widget w: this)
      {
         if (w instanceof ChunkPlotWidget)
         {
            return true;
         }
      }
      return false;
   }
   
   @Override
   public boolean hasHtmlWidgets()
   {
      for (Widget w: this)
      {
         if (w instanceof ChunkOutputFrame)
         {
            return true;
         }
      }
      
      return false;
   }
   
   @Override
   public boolean hasErrors()
   {
      return hasErrors_;
   }
   

   @Override
   public void onResize()
   {
      for (Widget w: this)
      {
         if (w instanceof ChunkDataWidget)
         {
            ChunkDataWidget widget = (ChunkDataWidget)w;
            widget.onResize();
         }
      }
   }

   public List<ChunkOutputPage> extractPages()
   {
      // flush any errors so they are properly accounted for
      flushQueuedErrors();
      
      List<ChunkOutputPage> pages = new ArrayList<>();
      List<Widget> removed = new ArrayList<>();
      for (Widget w: this)
      {
         // extract ordinal and metadata
         JavaScriptObject metadata = null;
         String ord = w.getElement().getAttribute(ORDINAL_ATTRIBUTE);
         int ordinal = 0;
         if (!StringUtil.isNullOrEmpty(ord))
            ordinal = StringUtil.parseInt(ord, 0);
         if (metadata_.containsKey(ordinal))
            metadata = metadata_.get(ordinal);

         if (w instanceof ChunkDataWidget)
         {
            ChunkDataWidget widget = (ChunkDataWidget)w;
            ChunkDataPage data = new ChunkDataPage(widget, 
                  (NotebookFrameMetadata)metadata.cast(), ordinal);
            pages.add(data);
            removed.add(w);
            continue;
         }
         else if (w instanceof ChunkOrdinalWidget)
         {
            pages.add(new ChunkOrdinalPage(ordinal));
            removed.add(w);
            continue;
         }

         // extract the inner element if this is a fixed-ratio widget (or just
         // use raw if it's not)
         Widget inner = w;
         if (w instanceof FixedRatioWidget)
            inner = ((FixedRatioWidget)w).getWidget();
         
         if (inner instanceof ChunkPlotWidget)
         {
            ChunkPlotWidget plot = (ChunkPlotWidget)inner;
            ChunkPlotPage page = new ChunkPlotPage(plot.plotUrl(),
                  plot.getMetadata(), ordinal, null, chunkOutputSize_);
            pages.add(page);
            removed.add(w);
         }
         else if (inner instanceof ChunkOutputFrame)
         {
            ChunkOutputFrame frame = (ChunkOutputFrame)inner;
            ChunkHtmlPage html = new ChunkHtmlPage(frame.getUrl(), 
                  (NotebookHtmlMetadata)metadata.cast(), ordinal, null, chunkOutputSize_);

            // cancel any pending page load
            frame.cancelPendingLoad();

            pages.add(html);
            removed.add(w);
         }
      }
      for (Widget r: removed)
         this.remove(r);
      return pages;
   }
   
   /**
    * Gets the ordinal of the content stream (determined by the first ordinal
    * of output).
    * 
    * @return The ordinal of the content stream, or 0 if no ordinal is known
    */
   public int getContentOrdinal()
   {
      for (Widget w: this)
      {
         // ignore consoles with no content
         if (w instanceof PreWidget && w.getElement().getChildCount() == 0)
            continue;
         
         // ignore ordinals
         if (w instanceof ChunkOrdinalWidget)
            continue;
         
         if (w.isVisible() && 
             w.getElement().getStyle().getDisplay() != "none")
         {
            int ord = getOrdinal(w.getElement());
            if (ord > 0)
               return ord;
            return 1;
         }
      }
      return 0;
   }
   
   public String getAllConsoleText()
   {
      String text = "";
      for (Widget w: this)
      {
         if (w instanceof PreWidget)
            text += w.getElement().getInnerText();
      }
      return text;
   }
   
   // Private methods ---------------------------------------------------------
   
   private void flushQueuedErrors()
   {
      if (!queuedError_.isEmpty())
      {
         initializeOutput(RmdChunkOutputUnit.TYPE_TEXT);
         renderConsoleOutput(queuedError_, ChunkConsolePage.CONSOLE_ERROR);
         queuedError_ = "";
      }
   }
   
   private void addWithOrdinal(Widget w, int ordinal)
   {
      w.getElement().setAttribute(ORDINAL_ATTRIBUTE, "" + ordinal);

      // record max observed ordinal
      if (ordinal > maxOrdinal_)
         maxOrdinal_ = ordinal;

      add(w);
   }
   
   private void initializeOutput(int outputType)
   {
      if (lastOutputType_ == outputType)
         return;
      
      if (outputType == RmdChunkOutputUnit.TYPE_TEXT)
      {
         // if switching to textual output, allocate a new virtual console
         initConsole();
      }
      else if (lastOutputType_ == RmdChunkOutputUnit.TYPE_TEXT)
      {
         // if switching from textual input, clear the text accumulator
         if (vconsole_ != null)
            vconsole_.clear();
         console_ = null;
      }
      
      lastOutputType_ = outputType;
   }

   private void initConsole()
   {
      if (console_ == null)
      {
         console_ = new PreWidget();
         console_.getElement().removeAttribute("tabIndex");
         console_.getElement().getStyle().setMarginTop(0, Unit.PX);
         console_.getElement().getStyle().setProperty("whiteSpace", "pre-wrap");
      }
      else
      {
         console_.getElement().setInnerHTML("");
      }
      if (vconsole_ == null)
      {
         vconsole_ = RStudioGinjector.INSTANCE.getVirtualConsoleFactory().create(console_.getElement());
      }
      else
      {
         vconsole_.clear();
      }

      // attach the console
      addWithOrdinal(console_, maxOrdinal_ + 1);
   }
   
   private void submit(String text, int type)
   {
      switch (type)
      {

      case ChunkConsolePage.CONSOLE_INPUT:
         vconsole_.submit(text, VirtualConsole.Type.STDIN);
         break;

      case ChunkConsolePage.CONSOLE_OUTPUT:
         vconsole_.submit(text, VirtualConsole.Type.STDOUT);
         break;

      case ChunkConsolePage.CONSOLE_ERROR:
         vconsole_.submit(text, VirtualConsole.Type.STDERR);
         break;

      }
   }
   
   private void renderConsoleOutput(String text, int type)
   {
      initializeOutput(RmdChunkOutputUnit.TYPE_TEXT);
      submit(text, type);
      onHeightChanged();
   }
   
   private void onHeightChanged()
   {
      host_.notifyHeightChanged();
   }
   
   private int getOrdinal(Element ele)
   {
      String ord = ele.getAttribute(ORDINAL_ATTRIBUTE);
      if (!StringUtil.isNullOrEmpty(ord))
      {
         try
         {
            int ordAttr = Integer.parseInt(ord);
            return ordAttr;
         }
         catch (Exception e)
         {
            return 0;
         }
      }
      return 0;
   }
   
   private final ChunkOutputPresenter.Host host_;
   private final Map<Integer, JavaScriptObject> metadata_;
   
   private PreWidget console_;
   private String queuedError_ = "";
   private VirtualConsole vconsole_;
   private int lastOutputType_ = RmdChunkOutputUnit.TYPE_NONE;
   private boolean hasErrors_ = false;
   private ChunkOutputSize chunkOutputSize_;
   private int maxOrdinal_ = 0;

   private final static String ORDINAL_ATTRIBUTE = "data-ordinal";
   private static final EditorsTextConstants constants_ = GWT.create(EditorsTextConstants.class);

   private Command afterRender_;
   private Colors themeColors_;
}
