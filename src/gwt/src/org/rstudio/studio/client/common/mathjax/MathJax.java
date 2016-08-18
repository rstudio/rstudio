/*
 * MathJax.java
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
package org.rstudio.studio.client.common.mathjax;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.container.SafeMap;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.layout.FadeOutAnimation;
import org.rstudio.studio.client.common.mathjax.display.MathJaxPopupPanel;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.PinnedLineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputHost;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

public class MathJax
{
   interface MathJaxTypesetCallback
   {
      void onMathJaxTypesetComplete(boolean error);
   }
   
   public MathJax(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      bgRenderer_ = new MathJaxBackgroundRenderer(this, docDisplay);
      popup_ = new MathJaxPopupPanel();
      cowToPlwMap_ = new SafeMap<ChunkOutputWidget, PinnedLineWidget>();
      
      docDisplay_.addBlurHandler(new BlurHandler()
      {
         @Override
         public void onBlur(BlurEvent event)
         {
            endRender();
         }
      });
      
      popup_.addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (event.isAttached())
               beginCursorMonitoring();
            else
               endCursorMonitoring();
         }
      });
   }
   
   public void renderLatex(Range range)
   {
      renderLatex(range, false);
   }
   
   public void renderLatex(Range range, boolean background)
   {
      String text = docDisplay_.getTextForRange(range);
      
      // render latex chunks as line widgets
      if (text.startsWith("$$") && text.endsWith("$$"))
      {
         // escape hatch for background renders of line widgets that
         // have not yet been added to the document
         do
         {
            // don't render if chunk contents empty
            if (isEmptyLatexChunk(text))
               return;

            // don't create new line widget on background render
            if (background)
            {
               int row = range.getEnd().getRow();
               LineWidget widget = docDisplay_.getLineWidgetForRow(row);
               if (widget == null)
                  break;
            }

            renderLatexLineWidget(range, text);
            return;
         }
         while (false);
      }
      
      if (popup_.isShowing())
      {
         render(text);
         return;
      }
      
      resetRenderState();
      coordinates_ = docDisplay_.documentPositionToScreenCoordinates(range.getEnd());
      anchor_ = docDisplay_.createAnchoredSelection(range.getStart(), range.getEnd());
      lastRenderedText_ = "";
      render(text);
   }
   
   // Private Methods ----
   
   private void renderLatexLineWidget(final Range range, final String text)
   {
      // if there's a popup showing for this chunk, remove it
      resetRenderState();
      popup_.hide();
      
      int row = range.getEnd().getRow();
      LineWidget widget = docDisplay_.getLineWidgetForRow(row);
      
      boolean hasWidget = widget != null;
      if (!hasWidget)
         widget = createMathJaxLineWidget(row);
      
      final Element el = DomUtils.getFirstElementWithClassName(
            widget.getElement(),
            "mathjax-root");
      
      // call 'onLineWidgetChanged' to ensure the widget is attached
      docDisplay_.onLineWidgetChanged(widget);
      
      // defer typesetting just to ensure that the widget has actually been
      // attached to the DOM
      final LineWidget lineWidget = widget;
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            mathjaxTypeset(el, text, new MathJaxTypesetCallback()
            {
               @Override
               public void onMathJaxTypesetComplete(boolean error)
               {
                  // re-position the element
                  int height = el.getOffsetHeight() + 30;
                  Element ppElement = el.getParentElement().getParentElement();
                  ppElement.getStyle().setHeight(height, Unit.PX);
                  docDisplay_.onLineWidgetChanged(lineWidget);
               }
            });
         }
      });
   }
   
   private LineWidget createMathJaxLineWidget(int row)
   {
      final FlowPanel panel = new FlowPanel();
      panel.getElement().addClassName("mathjax-root");
      
      ChunkOutputHost host = new ChunkOutputHost()
      {
         @Override
         public void onOutputRemoved(final ChunkOutputWidget widget)
         {
            final PinnedLineWidget plw = cowToPlwMap_.get(widget);
            if (plw == null)
               return;
            
            FadeOutAnimation anim = new FadeOutAnimation(widget, new Command()
            {
               @Override
               public void execute()
               {
                  cowToPlwMap_.remove(widget);
                  plw.detach();
               }
            });
            anim.run(400);
         }
         
         @Override
         public void onOutputHeightChanged(ChunkOutputWidget widget,
                                           int height,
                                           boolean ensureVisible)
         {
         }
      };
      
      ChunkOutputWidget outputWidget = new ChunkOutputWidget(
            StringUtil.makeRandomId(8),
            RmdChunkOptions.create(),
            ChunkOutputWidget.EXPANDED,
            host);
      
      outputWidget.setRootWidget(panel);

      PinnedLineWidget plWidget = new PinnedLineWidget(
            "mathjax",
            docDisplay_,
            outputWidget,
            row,
            null,
            null);

      cowToPlwMap_.put(outputWidget, plWidget);
      return plWidget.getLineWidget();
   }
   
   private void resetRenderState()
   {
      if (anchor_ != null)
      {
         anchor_.detach();
         anchor_ = null;
      }
      
      if (cursorChangedHandler_ != null)
      {
         cursorChangedHandler_.removeHandler();
         cursorChangedHandler_ = null;
      }
      
      lastRenderedText_ = "";
   }
   
   private void render(final String text)
   {
      // no need to re-render if text hasn't changed
      if (text.equals(lastRenderedText_))
         return;
      
      // no need to re-position popup if already showing
      if (popup_.isShowing())
      {
         mathjaxTypeset(popup_.getElement(), text);
         return;
      }
      
      // position popup and typeset
      popup_.setPopupPositionAndShow(new PositionCallback()
      {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            popup_.setPopupPosition(
                  coordinates_.getPageX() + 10,
                  coordinates_.getPageY() + 10);
            mathjaxTypeset(popup_.getElement(), text);
         }
      });
   }
   
   private void endRender()
   {
      resetRenderState();
      popup_.hide();
   }
   
   private void onMathJaxTypesetCompleted(String text,
                                          boolean error,
                                          Object commandObject)
   {
      if (commandObject != null && commandObject instanceof MathJaxTypesetCallback)
      {
         MathJaxTypesetCallback callback = (MathJaxTypesetCallback) commandObject;
         callback.onMathJaxTypesetComplete(error);
      }
      
      if (error) return;
      lastRenderedText_ = text;
   }
   
   private final void mathjaxTypeset(Element el, String currentText)
   {
      mathjaxTypeset(el, currentText, null);
   }
   
   private final native void mathjaxTypeset(Element el,
                                            String currentText,
                                            Object command)
   /*-{
      var MathJax = $wnd.MathJax;
      
      // save last rendered text
      var jax = MathJax.Hub.getAllJax(el)[0];
      var lastRenderedText = jax && jax.originalText || "";
      
      // update text in element
      el.innerText = currentText;
      
      // typeset element
      var self = this;
      MathJax.Hub.Queue($entry(function() {
         MathJax.Hub.Typeset(el, $entry(function() {
            // restore original typesetting on failure
            jax = MathJax.Hub.getAllJax(el)[0];
            var error = !!(jax && jax.texError);
            if (error && lastRenderedText.length)
               jax.Text(lastRenderedText);

            // callback to GWT
            self.@org.rstudio.studio.client.common.mathjax.MathJax::onMathJaxTypesetCompleted(Ljava/lang/String;ZLjava/lang/Object;)(currentText, error, command);
         }));
      }));
   }-*/;
   
   private void beginCursorMonitoring()
   {
      endCursorMonitoring();
      cursorChangedHandler_ = docDisplay_.addCursorChangedHandler(new CursorChangedHandler()
      {
         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            Position position = event.getPosition();
            if (anchor_ == null || !anchor_.getRange().contains(position))
               endRender();
         }
      });
   }
   
   private void endCursorMonitoring()
   {
      if (cursorChangedHandler_ != null)
      {
         cursorChangedHandler_.removeHandler();
         cursorChangedHandler_ = null;
      }
   }
   
   private boolean isEmptyLatexChunk(String text)
   {
      return text.matches("^\\$*\\s*\\$*$");
   }
   
   private final DocDisplay docDisplay_;
   private final MathJaxBackgroundRenderer bgRenderer_;
   private final MathJaxPopupPanel popup_;
   private final SafeMap<ChunkOutputWidget, PinnedLineWidget> cowToPlwMap_;
   
   private AnchoredSelection anchor_;
   private HandlerRegistration cursorChangedHandler_;
   private ScreenCoordinates coordinates_;
   private String lastRenderedText_ = "";
}
