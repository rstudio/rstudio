/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.dev.cfg.Rule;
import com.google.gwt.dev.cfg.Rules;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.javac.rebind.CachedRebindResult;
import com.google.gwt.dev.javac.rebind.RebindCache;
import com.google.gwt.dev.javac.rebind.RebindResult;
import com.google.gwt.dev.javac.rebind.RebindStatus;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements rebind logic in terms of a variety of other well-known oracles.
 */
public class StandardRebindOracle implements RebindOracle {

  /**
   * Makes the actual deferred binding decision by examining rules.
   */
  private final class Rebinder {

    private final Set<Rule> usedRules = new HashSet<Rule>();

    private final List<String> usedTypeNames = new ArrayList<String>();

    public String rebind(TreeLogger logger, String typeName,
        ArtifactAcceptor artifactAcceptor) throws UnableToCompleteException {
      Event rebindEvent = SpeedTracerLogger.start(DevModeEventType.REBIND, "Type Name", typeName);
      try {
        genCtx.setPropertyOracle(propOracle);
        Rule rule = getRebindRule(logger, typeName);

        if (rule == null) {
          return typeName;
        }
        
        CachedRebindResult cachedResult = rebindCacheGet(rule, typeName);
        if (cachedResult != null) {
          genCtx.setCachedGeneratorResult(cachedResult);
        }
        
        // realize the rule (call a generator, or do type replacement, etc.)
        RebindResult result = rule.realize(logger, genCtx, typeName);
        
        // handle rebind result caching (if enabled)
        String resultTypeName = processCacheableResult(logger, rule, typeName, 
            cachedResult, result);
        
        /*
         * Finalize new artifacts from the generator context
         */
        if (artifactAcceptor != null) {
          // Go ahead and call finish() to accept new artifacts.
          ArtifactSet newlyGeneratedArtifacts = genCtx.finish(logger);
          if (!newlyGeneratedArtifacts.isEmpty()) {
            artifactAcceptor.accept(logger, newlyGeneratedArtifacts);
          }
        }

        assert (resultTypeName != null);
        return resultTypeName;
      } finally {
        rebindEvent.end();
      }
    }

    private Rule getRebindRule(TreeLogger logger, String typeName)
        throws UnableToCompleteException {
      if (usedTypeNames.contains(typeName)) {
        // Found a cycle.
        //
        String[] cycle = Util.toArray(String.class, usedTypeNames);
        Messages.UNABLE_TO_REBIND_DUE_TO_CYCLE_IN_RULES.log(logger, cycle, null);
        throw new UnableToCompleteException();
      }

      // Remember that we've seen this one.
      //
      usedTypeNames.add(typeName);

      // Make the rebind decision.
      //
      if (rules.isEmpty()) {
        logger.log(TreeLogger.DEBUG,
            "No rules are defined, so no substitution can occur", null);
        return null;
      }

      for (Iterator<Rule> iter = rules.iterator(); iter.hasNext();) {
        Rule rule = iter.next();

        // Branch the logger.
        //
        TreeLogger branch = Messages.TRACE_CHECKING_RULE.branch(logger, rule,
            null);

        if (rule.isApplicable(branch, genCtx, typeName)) {
          // See if this rule has already been used. This is needed to prevent
          // infinite loops with 'when-assignable' conditions.
          //
          if (!usedRules.contains(rule)) {
            usedRules.add(rule);
            Messages.TRACE_RULE_MATCHED.log(logger, null);

            return rule;
          } else {
            // We are skipping this rule because it has already been used
            // in a previous iteration.
            //
          }
        } else {
          Messages.TRACE_RULE_DID_NOT_MATCH.log(logger, null);
        }
      }

      // No matching rule for this type.
      //
      return null;
    }

