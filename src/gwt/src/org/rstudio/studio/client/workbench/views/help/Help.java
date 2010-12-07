/*
 * Help.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.help;

import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.core.client.widget.events.GlassVisibilityHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.help.events.*;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations.LinksList;
import org.rstudio.studio.client.workbench.views.help.model.Link;

import java.util.ArrayList;

public class Help extends BasePresenter implements ShowHelpHandler
{
   public interface Binder extends CommandBinder<Commands, Help> {}

   public interface Display extends WorkbenchView, 
                                    HasHelpNavigateHandlers
   {
      String getUrl() ;
      String getDocTitle() ;
      void showHelp(String helpURL);
      void back() ;
      void forward() ;
      void print() ;
      void popout() ;
      void refresh() ;
      
      LinkMenu getHistory() ;
      LinkMenu getFavorites() ;

      /**
       * Returns true if this Help pane has ever been navigated. 
       */
      boolean navigated();
   }
   
   public interface LinkMenu extends HasSelectionHandlers<String>
   {
      void addLink(Link link) ;
      void removeLink(Link link) ;
      boolean containsLink(Link link) ;
      void clearLinks() ;
      ArrayList<Link> getLinks() ;
   }
   
   @Inject
   public Help(Display view,
               HelpServerOperations server,
               Commands commands,
               Binder binder,
               GlobalDisplay globalDisplay)
   {
      super(view);
      server_ = server ;
      view_ = view;
      globalDisplay_ = globalDisplay;

      binder.bind(commands, this);

      view_.addHelpNavigateHandler(new HelpNavigateHandler() {
         public void onNavigate(HelpNavigateEvent event)
         {
            if (!historyInitialized_)
               return;
            LinkMenu history = view_.getHistory() ;
            Link link = new Link(event.getUrl(), event.getTitle()) ;
            history.removeLink(link) ;
            history.addLink(link) ;
            persistHistory() ;
         }
      }) ;
      SelectionHandler<String> navigator = new SelectionHandler<String>() {
         public void onSelection(SelectionEvent<String> event)
         {
            view_.showHelp(event.getSelectedItem()) ;
         }
      } ;
      view_.getFavorites().addSelectionHandler(navigator) ;
      view_.getHistory().addSelectionHandler(navigator) ;

      loadHistory() ;
      //loadFavorites() ;
   }

   // Home handled by Shim for activation from main menu context
   public void onHelpHome() { view_.bringToFront(); home(); }
   
   @Handler public void onHelpBack() { view_.back(); }
   @Handler public void onHelpForward() { view_.forward(); }
   @Handler public void onPrintHelp() { view_.print(); }
   @Handler public void onAddToHelpFavorites() { addToFavorites(); }
   @Handler public void onHelpPopout() { view_.popout(); }
   @Handler public void onRefreshHelp() { view_.refresh(); }
   @Handler
   public void onClearHelpHistory()
   {
      Link current = null;
      ArrayList<Link> links = view_.getHistory().getLinks();
      if (links.size() > 0)
         current = links.get(0);
      view_.getHistory().clearLinks() ;
      if (current != null)
         view_.getHistory().addLink(current);
      persistHistory() ;
   }

   public void onShowHelp(ShowHelpEvent event)
   {
      view_.showHelp(server_.getHelpUrl(event.getTopicUrl()));
      view_.bringToFront();
   }

   private void loadHistory()
   {
      server_.getHelpLinks(HelpServerOperations.HISTORY,
                           new ServerRequestCallback<LinksList>() {
         @Override
         public void onError(ServerError error)
         {
            // Probably not worth displaying an error to the user for this.
            Debug.logError(error); ;
         }
         
         @Override
         public void onResponseReceived(LinksList linksList)
         {
            if (linksList != null)
            {
               LinkMenu history = view_.getHistory() ;
               ArrayList<Link> links = linksList.getLinks() ;
               for (int i = links.size() - 1; i >= 0; i--)
               {
                  if (!history.containsLink(links.get(i)))
                     history.addLink(links.get(i)) ;
               }

               // Restore the most recent link, but only if we haven't
               // already been navigated
               if (!view_.navigated() && links.size() > 0)
               {
                  view_.showHelp(links.get(0).getUrl());
               }
            }
            historyInitialized_ = true ;

            if (!view_.navigated())
               home();
         }
      }) ;
   }

   @SuppressWarnings("unused")
   private void loadFavorites()
   {
      server_.getHelpLinks(HelpServerOperations.FAVORITES,
                           new ServerRequestCallback<LinksList>() {
         @Override
         public void onError(ServerError error)
         {
            globalDisplay_.showErrorMessage("Error Loading Favorites",
                                            error.getUserMessage());
            Debug.logError(error); ;
         }
         
         @Override
         public void onResponseReceived(LinksList linksList)
         {
            if (linksList != null)
            {
               LinkMenu history = view_.getFavorites() ;
               ArrayList<Link> links = linksList.getLinks() ;
               for (Link link : links)
                  history.addLink(link) ;
            }
         }
      }) ;
   }

   private void home()
   {
      String url = "doc/html/index.html" ;
      view_.showHelp(server_.getHelpUrl(url));
   }
   
   public Display getDisplay()
   {
      return view_ ;
   }
   
   private void persistHistory()
   {
      if (historyInitialized_)
      {
         server_.setHelpLinks(HelpServerOperations.HISTORY,
                              view_.getHistory().getLinks()) ;
      }
   }
   
   private void persistFavorites()
   {
      server_.setHelpLinks(HelpServerOperations.FAVORITES, 
                           view_.getFavorites().getLinks()) ;
   }

   private void addToFavorites()
   {
      Link link = new Link(view_.getUrl(), view_.getDocTitle()) ;
      view_.getFavorites().removeLink(link) ;
      view_.getFavorites().addLink(link) ;
      persistFavorites() ;
   }

   private Display view_ ;
   private HelpServerOperations server_ ;
   private boolean historyInitialized_ ;
   private GlobalDisplay globalDisplay_;
}
