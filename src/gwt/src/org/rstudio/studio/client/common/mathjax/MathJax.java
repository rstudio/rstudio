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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Point;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.container.SafeMap;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.layout.FadeOutAnimation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.mathjax.display.MathJaxPopupPanel;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.inject.Inject;

public class MathJax
{
   interface MathJaxTypesetCallback
   {
      void onMathJaxTypesetComplete(boolean error);
   }
   
   public MathJax(DocDisplay docDisplay)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      docDisplay_ = docDisplay;
      popup_ = new MathJaxPopupPanel(this);
      bgRenderer_ = new MathJaxBackgroundRenderer(this, popup_, docDisplay_);
      handlers_ = new ArrayList<HandlerRegistration>();
      cowToPlwMap_ = new SafeMap<ChunkOutputWidget, PinnedLineWidget>();
      onPopupTypeset_ = new MathJaxTypesetCallback()
      {
         @Override
         public void onMathJaxTypesetComplete(boolean error)
         {
            int offsetWidth  = popup_.getOffsetWidth();
            int offsetHeight = popup_.getOffsetHeight();
            ScreenCoordinates coordinates = computePopupPosition(offsetWidth, offsetHeight);
            popup_.setPopupPosition(coordinates.getPageX(), coordinates.getPageY());
            popup_.show();
         }
      };
      
      handlers_.add(popup_.addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (event.isAttached())
               beginCursorMonitoring();
            else
               endCursorMonitoring();
         }
      }));
      
      handlers_.add(docDisplay_.addBlurHandler(new BlurHandler()
      {
         @Override
         public void onBlur(BlurEvent event)
         {
            endRender();
         }
      }));
      
      handlers_.add(docDisplay_.addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (!event.isAttached())
               detachHandlers();
         }
      }));
   }
   
   public void renderLatex(Range range)
   {
      renderLatex(range, false);
   }
   
   public void renderLatex(Range range, boolean background)
   {
      String text = docDisplay_.getTextForRange(range);
      String pref = uiPrefs_.showLatexPreviewOnCursorIdle().getGlobalValue();
      
      // never render background popups if user opted out
      if (background && pref.equals(UIPrefsAccessor.LATEX_PREVIEW_SHOW_NEVER))
         return;
      
      // render latex chunks as line widgets
      boolean isLatexChunk = text.startsWith("$$") && text.endsWith("$$");
      if (isLatexChunk)
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
            {
               // respect preference for rendering popups
               if (pref.equals(UIPrefsAccessor.LATEX_PREVIEW_SHOW_NEVER) ||
                   pref.equals(UIPrefsAccessor.LATEX_PREVIEW_SHOW_INLINE_ONLY))
               {
                  return;
               }
            }
         }

         renderLatexLineWidget(range, text);
         return;
      }
      
      // if the popup is already showing, just re-render within that popup
      // (don't reset render state)
      if (popup_.isShowing())
      {
         renderPopup(text);
         return;
      }
      
      resetRenderState();
      range_ = range;
      anchor_ = docDisplay_.createAnchoredSelection(range.getStart(), range.getEnd());
      lastRenderedText_ = "";
      popup_.setRenderToolbarVisible(isLatexChunk);
      renderPopup(text);
   }
   
   public void promotePopupToLineWidget()
   {
      if (range_ == null)
         return;
      
      renderLatex(range_, false);
   }
   
   // Private Methods ----
   
   @Inject
   private void initialize(UIPrefs uiPrefs)
   {
      uiPrefs_ = uiPrefs;
   }
   
   private void renderLatexLineWidget(final Range range, final String text)
   {
      // end a previous render session if necessary (e.g. if a popup is showing)
      endRender();
      
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
   
   private void renderPopup(final String text)
   {
      // no need to re-render if text hasn't changed or is empty
      if (text.equals(lastRenderedText_))
         return;
      
      // if empty, hide popup
      if (text.isEmpty())
      {
         endRender();
         return;
      }
      
      // no need to re-position popup if already showing;
      // just typeset
      if (popup_.isShowing())
      {
         mathjaxTypeset(popup_.getContentElement(), text);
         return;
      }
      
      // attach popup to DOM but render offscreen
      popup_.setPopupPosition(100000, 100000);
      popup_.show();
      
      // typeset and position after typesetting finished
      mathjaxTypeset(popup_.getContentElement(), text, onPopupTypeset_);
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
   
   private ScreenCoordinates computePopupPosition(int offsetWidth, int offsetHeight)
   {
      Rectangle bounds = docDisplay_.getRangeBounds(range_);
      Point center = bounds.center();
      
      int pageX = bounds.getRight() + 10;
      int pageY = center.getY() - (offsetHeight / 2);
      
      // check for overflow -- in such a case, display below rather than beside
      int windowWidth = Window.getClientWidth();
      if (pageX + offsetWidth >= (windowWidth - 10))
      {
         pageX = center.getX() - (offsetWidth / 2);
         pageY = bounds.getBottom() + 10;
      }
      
      return ScreenCoordinates.create(pageX, pageY);
   }
   
   private boolean isEmptyLatexChunk(String text)
   {
      return text.matches("^\\$*\\s*\\$*$");
   }
   
   private void detachHandlers()
   {
      for (HandlerRegistration handler : handlers_)
         handler.removeHandler();
      handlers_.clear();
   }
   
   private final DocDisplay docDisplay_;
   private final MathJaxPopupPanel popup_;
   private final MathJaxBackgroundRenderer bgRenderer_;
   private final MathJaxTypesetCallback onPopupTypeset_;
   private final List<HandlerRegistration> handlers_;
   private final SafeMap<ChunkOutputWidget, PinnedLineWidget> cowToPlwMap_;
   
   private AnchoredSelection anchor_;
   private Range range_;
   private HandlerRegistration cursorChangedHandler_;
   private String lastRenderedText_ = "";
   
   // Injected ----
   private UIPrefs uiPrefs_;
}
