/*
 * ScopeTreeManager.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ActiveScopeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;

// NOTE: Historically, scope tree management was implemented as part of
// an accompanying code model, written as part of our JavaScript Ace
// support files. Unfortunately the interface required by a particular
// code model and its associated scope tree was never made explicit,
// and implementing these tools in a dynamic language like JavaScript
// is somewhat cumbersome. This class provides a re-imagined interface
// towards the development of a scope tree.
public abstract class ScopeTreeManager
{
   public abstract void onToken(Token token,
                                Position position,
                                ScopeManager manager);
   
   public ScopeTreeManager(DocDisplay docDisplay)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      docDisplay_ = docDisplay;
      worker_ = new Worker();
      scopeManager_ = new ScopeManager();
      
      handlers_ = new HandlerRegistration[] {
            
            docDisplay.addAttachHandler((AttachEvent event) -> {
               if (!event.isAttached())
                  detach();
            }),
            
            docDisplay.addDocumentChangedHandler((DocumentChangedEvent event) -> {
               final Position position = event.getEvent().getRange().getStart();
               Scheduler.get().scheduleDeferred(() -> {
                  
                  Position rebuildPos = scopeManager_.invalidateFrom(Position.create(position));
                  if (rebuildPos == null)
                     rebuildPos = position;
                     
                  worker_.rebuildScopeTreeFromRow(rebuildPos.getRow());
               });
            }),
            
            docDisplay.addCursorChangedHandler((CursorChangedEvent event) -> {
               final Position position = event.getPosition();
               Scope scope = scopeManager_.getScopeAt(position);
               if (lastActiveScope_ != scope)
               {
                  lastActiveScope_ = scope;
                  docDisplay_.fireEvent(new ActiveScopeChangedEvent(scope));
               }
            })
            
      };
   }
   
   public Scope getScopeAt(Position position)
   {
      return scopeManager_.getScopeAt(position);
   }
   
   public JsArray<Scope> getScopeTree()
   {
      return scopeManager_.getScopeList();
   }
   
   public boolean isReady(int row)
   {
      Position parsePosition = scopeManager_.getParsePosition();
      return parsePosition.getRow() > row;
   }
   
   public void detach()
   {
      for (HandlerRegistration handler : handlers_)
         handler.removeHandler();
   }
   
   private class Worker
   {
      private Worker()
      {
         timer_ = new Timer()
         {
            @Override
            public void run()
            {
               work();
            }
         };
      }
      
      public void rebuildScopeTreeFromRow(int row)
      {
         startRow_ = row;
         endRow_ = Math.min(docDisplay_.getRowCount(), startRow_ + ROWS_TOKENIZED_PER_ITERATION);
         work();
      }
      
      private int work()
      {
         Position position = Position.create(startRow_ - 1, 0);
         
         // if editing near the start of the document, the token iterator may fail
         // to find any initial token. in that case, just step forward (this should
         // walk to the first token in the document)
         TokenIterator it = docDisplay_.createTokenIterator();
         Token token = it.moveToPosition(position, true);
         if (token == null)
            token = it.stepForward();
         
         while (true)
         {
            // if we don't have a token, that implies we've reached the end of the document.
            // notify listeners that we're done building the scope tree
            if (token == null)
            {
               // save the parse position (needed when invalidating rows as the document mutates)
               scopeManager_.setParsePosition(Position.create(it.getCurrentTokenRow(), -1));
               
               // notify listeners that we have a scope tree + the current scope
               JsArray<Scope> scopeTree = scopeManager_.getScopeList();
               Scope currentScope = scopeManager_.getScopeAt(docDisplay_.getCursorPosition());
               ScopeTreeReadyEvent event = new ScopeTreeReadyEvent(scopeTree, currentScope);
               docDisplay_.fireEvent(event);
               
               // we're done!
               return docDisplay_.getRowCount();
            }
            
            // if we've walked past the end row, bail
            int row = it.getCurrentTokenRow();
            if (row >= endRow_)
               break;
            
            // let subclass respond to current token, and move forward
            onToken(token, it.getCurrentTokenPosition(), scopeManager_);
            token = it.stepForward();
         }
         
         // save the parse position (needed when invalidating rows as the document mutates)
         scopeManager_.setParsePosition(Position.create(it.getCurrentTokenRow(), -1));
         
         // if there are still rows to be tokenized in the document,
         // schedule more work
         if (startRow_ < docDisplay_.getRowCount())
         {
            startRow_ = it.getCurrentTokenRow();
            endRow_ = Math.min(docDisplay_.getRowCount(), startRow_ + ROWS_TOKENIZED_PER_ITERATION);
            timer_.schedule(DELAY_MS);
         }
         
         return it.getCurrentTokenRow();
      }
      
      private int startRow_;
      private int endRow_;
      
      private final Timer timer_;
      
      private static final int DELAY_MS = 5;
      private static final int ROWS_TOKENIZED_PER_ITERATION = 200;
   }
   
   protected Scope lastActiveScope_;
   
   protected final DocDisplay docDisplay_;
   private final Worker worker_;
   private final ScopeManager scopeManager_;
   private final HandlerRegistration[] handlers_;
}
