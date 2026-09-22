#
# test-graphics.R
#
# Copyright (C) 2026 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

context("graphics")

# Checks that the "(Default)" value of a graphics preference defers to a value
# the user set directly on the mirrored R option (as an Rprofile.site would),
# while an explicit preference still takes precedence.
checkGraphicsPrefDefersToOption <- function(pref, option, userValue, prefValue) {
   oldPref <- .rs.readUiPref(pref)
   oldOption <- getOption(option)
   on.exit({
      if (is.null(oldPref))
         .rs.removePref(pref)
      else
         .rs.writeUiPref(pref, oldPref)
      do.call(options, stats::setNames(list(oldOption), option))
   }, add = TRUE)

   # simulate options(<option> = <userValue>) from an R profile
   do.call(options, stats::setNames(list(userValue), option))

   # saving the preference at its default must leave the option alone
   .rs.writeUiPref(pref, "default")
   expect_equal(getOption(option), userValue)

   # an explicit preference takes precedence
   .rs.writeUiPref(pref, prefValue)
   expect_equal(getOption(option), prefValue)

   # and going back to the default restores the user's value
   .rs.writeUiPref(pref, "default")
   expect_equal(getOption(option), userValue)
}

test_that("the default graphics backend preference defers to RStudioGD.backend (#9275)", {
   checkGraphicsPrefDefersToOption(
      pref = "graphics_backend",
      option = "RStudioGD.backend",
      userValue = "cairo",
      prefValue = "ragg"
   )
})

test_that("the default antialiasing preference defers to RStudioGD.antialias (#9275)", {
   checkGraphicsPrefDefersToOption(
      pref = "graphics_antialiasing",
      option = "RStudioGD.antialias",
      userValue = "gray",
      prefValue = "subpixel"
   )
})

# Writes the fixed plot size user state; object members must be scalars.
writeFixedPlotSize <- function(enabled, width = 7, height = 5, units = "in") {
   .rs.writeUserState("fixed_plot_size", list(
      enabled = .rs.scalar(enabled),
      width   = .rs.scalar(width),
      height  = .rs.scalar(height),
      units   = .rs.scalar(units)
   ))
}

test_that("a fixed plot size pins the size of the RStudio graphics device (#4422)", {
   oldState <- .rs.readUserState("fixed_plot_size")
   on.exit(.rs.writeUserState("fixed_plot_size", lapply(oldState, .rs.scalar)), add = TRUE)

   writeFixedPlotSize(FALSE)
   .rs.activateGraphicsDevice()
   expect_equal(names(dev.cur()), "RStudioGD")
   paneSize <- dev.size("in")

   writeFixedPlotSize(TRUE, width = 4, height = 3, units = "in")
   expect_equal(dev.size("in"), c(4, 3))

   writeFixedPlotSize(TRUE, width = 10.16, height = 5.08, units = "cm")
   expect_equal(dev.size("in"), c(4, 2))

   # pixels are at 96 DPI, regardless of the display's pixel ratio
   writeFixedPlotSize(TRUE, width = 480, height = 288, units = "px")
   expect_equal(dev.size("in"), c(5, 3))

   # sizes are clamped to the supported range (1 to 30 inches)
   writeFixedPlotSize(TRUE, width = 100, height = 0.5, units = "in")
   expect_equal(dev.size("in"), c(30, 1))

   # turning it off follows the pane again
   writeFixedPlotSize(FALSE, width = 4, height = 3, units = "in")
   expect_equal(dev.size("in"), paneSize)
})
