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

   writeFixedPlotSize(TRUE, width = 1e12, height = -1e12, units = "in")
   expect_equal(dev.size("in"), c(30, 1))

   # turning it off follows the pane again
   writeFixedPlotSize(FALSE, width = 4, height = 3, units = "in")
   expect_equal(dev.size("in"), paneSize)
})

# Reads the big-endian integer of `size` bytes following `offset`.
readBigEndianInt <- function(bytes, offset, size) {
   sum(as.integer(bytes[offset + seq_len(size)]) * 256^((size - 1):0))
}

# Reads the resolution, in DPI, recorded in a PNG (pHYs chunk) or JPEG (JFIF
# header) file; NA when there is none.
imageResolution <- function(path) {
   bytes <- readBin(path, "raw", file.size(path))

   if (identical(bytes[1:4], as.raw(c(0x89, 0x50, 0x4E, 0x47)))) {
      offset <- 8
      while (offset < length(bytes)) {
         chunkLength <- readBigEndianInt(bytes, offset, 4)
         type <- rawToChar(bytes[offset + 5:8])
         if (type == "pHYs" && as.integer(bytes[offset + 17]) == 1)
            return(round(readBigEndianInt(bytes, offset + 8, 4) * 0.0254))
         offset <- offset + 12 + chunkLength
      }
      return(NA)
   }

   isJfif <- identical(bytes[c(1:4, 7:10)], as.raw(c(0xFF, 0xD8, 0xFF, 0xE0, 0x4A, 0x46, 0x49, 0x46)))
   if (isJfif && as.integer(bytes[14]) == 1)
      return(readBigEndianInt(bytes, 14, 2))

   NA
}

test_that("saved plot images record their resolution (#4422)", {
   # R's quartz() bitmap devices don't record it themselves (PR#19076)
   .rs.activateGraphicsDevice()
   plot(1:10)

   for (format in c("png", "jpeg")) {
      file <- tempfile(fileext = paste0(".", format))
      on.exit(unlink(file), add = TRUE)
      .rs.api.savePlotAsImage(file, format, 480, 288)
      expect_equal(imageResolution(file), 96, info = format)
   }
})

test_that("saving a plot image refuses sizes that would exhaust memory", {
   .rs.activateGraphicsDevice()
   plot(1:10)

   file <- tempfile(fileext = ".png")
   on.exit(unlink(file), add = TRUE)
   expect_error(
      .rs.api.savePlotAsImage(file, "png", 20000, 20000),
      "20000 x 20000 pixels"
   )
   expect_false(file.exists(file))
})

# Reads the value of the compression tag (259) in a TIFF's first image
# directory; NA when there is none.
tiffCompression <- function(path) {
   bytes <- readBin(path, "raw", file.size(path))
   littleEndian <- rawToChar(bytes[1:2]) == "II"
   readInt <- function(offset, size) {
      values <- as.integer(bytes[offset + seq_len(size)])
      if (littleEndian)
         values <- rev(values)
      sum(values * 256^((size - 1):0))
   }

   # each 12-byte entry is a tag, a type, a count, then the value (a short
   # is stored in the value's first two bytes)
   directory <- readInt(4, 4)
   for (i in seq_len(readInt(directory, 2))) {
      entry <- directory + 2 + (i - 1) * 12
      if (readInt(entry, 2) == 259)
         return(readInt(entry + 8, 2))
   }
   NA
}

