/*
 * UserInterfaceHighlighter.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
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
         monitoredElement_ = monitoredElement;
         highlightedElement_ = highlightedElement;
      }

      public Element getMonitoredElement()
      {
         return monitoredElement_;
      }
      
      public Element getHighlightElement()
      {
         return highlightedElement_;
      }
      
      private final Element monitoredElement_;
      private final Element highlightedElement_;
   }
   
   @Inject
   public UserInterfaceHighlighter(Commands commands,
                                   EventBus events)
   {
      events.addHandler(CommandEvent.TYPE, this);
      events.addHandler(HighlightEvent.TYPE, this);
      
      highlightQueries_ = JsVector.createVector();
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
      //
      // debounce to avoid excessive responses to a cascade of events
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
      // TODO: Currently used for debugging + testing.
      
      /*
      JsVector<HighlightQuery> queries = JsVector.createVector();
      for (String prefix : new String[] { ".rstudio_", ".rstudio_tb_", "#rstudio_", "#rstudio_label_" })
      {
         String query = prefix + command.getId().toLowerCase();
         queries.push(HighlightQuery.create(query, 0));
      }
      
      highlightQueries_ = queries;
      refreshHighlighters();
      repositionTimer_.schedule(REPOSITION_DELAY_MS);
      */
   }

   @Override
   public void onHighlight(HighlightEvent event)
   {
      highlightQueries_ = event.getData();
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
            addHighlightElements(hq.getQuery(), hq.getParent());
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
   
   private void addHighlightElements(String query, int parent)
   {
      NodeList<Element> els = DomUtils.querySelectorAll(Document.get().getBody(), query);
      
      int n = Math.min(20, els.getLength());
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
         highlightEl.addClassName(RES.styles().highlightEl());
         Document.get().getBody().appendChild(highlightEl);
         
         // Ensure highlight displays above requested element.
         String zIndex = DomUtils.getInheritedProperty(el, "zIndex");
         if (zIndex != null)
         {
            int value = StringUtil.parseInt(zIndex, -1);
            if (value != -1)
               highlightEl.getStyle().setZIndex(value + 1);
         }
         
         // record the pair of elements
         highlightPairs_.add(new HighlightPair(el, highlightEl));
      }
      
      repositionTimer_.schedule(REPOSITION_DELAY_MS);
      repositionHighlighters();
   }
   
   private void removeHighlightElements()
   {
      for (HighlightPair pair : highlightPairs_)
         pair.getHighlightElement().removeFromParent();
      
      repositionTimer_.cancel();
      highlightPairs_.clear();
   }
   
   private void repositionHighlighters()
   {
      int scrollX = Window.getScrollLeft();
      int scrollY = Window.getScrollTop();
      
      final int borderPx = 0;
      
      for (HighlightPair pair : highlightPairs_)
      {
         Element monitoredEl = pair.getMonitoredElement();
         Element highlightEl = pair.getHighlightElement();
         
         if (monitoredEl == null)
         {
            highlightEl.getStyle().setVisibility(Visibility.HIDDEN);
            continue;
         }
         
         Style style = highlightEl.getStyle();
         if (style.getVisibility() != "visible")
            style.setVisibility(Visibility.VISIBLE);
         
         DOMRect bounds = ElementEx.getBoundingClientRect(monitoredEl);
         
         int top = scrollY + bounds.getTop() - borderPx;
         int left = scrollX + bounds.getLeft() - borderPx;
         int width = bounds.getWidth() + borderPx + borderPx;
         int height = bounds.getHeight() + borderPx + borderPx;
         
         // Ignore out-of-bounds elements.
         if (width == 0 && height == 0)
         {
            highlightEl.getStyle().setVisibility(Visibility.HIDDEN);
            continue;
         }
         
         // Avoid using too-narrow highlights.
         if (width < 20)
         {
            int rest = 20 - width;
            left = left - (rest / 2);
            width = 20;
         }
         
         // This is a hack to give buttons with labels a bit more padding.
         if (width > height + 2)
            width = width + 2;
         
         if (style.getTop() != top + "px")
            style.setTop(top, Unit.PX);
         
         if (style.getLeft() != left + "px")
            style.setLeft(left, Unit.PX);
         
         if (style.getWidth() != width + "px")
            style.setWidth(width, Unit.PX);
         
         if (style.getHeight() != height + "px")
            style.setHeight(height, Unit.PX);
      }
   }
   
   
   
   // Resources ----

   public interface Styles extends CssResource
   {
      String highlightEl();
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
   private final List<HighlightPair> highlightPairs_;
   private final Timer repositionTimer_;
   private final MutationObserver observer_;
   
   private static final int REPOSITION_DELAY_MS = 0;
}
