package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.core.client.CommandWithArg;
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
            if (isSelectable())
            {
               int index = menuBar_.getItemIndex(event.getSelectedItem());
               if (index != -1)
               {
                  CppCompletion completion = completions_.get(index);
                 
               }
            }
         }
      });
      
      addStyleName(ThemeStyles.INSTANCE.statusBarMenu());
   }
   
   public void setText(String text)
   {
      JsArray<CppCompletion> completions = JsArray.createArray().cast();
      completions.push(CppCompletion.create(text));
      setCompletions(completions, null);
   }
     
   public void setCompletions(JsArray<CppCompletion> completions, 
                              CommandWithArg<CppCompletion> onSelected)
   {
      // save completions and selectable state
      completions_ = completions;
      onSelected_ = onSelected;
      
      // clear existing items
      menuBar_.clearItems();
      
      // add items (remember first item for programmatic selection)
      MenuItem firstItem = null;
      for (int i = 0; i<completions.length(); i++)
      {
         final CppCompletion completion = completions.get(i);
         MenuItem menuItem = new MenuItem(completion.getTypedText(), 
               new ScheduledCommand() {
            @Override
            public void execute()
            {
               docDisplay_.setFocus(true); 
               if (isSelectable())
                  onSelected_.execute(completion);
            }
         });
         
         addItem(menuItem);
         
         if (i == 0)
            firstItem = menuItem;
      }
      
      // select first item
      if (isSelectable() && (firstItem != null))
         selectItem(firstItem);
      
      if (completions.length() > 0)
         showMenu();
      else
         setVisible(false);
   }
   
   public void acceptSelected()
   {
      int index = getSelectedIndex();
      if (index != -1)
      {
         if (isSelectable())
            onSelected_.execute(completions_.get(index));
      }
      hide();
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
            
            // figure out whether we should show below (do this 
            // only once so that we maintain the menu orientation
            // while filtering)
            if (showBelow_ == null)
               showBelow_ = windowBottom - cursorBottom >= offsetHeight;
            
            final int PAD = 3;
            if (showBelow_)
               setPopupPosition(bounds.getLeft(), cursorBottom + PAD) ;
            else
               setPopupPosition(bounds.getLeft(), 
                                bounds.getTop() - offsetHeight) ;
         }
      });
   }
   
   private boolean isSelectable()
   {
      return onSelected_ != null;
   }
   
   @Override
   protected int getMaxHeight()
   {
      return 180;
   }
   
   private final DocDisplay docDisplay_;
   private final Position completionPosition_;
   private JsArray<CppCompletion> completions_;
   private CommandWithArg<CppCompletion> onSelected_ = null;
   private Boolean showBelow_ = null;
}
