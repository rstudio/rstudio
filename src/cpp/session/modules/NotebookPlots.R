#
# NotebookPlots.R
#
# Copyright (C) 2009-16 by RStudio, Inc.
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

.rs.addFunction("replayNotebookPlots", function(width) {
  # read the names of the snapshot files from standard input -- these are 
  # passed one per line from the parent process
  snapshots <- scan(what = character())

  require(grDevices, quietly=TRUE)

  lapply(snapshots, function(snapshot) {
    # create the PNG device on which we'll regenerate the plot -- it's somewhat
    # wasteful to use a separate device per plot, but the alternative is
    # doing a lot of state management and post-processing of the files
    # output from the device
    output <- paste(tools::file_path_sans_ext(snapshot), "resized.png",
                    sep = ".")
    png(file = output, width = width, height = width / 1.618, units = "px", 
        res = 96)

    # actually replay the plot onto the device
    tryCatch({
      .rs.restoreGraphics(snapshot)
    }, error = function(e) {
      # nothing reasonable we can do here; we'll just omit this file from the
      # output
    })

    # close the device; has the side effect of committing the plot to disk
    dev.off()

    # if the plot file was written out, emit to standard out for the caller to 
    # consume
    if (file.exists(output)) {
      # remove the old copy of the plot if it existed
      final <- paste(tools::file_path_sans_ext(snapshot), "png", sep = ".")
      if (file.exists(final))
        unlink(final)
      file.rename(output, final)
      if (file.exists(final)) {
        cat(final, "\n")
      }
    }
  })

  invisible(NULL)
})
