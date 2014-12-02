package org.rstudio.studio.client.dataviewer;

import java.util.ArrayList;

import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.SuggestOracle;

public class DataTable
{
   public interface Host 
   {
      RStudioFrame getDataTableFrame();
   }

   public DataTable(Host host)
   {
      host_ = host;
   }
   
   public void initToolbar(Toolbar toolbar)
   {
      findButton_ = new ToolbarButton(
              FindReplaceBar.getFindIcon(),
              new ClickHandler() {
                 public void onClick(ClickEvent event)
                 {
                    filtered_ = !filtered_;
                    setFilterUIVisible(filtered_);
                    findButton_.setLeftImage(filtered_ ? 
                          FindReplaceBar.getFindLatchedIcon() :
                          FindReplaceBar.getFindIcon());
                 }
              });
      toolbar.addLeftWidget(findButton_);

      SearchWidget searchWidget = new SearchWidget(new SuggestOracle() {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            // no suggestions
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<Suggestion>()));
         }
      });
      searchWidget.addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            applySearch(getWindow(), event.getValue());
         }
      });

      toolbar.addRightWidget(searchWidget);
   }
   
   private WindowEx getWindow()
   {
      IFrameElementEx frameEl = (IFrameElementEx) host_.getDataTableFrame().getElement().cast();
      return frameEl.getContentWindow();
   }

   public void setFilterUIVisible(boolean visible)
   {
      setFilterUIVisible(getWindow(), visible);
   }
   
   public void refreshData(boolean structureChanged)
   {
      refreshData(getWindow(), structureChanged);
   }
   
   public void applySizeChange()
   {
      applySizeChange(getWindow());
   }
   
   private static final native void setFilterUIVisible (WindowEx frame, boolean visible) /*-{
      if (frame && frame.setFilterUIVisible)
         frame.setFilterUIVisible(visible);
   }-*/;
   
   private static final native void refreshData(WindowEx frame, boolean structureChanged) /*-{
      if (frame && frame.refreshData)
         frame.refreshData(structureChanged);
   }-*/;

   private static final native void applySearch(WindowEx frame, String text) /*-{
      if (frame && frame.applySearch)
         frame.applySearch(text);
   }-*/;
   
   private static final native void applySizeChange(WindowEx frame) /*-{
      if (frame && frame.applySizeChange)
         frame.applySizeChange();
   }-*/;

   private Host host_;
   private ToolbarButton findButton_;
   private boolean filtered_ = false;
}