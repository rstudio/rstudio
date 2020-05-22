/*
 * InlinePreviewer.java
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

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.TextEditingTargetNotebook;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;

public class InlinePreviewer
             implements ScopeTreeReadyEvent.Handler,
                        ValueChangeHandler<String>
{
   public InlinePreviewer(TextEditingTarget target, DocUpdateSentinel sentinel, 
         UserPrefs prefs)
   {
      display_ = target.getDocDisplay();
      sentinel_ = sentinel;
      prefs_ = prefs;
      regs_ = new HandlerRegistrations();
      images_ = new ImagePreviewer(display_, sentinel, prefs);
      mathjax_ = new MathJaxPreviewer(target);
   }
   
   public void preview()
   {
      String pref = prefs_.latexPreviewOnCursorIdle().getValue();
      regs_.add(sentinel_.addPropertyValueChangeHandler(
            TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED, this));
      regs_.add(sentinel_.addPropertyValueChangeHandler(
            TextEditingTargetNotebook.CONTENT_PREVIEW_INLINE, this));
      
      if (sentinel_.getBoolProperty(
                TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED,
                pref == UserPrefs.LATEX_PREVIEW_ON_CURSOR_IDLE_ALWAYS) &&
          sentinel_.getBoolProperty(
                TextEditingTargetNotebook.CONTENT_PREVIEW_INLINE,
                pref == UserPrefs.LATEX_PREVIEW_ON_CURSOR_IDLE_ALWAYS))
      { 
         scopeReg_ = display_.addScopeTreeReadyHandler(this);
      }
   }
   
   public void onDismiss()
   {
      regs_.removeHandler();
   }

   @Override
   public void onScopeTreeReady(ScopeTreeReadyEvent event)
   {
      // remove single-shot handler
      scopeReg_.removeHandler();
      
      images_.previewAllLinks();
      mathjax_.renderAllLatex();
   }
   
   @Override
   public void onValueChange(ValueChangeEvent<String> val)
   {
      if (sentinel_.getBoolProperty(
            TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED, true) &&
          sentinel_.getBoolProperty(
            TextEditingTargetNotebook.CONTENT_PREVIEW_INLINE, true))
      {
         images_.previewAllLinks();
         mathjax_.renderAllLatex();
      }
      else
      {
         images_.removeAllPreviews();
         mathjax_.removeAllLatex();
      }
   }

   private final DocDisplay display_;
   private final DocUpdateSentinel sentinel_;
   private final UserPrefs prefs_;
   private final ImagePreviewer images_;
   private final MathJaxPreviewer mathjax_;
   private HandlerRegistration scopeReg_;
   private HandlerRegistrations regs_;
}
