package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import org.rstudio.core.client.Debug;

public class AceEditorWidget extends Composite implements RequiresResize
{
   public AceEditorWidget()
   {
      super();

      initWidget(new HTML());
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();

      try
      {
      initAce(getElement());
      editor_.resize();
      }
      catch (Exception e)
      {
         Debug.log(e.toString());
      }
   }

   public void onResize()
   {
      if (editor_ != null)
         editor_.resize();
   }

   private native void initAce(Element el) /*-{
      var require = $wnd.require;
      require(["ace/editor"], function() {
         var Editor = require("ace/editor").Editor;
         var Renderer = require("ace/virtual_renderer").VirtualRenderer;
         var theme = require("ace/theme/textmate");
         var EditSession = require("ace/edit_session").EditSession;
         var JavaScriptMode = require("ace/mode/javascript").Mode;
         var UndoManager = require("ace/undomanager").UndoManager;

         var docs = {};
         docs.js = new EditSession("The quick brown fox jumped over the lazy dogs");
         docs.js.setMode(new JavaScriptMode());
         docs.js.setUndoManager(new UndoManager());
         var editor = new Editor(new Renderer(el, theme));
         editor.setSession(docs.js);
         editor.getSession().setMode(docs.js.getMode());
         this.@org.rstudio.studio.client.workbench.views.source.editors.text.AceEditorWidget::editor_
               = editor;

      });

   }-*/;

   private AceEditorNative editor_;
}
