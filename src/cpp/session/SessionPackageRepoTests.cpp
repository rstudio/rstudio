/*
 * SessionPackageRepoTests.cpp
 *
 * Copyright (C) 2024 by Posit Software, PBC
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

#include <gtest/gtest.h>

#include "SessionPackageRepo.hpp"

namespace rstudio {
namespace session {

const std::string defaultPPMRepo = "https://packagemanager.posit.co/cran";

TEST(PackageRepoTest, EmptyInputGivesEmptyCodename) {
   const auto id = "";
   const auto version = "";
   const auto versionCodename = "";
   EXPECT_EQ(getP3MLinuxCodename(id, version, versionCodename), "");
}

TEST(PackageRepoTest, UnknownDistributionGivesEmptyCodename) {
   const auto id = "unknown";
   const auto version = "1.0";
   const auto versionCodename = "unknown";
   EXPECT_EQ(getP3MLinuxCodename(id, version, versionCodename), "");
}

TEST(PackageRepoTest, UbuntuCodenameMatchesVersionCodename) {
   const auto id = "ubuntu";
   const auto version = "20.04";
   const auto versionCodename = "focal";
   EXPECT_EQ(getP3MLinuxCodename(id, version, versionCodename), versionCodename);
}

TEST(PackageRepoTest, DebianCodenameMatchesVersionCodename) {
   const auto id = "debian";
   const auto version = "11";
   const auto versionCodename = "bookworm";
   EXPECT_EQ(getP3MLinuxCodename(id, version, versionCodename), versionCodename);
}

TEST(PackageRepoTest, RhelCodenameIncludesMajorVersion) {
   const auto id = "rhel";
   const auto version = "8.3";
   const auto versionCodename = "";
   EXPECT_EQ(getP3MLinuxCodename(id, version, versionCodename), "rhel8");
}

TEST(PackageRepoTest, RhelMajorVersionOnlyIsHandledCorrectly) {
   const auto id = "rhel";
   const auto version = "9";
   const auto versionCodename = "";
   EXPECT_EQ(getP3MLinuxCodename(id, version, versionCodename), "rhel9");
}

TEST(PackageRepoTest, SlesCodenameIncludesFullVersion) {
   const auto id = "sles";
   const auto version = "15.5";
   const auto versionCodename = "";
   EXPECT_EQ(getP3MLinuxCodename(id, version, versionCodename), "sles155");
}

TEST(PackageRepoTest, OpenSuseMajorVersionIsHandledCorrectly) {
   const auto id = "opensuse-leap";
   const auto version = "15";
   const auto versionCodename = "";
   EXPECT_EQ(getP3MLinuxCodename(id, version, versionCodename), "opensuse15");
}

TEST(PackageRepoTest, OpenSuseCodenameIncludesFullVersion) {
   const auto id = "opensuse-leap";
   const auto version = "15.5";
   const auto versionCodename = "";
   EXPECT_EQ(getP3MLinuxCodename(id, version, versionCodename), "opensuse155");
}

TEST(PackageRepoTest, OpenSuseMajorVersionHandlingIsConsistent) {
   const auto id = "opensuse-leap";
   const auto version = "15";
   const auto versionCodename = "";
   EXPECT_EQ(getP3MLinuxCodename(id, version, versionCodename), "opensuse15");
}

TEST(PackageRepoTest, EmptyLinuxNameUsesGenericRepo) {
   const auto linuxName = "";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/latest");
}

TEST(PackageRepoTest, UnknownLinuxNameUsesGenericRepo) {
   const auto linuxName = "unknown";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/latest");
}

TEST(PackageRepoTest, UbuntuFocalUsesCorrectRepo) {
   const auto linuxName = "focal";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/__linux__/focal/latest");
}

TEST(PackageRepoTest, UbuntuJammyUsesCorrectRepo) {
   const auto linuxName = "jammy";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/__linux__/jammy/latest");
}

TEST(PackageRepoTest, UbuntuNobleUsesCorrectRepo) {
   const auto linuxName = "noble";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/__linux__/noble/latest");
}

TEST(PackageRepoTest, DebianBookwormUsesCorrectRepo) {
   const auto linuxName = "bookworm";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/__linux__/bookworm/latest");
}

TEST(PackageRepoTest, DebianBullseyeUsesCorrectRepo) {
   const auto linuxName = "bullseye";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/__linux__/bullseye/latest");
}

TEST(PackageRepoTest, Rhel8UsesCentos8Repo) {
   const auto linuxName = "rhel8";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/__linux__/centos8/latest");
}

TEST(PackageRepoTest, Rhel9UsesCorrectRepo) {
   const auto linuxName = "rhel9";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/__linux__/rhel9/latest");
}

TEST(PackageRepoTest, Sles155UsesOpenSuseRepo) {
   const auto linuxName = "sles155";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/__linux__/opensuse155/latest");
}

TEST(PackageRepoTest, Sles156UsesOpenSuseRepo) {
   const auto linuxName = "sles156";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/__linux__/opensuse156/latest");
}

TEST(PackageRepoTest, OpenSuse155HasCorrectRepo) {
   const auto linuxName = "opensuse155";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/__linux__/opensuse155/latest");
}

TEST(PackageRepoTest, OpenSuse156HasCorrectRepo) {
   const auto linuxName = "opensuse156";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, defaultPPMRepo), "https://packagemanager.posit.co/cran/__linux__/opensuse156/latest");
}

TEST(PackageRepoTest, OtherDistributionsHaveNoCodename) {
   const auto id = "some-other-distro";
   const auto version = "42";
   const auto versionCodename = "";
   EXPECT_EQ(getP3MLinuxCodename(id, version, versionCodename), "");
}

TEST(PackageRepoTest, CustomRepoIsReturnedAsIs) {
   const auto linuxName = "";
   const auto customRepo = "https://my-custom-repo.com/r";
   EXPECT_EQ(getP3MLinuxRepo(linuxName, customRepo), "https://my-custom-repo.com/r/latest");
}

} // namespace session
} // namespace rstudio
