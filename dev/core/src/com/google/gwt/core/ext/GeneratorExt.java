/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.core.ext;

import com.google.gwt.dev.javac.rebind.RebindResult;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>
 * Adds a new {@link #generateIncrementally} method.
 * <p> 
 * TODO(jbrosenberg): Merge this into {@link Generator} directly, once the api
 * has stabilized and we can remove the "experimental" moniker.
 */
public abstract class GeneratorExt extends Generator {
 
  /**
   * A default implementation of the abstract method defined in the base
   * {@link Generator} class.  This will wrap a call to
   * {@link #generateIncrementally}, and attempt no caching.  This supports
   * backwards compatibility for applications or other generators which call 
   * this generator directly, as outside of the normal internal rebind process.
   * <p>
   * It is recommended that {@link #generateIncrementally} be used instead.
   * 
   * @return the name of a subclass to substitute for the requested class, or
   *         return <code>null</code> to cause the requested type itself to be
   *         used
   */
  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    
    // wrap the passed in context
    GeneratorContextExt contextExt = context instanceof GeneratorContextExt
        ? (GeneratorContextExt) context
        : GeneratorContextExtWrapper.newInstance(context);
    
    RebindResult result = generateIncrementally(logger, contextExt, typeName);
    return result.getReturnedTypeName();
  }
  
  /**
   * Incrementally generate a default constructible subclass of the requested 
   * type.  The generator can use information from the context to determine
   * whether it needs to regenerate everything, or whether it can selectively
   * regenerate a subset of its output, or whether it can return quickly to
   * allow reuse of all previously cached objects.  It will return a 
   * {@link RebindResult}, which contains a
   * {@link com.google.gwt.dev.javac.rebind.RebindStatus} field indicating
   * whether to use previously cached artifacts, newly generated ones, or a
   * partial mixture of both cached and newly generated objects.
   * <p>
   * The result also includes a field for the name of the subclass to 
   * substitute for the requested class.
   * <p>
   * The generator throws an <code>UnableToCompleteException</code> if for 
   * any reason it cannot complete successfully.
   * 
   * @return a RebindResult
   */
  public abstract RebindResult generateIncrementally(TreeLogger logger, 
      GeneratorContextExt context, String typeName) 
      throws UnableToCompleteException;
}
