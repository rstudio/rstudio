/*
 * TextEditingTargetThemeHelper.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeStyleChangedEvent;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;

public class TextEditingTargetThemeHelper
{  
   public TextEditingTargetThemeHelper(final TextEditingTarget editingTarget,
                                       final EventBus eventBus)
   {
      // do an initial sync after 100ms (to allow initial render)
      new Timer() {
         @Override
         public void run()
         {
            // do the sync
            syncToEditorTheme(editingTarget);
            
            // register for notification on subsquent changes
            eventBus.addHandler(
               EditorThemeChangedEvent.TYPE,
               new EditorThemeChangedEvent.Handler()
               {
                  @Override
                  public void onEditorThemeChanged(EditorThemeChangedEvent e)
                  {
                     syncToEditorTheme(editingTarget);
                  }
               });
         }
      }.schedule(100);;
   }
   
   public HandlerRegistration addEditorThemeStyleChangedHandler(
                           EditorThemeStyleChangedEvent.Handler handler)
   {
      // if we already have a style then call the handler back right away
      if (currentStyle_ != null && currentContent_ != null)
      {
         EditorThemeStyleChangedEvent event = 
                        new EditorThemeStyleChangedEvent(currentContent_,
                              currentStyle_);
         handler.onEditorThemeStyleChanged(event);
      }
      
      // register for future notification
      return handlers_.addHandler(EditorThemeStyleChangedEvent.TYPE, handler);
   }
  
 
   private void syncToEditorTheme(TextEditingTarget editingTarget)
   {
      Element editorContainer = editingTarget.asWidget().getElement();
      Element[] aceContentElements =
            DomUtils.getElementsByClassName(editorContainer, "ace_scroller");
      
      int n = aceContentElements.length;
      assert n == 1
            : "Expected a single editor instance; found " + n;
      
      Element content = aceContentElements[0];
      currentStyle_ = DomUtils.getComputedStyles(content);
      currentContent_ = content;
      
      // call all registered handlers
      handlers_.fireEvent(new EditorThemeStyleChangedEvent(content, 
            currentStyle_));
   }
   
   private HandlerManager handlers_ = new HandlerManager(this);
   
   private Style currentStyle_;
   private Element currentContent_;
}
