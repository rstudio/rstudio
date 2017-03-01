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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.js.JsArrayEx;
import org.rstudio.core.client.widget.FixedRatioWidget;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.common.debugging.ui.ConsoleError;
import org.rstudio.studio.client.rmarkdown.model.NotebookFrameMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookHtmlMetadata;
import org.rstudio.studio.client.rmarkdown.model.NotebookPlotMetadata;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutputUnit;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
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
      metadata_ = new HashMap<Integer, JavaScriptObject>();

      if (chunkOutputSize_ == ChunkOutputSize.Full) {
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
         url += "viewer_pane=1";
      }

      final ChunkOutputFrame frame = new ChunkOutputFrame();

      if (chunkOutputSize_ == ChunkOutputSize.Default) {
         if (knitrFigure) {
            final FixedRatioWidget fixedFrame = new FixedRatioWidget(frame, 
                        ChunkOutputUi.OUTPUT_ASPECT, 
                        ChunkOutputUi.MAX_HTMLWIDGET_WIDTH);

            addWithOrdinal(fixedFrame, ordinal);
         }
         else {
            // reduce size of html widget as much as possible and add scroll,
            // once it loads, we will adjust the height appropriately.
            frame.getElement().getStyle().setHeight(25, Unit.PX);
            frame.getElement().getStyle().setOverflow(Overflow.SCROLL);

            frame.getElement().getStyle().setWidth(100, Unit.PCT);
            addWithOrdinal(frame, ordinal);
         }
      }
      else if (chunkOutputSize_ == ChunkOutputSize.Full) {
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
         };
      });

      themeColors_ = ChunkOutputWidget.getEditorColors();
      afterRender_ = new Command()
      {
         @Override
         public void execute()
         {
            if (themeColors_ != null) {
               Element body = frame.getDocument().getBody();
               
               Style bodyStyle = body.getStyle();
               bodyStyle.setColor(themeColors_.foreground);
            }
         }
      };

      frame.runAfterRender(afterRender_);
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
      addWithOrdinal(new ChunkDataWidget(data, chunkOutputSize_), ordinal);
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
      
      List<ChunkOutputPage> pages = new ArrayList<ChunkOutputPage>();
      List<Widget> removed = new ArrayList<Widget>();
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
   
   private String classOfOutput(int type)
   {
      if (type == ChunkConsolePage.CONSOLE_ERROR)
        return RStudioGinjector.INSTANCE.getUIPrefs().getThemeErrorClass();
      else if (type == ChunkConsolePage.CONSOLE_INPUT)
        return "ace_keyword";
      return null;
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
         vconsole_ = new VirtualConsole(console_.getElement());
      else
         vconsole_.clear();

      // attach the console
      addWithOrdinal(console_, maxOrdinal_ + 1);
   }
   
   private void renderConsoleOutput(String text, String clazz)
   {
      initializeOutput(RmdChunkOutputUnit.TYPE_TEXT);
      vconsole_.submit(text, clazz);
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
         catch(Exception e)
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

   private Command afterRender_;
   private Colors themeColors_;
}
