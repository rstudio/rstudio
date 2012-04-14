package org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling;

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;

public interface TokenPredicate
{
   boolean test(Token token);
}
