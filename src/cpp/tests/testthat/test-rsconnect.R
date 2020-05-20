#
# test-rsconnect.R
#
# Copyright (C) 2020 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

context("rsconnect")

test_that("setting UI prefs updates options", {
   # save old preference values and restore on exit
   checkCerts <- getOption("rsconnect.check.certificate")
   caBundle <- getOption("rsconnect.ca.bundle")
   on.exit({
      options(rsconnect.check.certificate = checkCerts,
              rsconnect.ca.bundle         = caBundle)
   }, add = TRUE)

   # save old UI prefs and restore on exit
   publishCheckCertificates <- .rs.readUiPref("publish_check_certificates")
   usePublishCaBundle <- .rs.readUiPref("use_publish_ca_bundle")
   publishCaBundle <- .rs.readUiPref("publish_ca_bundle")
   on.exit({
      .rs.writeUiPref("publish_check_certificates", publishCheckCertificates)
      .rs.writeUiPref("use_publish_ca_bundle", usePublishCaBundle)
      .rs.writeUiPref("publish_ca_bundle", publishCaBundle)
   }, add = TRUE)

   # check toggle for certificates
   .rs.writeUiPref("publish_check_certificates", TRUE)
   expect_true(getOption("rsconnect.check.certificate"))
   .rs.writeUiPref("publish_check_certificates", FALSE)
   expect_false(getOption("rsconnect.check.certificate"))

   # use a custom bundle
   .rs.writeUiPref("publish_ca_bundle", "42")
   .rs.writeUiPref("use_publish_ca_bundle", TRUE)
   expect_equal(getOption("rsconnect.ca.bundle"), "42")

   # don't use a bundle
   .rs.writeUiPref("use_publish_ca_bundle", FALSE)
   expect_null(getOption("rsconnect.ca.bundle"))
})
