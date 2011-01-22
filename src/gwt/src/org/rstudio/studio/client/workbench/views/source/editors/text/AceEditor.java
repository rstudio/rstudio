package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.codemirror.client.events.EditorFocusHandler;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.events.NativeKeyDownHandler;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget.DocDisplay;

public class AceEditor implements DocDisplay
{
   private AceEditor(AceEditorWidget widget)
   {
      widget_ = widget;
   }

   public static void create(final CommandWithArg<AceEditor> callback)
   {
      AceEditorWidget.create(new CommandWithArg<AceEditorWidget>()
      {
         public void execute(AceEditorWidget arg)
         {
            callback.execute(new AceEditor(arg));
         }
      });
   }

   public void setFileType(TextFileType fileType)
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public String getCode()
   {
      return widget_.getEditor().getSession().getValue();
   }

   public void setCode(String code)
   {
      widget_.setCode(code);
   }

   public void insertCode(String code, boolean blockMode)
   {
      // TODO: implement block mode
      widget_.getEditor().getSession().replace(
            widget_.getEditor().getSession().getSelection().getRange(), code);
   }

   public void focus()
   {
      widget_.getEditor().focus();
   }

   public void print()
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public String getSelection()
   {
      return widget_.getEditor().getSession().getTextRange(
            widget_.getEditor().getSession().getSelection().getRange());
   }

   public String getCurrentLine()
   {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public void replaceSelection(String code)
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public boolean moveSelectionToNextLine()
   {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public ChangeTracker getChangeTracker()
   {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public void setTextWrapping(boolean wrap)
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public HandlerRegistration addEditorFocusHandler(EditorFocusHandler handler)
   {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public HandlerRegistration addNativeKeyDownHandler(NativeKeyDownHandler handler)
   {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public void markScrollPosition()
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void restoreScrollPosition()
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void fitSelectionToLines(boolean expand)
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public int getSelectionOffset(boolean start)
   {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public void updateBodyMinHeight()
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Void> voidValueChangeHandler)
   {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public void fireEvent(GwtEvent<?> event)
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public Widget toWidget()
   {
      return widget_;
   }

   private final AceEditorWidget widget_;
}
