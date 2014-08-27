/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.Generator.RunsLocal;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.io.PrintWriter;

/**
 * Generates multiple classes and a GWT.create() -> Foo -> Bar reference path.
 */
@RunsLocal
public class MultipleClassGenerator extends Generator {

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {

    // GWT.create() -> generated class Foo.
    PrintWriter fooWriter = context.tryCreate(logger, "com.foo", "Foo");
    if (fooWriter != null) {
      fooWriter.println("package com.foo;");
      fooWriter.println("public class Foo {");
      fooWriter.println("  private Bar bar = new Bar();");
      fooWriter.println("}");
      context.commit(logger, fooWriter);
    }

    // Generated class Foo -> generated class Bar.
    PrintWriter barWriter = context.tryCreate(logger, "com.foo", "Bar");
    if (barWriter != null) {
      barWriter.println("package com.foo;");
      barWriter.println("public class Bar {");
      // References class Baz so that Baz can be modified to trigger invalidation.
      barWriter.println("  private Baz baz = new Baz();");
      barWriter.println("}");
      context.commit(logger, barWriter);
    }

    return "com.foo.Foo";
  }
}
