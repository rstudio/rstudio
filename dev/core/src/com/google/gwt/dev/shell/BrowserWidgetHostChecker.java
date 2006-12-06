// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class contains utility functions to do whitelist/blacklist handling.
 */
public class BrowserWidgetHostChecker {

  /**
   * The set of always allowed URLs, which are immune to blacklisting.
   */
  private static final Set alwaysValidHttpHosts = new HashSet();

  /**
   * The set of blacklisted URLs.
   */
  private static final Set invalidHttpHosts = new HashSet();

  private static String oldBlackList = null;

  private static String oldWhiteList = null;

  /**
   * The set of whitelisted URLs.
   */
  private static final Set validHttpHosts = new HashSet();

  /**
   * This method blacklists the supplied regexes, separated by comma or space.
   * 
   * @param regexes the regexes to be forbidden
   */
  public static boolean blacklistRegexes(String regexes) {
    return addRegex(regexes, false);
  }

  /**
   * This method blacklists the supplied URL, and any that share the same host.
   * 
   * @param url the host to be forbidden
   */
  public static void blacklistURL(String url) {
    String hostRegex = computeHostRegex(url);
    blacklistRegexes(hostRegex);
  }

  /**
   * This method checks the host to see if it is in the supplied set of regexes.
   * 
   * @param hostUnderConsideration the host to be checked
   * @param hosts the set of regexes to be checked against
   * @return true if the host matches
   */
  public static String checkHost(String hostUnderConsideration, Set hosts) {
    hostUnderConsideration = hostUnderConsideration.toLowerCase();
    for (Iterator i = hosts.iterator(); i.hasNext();) {
      String rule = i.next().toString().toLowerCase();
      // match on lowercased regex
      if (hostUnderConsideration.matches(".*" + rule + ".*")) {
        return rule;
      }
    }
    return null;
  }

  /**
   * This method computes the host regular expression for the given url.
   * 
   * @param url the url to be allowed or disallowed
   * @return the regex that matches the host in the url
   */
  public static String computeHostRegex(String url) {
    // the entire URL up to the first slash not prefixed by a slash or colon.
    String raw = url.split("(?<![:/])/")[0];
    // escape the dots and put a begin line specifier on the result
    return "^" + escapeString(raw);
  }

