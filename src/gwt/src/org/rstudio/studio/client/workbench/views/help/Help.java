/*
 * Help.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import org.rstudio.core.client.CsvReader;
import org.rstudio.core.client.CsvWriter;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.workbench.WorkbenchList;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.ListChangedHandler;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.help.events.*;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;
import org.rstudio.studio.client.workbench.views.help.model.Link;

import java.util.ArrayList;
import java.util.Iterator;

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
      void focus();
      
      LinkMenu getHistory() ;

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
               WorkbenchListManager listManager,
               Commands commands,
               Binder binder)
   {
      super(view);
      server_ = server ;
      helpHistoryList_ = listManager.getHelpHistoryList();
      view_ = view;

      binder.bind(commands, this);

      view_.addHelpNavigateHandler(new HelpNavigateHandler() {
         public void onNavigate(HelpNavigateEvent event)
         {
            if (!historyInitialized_)
               return;
            
            CsvWriter csvWriter = new CsvWriter();
            csvWriter.writeValue(getApplicationRelativeHelpUrl(event.getUrl()));
            csvWriter.writeValue(event.getTitle());
            helpHistoryList_.append(csvWriter.getValue());

         }
      }) ;
      SelectionHandler<String> navigator = new SelectionHandler<String>() {
         public void onSelection(SelectionEvent<String> event)
         {
            showHelp(event.getSelectedItem()) ;
         }
      } ;
      view_.getHistory().addSelectionHandler(navigator) ;

      // initialize help history
      helpHistoryList_.addListChangedHandler(new ListChangedHandler() {
         @Override
         public void onListChanged(ListChangedEvent event)
         {
            // clear existing
            final LinkMenu history = view_.getHistory() ;
            history.clearLinks();
            
            // intialize from the list
            ArrayList<String> list = event.getList();
            for (int i=0; i<list.size(); i++)
            {
               // parse the two fields out
               CsvReader csvReader = new CsvReader(list.get(i));
               Iterator<String[]> it = csvReader.iterator();
               if (!it.hasNext())
                  continue;
               String[] fields = it.next();
               if (fields.length != 2)
                  continue;
            
               // add the link
               Link link = new Link(fields[0], fields[1]);
               history.addLink(link);
            }
            
            // one time init
            if (!historyInitialized_)
            {
               // mark us initialized
               historyInitialized_ = true ;
               
               if (!view_.navigated())
               {
                  ArrayList<Link> links = history.getLinks();
                  if (links.size() > 0)
                     showHelp(links.get(0).getUrl());
                  else
                     home();
               }    
            }
         }  
      });
      
   }

   // Home handled by Shim for activation from main menu context
   public void onHelpHome() { view_.bringToFront(); home(); }
   
    
   
   @Handler public void onHelpBack() { view_.back(); }
   @Handler public void onHelpForward() { view_.forward(); }
   @Handler public void onPrintHelp() { view_.print(); }
   @Handler public void onHelpPopout() { view_.popout(); }
   @Handler public void onRefreshHelp() { view_.refresh(); }
   @Handler
   public void onClearHelpHistory()
   {
      if (!historyInitialized_)
         return;
      
      helpHistoryList_.clear();
   }

   public void onShowHelp(ShowHelpEvent event)
   {
      showHelp(event.getTopicUrl());
      view_.bringToFront();
   }
   
   public void onActivateHelp(ActivateHelpEvent event)
   {
      view_.bringToFront();
      view_.focus();
   }

   private void home()
   {
      showHelp("help/doc/html/index.html");
   }
   
   public Display getDisplay()
   {
      return view_ ;
   }
   
   private void showHelp(String topicUrl)
   {
      view_.showHelp(server_.getApplicationURL(topicUrl));
   }
   
   private String getApplicationRelativeHelpUrl(String helpUrl)
   {
      String appUrl = server_.getApplicationURL("");
      if (helpUrl.startsWith(appUrl) && !helpUrl.equals(appUrl))
         return helpUrl.substring(appUrl.length());
      else
         return helpUrl;
   }

   private Display view_ ;
   private HelpServerOperations server_ ;
   private WorkbenchList helpHistoryList_;
   private boolean historyInitialized_ ;
}
