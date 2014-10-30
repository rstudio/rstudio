package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletion;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuItem;

public class CppCompletionPopupMenu extends ScrollableToolbarPopupMenu
{
   CppCompletionPopupMenu(DocDisplay docDisplay, Position completionPosition)
   {  
      docDisplay_ = docDisplay;
      completionPosition_ = completionPosition;
        
      addSelectionHandler(new SelectionHandler<MenuItem>()
      {
         public void onSelection(SelectionEvent<MenuItem> event)
         {
            int index = menuBar_.getItemIndex(event.getSelectedItem());
            if (index != -1)
            {
               CppCompletion completion = completions_.get(index);
               Debug.logToConsole(completion.getText());
            }
         }
      });
      
      addStyleName(ThemeStyles.INSTANCE.statusBarMenu());
   }
   
   public void setCompletions(JsArray<CppCompletion> completions)
   {
      // save completions
      completions_ = completions;
      
      // clear existing items
      menuBar_.clearItems();
      
      // add items (remember first item for programmatic selection)
      MenuItem firstItem = null;
      for (int i = 0; i<completions.length(); i++)
      {
         final CppCompletion completion = completions.get(i);
         MenuItem menuItem = new MenuItem(completion.getText(), 
               new ScheduledCommand() {
            @Override
            public void execute()
            {
               docDisplay_.setFocus(true); 
               docDisplay_.setSelection(docDisplay_.createSelection(
                     completionPosition_, docDisplay_.getCursorPosition()));
               docDisplay_.replaceSelection(completion.getText(), true) ; 

            }
         });
         
         addItem(menuItem);
         
         if (i == 0)
            firstItem = menuItem;
      }
      
      // select first item
      if (firstItem != null)
         selectItem(firstItem);
      
      // ensure the menu is visible
      if (!isAttached())
         showMenu();
      else if (!isVisible())
         setVisible(true);
   }
   
   private void showMenu()
   {
      setPopupPositionAndShow(new PositionCallback()
      {
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            InputEditorPosition position = 
               docDisplay_.createInputEditorPosition(completionPosition_);  
            Rectangle bounds = docDisplay_.getPositionBounds(position);
            
            int windowBottom = Window.getScrollTop() + 
                               Window.getClientHeight() ;
            int cursorBottom = bounds.getBottom() ;
            
            final int PAD = 3;
            if (windowBottom - cursorBottom >= offsetHeight)
               setPopupPosition(bounds.getLeft(), cursorBottom + PAD) ;
            else
               setPopupPosition(bounds.getLeft(), 
                                bounds.getTop() - offsetHeight) ;
         }
      });
   }
   
   private final DocDisplay docDisplay_;
   private final Position completionPosition_;
   private JsArray<CppCompletion> completions_;
}
