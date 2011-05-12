/*
 * AceEditorWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar;

public class AceEditorWidget extends Composite
      implements RequiresResize,
                 HasValueChangeHandlers<Void>,
                 HasKeyDownHandlers
{
   public AceEditorWidget()
   {
      initWidget(new HTML());
      FontSizer.applyNormalFontSize(this);
      setSize("100%", "100%");

      addStyleName("loading");

      editor_ = AceEditorNative.createEditor(getElement());
      editor_.getRenderer().setHScrollBarAlwaysVisible(false);
      editor_.setShowPrintMargin(false);
      editor_.setPrintMarginColumn(0);
      editor_.setHighlightActiveLine(false);
      editor_.delegateEventsTo(AceEditorWidget.this);
      editor_.onChange(new Command()
      {
         public void execute()
         {
            ValueChangeEvent.fire(AceEditorWidget.this, null);
         }
      });
      editor_.getSession().getSelection().addCursorChangeHandler(new CommandWithArg<Position>()
      {
         public void execute(Position arg)
         {
            AceEditorWidget.this.fireEvent(new CursorChangedEvent(arg));
         }
      });
   }

   public HandlerRegistration addCursorChangedHandler(
         CursorChangedHandler handler)
   {
      return addHandler(handler, CursorChangedEvent.TYPE);
   }

   public AceEditorNative getEditor() {
      return editor_;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();

      editor_.getRenderer().updateFontSize();
      onResize();
      fireEvent(new EditorLoadedEvent());

      int delayMs = initToEmptyString_ ? 100 : 500;

      // On Windows desktop sometimes we inexplicably end up at the wrong size
      // if the editor is being resized while it's loading (such as when a new
      // document is created while the source pane is hidden)
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         public boolean execute()
         {
            if (isAttached())
               onResize();
            removeStyleName("loading");
            return false;
         }
      }, delayMs);
   }

   public void onResize()
   {
      editor_.resize();
   }

   public void onActivate()
   {
      if (editor_ != null)
      {
         if (BrowseCap.INSTANCE.aceVerticalScrollBarIssue())
            editor_.getRenderer().forceScrollbarUpdate();
         editor_.getRenderer().updateFontSize();
      }
   }

   public void setCode(String code)
   {
      code = StringUtil.notNull(code);
      initToEmptyString_ = code.length() == 0;
      editor_.getSession().setValue(code);
   }

   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Void> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }

   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return addHandler(handler, FocusEvent.getType());
   }

   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return addHandler(handler, BlurEvent.getType());
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }

   public HandlerRegistration addEditorLoadedHandler(EditorLoadedHandler handler)
   {
      return addHandler(handler, EditorLoadedEvent.TYPE);
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return addHandler(handler, KeyDownEvent.getType());
   }

   public void forceResize()
   {
      editor_.getRenderer().onResize(true);
   }

   public void autoHeight()
   {
      editor_.autoHeight();
   }

   private final AceEditorNative editor_;
   private boolean initToEmptyString_ = true;
}