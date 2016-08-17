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

import org.rstudio.studio.client.common.mathjax.display.MathJaxPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

public class MathJax
{
   public MathJax(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      bgRenderer_ = new MathJaxBackgroundRenderer(docDisplay);
      popup_ = new MathJaxPopupPanel();
      
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
      String text = docDisplay_.getTextForRange(range);
      if (text.startsWith("$$") && text.endsWith("$$"))
      {
         renderLatexLineWidget(range, text);
         return;
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
   
   private void renderLatexLineWidget(Range range, String text)
   {
      int row = range.getEnd().getRow() + 1;
      LineWidget widget = docDisplay_.getLineWidgetForRow(row);
      
      boolean hasWidget = widget != null;
      if (!hasWidget)
      {
         Element el = createLineWidgetElement();
         widget = LineWidget.create("mathjax", row, el);
         docDisplay_.addLineWidget(widget);
      }
      
      mathjaxTypeset(widget.getElement(), text);
      widget.setPixelHeight(widget.getElement().getOffsetHeight());
      docDisplay_.onLineWidgetChanged(widget);
   }
   
   private Element createLineWidgetElement()
   {
      Element el = Document.get().createDivElement().cast();
      el.getStyle().setPaddingLeft(20, Unit.PX);
      return el;
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
   
   private void onMathJaxTypesetCompleted(String text, boolean error)
   {
      if (error) return;
      lastRenderedText_ = text;
   }
   
   private final native void mathjaxTypeset(Element el, String currentText)
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
            if (error) jax.Text(lastRenderedText);

            // callback to GWT
            self.@org.rstudio.studio.client.common.mathjax.MathJax::onMathJaxTypesetCompleted(Ljava/lang/String;Z)(currentText, error);
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
   
   private final DocDisplay docDisplay_;
   private final MathJaxBackgroundRenderer bgRenderer_;
   private final MathJaxPopupPanel popup_;
   
   private AnchoredSelection anchor_;
   private HandlerRegistration cursorChangedHandler_;
   private ScreenCoordinates coordinates_;
   private String lastRenderedText_ = "";
}
