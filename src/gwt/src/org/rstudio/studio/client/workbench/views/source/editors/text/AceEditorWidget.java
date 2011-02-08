package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedHandler;

public class AceEditorWidget extends Composite
      implements RequiresResize,
                 HasValueChangeHandlers<Void>,
                 HasKeyDownHandlers
{
   public static void create(final CommandWithArg<AceEditorWidget> callback)
   {
      AceEditorNative.createEnvironment(new CommandWithArg<JavaScriptObject>()
      {
         public void execute(JavaScriptObject environment)
         {
            callback.execute(new AceEditorWidget(environment));
         }
      });
   }

   protected AceEditorWidget(JavaScriptObject environment)
   {
      super();
      env_ = environment;

      initWidget(new HTML());
      FontSizer.applyNormalFontSize(this);
      setSize("100%", "100%");
   }

   public AceEditorNative getEditor() {
      return editor_;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();

      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            editor_ = AceEditorNative.createEditor(env_, getElement());
            editor_.setShowPrintMargin(false);
            editor_.setPrintMarginColumn(0);
            editor_.setHighlightActiveLine(false);
            editor_.delegateEventsTo(AceEditorWidget.this);
            if (initialCode_ != null)
            {
               editor_.getSession().setValue(initialCode_);
               initialCode_ = null;
            }

            editor_.onChange(new Command()
            {
               public void execute()
               {
                  ValueChangeEvent.fire(AceEditorWidget.this, null);
               }
            });

            fireEvent(new EditorLoadedEvent());

            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  onResize();
               }
            });
         }
      });
   }

   public void onResize()
   {
      if (editor_ != null)
         editor_.resize();
   }

   public void setCode(String code)
   {
      if (editor_ != null)
         editor_.getSession().setValue(code);
      else
         initialCode_ = code;
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

   private AceEditorNative editor_;
   private JavaScriptObject env_;
   private String initialCode_;
}