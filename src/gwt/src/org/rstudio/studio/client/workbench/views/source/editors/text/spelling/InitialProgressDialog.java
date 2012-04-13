package org.rstudio.studio.client.workbench.views.source.editors.text.spelling;

import com.google.gwt.event.dom.client.HasClickHandlers;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.CheckSpelling.ProgressDisplay;

public class InitialProgressDialog implements ProgressDisplay
{
   public InitialProgressDialog()
   {
      dialog_ = new MessageDialog(MessageDialog.INFO,
                                  "Check Spelling",
                                  "Spell check in progress...");
      cancel_ = dialog_.addButton("Cancel", (Operation) null, true, true);
   }

   @Override
   public void show()
   {
      dialog_.showModal();
   }

   @Override
   public void hide()
   {
      dialog_.closeDialog();
   }

   @Override
   public boolean isShowing()
   {
      return dialog_.isShowing();
   }

   @Override
   public HasClickHandlers getCancelButton()
   {
      return cancel_;
   }

   private final MessageDialog dialog_;
   private ThemedButton cancel_;
}
