package org.rstudio.studio.client.common.mathjax;

import org.rstudio.core.client.widget.ThemedPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.inject.Singleton;

@Singleton
public class MathJax
{
   public MathJax()
   {
      ensureMathJaxLoaded();
   }
   
   public static void renderLaTeX(final String text,
                                  final ScreenCoordinates coordinates)
   {
      final ThemedPopupPanel popup = new ThemedPopupPanel(true, false);
      final FlowPanel panel = new FlowPanel();
      panel.getElement().setInnerText(text);
      popup.setWidget(panel);
      
      popup.setPopupPositionAndShow(new PositionCallback()
      {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            popup.setPopupPosition(
                  coordinates.getPageX() + 10,
                  coordinates.getPageY() + 10);
            mathjaxTypeset(panel.getElement());
         }
      });
   }
   
   // Private Members ----
   
   private static final native void initializeMathJaxConfig() /*-{
      
      if (typeof $wnd.MathJax !== "undefined")
         return;
         
      $wnd.MathJax = {
         extensions: ['tex2jas.js'],
         jax: ['input/TeX', 'output/HTML-CSS'],
         tex2jax: {
            inlineMath:  [['$', '$'], ['\\(', '\\)']],
            displayMath: [['$$', '$$'], ['\\[', '\\]']],
            processEscapes: true
         },
         "HTML-CSS": {
            availableFonts: ['TeX']
         },
         showProcessingMessage: false,
         messageStyle: "none",
         skipStartupTypeset: true,
         menuSettings: {
            zoom: "Click"
         }
      };
      
   }-*/;
   
   private ScriptElement createMathJaxScriptElement()
   {
      ScriptElement el = Document.get().createScriptElement();
      el.setAttribute("type", "text/javascript");
      el.setSrc("mathjax/MathJax.js?config=TeX-MML-AM_CHTML");
      el.setAttribute("async", "true");
      return el;
   }
   
   private void ensureMathJaxLoaded()
   {
      if (MATHJAX_LOADED)
         return;
      
      initializeMathJaxConfig();
      ScriptElement mathJaxEl = createMathJaxScriptElement();
      HeadElement headEl = Document.get().getHead();
      headEl.appendChild(mathJaxEl);
      MATHJAX_LOADED = true;
   }
   
   private static final native void mathjaxTypeset(Element el) /*-{
      var MathJax = $wnd.MathJax;
      MathJax.Hub.Typeset(el);
   }-*/;
   
   private static boolean MATHJAX_LOADED = false;
}
