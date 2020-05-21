/*
 * StaticDataResourceGenerator.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.resources.ext.AbstractResourceGenerator;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;
import org.rstudio.core.client.resources.StaticDataResource;

import java.net.URL;

/**
 * Forked from GWT's DataResourceGenerator. Only difference is that the call
 * to context.deploy passes true for xhrCompatible.
 */
public final class StaticDataResourceGenerator extends AbstractResourceGenerator
{
  @Override
  public String createAssignment(TreeLogger logger, ResourceContext context,
      JMethod method) throws UnableToCompleteException
  {

    URL[] resources = ResourceGeneratorUtil.findResources(logger, context,
        method);

    if (resources.length != 1) {
      logger.log(TreeLogger.ERROR, "Exactly one resource must be specified",
          null);
      throw new UnableToCompleteException();
    }

    URL resource = resources[0];
    String outputUrlExpression = context.deploy(resource, null, true);

    SourceWriter sw = new StringSourceWriter();
    // Write the expression to create the subtype.
    sw.println("new " + StaticDataResource.class.getName() + "() {");
    sw.indent();

    // Convenience when examining the generated code.
    sw.println("// " + resource.toExternalForm());

    sw.println("public String getUrl() {");
    sw.indent();
    sw.println("return " + outputUrlExpression + ";");
    sw.outdent();
    sw.println("}");

    sw.println("public com.google.gwt.safehtml.shared.SafeUri getSafeUri() {");
    sw.indent();
    sw.println("return new org.rstudio.core.client.SafeUriStringImpl(" + outputUrlExpression + ");");
    sw.outdent();
    sw.println("}");

    sw.println("public String getName() {");
    sw.indent();
    sw.println("return \"" + method.getName() + "\";");
    sw.outdent();
    sw.println("}");

    sw.outdent();
    sw.println("}");

    return sw.toString();
  }
}