  private static String escapeString(String raw) {
    StringBuffer out = new StringBuffer();
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
        out.append(c);
      } else if (c == '\\') {
        out.append("[\\\\]");
      } else if (c == ']') {
        out.append("[\\]]");
      } else if (c == '^') {
        out.append("[\\^]");
      } else if (c == '[') {
        out.append("[\\[]");
      } else {
        out.append("[");
        out.append(c);
        out.append("]");
      }
    }
    return out.toString();
  }

  /**
   * This method formats the blacklist for display in the treeLogger.
   * 
   * @return the list of regexes as a String
   */
  public static String formatBlackList() {
    return formatRules(invalidHttpHosts);
  }

  /**
   * This method formats the list of rules for display in the treeLogger.
   * 
   * @param hosts the set of regexes that match hosts
   * @return the list of regexes as a String
   */
  public static String formatRules(Set hosts) {
    StringBuffer out = new StringBuffer();
    for (Iterator i = hosts.iterator(); i.hasNext();) {
      String rule = (String) i.next();
      out.append(rule);
      out.append(" ");
    }
    return out.toString();
  }

  /**
   * This method formats the whitelist for display in the treeLogger.
   * 
   * @return the list of regexes as a String
   */
  public static String formatWhiteList() {
    return formatRules(validHttpHosts);
  }

  /**
   * This method returns true if the host is always admissable, regardless of
   * the blacklist.
   * 
   * @param url the URL to be verified
   * @return returns true if the host is always admissable
   */
  public static boolean isAlwaysWhitelisted(String url) {
    String whitelistRuleFound;
    whitelistRuleFound = checkHost(url, alwaysValidHttpHosts);
    return whitelistRuleFound != null;
  }

  /**
   * This method returns true if the host is forbidden.
   * 
   * @param url the URL to be verified
   * @return returns the regex that specified the host matches the blacklist
   */
  public static String matchBlacklisted(String url) {
    oldBlackList = formatBlackList();
    return checkHost(url, invalidHttpHosts);
  }

  /**
   * This method returns true if the host is admissable, provided it is not on
   * the blacklist.
   * 
   * @param url the URL to be verified
   * @return returns the regex that specified the host matches the whitelist
   */
  public static String matchWhitelisted(String url) {
    oldWhiteList = formatWhiteList();
    return checkHost(url, validHttpHosts);
  }

  /**
   * This method formats a message, and logs it to the treelogger, stating that
   * the url was blocked.
   * 
   * @param url the URL that was disallowed
   * @param header the treelogger under which these messages will be put
   * @param msgType either a caution or an error
   */
  public static void notifyBlacklistedHost(String blacklistRuleFound,
      String url, TreeLogger header, TreeLogger.Type msgType) {
    TreeLogger reason = header.branch(msgType, "reason: " + url
      + " is blacklisted", null);
    reason.log(msgType, "To fix: remove \"" + blacklistRuleFound
      + "\" from system property gwt.hosts.blacklist", null);
  }

  /**
   * This method formats a message, and logs it to the treelogger, stating that
   * the url was not trusted.
   * 
   * @param url the URL that provoked the dialog box
   * @param header the treelogger under which these messages will be put
   * @param msgType either a caution or an error
   */
  public static void notifyUntrustedHost(String url, TreeLogger header,
      TreeLogger.Type msgType) {
    String whiteListStr = oldWhiteList;
    String blackListStr = oldBlackList;
    String hostRegex = computeHostRegex(url);
    TreeLogger reason = header.branch(msgType, "reason: " + url
      + " is not in the whitelist", null);
    reason.log(msgType, "whitelist: " + whiteListStr, null);
    reason.log(msgType, "blacklist: " + blackListStr, null);
    TreeLogger fix = header.branch(msgType, "To fix: add regex matching "
      + "URL to -whitelist command line argument", null);
    fix.log(msgType, "Example: -whitelist=\"" + whiteListStr + " " + hostRegex
      + "\"", null);
    TreeLogger reject = header.branch(msgType,
      "To reject automatically: add regex matching "
        + "URL to -blacklist command line argument", null);
    reject.log(msgType, "Example: -blacklist=\"" + blackListStr + " "
      + hostRegex + "\"", null);
  }

  /**
   * This method whitelists the supplied String of regexes, separated by comma
   * or space.
   * 
   * @param regexes the regexes to be allowed
   */
  public static boolean whitelistRegexes(String regexes) {
    return addRegex(regexes, true);
  }

  /**
   * This method whitelists the supplied URL, and any that share the same host.
   * 
   * @param url the host to be allowed
   */
  public static void whitelistURL(String url) {
    String hostRegex = computeHostRegex(url);
    whitelistRegexes(hostRegex);
  }

  /**
   * This method blacklists or whitelists the supplied regexes, and any that
   * share the same host.
   * 
   * @param whitelist if <code>true</code> the host will be whitelisted
   * @param regexes the regular expressions to be forbidden, seperated by comma
   *          or space
   */
  private static boolean addRegex(String regexes, boolean whitelist) {
    if (regexes.equals("")) {
      return true; // adding empty string is harmless and happens by default
    }
    String[] items = regexes.split("[ ,]");
    for (int i = 0; i < items.length; i++) {
      try {
        Pattern.compile(items[i]);
      } catch (PatternSyntaxException e) {
        System.err.println("The regex '" + items[i] + " has syntax errors.");
        System.err.println(e.toString());
        return false;
      }
      if (whitelist) {
        validHttpHosts.add(items[i]);
      } else {
        invalidHttpHosts.add(items[i]);
      }
    }
    return true;
  }

  static {
    alwaysValidHttpHosts.add("^https?://localhost");
    alwaysValidHttpHosts.add("^file:");
    alwaysValidHttpHosts.add("^about:");
    alwaysValidHttpHosts.add("^res:");
    alwaysValidHttpHosts.add("^javascript:");
    alwaysValidHttpHosts.add("^([a-zA-Z][:])[/\\\\]"); 
    // matches c:\ and c:/
    alwaysValidHttpHosts.add("^https?://localhost/");
    alwaysValidHttpHosts.add("^https?://localhost[.]localdomain/");
    alwaysValidHttpHosts.add("^https?://127[.]0[.]0[.]1/");
    alwaysValidHttpHosts.add("^https?://localhost$");
    alwaysValidHttpHosts.add("^https?://localhost[.]localdomain$");
    alwaysValidHttpHosts.add("^https?://127[.]0[.]0[.]1$");
  }

}
