package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplace;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

public class TextEditingTargetFindReplace
{
   public interface Container
   {
      AceEditor getEditor();
      void insertFindReplace(FindReplaceBar findReplaceBar);
      void removeFindReplace(FindReplaceBar findReplaceBar);
   }
   
   public TextEditingTargetFindReplace(Container container)
   {
      this(container, true);
   }
   
   public TextEditingTargetFindReplace(Container container, boolean showReplace)                                  
   {
      container_ = container;
      showReplace_ = showReplace;
   }
   
   public Widget createFindReplaceButton()
   {
      if (findReplaceBar_ == null)
      {
         findReplaceButton_ = new ToolbarButton(
               FindReplaceBar.getFindIcon(),
               new ClickHandler() {
                  public void onClick(ClickEvent event)
                  {
                     if (findReplaceBar_ == null)
                        showFindReplace();
                     else
                        hideFindReplace();
                  }
               });
         String title = showReplace_ ? "Find/Replace" : "Find";
         findReplaceButton_.setTitle(title);
      }
      return findReplaceButton_;
   }
   
   public void showFindReplace()
   {
      if (findReplaceBar_ == null)
      {
         findReplaceBar_ = new FindReplaceBar(showReplace_);
         new FindReplace(container_.getEditor(),
                         findReplaceBar_,
                         RStudioGinjector.INSTANCE.getGlobalDisplay(),
                         showReplace_);
         container_.insertFindReplace(findReplaceBar_);
         findReplaceBar_.getCloseButton().addClickHandler(new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               hideFindReplace();
            }
         });

         findReplaceButton_.setLeftImage(FindReplaceBar.getFindLatchedIcon());
      }
      findReplaceBar_.focusFindField(true);
   }

   private void hideFindReplace()
   {
      if (findReplaceBar_ != null)
      {
         container_.removeFindReplace(findReplaceBar_);
         findReplaceBar_ = null;
         findReplaceButton_.setLeftImage(FindReplaceBar.getFindIcon());
      }
      container_.getEditor().focus();
   }
   
   private final Container container_;
   private final boolean showReplace_;
   private FindReplaceBar findReplaceBar_;
   private ToolbarButton findReplaceButton_;
}
