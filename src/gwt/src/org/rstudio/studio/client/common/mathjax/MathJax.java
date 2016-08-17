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
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

public class MathJax
{
   public MathJax()
   {
      popup_ = new MathJaxPopupPanel();
      renderTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            String text = docDisplay_.getTextForRange(anchor_.getRange());
            if (!text.equals(lastRenderedText_))
               render(text, false);
         }
      };
   }
   
   public void renderLaTeX(final DocDisplay docDisplay, final Range range)
   {
      initializeRender(docDisplay, range);
   }
   
   public void renderLaTeX(final String text,
                           final ScreenCoordinates coordinates)
   {
      popup_.setText(text);
   }
   
   // Private Members ----
   
   private void initializeRender(final DocDisplay docDisplay, final Range range)
   {
      resetRenderState();
      
      docDisplay_ = docDisplay;
      coordinates_ = docDisplay.documentPositionToScreenCoordinates(range.getEnd());
      anchor_ = docDisplay.createAnchoredSelection(range.getStart(), range.getEnd());
      cursorChangedHandler_ = docDisplay.addCursorChangedHandler(new CursorChangedHandler()
      {
         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            Position cursorPos = event.getPosition();
            if (anchor_ == null || !anchor_.getRange().contains(cursorPos))
            {
               finishRender();
               return;
            }
            
            scheduleRender(300);
         }
      });
      
      render(docDisplay.getTextForRange(range), true);
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
   
   private void finishRender()
   {
      popup_.hide();
   }
   
   private void render(final String text, boolean positionPopup)
   {
      if (!positionPopup)
      {
         mathjaxTypeset(popup_.getElement(), text);
         return;
      }
      
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
   
   private void scheduleRender(int delayMs)
   {
      renderTimer_.schedule(delayMs);
   }
   
   private void onMathJaxTypesetCompleted(String text, boolean error)
   {
      if (error)
         return;
      
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
      MathJax.Hub.Typeset(el, $entry(function() {
         
         // restore original typesetting on failure
         jax = MathJax.Hub.getAllJax(el)[0];
         var error = !!(jax && jax.texError);
         if (error) jax.Text(lastRenderedText);
            
         // callback to GWT
         self.@org.rstudio.studio.client.common.mathjax.MathJax::onMathJaxTypesetCompleted(Ljava/lang/String;Z)(currentText, error);
      }));
   }-*/;
   
   private final Timer renderTimer_;
   private final MathJaxPopupPanel popup_;
   
   private DocDisplay docDisplay_;
   private AnchoredSelection anchor_;
   private HandlerRegistration cursorChangedHandler_;
   private ScreenCoordinates coordinates_;
   private String lastRenderedText_ = "";
}
