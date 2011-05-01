package org.rstudio.core.client.widget;


import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.TextBoxBase;

// TextBox designed for use with SuggestBox that supports the entry
// of multiple items
public class MultipleItemSuggestTextBox extends TextBoxBase
{
   public MultipleItemSuggestTextBox()
   {
      super(Document.get().createTextInputElement());
      setStyleName("gwt-TextBox");
   }
   
   public List<String> getItems()
   {
      ArrayList<String> items = new ArrayList<String>();
      String text = super.getText();
      if (!StringUtil.isNullOrEmpty(text))
      {
         // split text
         String[] words = text.split("[ ,]");
         
         // return non-empty
         for (int i=0; i<words.length; i++)
         {
            String word = words[i].trim();
            if (word.length() > 0)
               items.add(word);
         }   
      }
      return items;
   }
   
   
   @Override
   public String getText() 
   {   
      // get text
      String text = super.getText();
      if (text == null)
         return "";
      
     // if it ends with one of the separators then return empty
      if (text.endsWith(",") || text.endsWith(" "))
         return "";
      
     // split text
     String[] words = text.split("[ ,]");
         
     // if no words then empty
     if (words.length == 0)
        return "";
     
     // return last word
     return words[words.length-1];
   }

   @Override
   public void setText(String text) 
   {
      if (StringUtil.isNullOrEmpty(text))
      {
         setText("");
      }
      else
      {
         // find last separator
         String fullText = super.getText();
         int lastCommaIndex = fullText.lastIndexOf(',');
         int lastSpaceIndex = fullText.lastIndexOf(' ');
         int lastSepIndex = Math.max(lastCommaIndex, lastSpaceIndex);
         
         // create new text
         String previous = "";
         if (lastSepIndex != -1)
         {
            previous = fullText.substring(0, lastSepIndex);
            if (!previous.endsWith(" "))
               previous = previous + " ";
         }
         super.setText(previous + text);
      }
   }
}
