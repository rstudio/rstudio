/*
 * TextEditingTargetThemeHelper.java
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

import java.util.ArrayList;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeStyleChangedEvent;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

public class TextEditingTargetThemeHelper
{  
   public TextEditingTargetThemeHelper(final TextEditingTarget editingTarget,
                                       final EventBus eventBus,
                                       final ArrayList<HandlerRegistration> releaseOnDismiss)
   {
      // do an initial sync after 100ms (to allow initial render)
      Timers.singleShot(100, () -> {

         // do the sync
         syncToEditorTheme(editingTarget);

         // register for notification on subsequent changes
         releaseOnDismiss.add(
               eventBus.addHandler(
                     EditorThemeChangedEvent.TYPE,
                     (EditorThemeChangedEvent e) -> {
                        syncToEditorTheme(editingTarget);
                     }));
      });
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
      // ensure we're passed a real widget
      Widget editingWidget = editingTarget.asWidget();
      if (editingWidget == null)
         return;
      
      Element editorContainer = editingWidget.getElement();
      Element[] aceContentElements =
            DomUtils.getElementsByClassName(editorContainer, "ace_scroller");
      
      int n = aceContentElements.length;
      if (editingTarget.isVisualModeActivated())
      {
         // In visual mode, we may have no editors, or we may have multiple
         // editors. We need to read computed styles from an editor, so skip if
         // none are present.
         if (n < 1)
            return;
      }
      else
      {
         // Otherwise, we expect a single editor instance.
         assert n == 1
               : "Expected a single editor instance; found " + n;
      }
      
      Element content = aceContentElements[0];
      currentStyle_ = DomUtils.getComputedStyles(content);
      currentContent_ = content;
      
      // call all registered handlers
      handlers_.fireEvent(new EditorThemeStyleChangedEvent(content, currentStyle_));
   }
   
   private HandlerManager handlers_ = new HandlerManager(this);
   
   private Style currentStyle_;
   private Element currentContent_;
}
