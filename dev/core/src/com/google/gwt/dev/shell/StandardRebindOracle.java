// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.Rule;
import com.google.gwt.dev.cfg.Rules;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.jdt.CacheManager;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Implements rebind logic in terms of a
 * {@link com.google.gwt.dev.cfg.ModuleDef}.
 */
public class StandardRebindOracle implements RebindOracle {

  /**
   * Makes the actual deferred binding decision by examining rules.
   */
  private final class Rebinder {

    public Rebinder(TypeOracle typeOracle, PropertyOracle propOracle) {
      genCtx = new StandardGeneratorContext(typeOracle, propOracle, genDir,
        cacheManager);
    }

    public String rebind(TreeLogger logger, String typeName)
        throws UnableToCompleteException {

      String result = tryRebind(logger, typeName);
      if (result == null) {
        result = typeName;
      }

      // Announce the newly-generated types.
      //
      JClassType[] genTypes = genCtx.finish(logger);
      if (genTypes.length > 0) {
        onGeneratedTypes(result, genTypes);
      }

      return result;
    }

    private String tryRebind(TreeLogger logger, String typeName)
        throws UnableToCompleteException {
      if (usedTypeNames.contains(typeName)) {
        // Found a cycle.
        //
        String[] cycle = (String[]) Util
          .toArray(String.class, usedTypeNames);
        Messages.UNABLE_TO_REBIND_DUE_TO_CYCLE_IN_RULES
          .log(logger, cycle, null);
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

      for (Iterator iter = rules.iterator(); iter.hasNext();) {
        Rule rule = (Rule) iter.next();

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

            // Invoke the rule.
            //
            return rule.realize(logger, genCtx, typeName);

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

    private final StandardGeneratorContext genCtx;
    private final Set usedRules = new HashSet();
    private final List usedTypeNames = new ArrayList();
  }
  public StandardRebindOracle(TypeOracle typeOracle, PropertyOracle propOracle,
      Rules rules, File genDir, CacheManager cacheManager) {
    this.typeOracle = typeOracle;
    this.propOracle = propOracle;
    this.rules = rules;
    this.genDir = genDir;
    if(cacheManager!= null) {
      this.cacheManager = cacheManager;
    } else {
      this.cacheManager = new CacheManager(typeOracle);
    }
    
  }

  public StandardRebindOracle(TypeOracle typeOracle, StaticPropertyOracle propOracle, Rules rules, File genDir) {
    // This is a path used for non-hosted mode execution; therefore no caching.
    this(typeOracle, propOracle, rules, genDir, null);
  }

  public String rebind(TreeLogger logger, String typeName)
      throws UnableToCompleteException {

    logger = Messages.TRACE_TOPLEVEL_REBIND.branch(logger, typeName, null);

    Rebinder rebinder = new Rebinder(typeOracle, propOracle);
    String result = rebinder.rebind(logger, typeName);

    Messages.TRACE_TOPLEVEL_REBIND_RESULT.log(logger, result, null);

    if (!isKnownToBeUninstantiable(result)) {
      return result;
    } else {
      Messages.REBIND_RESULT_TYPE_IS_NOT_INSTANTIABLE.log(logger, result, null);
      throw new UnableToCompleteException();
    }
  }

  protected void onGeneratedTypes(String result, JClassType[] genTypes) {
  }

  private boolean isKnownToBeUninstantiable(String name) {
    JClassType type = typeOracle.findType(name);
    if (type != null) {
      if (!type.isDefaultInstantiable()) {
        return true;
      }
    }
    return false;
  }

  private final CacheManager cacheManager;
  private final File genDir;
  private final PropertyOracle propOracle;
  private final Rules rules;
  private final TypeOracle typeOracle;

}
