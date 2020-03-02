package org.rstudio.studio.client.panmirror.dialogs;



import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

public class PanmirrorPopupLink extends SimplePanel
{
   public PanmirrorPopupLink(Element parent, String url)
   {
      super(parent);
      
      getElement().getStyle().setBackgroundColor("lightgrey");
      
      HorizontalPanel panel = new HorizontalPanel();
      
      panel.add(new Label(url));
      
      setWidget(panel);
      
   }
  
   protected static final String kOpenResult = "open";
   protected static final String kRemoveResult = "remove";
   protected static final String kEditResult = "edit";
   
}

