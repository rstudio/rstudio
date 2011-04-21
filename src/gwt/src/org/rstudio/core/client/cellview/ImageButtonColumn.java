package org.rstudio.core.client.cellview;

import org.rstudio.core.client.widget.OperationWithInput;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class ImageButtonColumn<T> extends Column<T, String>
{
   public ImageButtonColumn(final AbstractImagePrototype imagePrototype,
                            final OperationWithInput<T> onClick)
   {
      super(new ButtonCell(){
         @Override
         public void render(Context context, 
                            SafeHtml value, 
                            SafeHtmlBuilder sb) 
         {   
            if (value != null)
               sb.appendHtmlConstant(imagePrototype.getHTML());
         }                                
      });

      setFieldUpdater(new FieldUpdater<T,String>() {
         public void update(int index, T object, String value)
         {
            if (value != null)
               onClick.execute(object);
         }
      });
   }


   @Override
   public String getValue(T object)
   {
      if (showButton(object))
         return new String();
      else
         return null;
   }
   
   protected boolean showButton(T object)
   {
      return true;
   }
}