/*
 * PackageLinkColumnTests.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages.ui;

import java.util.List;

import org.rstudio.studio.client.workbench.views.packages.model.PackageVulnerabilityTypes.PackageVulnerability;
import org.rstudio.studio.client.workbench.views.packages.model.PackageVulnerabilityTypes.RepositoryPackageVulnerabilityListMap;

import com.google.gwt.junit.client.GWTTestCase;

public class PackageLinkColumnTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testFiltersOutOtherVersions()
   {
      // each vuln carries its own versions map; only the records that apply to
      // the requested version should survive.
      RepositoryPackageVulnerabilityListMap vulns = emptyRepoMap();
      addVuln(vulns, "repo1", "foo", "CVE-1", "1.0");
      addVuln(vulns, "repo1", "foo", "CVE-2", "2.0");

      List<PackageVulnerability> result =
         PackageLinkColumn.PackageLinkCell.vulnerabilitiesForPackage(vulns, "foo", "1.0");

      assertEquals(1, result.size());
      assertEquals("CVE-1", result.get(0).id);
   }

   public void testEmptyWhenNoVersionMatches()
   {
      RepositoryPackageVulnerabilityListMap vulns = emptyRepoMap();
      addVuln(vulns, "repo1", "foo", "CVE-1", "1.0");

      assertTrue(PackageLinkColumn.PackageLinkCell
         .vulnerabilitiesForPackage(vulns, "foo", "9.9").isEmpty());
   }

   public void testEmptyWhenPackageAbsent()
   {
      RepositoryPackageVulnerabilityListMap vulns = emptyRepoMap();
      addVuln(vulns, "repo1", "foo", "CVE-1", "1.0");

      assertTrue(PackageLinkColumn.PackageLinkCell
         .vulnerabilitiesForPackage(vulns, "bar", "1.0").isEmpty());
   }

   public void testCombinesAcrossRepositories()
   {
      // the same package+version can appear under more than one repository key;
      // distinct CVEs from each should be merged into a single list.
      RepositoryPackageVulnerabilityListMap vulns = emptyRepoMap();
      addVuln(vulns, "repo1", "foo", "CVE-1", "1.0");
      addVuln(vulns, "repo2", "foo", "CVE-2", "1.0");

      List<PackageVulnerability> result =
         PackageLinkColumn.PackageLinkCell.vulnerabilitiesForPackage(vulns, "foo", "1.0");

      assertEquals(2, result.size());
      assertEquals("CVE-1", result.get(0).id);
      assertEquals("CVE-2", result.get(1).id);
   }

   public void testDeduplicatesCveAcrossRepositories()
   {
      // the same CVE surfacing under two repository keys must appear once.
      RepositoryPackageVulnerabilityListMap vulns = emptyRepoMap();
      addVuln(vulns, "repo1", "foo", "CVE-1", "1.0");
      addVuln(vulns, "repo2", "foo", "CVE-1", "1.0");

      List<PackageVulnerability> result =
         PackageLinkColumn.PackageLinkCell.vulnerabilitiesForPackage(vulns, "foo", "1.0");

      assertEquals(1, result.size());
      assertEquals("CVE-1", result.get(0).id);
   }

   public void testSortsById()
   {
      RepositoryPackageVulnerabilityListMap vulns = emptyRepoMap();
      addVuln(vulns, "repo1", "foo", "CVE-9", "1.0");
      addVuln(vulns, "repo1", "foo", "CVE-1", "1.0");
      addVuln(vulns, "repo1", "foo", "CVE-5", "1.0");

      List<PackageVulnerability> result =
         PackageLinkColumn.PackageLinkCell.vulnerabilitiesForPackage(vulns, "foo", "1.0");

      assertEquals(3, result.size());
      assertEquals("CVE-1", result.get(0).id);
      assertEquals("CVE-5", result.get(1).id);
      assertEquals("CVE-9", result.get(2).id);
   }

   public void testSkipsRepositorySerializedAsArray()
   {
      // empty R lists can serialize as JS arrays instead of objects; those
      // entries must be skipped rather than scanned via Array.prototype.
      RepositoryPackageVulnerabilityListMap vulns = emptyRepoMap();
      addVuln(vulns, "repo1", "foo", "CVE-1", "1.0");
      addArrayRepo(vulns, "repo2");

      List<PackageVulnerability> result =
         PackageLinkColumn.PackageLinkCell.vulnerabilitiesForPackage(vulns, "foo", "1.0");

      assertEquals(1, result.size());
      assertEquals("CVE-1", result.get(0).id);
   }

   private static native RepositoryPackageVulnerabilityListMap emptyRepoMap() /*-{
      return {};
   }-*/;

   private static native void addVuln(
      RepositoryPackageVulnerabilityListMap vulns,
      String repo, String name, String id, String version) /*-{
      if (!vulns[repo])
         vulns[repo] = {};
      if (!vulns[repo][name])
         vulns[repo][name] = [];
      var versions = {};
      versions[version] = true;
      vulns[repo][name].push({
         id: id,
         summary: "",
         details: "",
         versions: versions,
         ranges: []
      });
   }-*/;

   private static native void addArrayRepo(
      RepositoryPackageVulnerabilityListMap vulns, String repo) /*-{
      vulns[repo] = [];
   }-*/;
}
