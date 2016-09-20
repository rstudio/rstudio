/*
 * MathJaxPreviewer.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.common.mathjax.MathJax;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.TextEditingTargetNotebook;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;

public class MathJaxPreviewer
             implements ScopeTreeReadyEvent.Handler,
                        ValueChangeHandler<String>
{
   public MathJaxPreviewer(TextEditingTarget target, DocUpdateSentinel sentinel, 
         UIPrefs prefs)
   {
      target_ = target;
      display_= target.getDocDisplay();
      sentinel_ = sentinel;
      String pref = prefs.showLatexPreviewOnCursorIdle().getValue();
      
      sentinel.addPropertyValueChangeHandler(
            TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED, this);
      sentinel.addPropertyValueChangeHandler(
            TextEditingTargetNotebook.CONTENT_PREVIEW_INLINE, this);
      
      if (sentinel.getBoolProperty(
                TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED,
                pref == UIPrefsAccessor.LATEX_PREVIEW_SHOW_ALWAYS) &&
          sentinel.getBoolProperty(
                TextEditingTargetNotebook.CONTENT_PREVIEW_INLINE,
                pref == UIPrefsAccessor.LATEX_PREVIEW_SHOW_ALWAYS))
      {
         reg_ = display_.addScopeTreeReadyHandler(this);
      }
   }
   
   @Override
   public void onScopeTreeReady(ScopeTreeReadyEvent event)
   {
      reg_.removeHandler();

      renderAllLatex();
   }

   @Override
   public void onValueChange(ValueChangeEvent<String> arg0)
   {
      if (sentinel_.getBoolProperty(
            TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED, true) &&
          sentinel_.getBoolProperty(
            TextEditingTargetNotebook.CONTENT_PREVIEW_INLINE, true))
      {
         renderAllLatex();
      }
      else
      {
         removeAllLatex();
      }
   }
  
   private void renderAllLatex()
   {
      target_.renderLatex();
   }
   
   private void removeAllLatex()
   {
      JsArray<LineWidget> widgets = display_.getLineWidgets();
      for (int i = 0; i < widgets.length(); i++)
      {
         LineWidget widget = widgets.get(i);
         if (widget.getType() == MathJax.LINE_WIDGET_TYPE)
            display_.removeLineWidget(widget);
      }
   }
   
   private final DocDisplay display_;
   private final TextEditingTarget target_;
   private final DocUpdateSentinel sentinel_;
   private HandlerRegistration reg_;
}
