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

import com.google.gwt.core.ext.CachedGeneratorResult;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.RebindRuleResolver;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.dev.RebindCache;
import com.google.gwt.dev.cfg.Rule;
import com.google.gwt.dev.cfg.Rules;
import com.google.gwt.dev.javac.CachedGeneratorResultImpl;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements rebind logic in terms of a variety of other well-known oracles.
 */
public class StandardRebindOracle implements RebindOracle {

  /**
   * Makes the actual deferred binding decision by examining rules.
   */
  private final class Rebinder implements RebindRuleResolver {

    @Override
    public boolean checkRebindRuleResolvable(String typeName) {
      try {
        if (getRebindRule(TreeLogger.NULL, typeName) != null) {
          return true;
        }
      } catch (UnableToCompleteException utcEx) {
      }
      return false;
    }

    public String rebind(TreeLogger logger, String typeName, ArtifactAcceptor artifactAcceptor)
        throws UnableToCompleteException {
      Event rebindEvent = SpeedTracerLogger.start(DevModeEventType.REBIND, "Type Name", typeName);
      try {
        genCtx.setPropertyOracle(propOracle);
        genCtx.setRebindRuleResolver(this);
        Rule rule = getRebindRule(logger, typeName);

        if (rule == null) {
          return typeName;
        }

        CachedGeneratorResult cachedResult = rebindCacheGet(rule, typeName);
        if (cachedResult != null) {
          genCtx.setCachedGeneratorResult(cachedResult);
        }

        // realize the rule (call a generator, or do type replacement, etc.)
        RebindResult result = rule.realize(logger, genCtx, typeName);

        // handle rebind result caching (if enabled)
        String resultTypeName =
            processCacheableResult(logger, rule, typeName, cachedResult, result);

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

    private Rule getRebindRule(TreeLogger logger, String typeName) throws UnableToCompleteException {

      // Make the rebind decision.
      //
      if (rules.isEmpty()) {
        logger.log(TreeLogger.DEBUG, "No rules are defined, so no substitution can occur", null);
        return null;
      }

      Rule minCostRuleSoFar = null;

      for (Iterator<Rule> iter = rules.iterator(); iter.hasNext();) {
        Rule rule = iter.next();

        // Branch the logger.
        //
        TreeLogger branch = Messages.TRACE_CHECKING_RULE.branch(logger, rule, null);

        if (rule.isApplicable(branch, genCtx, typeName)) {
          Messages.TRACE_RULE_MATCHED.log(logger, null);
          return rule;
        } else {
          Messages.TRACE_RULE_DID_NOT_MATCH.log(logger, null);

          // keep track of fallback partial matches
          if (minCostRuleSoFar == null) {
            minCostRuleSoFar = rule;
          }
          assert rule.getFallbackEvaluationCost() != 0;
          // if we found a better match, keep that as the best candidate so far
          if (rule.getFallbackEvaluationCost() <= minCostRuleSoFar.getFallbackEvaluationCost()) {
            if (logger.isLoggable(TreeLogger.DEBUG)) {
              logger.log(TreeLogger.DEBUG, "Found better fallback match for " + rule);
            }
            minCostRuleSoFar = rule;
          }
        }
      }

      // if we reach this point, it means we did not find an exact match
      // and we may have a partial match based on fall back values
      assert minCostRuleSoFar != null;
      if (minCostRuleSoFar.getFallbackEvaluationCost() < Integer.MAX_VALUE) {
        if (logger.isLoggable(TreeLogger.INFO)) {
          logger.log(TreeLogger.INFO, "Could not find an exact match rule. Using 'closest' rule "
              + minCostRuleSoFar
              + " based on fall back values. You may need to implement a specific "
              + "binding in case the fall back behavior does not replace the missing binding");
        }
        if (logger.isLoggable(TreeLogger.DEBUG)) {
          logger.log(TreeLogger.DEBUG, "No exact match was found, using closest match rule "
              + minCostRuleSoFar);
        }
        return minCostRuleSoFar;
      }

      // No matching rule for this type.
      return null;
    }

    /*
     * Decide how to handle integrating a previously cached result, and whether
     * to cache the new result for the future.
     */
    private String processCacheableResult(TreeLogger logger, Rule rule, String typeName,
        CachedGeneratorResult cachedResult, RebindResult newResult) {

      String resultTypeName = newResult.getResultTypeName();

      if (!genCtx.isGeneratorResultCachingEnabled()) {
        return resultTypeName;
      }

      RebindMode mode = newResult.getRebindMode();
      switch (mode) {

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
          cachedResult =
              new CachedGeneratorResultImpl(newResult.getResultTypeName(), genCtx.getArtifacts(),
                  genCtx.getGeneratedUnitMap(), System.currentTimeMillis(), newResult
                      .getClientDataMap());
          rebindCachePut(rule, typeName, cachedResult);
          break;

        case USE_ALL_CACHED:
          // use all cached results
          assert (cachedResult != null);

          genCtx.commitArtifactsFromCache(logger);
          genCtx.addGeneratedUnitsFromCache();

          // use cached type name
          resultTypeName = cachedResult.getResultTypeName();
          break;

        case USE_PARTIAL_CACHED:
          /*
           * Add cached generated units marked for reuse to the context.
           * TODO(jbrosenberg): add support for reusing artifacts as well as
           * GeneratedUnits.
           */
          genCtx.addGeneratedUnitsMarkedForReuseFromCache();

          /*
           * Create a new cache entry using the composite set of new and reused
           * cached results currently in genCtx.
           */
          cachedResult =
              new CachedGeneratorResultImpl(newResult.getResultTypeName(), genCtx.getArtifacts(),
                  genCtx.getGeneratedUnitMap(), System.currentTimeMillis(), newResult
                      .getClientDataMap());
          rebindCachePut(rule, typeName, cachedResult);
          break;
      }

      // clear the current cached result
      genCtx.setCachedGeneratorResult(null);

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

  @Override
  public String rebind(TreeLogger logger, String typeName) throws UnableToCompleteException {
    return rebind(logger, typeName, null);
  }

  public String rebind(TreeLogger logger, String typeName, ArtifactAcceptor artifactAcceptor)
      throws UnableToCompleteException {

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

  private CachedGeneratorResult rebindCacheGet(Rule rule, String typeName) {
    if (rebindCache != null) {
      return rebindCache.get(rule, typeName);
    }
    return null;
  }

  private void rebindCachePut(Rule rule, String typeName, CachedGeneratorResult result) {
    if (rebindCache != null) {
      rebindCache.put(rule, typeName, result);
    }
  }
}
