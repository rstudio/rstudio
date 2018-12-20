package org.rstudio.studio.client.common.spelling;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import org.rstudio.core.client.resources.StaticDataResource;

public interface TypoResources extends ClientBundle
{
   TypoResources INSTANCE = GWT.create(TypoResources.class);

   @Source("typo.js")
   StaticDataResource typojs();
}
