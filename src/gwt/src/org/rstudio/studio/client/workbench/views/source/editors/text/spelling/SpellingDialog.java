package org.rstudio.studio.client.workbench.views.source.editors.text.spelling;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;

import java.util.ArrayList;

public class SpellingDialog extends ModalDialogBase implements CheckSpelling.Display
{
   interface Binder extends UiBinder<Widget, SpellingDialog>
   {}

   public SpellingDialog()
   {
      setText("Check Spelling");

      btnAdd_ = new ThemedButton("Add");
      btnAdd_.setTitle("Add word to user dictionary");
      btnIgnoreAll_ = new ThemedButton("Ignore All");
      btnSkip_ = new ThemedButton("Skip");
      btnChange_ = new ThemedButton("Change");
      btnChangeAll_ = new ThemedButton("Change All");

      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);

      addCancelButton();
   }



   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   @Override
   public void setMisspelling(String text,
                              int highlightOffset,
                              int highlightLength)
   {
      // TODO: Implement highlight
      divDisplay_.getElement().setInnerText(text);
   }

   @Override
   public HasClickHandlers getAddButton()
   {
      return btnAdd_;
   }

   @Override
   public HasClickHandlers getIgnoreAllButton()
   {
      return btnIgnoreAll_;
   }

   @Override
   public HasClickHandlers getSkipButton()
   {
      return btnSkip_;
   }

   @Override
   public HasClickHandlers getChangeButton()
   {
      return btnChange_;
   }

   @Override
   public HasClickHandlers getChangeAllButton()
   {
      return btnChangeAll_;
   }

   @Override
   public HasText getReplacement()
   {
      return txtReplacement_;
   }

   @Override
   public void setSuggestions(String[] values)
   {
      lstSuggestions_.clear();
      for (String value : values)
         lstSuggestions_.addItem(value);

      if (values.length > 0)
         lstSuggestions_.setSelectedIndex(0);
   }

   @Override
   public void clearSuggestions()
   {
      lstSuggestions_.clear();
   }

   @Override
   public HasChangeHandlers getSuggestionList()
   {
      return lstSuggestions_;
   }

   @Override
   public String getSelectedSuggestion()
   {
      int index = lstSuggestions_.getSelectedIndex();
      if (index < 0)
         return null;
      return lstSuggestions_.getItemText(index);
   }

   @Override
   public void closeDialog()
   {
      super.closeDialog();
   }

   @UiField(provided = true)
   ThemedButton btnAdd_;
   @UiField(provided = true)
   ThemedButton btnIgnoreAll_;
   @UiField(provided = true)
   ThemedButton btnSkip_;
   @UiField(provided = true)
   ThemedButton btnChange_;
   @UiField(provided = true)
   ThemedButton btnChangeAll_;
   @UiField
   TextBox txtReplacement_;
   @UiField
   ListBox lstSuggestions_;
   @UiField
   SimplePanel divDisplay_;
   private final Widget mainWidget_;
}
