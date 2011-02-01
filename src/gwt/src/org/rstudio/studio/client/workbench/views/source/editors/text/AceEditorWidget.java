package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.studio.client.server.Void;

public class AceEditorWidget extends Composite
      implements RequiresResize, HasValueChangeHandlers<Void>
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
            if (initialCode_ != null)
            {
               editor_.getSession().setValue(initialCode_);
               initialCode_ = null;
            }
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
              pilot: "../support/pilot/lib/pilot"
          }
      };

      var deps = [ "pilot/fixoldbrowsers", "pilot/plugin_manager", "pilot/settings",
                   "pilot/environment", "demo/demo" ];

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
         Element el) /*-{
      var require = $wnd.require;
      var event = require("pilot/event");
      var Editor = require("ace/editor").Editor;
      var Renderer = require("ace/virtual_renderer").VirtualRenderer;
      var theme = require("ace/theme/textmate");
      var EditSession = require("ace/edit_session").EditSession;
      var JavaScriptMode = require("ace/mode/javascript").Mode;
      var CssMode = require("ace/mode/css").Mode;
      var HtmlMode = require("ace/mode/html").Mode;
      var XmlMode = require("ace/mode/xml").Mode;
      var PythonMode = require("ace/mode/python").Mode;
      var PhpMode = require("ace/mode/php").Mode;
      var TextMode = require("ace/mode/text").Mode;
      var UndoManager = require("ace/undomanager").UndoManager;

      var vim = require("ace/keyboard/keybinding/vim").Vim;
      var emacs = require("ace/keyboard/keybinding/emacs").Emacs;
      var HashHandler = require("ace/keyboard/hash_handler").HashHandler;

      var docs = {};

      var document = $wnd.document;

      docs.js = new EditSession('');
      docs.js.setMode(new JavaScriptMode());
      docs.js.setUndoManager(new UndoManager());

      docs.css = new EditSession('');
      docs.css.setMode(new CssMode());
      docs.css.setUndoManager(new UndoManager());

      docs.html = new EditSession('');
      docs.html.setMode(new HtmlMode());
      docs.html.setUndoManager(new UndoManager());

      docs.python = new EditSession('');
      docs.python.setMode(new PythonMode());
      docs.python.setUndoManager(new UndoManager());

      docs.php = new EditSession('');
      docs.php.setMode(new PhpMode());
      docs.php.setUndoManager(new UndoManager());


      var container = el;
      env.editor = new Editor(new Renderer(container, theme));

      var modes = {
          text: new TextMode(),
          xml: new XmlMode(),
          html: new HtmlMode(),
          css: new CssMode(),
          javascript: new JavaScriptMode(),
          python: new PythonMode(),
          php: new PhpMode()
      };

      function getMode() {
          return modes[modeEl.value];
      }

      // for debugging
      window.jump = function() {
          var jump = document.getElementById("jump");
          var cursor = env.editor.getCursorPosition();
          var pos = env.editor.renderer.textToScreenCoordinates(cursor.row, cursor.column);
          jump.style.left = pos.pageX + "px";
          jump.style.top = pos.pageY + "px";
          jump.style.display = "block";
      };

      function onResize() {
          env.editor.resize();
      };

      window.onresize = onResize;

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

   private AceEditorNative editor_;
   private JavaScriptObject env_;
   private String initialCode_;
}