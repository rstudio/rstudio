/*
 * UserInterfaceHighlighter.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsVector;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandEvent;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.dom.DOMRect;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.dom.MutationObserver;
import org.rstudio.core.client.events.HighlightEvent;
import org.rstudio.core.client.events.HighlightEvent.HighlightQuery;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UserInterfaceHighlighter
      implements CommandHandler,
                 HighlightEvent.Handler
                 
{
   private static class HighlightPair
   {
      public HighlightPair(Element monitoredElement,
                           Element highlightedElement)
      {
         this(monitoredElement, highlightedElement, null, null, 0);
      }

      public HighlightPair(Element monitoredElement,
                           Element highlightedElement,
                           String callback,
                           UserInterfaceHighlighter highlighter,
                           int index)
      {
         monitoredElement_ = monitoredElement;
         highlightedElement_ = highlightedElement;
         highlighter_ = highlighter;
         callback_ = callback;
         index_ = index;
         handler_ = highlighter_ != null ? addListener() : null;
      }

      public Element getMonitoredElement()
      {
         return monitoredElement_;
      }
      
      public Element getHighlightElement()
      {
         return highlightedElement_;
      }

      public void clearHandler()
      {
         if (handler_ != null)
            handler_.removeHandler();
      }

      public void executeCallback()
      {
         // This method must be called by a single HighlightPair, but because there can be multiple
         // pairs per query, we need to check if the callback has already executed before proceeding.
         if(!highlighter_.getCallbackProcessed(index_))
         {
            highlighter_.setCallbackProcessed(index_, true);
            highlighter_.getServer().executeRCode(callback_, new ServerRequestCallback<String>(){

               @Override
               public void onResponseReceived(String results)
               {
                  // Remove listener from this element and all other elements with the same query
                  highlighter_.clearEvents();
               }

               @Override
               public void onError(ServerError error)
               {
                  // Remove listener from this element and all other elements with the same query
                  highlighter_.clearEvents();
                  Debug.logError(error);
               }
            });
         }
      }

      private HandlerRegistration addListener()
      {
         final JavaScriptObject function = addEventListener(callback_, monitoredElement_);

         return new HandlerRegistration()
         {
            public void removeHandler()
            {
               invokeJavaScriptFunction(function);
            }
         };
      }

      private native JavaScriptObject addEventListener(String code, Element el)/*-{
         var thiz = this;
         var callback = $entry(function() {
            thiz.@org.rstudio.studio.client.workbench.UserInterfaceHighlighter.HighlightPair::executeCallback()();
            });
         el.addEventListener("click", callback, true);
         el.addEventListener("focus", callback, true);

         return function() {
            el.removeEventListener("click", callback);
            el.removeEventListener("focus", callback);
         };
      }-*/;

      private static native void invokeJavaScriptFunction(JavaScriptObject jsFunc)/*-{
         jsFunc();
      }-*/;

      private final int index_;
      private final UserInterfaceHighlighter highlighter_;
      private final Element monitoredElement_;
      private final Element highlightedElement_;
      private final String callback_;
      private final HandlerRegistration handler_;
   }

   public SourceServerOperations getServer()
   {
      return server_;
   }

   public boolean getCallbackProcessed(int index)
   {
      return queryCallbackStatuses_.get(index);
   }

   public void setCallbackProcessed(int index, boolean value)
   {
      queryCallbackStatuses_.set(index, value);
   }

   public void clearEvents()
   {
      for (HighlightPair pair : highlightPairs_)
         pair.clearHandler();
   }

   @Inject
   public UserInterfaceHighlighter(Commands commands,
                                   EventBus events,
                                   SourceServerOperations server)
   {
      server_ = server;
      events.addHandler(CommandEvent.TYPE, this);
      events.addHandler(HighlightEvent.TYPE, this);
      
      highlightQueries_ = JsVector.createVector();
      queryCallbackStatuses_ = new ArrayList<>();
      highlightPairs_ = new ArrayList<>();
      
      // use a timer that aggressively re-positions the highlight elements.
      // while we might normally want a more methodological approach here,
      // there's simply too many things that can cause the position of an
      // element to change in the DOM, and because we want to react to those
      // changes as fast as possible (so that the highlight UI remains 'in sync')
      // we ultimately use an aggressively-scheduled timer.
      repositionTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            repositionHighlighters();
            repositionTimer_.schedule(REPOSITION_DELAY_MS);
         }
      };
      
      // use mutation observer to detect changes to the DOM (primarily, for GWT
      // popups) and use that as a signal to refresh and reposition highlighters
      Command callback = () -> refreshHighlighters();
      observer_ =
            new MutationObserver.Builder(callback)
            .childList(true)
            .get();
      
      observer_.observe(Document.get().getBody());
      
   }
   
   
   // Event Handlers ----
   
   @Override
   public void onCommand(AppCommand command)
   {
   }

   @Override
   public void onHighlight(HighlightEvent event)
   {
      highlightQueries_ = event.getData();
      queryCallbackStatuses_.clear();
      for (int i = 0; i < highlightQueries_.size(); i++)
         queryCallbackStatuses_.add(false);
      refreshHighlighters();
      repositionTimer_.schedule(REPOSITION_DELAY_MS);
   }
   
   
   
   // Private methods ----
   
   private void refreshHighlighters()
   {
      try
      {
         // we will be mutating the DOM here explicitly, but do not
         // want to be notified of these events (otherwise we risk
         // getting into an infinite loop). temporarily disable and
         // then re-enable our mutation observer
         observer_.disconnect();
         removeHighlightElements();
         for (int i = 0, n = highlightQueries_.size(); i < n; i++)
         {
            HighlightQuery hq = highlightQueries_.get(i);
            if (queryCallbackStatuses_.get(i))
               addHighlightElements(hq.getQuery(), hq.getParent(), new String(), 0);
            else
               addHighlightElements(hq.getQuery(), hq.getParent(), hq.getCallback(), i);
         }
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
      finally
      {
         observer_.observe(Document.get().getBody());
      }
   }
   
   private void addHighlightElements(String query, int parent, String code, int index)
   {
      NodeList<Element> els = DomUtils.querySelectorAll(Document.get().getBody(), query);
      
      // guard against queries that might select an excessive number
      // of UI elements
      int n = Math.min(HIGHLIGHT_ELEMENT_MAX, els.getLength());
      if (n == 0)
         return;
      
      for (int i = 0; i < n; i++)
      {
         // retrieve the requested element (selecting a parent if so requested)
         Element el = els.getItem(i);
         for (int j = 0; j < parent; j++)
            el = el.getParentElement();
         
         // create highlight element
         Element highlightEl = Document.get().createDivElement();
         if (!RStudioGinjector.INSTANCE.getUserPrefs().reducedMotion().getValue())
            highlightEl.addClassName(RES.styles().highlightEl());
         else
            highlightEl.addClassName(RES.styles().staticHighlightEl());
         Document.get().getBody().appendChild(highlightEl);
         
         // record the pair of elements
         if (StringUtil.isNullOrEmpty(code))
            highlightPairs_.add(new HighlightPair(el, highlightEl));
         else
            highlightPairs_.add(new HighlightPair(el, highlightEl, code, this, index));
      }
      
      repositionTimer_.schedule(REPOSITION_DELAY_MS);
      repositionHighlighters();
   }
   
   private void removeHighlightElements()
   {
      for (HighlightPair pair : highlightPairs_)
         pair.getHighlightElement().removeFromParent();
      clearEvents();
      
      repositionTimer_.cancel();
      highlightPairs_.clear();
   }
   
   private void repositionHighlighters()
   {
      int scrollX = Window.getScrollLeft();
      int scrollY = Window.getScrollTop();
      
      for (HighlightPair pair : highlightPairs_)
      {
         Element monitoredEl = pair.getMonitoredElement();
         Element highlightEl = pair.getHighlightElement();
         
         if (monitoredEl == null)
         {
            highlightEl.getStyle().setVisibility(Visibility.HIDDEN);
            continue;
         }
         
         // Ensure highlight displays above requested element.
         if (StringUtil.isNullOrEmpty(highlightEl.getStyle().getZIndex()))
         {
            DomUtils.findParentElement(monitoredEl, (Element el) -> {
               
               Style style = DomUtils.getComputedStyles(el);
               String zIndex = style.getZIndex();
               if (StringUtil.isNullOrEmpty(zIndex))
                  return false;
               
               if (zIndex.contentEquals("-1"))
                  return false;
               
               int value = StringUtil.parseInt(zIndex, -1);
               if (value == -1)
                  return false;
               
               highlightEl.getStyle().setZIndex(value);
               return true;
               
            });
         }
         
         Style style = highlightEl.getStyle();
         if (style.getVisibility() != "visible")
            style.setVisibility(Visibility.VISIBLE);
         
         DOMRect bounds = ElementEx.getBoundingClientRect(monitoredEl);
         
         int top = bounds.getTop();
         int left = scrollX + bounds.getLeft();
         int width = bounds.getWidth();
         int height = bounds.getHeight();

         // Ignore out-of-bounds elements.
         if (width == 0 || height == 0)
         {
            highlightEl.getStyle().setVisibility(Visibility.HIDDEN);
            continue;
         }

         // Ignore if monitoredEl is hidden by a scrollable parent.
         Element el = monitoredEl.getParentElement();
         while (el != Document.get().getBody())
         {
            bounds = ElementEx.getBoundingClientRect(el);
            if (bounds.getHeight() > 0 &&
               ((top + height) <= bounds.getTop() ||
                 top > bounds.getBottom()))
            {
               highlightEl.getStyle().setVisibility(Visibility.HIDDEN);
               break;
            }
            el = el.getParentElement();
         }
         if (StringUtil.equals(Visibility.HIDDEN.getCssName(),
                               highlightEl.getStyle().getVisibility()))
            continue;

         // Avoid using too-narrow highlights.
         if (width < 20)
         {
            int rest = 20 - width;
            left = left - (rest / 2);
            width = 20;
         }

         top += scrollY;
         style.setTop(top, Unit.PX);
         style.setLeft(left, Unit.PX);
         style.setWidth(width, Unit.PX);
         style.setHeight(height, Unit.PX);
      }
   }
   
   
   
   // Resources ----

   public interface Styles extends CssResource
   {
      String highlightEl();
      String staticHighlightEl();
   }

   public interface Resources extends ClientBundle
   {
      @Source("UserInterfaceHighlighter.css")
      Styles styles();
   }

   private static final Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }
   
   
   // Private members ----
   
   private JsVector<HighlightQuery> highlightQueries_;
   private List<Boolean> queryCallbackStatuses_;
   private final List<HighlightPair> highlightPairs_;
   private final Timer repositionTimer_;
   private final MutationObserver observer_;
   private final SourceServerOperations server_;
   
   private static final int HIGHLIGHT_ELEMENT_MAX = 10;
   private static final int REPOSITION_DELAY_MS = 0;
}
