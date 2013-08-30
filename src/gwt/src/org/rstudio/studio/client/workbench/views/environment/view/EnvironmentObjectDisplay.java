package org.rstudio.studio.client.workbench.views.environment.view;

import org.rstudio.core.client.cellview.ScrollingDataGrid;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;

public class EnvironmentObjectDisplay extends ScrollingDataGrid<RObjectEntry>
{
   public interface Host
   {
      public boolean enableClickableObjects();
      public boolean useStatePersistence();
   }

   public EnvironmentObjectDisplay(Host host, 
                                   EnvironmentObjectsObserver observer)
   {
      super(1024, RObjectEntry.KEY_PROVIDER);

      observer_ = observer;
      host_ = host;
      filterRenderer_ = new AbstractSafeHtmlRenderer<String>()
      {
         @Override
         public SafeHtml render(String str)
         {
            SafeHtmlBuilder sb = new SafeHtmlBuilder();
            boolean hasMatch = false;
            if (filterText_.length() > 0)
            {
               int idx = str.toLowerCase().indexOf(filterText_);
               if (idx >= 0)
               {
                  hasMatch = true;
                  sb.appendEscaped(str.substring(0, idx));
                  sb.appendHtmlConstant(
                        "<span class=\"" + 
                        EnvironmentStyle.INSTANCE.filterMatch() + 
                        "\">");
                  sb.appendEscaped(str.substring(idx, 
                        idx + filterText_.length()));
                  sb.appendHtmlConstant("</span>");
                  sb.appendEscaped(str.substring(idx + filterText_.length(), 
                        str.length()));
               }
            }
            if (!hasMatch)
               sb.appendEscaped(str);
            return sb.toSafeHtml();
         }
      };
   }
   
   protected AbstractSafeHtmlRenderer<String> filterRenderer_;
   protected EnvironmentObjectsObserver observer_;
   protected Host host_;

   private String filterText_ = "";
}
