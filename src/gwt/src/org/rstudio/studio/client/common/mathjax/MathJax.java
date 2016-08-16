package org.rstudio.studio.client.common.mathjax;

import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.widget.ThemedPopupPanel;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.inject.Singleton;

@Singleton
public class MathJax
{
   public MathJax()
   {
      // load mathjax
      ScriptElement el = Document.get().createScriptElement();
      el.setAttribute("type", "text/javascript");
      el.setSrc("https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-MML-AM_CHTML");
      el.setAttribute("async", "true");
      Document.get().getHead().appendChild(el);
      
      mathjaxLoader_.addCallback(new Callback()
      {
         @Override
         public void onLoaded()
         {
            
         }
      });
      
      Timer timer = new Timer()
      {
         @Override
         public void run()
         {
            mathjaxDemo();
         }
      };
      timer.schedule(3000);
   }
   
   public void mathjaxDemo()
   {
      final ThemedPopupPanel popup = new ThemedPopupPanel();
      
      // create panel with some LaTeX
      final FlowPanel panel = new FlowPanel();
      final String contents =
            "This is some math. Let's compute the sum: " +
            "$\\sum_{i=1}^{N}x_i$" +
            ".";
      
      panel.getElement().setInnerHTML(contents);
      panel.getElement().setId("mathjax-target");
      panel.setWidth("400px");
      panel.setHeight("300px");
      popup.setWidget(panel);
      
      // show rendered LaTeX
      popup.setPopupPositionAndShow(new PositionCallback()
      {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            popup.setPopupPosition(200, 200);
            new Timer()
            {
               @Override
               public void run()
               {
                  renderLaTeX(panel.getElement().getId());
               }
            }.schedule(1000);
         }
      });
   }
   
   private static final native void renderLaTeX(String id) /*-{
      var MathJax = $wnd.MathJax;
      
      MathJax.Hub.Config({
         
         TeX: {
            equationNumbers: {
               autoNumber: "AMS"
            }
         },
         
         tex2jax: {
            inlineMath: [
               ["$", "$"],
               ["\\(", "\\)"]
            ],
            processEscapes: true
         }
         
      });
      
      MathJax.Hub.Queue(
          function () {
            if (MathJax.InputJax.TeX.resetEquationNumbers) {
              MathJax.InputJax.TeX.resetEquationNumbers();
            }
          },
          ['Typeset', MathJax.Hub, id]
        );
   }-*/;
   
   // Private Members ----
   private ExternalJavaScriptLoader mathjaxLoader_ =
         new ExternalJavaScriptLoader(MathJaxResources.INSTANCE.mathjaxJs().getSafeUri().asString());
}
