/*
 * ChunkOutputStream.java
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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.dom.ImageElementEx;
import org.rstudio.core.client.js.JsArrayEx;
import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.common.debugging.ui.ConsoleError;
import org.rstudio.studio.client.rmarkdown.model.NotebookFrameMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookHtmlMetadata;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutputUnit;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class ChunkOutputStream extends FlowPanel
                               implements ChunkOutputPresenter,
                                          ConsoleError.Observer
{
   public ChunkOutputStream(ChunkOutputPresenter.Host host)
   {
      host_ = host;
   }

   @Override
   public void showConsoleText(String text)
   {
      // flush any queued errors 
      flushQueuedErrors();

      renderConsoleOutput(text, classOfOutput(ChunkConsolePage.CONSOLE_OUTPUT));
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
               vconsole_.submit(queuedError_, classOfOutput(
                     ChunkConsolePage.CONSOLE_ERROR));
               queuedError_ = "";
            }

            vconsole_.submit(outputText, classOfOutput(outputType));
         }
      }
      vconsole_.redraw(console_.getElement());
   }
   
   @Override
   public void showPlotOutput(String url, int ordinal, Command onRenderComplete)
   {
      // flush any queued errors
      initializeOutput(RmdChunkOutputUnit.TYPE_PLOT);
      flushQueuedErrors();
      
      final Image plot = new Image();
      Widget plotWidget = null;
      
      if (isFixedSizePlotUrl(url))
      {
         // if the plot is of fixed size, emit it directly, but make it
         // initially invisible until we get sizing information (as we may 
         // have to downsample)
         plot.setVisible(false);
         plotWidget = plot;
      }
      else
      {
         // if we can scale the plot, scale it
         FixedRatioWidget fixedFrame = new FixedRatioWidget(plot, 
                     ChunkOutputUi.OUTPUT_ASPECT, 
                     ChunkOutputUi.MAX_PLOT_WIDTH);
         plotWidget = fixedFrame;
      }

      // check to see if the given ordinal matches one of the existing
      // placeholder elements
      boolean placed = false;
      for (int i = 0; i < getWidgetCount(); i++) 
      {
         Widget w = getWidget(i);
         String ord = w.getElement().getAttribute(ORDINAL_ATTRIBUTE);
         if (!StringUtil.isNullOrEmpty(ord))
         {
            try
            {
               int ordAttr = Integer.parseInt(ord);
               if (ordAttr == ordinal)
               {
                  // insert the plot widget after the ordinal 
                  plotWidget.getElement().setAttribute(
                        ORDINAL_ATTRIBUTE, "" + ordinal);
                  if (i < getWidgetCount() - 1)
                     insert(plotWidget, i + 1);
                  else
                     add(plotWidget);
                  placed = true;
                  break;
               }
            }
            catch(Exception e)
            {
               Debug.logException(e);
            }
         }
      }

      // if we haven't placed the plot yet, add it at the end of the output
      if (!placed)
         addWithOrdinal(plotWidget, ordinal);
      
      DOM.sinkEvents(plot.getElement(), Event.ONLOAD);
      DOM.setEventListener(plot.getElement(), createPlotListener(plot, 
            onRenderComplete));

      plot.setUrl(url);
   }

   @Override
   public void showHtmlOutput(String url, NotebookHtmlMetadata metadata, 
         int ordinal, final Command onRenderComplete)
   {
      // flush any queued errors
      initializeOutput(RmdChunkOutputUnit.TYPE_HTML);
      flushQueuedErrors();
      
      // amend the URL to cause any contained widget to use the RStudio viewer
      // sizing policy
      if (url.indexOf('?') > 0)
         url += "&";
      else
         url += "?";
      url += "viewer_pane=1";

      final ChunkOutputFrame frame = new ChunkOutputFrame();
      final FixedRatioWidget fixedFrame = new FixedRatioWidget(frame, 
                  ChunkOutputUi.OUTPUT_ASPECT, 
                  ChunkOutputUi.MAX_HTMLWIDGET_WIDTH);

      addWithOrdinal(fixedFrame, ordinal);

      frame.loadUrl(url, new Command() 
      {
         @Override
         public void execute()
         {
            Element body = frame.getDocument().getBody();
            Style bodyStyle = body.getStyle();
            
            bodyStyle.setPadding(0, Unit.PX);
            bodyStyle.setMargin(0, Unit.PX);
            bodyStyle.setColor(ChunkOutputWidget.getForegroundColor());
            
            onRenderComplete.execute();
         };
      });
   }

   @Override
   public void showErrorOutput(UnhandledError err)
   {
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
            renderConsoleOutput(queuedError_.substring(0, idx), 
                  classOfOutput(ChunkConsolePage.CONSOLE_ERROR));
            initializeOutput(RmdChunkOutputUnit.TYPE_ERROR);
         }
         // leave messages following the error in the queue
         queuedError_ = queuedError_.substring(
               idx + err.getErrorMessage().length());
      }
      else
      {
         // flush any irrelevant messages from the stream
         flushQueuedErrors();
      }
      
      UIPrefs prefs =  RStudioGinjector.INSTANCE.getUIPrefs();
      ConsoleError error = new ConsoleError(err, prefs.getThemeErrorClass(), 
            this, null);
      error.setTracebackVisible(prefs.autoExpandErrorTracebacks().getValue());

      add(error);
      flushQueuedErrors();
      host_.notifyHeightChanged();
   }

   @Override
   public void showOrdinalOutput(int ordinal)
   {
      // ordinals are placeholder elements which can be replaced with content
      // later
      HTML ord = new HTML("");
      ord.getElement().getStyle().setDisplay(Display.NONE);
      addWithOrdinal(ord, ordinal);
   }
   
   @Override
   public void showDataOutput(JavaScriptObject data, 
         NotebookFrameMetadata metadata)
   {
      add(new ChunkDataWidget(data));
   }

   @Override
   public void onErrorBoxResize()
   {
      host_.notifyHeightChanged();
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
         if (w instanceof FixedRatioWidget)
         {
            // extract the wrapped plot
            FixedRatioWidget fixedFrame = (FixedRatioWidget)w;
            if (!(fixedFrame.getWidget() instanceof Image))
               continue;
            Image plot = (Image)fixedFrame.getWidget();
            
            ChunkPlotPage.updateImageUrl(w, plot, plotUrl, pendingStyle);
         }
      }
   }
   
   @Override
   public void syncEditorColor(String color)
   {
      // apply the style to any frames in the output
      for (Widget w: this)
      {
         if (w instanceof ChunkOutputFrame)
         {
            ChunkOutputFrame frame = (ChunkOutputFrame)w;
            Style bodyStyle = frame.getDocument().getBody().getStyle();
            bodyStyle.setColor(color);
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
         if (isPlotWidget(w))
         {
            return true;
         }
      }
      return false;
   }
   
   public List<ChunkOutputPage> extractPages()
   {
      List<ChunkOutputPage> pages = new ArrayList<ChunkOutputPage>();
      for (Widget w: this)
      {
         if (w instanceof ChunkDataWidget)
         {
            ChunkDataWidget widget = (ChunkDataWidget)w;
            ChunkDataPage data = new ChunkDataPage(widget);
            pages.add(data);
            remove(w);
            continue;
         }

         if (!(w instanceof FixedRatioWidget))
            continue;
         
         Widget inner = ((FixedRatioWidget)w).getWidget();
         
         if (inner instanceof Image)
         {
            Image image = (Image)inner;
            ChunkPlotPage plot = new ChunkPlotPage(image.getUrl());
            pages.add(plot);
            remove(w);
         }
         else if (inner instanceof ChunkOutputFrame)
         {
            ChunkOutputFrame frame = (ChunkOutputFrame)inner;
            ChunkHtmlPage html = new ChunkHtmlPage(frame.getUrl(), null, null);
            pages.add(html);
            remove(w);
         }
      }
      return pages;
   }
   
   public boolean hasContent()
   {
      for (Widget w: this)
      {
         if (w.isVisible() && 
             w.getElement().getStyle().getDisplay() != "none")
            return true;
      }
      return false;
   }

   // Private methods ---------------------------------------------------------
   
   private boolean isPlotWidget(Widget w)
   {
     return w instanceof FixedRatioWidget && 
             ((FixedRatioWidget)w).getWidget() instanceof Image;
   }

   private String classOfOutput(int type)
   {
      if (type == ChunkConsolePage.CONSOLE_ERROR)
        return RStudioGinjector.INSTANCE.getUIPrefs().getThemeErrorClass();
      else if (type == ChunkConsolePage.CONSOLE_INPUT)
        return "ace_keyword";
      return null;
   }
   
   private EventListener createPlotListener(final Image plot, 
         final Command onRenderComplete)
   {
      return new EventListener()
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (DOM.eventGetType(event) != Event.ONLOAD)
               return;
            
            // if the image is of fixed size, just clamp its width to the editor
            // surface while preserving its aspect ratio
            if (isFixedSizePlotUrl(plot.getUrl()))
            {
               ImageElementEx img = plot.getElement().cast();
               img.getStyle().setProperty("height", "auto");
               img.getStyle().setProperty("maxWidth", "100%");
            }
               
            plot.setVisible(true);
            if (onRenderComplete != null)
               onRenderComplete.execute();
            host_.notifyHeightChanged();
         }
      };
   }

   private void flushQueuedErrors()
   {
      if (!queuedError_.isEmpty())
      {
         initializeOutput(RmdChunkOutputUnit.TYPE_TEXT);
         renderConsoleOutput(queuedError_, classOfOutput(
               ChunkConsolePage.CONSOLE_ERROR));
         queuedError_ = "";
      }
   }
   
   private boolean isFixedSizePlotUrl(String url)
   {
      return url.contains("fixed_size=1");
   }
   
   private void addWithOrdinal(Widget w, int ordinal)
   {
      w.getElement().setAttribute(ORDINAL_ATTRIBUTE, "" + ordinal);
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
      if (vconsole_ == null)
         vconsole_ = new VirtualConsole();
      else
         vconsole_.clear();
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

      // attach the console
      add(console_);
   }
   
   private void renderConsoleOutput(String text, String clazz)
   {
      initializeOutput(RmdChunkOutputUnit.TYPE_TEXT);
      vconsole_.submitAndRender(text, clazz,
            console_.getElement());
      host_.notifyHeightChanged();
   }
   
   private final ChunkOutputPresenter.Host host_;
   
   private PreWidget console_;
   private String queuedError_ = "";
   private VirtualConsole vconsole_;
   private int lastOutputType_ = RmdChunkOutputUnit.TYPE_NONE;

   private final static String ORDINAL_ATTRIBUTE = "data-ordinal";
}