test_that("saved TIFFs are compressed as asked, with LZW by default (#19114)", {
   .rs.activateGraphicsDevice()
   plot(1:10)

   saveTiff <- function(compression = NULL) {
      file <- tempfile(fileext = ".tiff")
      .rs.api.savePlotAsImage(file, "tiff", 480, 288, compression = compression)
      file
   }

   # the compression tag's value for each type the session offers
   tags <- c(none = 1, lzw = 5, zip = 8, "lzw+p" = 5, "zip+p" = 8)
   offered <- .rs.graphics.offeredTiffCompressionTypes()
   expect_true(all(c("none", "lzw") %in% offered))
   for (type in offered) {
      file <- saveTiff(type)
      on.exit(unlink(file), add = TRUE)
      expect_equal(tiffCompression(file), tags[[type]], info = type)
   }

   # a type R knows but the device can't apply (the predictor variants on
   # Quartz, which writes through ImageIO) is an error, not a silent fallback
   for (type in setdiff(.rs.graphics.tiffCompressionTypes(), offered))
      expect_error(saveTiff(type), "Invalid TIFF compression", info = type)

   default <- saveTiff()
   on.exit(unlink(default), add = TRUE)
   expect_equal(tiffCompression(default), 5)

   none <- saveTiff("none")
   on.exit(unlink(none), add = TRUE)
   expect_lt(file.size(default), file.size(none) / 2)

   expect_error(saveTiff("bogus; stop()"), "Invalid TIFF compression")
})

test_that("savePlotAsImage() rejects invalid compression and quality arguments (#19114)", {
   .rs.activateGraphicsDevice()
   plot(1:10)

   file <- tempfile(fileext = ".png")
   on.exit(unlink(file), add = TRUE)

   expect_error(.rs.api.savePlotAsImage(file, "tiff", 480, 288, compression = 1), "compression argument")
   expect_error(.rs.api.savePlotAsImage(file, "tiff", 480, 288, compression = c("lzw", "zip")), "compression argument")
   expect_error(.rs.api.savePlotAsImage(file, "tiff", 480, 288, compression = NA_character_), "compression argument")
   for (quality in list("high", NA_real_, 0, 150, 95.7, c(90, 95)))
      expect_error(.rs.api.savePlotAsImage(file, "jpeg", 480, 288, quality = quality), "quality argument")
   expect_false(file.exists(file))

   # the compression only matters for TIFFs
   .rs.api.savePlotAsImage(file, "png", 480, 288, compression = "LZW")
   expect_true(file.exists(file))
})

test_that("saved JPEGs are encoded at the asked quality (#19114)", {
   .rs.activateGraphicsDevice()
   plot(1:10)

   saveJpeg <- function(quality = NULL) {
      file <- tempfile(fileext = ".jpeg")
      .rs.api.savePlotAsImage(file, "jpeg", 480, 288, quality = quality)
      file
   }

   best <- saveJpeg()
   on.exit(unlink(best), add = TRUE)
   low <- saveJpeg(50)
   on.exit(unlink(low), add = TRUE)
   expect_gt(file.size(best), file.size(low))

   # the resolution is recorded whichever way the JPEG is encoded
   expect_equal(imageResolution(low), 96)
})

# Reads the width and height, in pixels, from a PNG's IHDR chunk.
pngDimensions <- function(path) {
   bytes <- readBin(path, "raw", 24)
   c(readBigEndianInt(bytes, 16, 4), readBigEndianInt(bytes, 20, 4))
}

test_that("a plot with a fixed size is published to RPubs at that size (#4422)", {
   oldState <- .rs.readUserState("fixed_plot_size")
   on.exit(.rs.writeUserState("fixed_plot_size", lapply(oldState, .rs.scalar)), add = TRUE)

   writeFixedPlotSize(TRUE, width = 4, height = 3, units = "in")
   .rs.activateGraphicsDevice()
   plot(1:10)

   # the images are written next to the page; the size the client sends,
   # derived from the pane, is ignored
   target <- .rs.invokeRpc("plots_create_rpubs_html", "Plot", "", 400L, 350L)
   dir <- dirname(path.expand(target))
   on.exit(unlink(dir, recursive = TRUE), add = TRUE)

   expect_equal(pngDimensions(file.path(dir, "plot-small.png")), c(384, 288))
   expect_equal(pngDimensions(file.path(dir, "plot-full.png")), c(768, 576))

   # a plot larger than the standard 1024 x 768 size keeps its layout, but is
   # drawn with fewer pixels so the page stays a reasonable size
   writeFixedPlotSize(TRUE, width = 30, height = 30, units = "in")
   expect_equal(dev.size("in"), c(30, 30))

   largeTarget <- .rs.invokeRpc("plots_create_rpubs_html", "Plot", "", 400L, 350L)
   largeDir <- dirname(path.expand(largeTarget))
   on.exit(unlink(largeDir, recursive = TRUE), add = TRUE)

   expect_equal(pngDimensions(file.path(largeDir, "plot-small.png")), c(768, 768))
   expect_equal(pngDimensions(file.path(largeDir, "plot-full.png")), c(1536, 1536))
})

