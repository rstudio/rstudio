/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jdt.RebindOracle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages a set of {@link Compilation} objects keyed by deferred binding
 * decisions. This class encapsulaes the idea of caching a set of compilations,
 * some of which may be reusable between permutations to avoid redundant
 * recompilation.
 */
public class Compilations {

  private final List<Compilation> list = new ArrayList<Compilation>();

  public void add(Compilation compilation) {
    list.add(compilation);
  }

  /**
   * Finds an existing compilation that is equivalent to what would be
   * generated.
   */
  public Compilation find(TreeLogger logger, RebindOracle rebindOracle,
      String[] entryPts) throws UnableToCompleteException {
    // NOTE: This could be optimized in a wide variety of ways.
    //

    // Clone the list so that we can easily whittle it down.
    //
    List<Compilation> candidates = new ArrayList<Compilation>(list);

    // First, the entry points must all be present and map the same way in the
    // correct compilation.
    //
    for (int i = 0; !candidates.isEmpty() && i < entryPts.length; i++) {
      String in = entryPts[i];
      String out = rebindOracle.rebind(logger, in);
      removeMismatches(candidates, in, out);
    }

    // For the remaining compilations, all recorded rebinds must still match.
    //
    for (Iterator<Compilation> iter = candidates.iterator(); iter.hasNext();) {
      Compilation c = iter.next();
      String[] cachedIns = c.getRebindInputs();
      for (int i = 0; i < cachedIns.length; i++) {
        String cachedIn = cachedIns[i];
        String cachedOut = c.getRebindOutput(cachedIn);
        String out = rebindOracle.rebind(logger, cachedIn);
        if (!cachedOut.equals(out)) {
          // Not a candidate anymore; remove it.
          //
          iter.remove();
          break;
        }
      }
    }

    if (candidates.isEmpty()) {
      // Not found in the cache.
      //
      return null;
    } else if (candidates.size() == 1) {
      // Found the perfect match.
      //
      return candidates.get(0);
    } else {
      // Unexpected situation that means something really weird happened.
      //
      String msg = "Cannot decided between multiple existing compilations; cannot continue";
      logger.log(TreeLogger.ERROR, msg, null);
      throw new UnableToCompleteException();
    }
  }

  public Iterator<Compilation> iterator() {
    return list.iterator();
  }

  private void removeMismatches(List<Compilation> candidates, String in,
      String out) {
    for (Iterator<Compilation> iter = candidates.iterator(); iter.hasNext();) {
      Compilation c = iter.next();
      String cachedOut = c.getRebindOutput(in);
      if (!out.equals(cachedOut)) {
        iter.remove();
      }
    }
  }
}
