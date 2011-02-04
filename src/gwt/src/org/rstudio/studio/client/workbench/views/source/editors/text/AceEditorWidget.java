package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusHandler;
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
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedHandler;

public class AceEditorWidget extends Composite
      implements RequiresResize,
                 HasValueChangeHandlers<Void>,
                 HasNativeKeyHandlers
{
   public static void create(final CommandWithArg<AceEditorWidget> callback)
   {
      createEnvironment(new CommandWithArg<JavaScriptObject>()
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
            editor_ = createEditor(env_, getElement());
            editor_.setShowPrintMargin(false);
            editor_.setPrintMarginColumn(0);
            editor_.setHighlightActiveLine(false);
            editor_.onChange(new Command()
            {
               public void execute()
               {
                  ValueChangeEvent.fire(AceEditorWidget.this, null);
               }
            });
            editor_.onKeyDown(AceEditorWidget.this);
            editor_.onKeyPress(AceEditorWidget.this);
            if (initialCode_ != null)
            {
               editor_.getSession().setValue(initialCode_);
               initialCode_ = null;
            }

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

   private static native void createEnvironment(
         CommandWithArg<JavaScriptObject> callback) /*-{
      var require = $wnd.require;

      var config = {
          paths: {
              demo: "../demo",
              ace: "../lib/ace",
              pilot: "../support/pilot/lib/pilot",
              mode: "../../js/acemode"
          }
      };

      var deps = [ "pilot/fixoldbrowsers",
                   "pilot/plugin_manager",
                   "pilot/settings",
                   "pilot/environment",
                   "demo/demo",
                   "mode/r",
                   "mode/tex",
                   "mode/sweave" ];

      require(config);
      require(deps, function() {
          var catalog = require("pilot/plugin_manager").catalog;
          catalog.registerPlugins([ "pilot/index" ]).then(function() {
              var env = require("pilot/environment").create();
              catalog.startupPlugins({ env: env }).then($entry(function() {
                  callback.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(env);
              }));
          });
      });
   }-*/;

   private static native AceEditorNative createEditor(
         JavaScriptObject env,
         Element container) /*-{
      var require = $wnd.require;
      var event = require("pilot/event");
      var Editor = require("ace/editor").Editor;
      var Renderer = require("ace/virtual_renderer").VirtualRenderer;
      var theme = require("ace/theme/textmate");
      var EditSession = require("ace/edit_session").EditSession;
      var UndoManager = require("ace/undomanager").UndoManager;

      var TextMode = require("ace/mode/text").Mode;
      var RMode = require("mode/r").Mode;
      var TexMode = require("mode/tex").Mode;
      var SweaveMode = require("mode/sweave").Mode;
   
      var vim = require("ace/keyboard/keybinding/vim").Vim;
      var emacs = require("ace/keyboard/keybinding/emacs").Emacs;
      var HashHandler = require("ace/keyboard/hash_handler").HashHandler;

      env.editor = new Editor(new Renderer(container, theme));
      env.editor.getSession().setMode(new RMode());

      return env.editor;
   }-*/;

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

   public HandlerRegistration addNativeKeyDownHandler(NativeKeyDownHandler handler)
   {
      return addHandler(handler, NativeKeyDownEvent.TYPE);
   }

   public HandlerRegistration addNativeKeyPressHandler(NativeKeyPressHandler handler)
   {
      return addHandler(handler, NativeKeyPressEvent.TYPE);
   }

   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return null;  //To change body of created methods use File | Settings | File Templates.
   }

   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return null;  //To change body of created methods use File | Settings | File Templates.
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return null;  //To change body of created methods use File | Settings | File Templates.
   }

   public HandlerRegistration addEditorLoadedHandler(EditorLoadedHandler handler)
   {
      return addHandler(handler, EditorLoadedEvent.TYPE);
   }

   private AceEditorNative editor_;
   private JavaScriptObject env_;
   private String initialCode_;
}