# Draws on a throwaway device that records its display list, as the RStudio
# device does, and returns the alt text found on the current page.
withAltTextDevice <- function(code) {
   grDevices::pdf(NULL)
   on.exit(grDevices::dev.off(), add = TRUE)
   grDevices::dev.control("enable")
   force(code)
   .rs.graphicsAltText(grDevices::recordPlot()[[1]])
}

altTextGrob <- function(alt) {
   grob <- grid::rectGrob()
   attr(grob, "alt-label") <- alt
   grob
}

test_that("alt text attached to a drawn grob is found (#19337)", {
   altText <- withAltTextDevice({
      grid::grid.newpage()
      grid::grid.draw(altTextGrob("A bar chart of sales"))
   })
   expect_equal(altText, "A bar chart of sales")
})

test_that("alt text of several grobs on one page is combined (#19337)", {
   altText <- withAltTextDevice({
      grid::grid.newpage()
      grid::grid.draw(altTextGrob("First panel."))
      grid::grid.draw(altTextGrob(""))
      grid::grid.draw(altTextGrob("Second panel."))
   })
   expect_equal(altText, "First panel. Second panel.")
})

test_that("plots without alt text have none (#19337)", {
   expect_equal(withAltTextDevice(plot(1:10)), "")
   expect_equal(withAltTextDevice(grid::grid.draw(grid::rectGrob())), "")
   expect_equal(withAltTextDevice(grid::grid.draw(altTextGrob(NA_character_))), "")
   expect_equal(withAltTextDevice(grid::grid.draw(altTextGrob(c("a", "b")))), "")
})

test_that("alt text does not carry over to a later page (#19337)", {
   altText <- withAltTextDevice({
      grid::grid.newpage()
      grid::grid.draw(altTextGrob("A bar chart of sales"))
      plot(1:10)
   })
   expect_equal(altText, "")
})

test_that("alt text survives saving and restoring a plot (#19337)", {
   snapshot <- tempfile(fileext = ".snapshot")
   on.exit(unlink(snapshot), add = TRUE)

   altText <- withAltTextDevice({
      grid::grid.newpage()
      grid::grid.draw(altTextGrob("A bar chart of sales"))
      .rs.saveGraphics(snapshot)
      plot(1:10)
      .rs.restoreGraphics(snapshot)
   })
   expect_equal(altText, "A bar chart of sales")
})

test_that("ggplot2 alt text is found (#19337)", {
   skip_if_not_installed("ggplot2")
   plot <- ggplot2::ggplot(mtcars, ggplot2::aes(mpg, wt)) +
      ggplot2::geom_point() +
      ggplot2::labs(alt = "Weight against fuel economy")
   expect_equal(withAltTextDevice(print(plot)), "Weight against fuel economy")
})

test_that("display list entries with a failing `[[` method are skipped (#19337)", {
   failing <- structure(list(x = 1), class = "rstudioFailingSubset")
   registerS3method("[[", "rstudioFailingSubset",
                    function(x, i, ...) stop("`[[` should not be called"))
   displayList <- list(
      list(NULL, list(NULL, failing)),
      list(NULL, list(NULL, list(x = altTextGrob("A bar chart of sales"))))
   )
   expect_equal(.rs.graphicsAltText(displayList), "A bar chart of sales")
})
