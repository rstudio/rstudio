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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
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
  private static final Pattern alwaysValidHttpHosts;

  /**
   * The set of blacklisted URLs.
   */
  private static final Set<String> invalidHttpHosts = new HashSet<String>();

  private static String oldBlackList = null;

  private static String oldWhiteList = null;

  /**
   * The set of whitelisted URLs.
   */
  private static final Set<String> validHttpHosts = new HashSet<String>();

  static {
    Set<String> regexes = new HashSet<String>();
    // Regular URLs may or may not have a port, and must either end with
    // the host+port, or be followed by a slash (to avoid attacks like
    // localhost.evildomain.org).
    String portSuffix = "(:\\d+)?(/.*)?";
    regexes.add("https?://localhost" + portSuffix);
    regexes.add("https?://localhost[.]localdomain" + portSuffix);
    regexes.add("https?://127[.]0[.]0[.]1" + portSuffix);
    String hostName;
    try {
      hostName = InetAddress.getLocalHost().getHostName();
      if (hostName != null) {
        hostName = hostName.replace(".", "[.]");
        regexes.add("https?://" + hostName + portSuffix);
      }
    } catch (UnknownHostException e) {
      // Ignore
    }
    String addr;
    try {
      addr = InetAddress.getLocalHost().getHostAddress();
      if (addr != null) {
        addr = addr.replace(".", "[.]");
        regexes.add("https?://" + addr + portSuffix);
      }
    } catch (UnknownHostException e) {
      // Ignore
    }
    regexes.add("file:.*");
    regexes.add("about:.*");
    regexes.add("res:.*");
    regexes.add("javascript:.*");
    regexes.add("([a-z][:])[/\\\\].*");
    // matches c:\ and c:/
    StringBuilder buf = new StringBuilder();
    String prefix = "(";
    for (String regex : regexes) {
      buf.append(prefix).append('(').append(regex).append(')');
      prefix = "|";
    }
    buf.append(")");
    alwaysValidHttpHosts = Pattern.compile(buf.toString(),
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  }

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
  public static String checkHost(String hostUnderConsideration,
      Set<String> hosts) {
    // TODO(jat): build a single regex instead of looping
    hostUnderConsideration = hostUnderConsideration.toLowerCase(Locale.ENGLISH);
    for (String rule : hosts) {
      rule = rule.toLowerCase(Locale.ENGLISH);
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
  public static String formatRules(Set<String> hosts) {
    StringBuffer out = new StringBuffer();
    for (Iterator<String> i = hosts.iterator(); i.hasNext();) {
      String rule = i.next();
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
   * This method returns true if the host is always admissible, regardless of
   * the blacklist.
   *
   * @param url the URL to be verified
   * @return returns true if the host is always admissible
   */
  public static boolean isAlwaysWhitelisted(String url) {
    return alwaysValidHttpHosts.matcher(url).matches();
  }

  /**
   * This method returns non-null if the host is forbidden.
   *
   * @param url the URL to be verified
   * @return returns the regex that specified the host matches the blacklist
   */
  public static String matchBlacklisted(String url) {
    oldBlackList = formatBlackList();
    return checkHost(url, invalidHttpHosts);
  }

  /**
   * This method returns null if the host is admissible, provided it is not on
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
    fix.log(msgType, "Example: -whitelist \"" + whiteListStr + " " + hostRegex
        + "\"", null);
    TreeLogger reject = header.branch(msgType,
        "To reject automatically: add regex matching "
            + "URL to -blacklist command line argument", null);
    reject.log(msgType, "Example: -blacklist \"" + blackListStr + " "
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

}
