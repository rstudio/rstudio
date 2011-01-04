package org.rstudio.studio.client.workbench.views.console.shell.editor.impl;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.console.shell.editor.PasteStrategy;
import org.rstudio.studio.client.workbench.views.console.shell.editor.PlainTextEditor;

public class TextareaPasteStrategy implements PasteStrategy
{
   public void initialize(PlainTextEditor editor, Element textContainer)
   {
      editor_ = editor;
      textContainer_ = textContainer;

      textArea_ = Document.get().createTextAreaElement();
      Document.get().getBody().appendChild(textArea_);
      textArea_.setTabIndex(-1);
      textArea_.getStyle().setZIndex(-100);
      textArea_.getStyle().setPosition(Position.ABSOLUTE);
      textArea_.getStyle().setBottom(140, Unit.PX);
      textArea_.getStyle().setRight(140, Unit.PX);
      textArea_.getStyle().setWidth(100, Unit.PX);
      textArea_.getStyle().setHeight(100, Unit.PX);
      textArea_.getStyle().setOpacity(0.1);
      textArea_.setReadOnly(false);

      hookPaste(textContainer_);
   }

   private native void hookPaste(Element el) /*-{
      var thiz = this;
      el.addEventListener("paste",
            function (evt) {
               thiz.@org.rstudio.studio.client.workbench.views.console.shell.editor.impl.TextareaPasteStrategy::onPaste(Lcom/google/gwt/dom/client/NativeEvent;)(evt);
            },
            false);
   }-*/;

   @SuppressWarnings("unused")
   private void onPaste(NativeEvent event)
   {
      final InputEditorSelection selection = editor_.getSelection();
      textArea_.setValue("");
      textArea_.focus();

      new Timer() {
         @Override
         public void run()
         {
            final String value = textArea_.getValue();
            textArea_.setValue("");
            editor_.setFocus(true);
            editor_.beginSetSelection(selection, new Command()
            {
               public void execute()
               {
                  editor_.replaceSelection(value, true);
                  if (editor_.getSelection() == null)
                     Debug.log("Warning: Null selection after TextareaPasteStrategy cleanup");
               }
            });
         }
      }.schedule(100);
   }


   private PlainTextEditor editor_;
   private Element textContainer_;
   private TextAreaElement textArea_;
}