    /*
     * Decide how to handle integrating a previously cached result, and whether
     * to cache the new result for the future.
     */
    private String processCacheableResult(TreeLogger logger, Rule rule, 
        String typeName, CachedRebindResult cachedResult, RebindResult newResult) {
      
      String resultTypeName = newResult.getReturnedTypeName();
      
      if (!genCtx.isGeneratorResultCachingEnabled()) {
        return resultTypeName;
      }
      
      RebindStatus status = newResult.getResultStatus();
      switch (status) {
        
        case USE_EXISTING:
          // in this case, no newly generated or cached types are needed
          break; 
          
        case USE_ALL_NEW_WITH_NO_CACHING:
          /*
           * in this case, new artifacts have been generated, but no need to
           * cache results (as the generator is probably not able to take
           * advantage of caching).
           */
          break;
          
        case USE_ALL_NEW:
          // use all new results, add a new cache entry
          cachedResult = new CachedRebindResult(newResult.getReturnedTypeName(),
              genCtx.getArtifacts(), genCtx.getGeneratedUnitMap(), 
              System.currentTimeMillis(), newResult.getClientDataMap());
          rebindCachePut(rule, typeName, cachedResult);
          break;
          
        case USE_ALL_CACHED:
          // use all cached results
          assert (cachedResult != null);
          
          genCtx.commitArtifactsFromCache(logger);
          genCtx.addGeneratedUnitsFromCache();
          
          // use cached type name
          resultTypeName = cachedResult.getReturnedTypeName();
          break;
          
        case USE_PARTIAL_CACHED:
          /*
           * Add cached generated units marked for reuse to the context.  
           * TODO(jbrosenberg): add support for reusing artifacts as well
           * as GeneratedUnits.
           */
          genCtx.addGeneratedUnitsMarkedForReuseFromCache();
          
          /*
           * Create a new cache entry using the composite set of new and 
           * reused cached results currently in genCtx.
           */
          cachedResult = new CachedRebindResult(newResult.getReturnedTypeName(),
              genCtx.getArtifacts(), genCtx.getGeneratedUnitMap(), 
              System.currentTimeMillis(), newResult.getClientDataMap());
          rebindCachePut(rule, typeName, cachedResult);
          break;
      }
      return resultTypeName;
    }
  }

  private final Map<String, String> typeNameBindingMap = new HashMap<String, String>();

  private final StandardGeneratorContext genCtx;

  private final PropertyOracle propOracle;

  private RebindCache rebindCache = null;

  private final Rules rules;

  public StandardRebindOracle(PropertyOracle propOracle, Rules rules,
      StandardGeneratorContext genCtx) {
    this.propOracle = propOracle;
    this.rules = rules;
    this.genCtx = genCtx;
  }

  /**
   * Invalidates the given source type name, so the next rebind request will
   * generate the type again.
   */
  public void invalidateRebind(String sourceTypeName) {
    typeNameBindingMap.remove(sourceTypeName);
  }

  public String rebind(TreeLogger logger, String typeName)
      throws UnableToCompleteException {
    return rebind(logger, typeName, null);
  }

  public String rebind(TreeLogger logger, String typeName,
      ArtifactAcceptor artifactAcceptor) throws UnableToCompleteException {

    String resultTypeName = typeNameBindingMap.get(typeName);
    if (resultTypeName == null) {
      logger = Messages.TRACE_TOPLEVEL_REBIND.branch(logger, typeName, null);

      Rebinder rebinder = new Rebinder();
      resultTypeName = rebinder.rebind(logger, typeName, artifactAcceptor);
      typeNameBindingMap.put(typeName, resultTypeName);

      Messages.TRACE_TOPLEVEL_REBIND_RESULT.log(logger, resultTypeName, null);
    }
    return resultTypeName;
  }

  public void setRebindCache(RebindCache cache) {
    this.rebindCache = cache;
  }
  
  private CachedRebindResult rebindCacheGet(Rule rule, String typeName) {
    if (rebindCache != null) {
      return rebindCache.get(rule, typeName);
    }
    return null;
  }
  
  private void rebindCachePut(Rule rule, String typeName, CachedRebindResult result) {
    if (rebindCache != null) {
      rebindCache.put(rule, typeName, result);
    }
  }
}
