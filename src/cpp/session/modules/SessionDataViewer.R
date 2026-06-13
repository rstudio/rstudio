#
# SessionDataViewer.R
#
# Copyright (C) 2022 by Posit Software, PBC
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

# host environment for cached data; this allows us to continue to view data 
# even if the original object is deleted
.rs.setVar("CachedDataEnv", new.env(parent = emptyenv()))

# host environment for working data; this allows us to sort/filter/page the
# data without recomputing on the original object every time
.rs.setVar("WorkingDataEnv", new.env(parent = emptyenv()))

.rs.addFunction("subsetData", function(data, maxRows = -1, maxCols = -1)
{
   if (!is.na(maxRows) && maxRows != -1 && nrow(data) > maxRows)
      data <- head(data, n = maxRows)
   
   if (!is.na(maxCols) && maxCols != -1 && ncol(data) > maxCols)
      data <- data[1:maxCols]
   
   data
})

.rs.addFunction("formatDataColumn", function(x, start, len, ...)
{
   # extract the visible part of the column
   col <- x[start:min(NROW(x), start + len)]
   
   # if this object has a format method, use it. catch errors
   # and validate that the format method has given us something 'sane'
   formatted <- .rs.tryCatch(.rs.formatDataColumnDispatch(col, ...))
   if (is.character(formatted) && length(formatted) == length(col))
      return(formatted)
   
   # otherwise, delegate to internal methods
   if (is.numeric(col))
      .rs.formatDataColumnNumeric(col, ...)
   else if (is.list(col) && !is.data.frame(col))
      .rs.formatDataColumnList(col, ...)
   else
      .rs.formatDataColumnDefault(col, ...)
})

.rs.addFunction("formatDataColumnDispatch", function(col, ...)
{
   formatter <- NULL
   for (class in class(col))
   {
      formatter <- utils::getS3method(
         "format",
         class = class,
         optional = TRUE
      )
      if (!is.null(formatter))
         break
   }
   
   if (is.null(formatter))
      return(NULL)
   
   formatted <- formatter(col, trim = TRUE, justify = "none", ...)
   
   if (is.character(formatted) && length(formatted) == length(col))
      formatted[is.na(col)] <- NA
   
   formatted
   
})

.rs.addFunction("formatDataColumnNumeric", function(col, ...)
{
   # show numbers as doubles
   storage.mode(col) <- "double"
   
   # remember which values are NA 
   naVals <- is.na(col) 
   
   # format all the numeric values; this drops NAs (the na.encode option only
   # preserves NA for character cols)
   vals <- format(col, trim = TRUE, justify = "none", ...)
   
   # restore NA values if there were any
   if (any(naVals))
      vals[naVals] <- col[naVals]
   
   # return formatted values
   vals
})

.rs.addFunction("formatDataColumnList", function(col, ...)
{
   limit <- .rs.nullCoalesce(
      .rs.readUserPref("data_viewer_max_cell_size"),
      50L
   )
   
   # handle data.frame entries in a column
   # https://github.com/rstudio/rstudio/issues/14257
   for (i in seq_along(col))
   {
      if (is.data.frame(col[[i]]))
      {
         col[[i]] <- sprintf("<data.frame[%i x %i]>", nrow(col[[i]]), ncol(col[[i]]))
      }
      else
      {
         col[[i]] <- format(col[[i]])
      }
   }
   
   formatted <- as.character(col)
   na <- is.na(formatted)
   large <- !na & nchar(formatted) > limit
   formatted <- substr(formatted, 1, limit)
   formatted <- paste0(formatted, ifelse(large, " [...]", ""))
   formatted[na] <- NA_character_
   formatted
})

.rs.addFunction("formatDataColumnDefault", function(col, ...)
{
   # show everything else as characters
   as.character(col)
})

# Compact, collision-resistant hash of a frame's column structure. Used as a
# fingerprint to detect object reassignment between data viewer loads.
#
# The fingerprint covers column names, column classes, and factor levels --
# not just names. Saved filters are typed (numeric ranges, factor level
# indices), so a column changing type, or a factor being re-leveled, with
# unchanged names must also invalidate saved state; otherwise a restored
# filter is silently applied with the wrong semantics (an inexplicably empty
# grid, or a factor filter matching the wrong level).
#
# Returns NA_character_ for empty / NULL names so the client can treat
# "no anchor" as always-mismatch; without anchors there's no way to
# align saved positional state with the current frame, so we want it
# discarded.
.rs.addFunction("dataViewer.colsFingerprint", function(x)
{
   nms <- names(x)
   n <- length(nms)
   if (n == 0L)
      return(NA_character_)

   sig <- list(names = nms)
   if (is.list(x))
   {
      sig$types <- vapply(x, function(col) {
         type <- paste(class(col), collapse = "/")
         if (is.factor(col))
            type <- paste(c(type, levels(col)), collapse = "/")
         type
      }, character(1), USE.NAMES = FALSE)
   }

   paste0(n, ":", .rs.digest(sig))
})

#' Column Names
#'
#' Return the column names of a data object as a character vector, for the
#' data viewer's go-to-column popup. Lightweight by design: no per-column
#' statistics, just names -- the popup needs every column of the frame, not
#' only the fetched window. Unnamed or missing entries come back as empty
#' strings so positions stay aligned with column indices.
#'
#' @param x The data object being viewed.
.rs.addFunction("dataViewer.columnNames", function(x)
{
   nms <- colnames(x)
   if (is.null(nms))
      nms <- character(.rs.ncol(x))

   nms <- as.character(nms)
   nms[is.na(nms)] <- ""
   nms
})

#' Column Index
#'
#' Return lightweight identity for every column of a data object -- name,
#' typeof, and class -- for the data viewer's summary sidebar, which lists all
#' columns of the frame and lazy-loads the (expensive) per-column statistics
#' as entries scroll into view. Deliberately cheap: no histograms, ranges, or
#' factor levels, so this scales to very wide frames where describeCols would
#' not. The result is a list with one entry per column, parallel to the
#' frame's columns.
#'
#' @param x The data object being viewed.
# The timezone a POSIXct column is displayed in. Returns "" for non-POSIXct
# columns. An absent or empty tzone attribute means the column is shown in the
# session's local timezone, so resolve that to a concrete name (Sys.timezone())
# rather than reporting a blank -- the client surfaces this next to the column
# type so two columns in different zones don't read identically.
.rs.addFunction("dataViewer.columnTimezone", function(col)
{
   if (!inherits(col, "POSIXct"))
      return("")

   tz <- attr(col, "tzone")
   if (is.null(tz) || !nzchar(tz))
      tz <- .rs.tryOr("", Sys.timezone())

   if (is.null(tz) || is.na(tz) || !nzchar(tz))
      return("")

   tz
})

.rs.addFunction("dataViewer.columnIndex", function(x)
{
   nms <- .rs.dataViewer.columnNames(x)

   # A non-list object (matrix, atomic vector coerced to a frame upstream) has
   # no per-column classes to report; treat every column as the object's own
   # class so the client's type predicates still resolve.
   columns <- lapply(seq_along(nms), function(idx) {
      col <- if (is.list(x)) x[[idx]] else x
      list(
         col_name  = .rs.scalar(nms[[idx]]),
         col_type  = .rs.scalar(typeof(col)),
         col_class = as.character(class(col)),
         col_index = .rs.scalar(as.integer(idx)),
         col_tz    = .rs.scalar(.rs.dataViewer.columnTimezone(col))
      )
   })

   columns
})

.rs.addFunction("describeCols", function(x,
                                         maxRows = -1,
                                         maxCols = -1,
                                         maxFactors = 64,
                                         totalCols = -1,
                                         colIndices = NULL,
                                         colsFingerprint = NULL)
{
   # The client compares this fingerprint against the one stored alongside
   # saved per-object UI state (pins, widths, sort, filters, sidebar
   # visibility). A mismatch invalidates the saved state -- without it,
   # positional indices saved against one frame can land on an unrelated
   # frame after reassignment (df <- iris; df <- mtcars). Callers that have
   # already subset x (e.g. describeColsByIndex) must compute the fingerprint
   # from the underlying frame and pass it in, otherwise pagination would
   # silently invalidate state on every column-frame change.
   if (is.null(colsFingerprint))
      colsFingerprint <- .rs.dataViewer.colsFingerprint(x)

   # subset the data if requested
   x <- .rs.subsetData(x, maxRows, maxCols)

   # absolute (1-based) column indices for the columns being described. The
   # client uses these as the stable identity for pinning, sorting, and
   # filtering, so they survive column pagination (callers that describe an
   # arbitrary slice -- describeColsByIndex -- pass the original indices in).
   # Default to a contiguous range for the full-frame case.
   if (is.null(colIndices))
      colIndices <- seq_len(ncol(x))

   # get the variable labels, if any--labels may be provided either by this 
   # global attribute or by a 'label' attribute on an individual column (as in
   # e.g. Hmisc), which takes precedence if both are present
   colNames <- names(x)
   colLabels <- attr(x, "variable.labels", exact = TRUE)
   if (!is.character(colLabels)) 
      colLabels <- character()
   
   # we pass totalCols in the rownames col so we can pass this information
   # along when we retrieve column data, without changing the response format
   totalCols <- if (totalCols > 0) totalCols else ncol(x)

   # Cap the col_max_chars hint computation at this many columns. Above
   # the cap, each per-column scan would add up; the client falls back to
   # sampling the visible rows instead.
   maxCharsCap <- 200L
   computeMaxChars <- ncol(x) > 0 && ncol(x) <= maxCharsCap

   # Maximum number of distinct values for which the summary sidebar draws
   # one frequency bar per category; above this, a factor / character column
   # gets a text summary (count of uniques + dominant value) instead.
   maxCategoryBars <- 24L

   # Character columns are only categorized (a full table() hash) when they
   # have at most this many rows. The option is user-supplied, so validate
   # it here; a malformed value (NA, wrong length, non-numeric) must not be
   # able to break the whole describe call.
   maxCategorizeRows <- getOption("rstudio.dataViewer.maxCategorizeRows", 1e6)
   if (!is.numeric(maxCategorizeRows) || length(maxCategorizeRows) != 1 || is.na(maxCategorizeRows))
      maxCategorizeRows <- 1e6

   # Cheap upper bound on the displayed character count for a column.
   # Only handles types where the bound is a property of range/levels/type
   # rather than a per-element format() call. Returns NA if the column type
   # is not a known-cheap case so the client falls back to sampling.
   colMaxChars <- function(col) {
      if (length(col) == 0L)
         return(NA_integer_)
      if (is.logical(col))
         return(5L)  # "FALSE"
      if (inherits(col, "Date"))
         return(10L) # "YYYY-MM-DD"
      if (inherits(col, "POSIXt"))
         return(19L) # "YYYY-MM-DD HH:MM:SS"
      if (is.factor(col)) {
         lvls <- levels(col)
         if (length(lvls) == 0L) return(0L)
         return(max(nchar(lvls, type = "width"), na.rm = TRUE))
      }
      if (is.character(col))
         return(max(nchar(col, type = "width"), 0L, na.rm = TRUE))
      if (is.integer(col)) {
         rng <- suppressWarnings(range(col, na.rm = TRUE))
         if (!all(is.finite(rng))) return(NA_integer_)
         return(max(nchar(as.character(rng))))
      }
      if (is.numeric(col) && !is.object(col)) {
         # Treat doubles whose values are all integral the same as integer;
         # avoid format() on the full column otherwise (expensive and
         # options-dependent).
         finiteCol <- col[is.finite(col)]
         if (length(finiteCol) == 0L) return(NA_integer_)
         if (all(finiteCol == trunc(finiteCol))) {
            rng <- range(finiteCol)
            return(max(nchar(as.character(rng))))
         }
         return(NA_integer_)
      }
      NA_integer_
   }

   # the first column is always the row names
   rowNameCol <- list(
      col_name        = .rs.scalar(""),
      col_type        = .rs.scalar("rownames"),
      col_min         = .rs.scalar(0),
      col_max         = .rs.scalar(0),
      col_search_type = .rs.scalar("none"),
      col_label       = .rs.scalar(""),
      col_vals        = "",
      col_class       = "rownames",
      col_na_count    = .rs.scalar(0),
      col_index       = .rs.scalar(0L),
      total_cols      = .rs.scalar(totalCols),
      total_rows      = .rs.scalar(nrow(x)),
      cols_fingerprint = .rs.scalar(colsFingerprint))

   # Add a max-chars hint for the row names column. Use .row_names_info()
   # so we avoid materializing the full row-name vector for the common
   # case of automatic rownames -- type=1L returns a signed row count:
   # negative for the compact c(NA, -n) form (auto), positive otherwise.
   if (computeMaxChars) {
      rnInfo <- .row_names_info(x, type = 1L)
      rnChars <- NA_integer_
      if (is.integer(rnInfo) && length(rnInfo) == 1L && !is.na(rnInfo)) {
         if (rnInfo < 0L) {
            # Automatic rownames: widest displayed value is the row count.
            rnChars <- nchar(as.character(-rnInfo))
         } else if (rnInfo > 0L) {
            # Explicit rownames -- cheap to bound for integer/character.
            rnAttr <- attr(x, "row.names", exact = TRUE)
            if (is.integer(rnAttr)) {
               rng <- suppressWarnings(range(rnAttr, na.rm = TRUE))
               if (all(is.finite(rng)))
                  rnChars <- max(nchar(as.character(rng)))
            } else if (is.character(rnAttr)) {
               rnChars <- max(nchar(rnAttr, type = "width"), 0L, na.rm = TRUE)
            }
         }
      }
      if (!is.na(rnChars))
         rowNameCol$col_max_chars <- .rs.scalar(as.integer(rnChars))
   }
   
   # if there are no columns, bail out
   if (length(colNames) == 0) {
      return(rowNameCol)
   }
   
   # get the attributes for each column
   colAttrs <- lapply(seq_along(colNames), function(idx) {
      col_name <- if (idx <= length(colNames)) 
         colNames[idx] 
      else 
         as.character(idx)
      # col_type and col_class are reported faithfully from the column itself
      # (typeof / class); the cascade below only derives the search type and
      # the histogram / factor-level metadata used by the client.
      col_type <- typeof(x[[idx]])
      col_class <- class(x[[idx]])
      col_breaks <- c()
      col_counts <- c()
      col_min <- NULL
      col_max <- NULL
      col_vals <- ""
      col_search_type <- ""
      col_cat_vals <- NULL
      col_cat_counts <- NULL
      col_n_unique <- NULL
      col_top_value <- NULL
      col_top_count <- NULL
      # Date/POSIXct extras: formatted labels parallel to col_breaks plus the
      # true min/max, so the client can show dates without re-doing timezone
      # math, and the column's display timezone (POSIXct only).
      col_break_labels <- NULL
      col_min_label <- NULL
      col_max_label <- NULL
      col_tz <- .rs.dataViewer.columnTimezone(x[[idx]])
      
      # extract label, if any, or use global label, if any
      label <- attr(x[[idx]], "label", exact = TRUE)
      col_label <- if (is.character(label))
      {
         label
      }
      else if (idx <= length(colLabels))
      {
         if (!is.null(names(colLabels)))
         {
            if (col_name %in% names(colLabels))
               colLabels[[col_name]]
            else
               ""
         }
         else
         {
            colLabels[[idx]]
         }
      }
      else
      {
         ""
      }
      
      # ensure that the column contains some scalar values we can examine 
      if (length(x[[idx]]) > 0)
      {
         val <- x[[idx]][[1]]
         if (is.factor(val))
         {
            # we previously used the 'maxFactors' variable to try and guess
            # where a factor variable might have actually been intended to be
            # used as a character vector. nowdays, with stringsAsFactors = FALSE
            # being the default, this is no longer necessary and so we just
            # ignore the 'maxFactors' parameter.
            #
            # https://github.com/rstudio/rstudio/issues/14113
            col_search_type <- "factor"
            col_vals <- levels(val)

            # per-level counts for the sidebar's category mini-plot: cheap
            # via tabulate() on the underlying integer codes. at most
            # maxCategoryBars bars are drawn (in level order, preserving
            # ordinal structure); above that the sidebar falls back to a
            # text summary, so only the dominant level is shipped.
            #
            # gate on the column type, not val: list columns whose first
            # element is a factor reach this branch, and as.integer() on a
            # list is an error. the counts are decorative (the client
            # degrades gracefully without them), so any failure just drops
            # them instead of failing the whole describe call
            status <- .rs.tryCatch({
               if (is.factor(x[[idx]]) && length(col_vals) > 0)
               {
                  counts <- tabulate(as.integer(x[[idx]]), nbins = length(col_vals))
                  if (length(col_vals) <= maxCategoryBars)
                  {
                     col_cat_vals <- col_vals
                     col_cat_counts <- counts
                  }
                  else if (any(counts > 0))
                  {
                     top <- which.max(counts)
                     col_top_value <- col_vals[[top]]
                     col_top_count <- counts[[top]]
                  }
               }
            })

            if (inherits(status, "error"))
            {
               col_cat_vals <- NULL
               col_cat_counts <- NULL
               col_top_value <- NULL
               col_top_count <- NULL
               .rs.logErrorMessage(
                  "Error computing level counts for column '%s': %s",
                  col_name,
                  conditionMessage(status))
            }
         }
         # for histograms, we support only the base R numeric class and its derivatives;
         # is.numeric can return true for values that can only be manipulated using
         # packages that are currently loaded (e.g. bit64's integer64)
         else if (is.numeric(x[[idx]]) && !is.object(x[[idx]]))
         {
            # ignore missing and infinite values (i.e. let any filter applied
            # implicitly remove those values); if that leaves us with nothing,
            # treat this column as untyped since we can do no meaningful filtering
            # on it
            #
            # coerce to double up front: integer columns whose range exceeds
            # .Machine$integer.max otherwise overflow both in the range
            # computation below and in hist()'s break selection (#17951)
            hist_vals <- as.numeric(x[[idx]][is.finite(x[[idx]])])
            if (length(hist_vals) > 1)
            {
               # For whole-number columns spanning a small range, draw one
               # bar per integer value (so e.g. a 1-5 Likert column shows 5
               # distinct bars rather than Sturges' default binning, which
               # smears the discrete structure). Gaps in the range get a
               # zero-height bar, which is itself informative.
               int_breaks <- NULL
               min_v <- min(hist_vals)
               max_v <- max(hist_vals)
               n_distinct <- max_v - min_v + 1
               if (n_distinct <= 12 && isTRUE(all(hist_vals %% 1 == 0)))
                  int_breaks <- seq(min_v - 0.5, max_v + 0.5, by = 1)

               # create histogram for brushing -- suppress warnings as in rare cases
               # an otherwise benign integer overflow can occurs; see
               # https://github.com/rstudio/rstudio/issues/3232
               h <- if (is.null(int_breaks))
                  suppressWarnings(graphics::hist(hist_vals, plot = FALSE))
               else
                  suppressWarnings(graphics::hist(hist_vals, breaks = int_breaks, plot = FALSE))
               col_breaks <- h$breaks
               col_counts <- h$counts

               # the actual (finite) data range; the histogram breaks can
               # extend past it (pretty()-ed for default binning, padded by
               # 0.5 on each side for integer bins). shown in the column
               # summary sidebar, where the sparkline has no axis ticks
               col_min <- min_v
               col_max <- max_v

               # record search type
               col_search_type <- "numeric"
            }
         }
         else if (inherits(x[[idx]], "Date") || inherits(x[[idx]], "POSIXct"))
         {
            # Date/POSIXct get the same brushable histogram + range filter as
            # numeric columns. hist() needs a plain numeric, so bin on the
            # epoch representation (days since 1970 for Date, seconds since
            # 1970 for POSIXct). The numeric epoch breaks drive the sparkline
            # and brush geometry; the parallel col_break_labels (and the
            # min/max labels) carry the formatted dates for display.
            col_obj <- x[[idx]]
            epoch <- as.numeric(col_obj[is.finite(col_obj)])
            if (length(epoch) > 1)
            {
               h <- suppressWarnings(graphics::hist(epoch, plot = FALSE))
               col_breaks <- h$breaks
               col_counts <- h$counts
               col_search_type <- "date"

               # Format the breaks and the true data range in one pass, using
               # the column's own class/timezone so the labels match how the
               # cells render. col_min/col_max are left NULL (the numeric
               # footer path keys off their being numbers); the date footer
               # uses col_min_label/col_max_label instead.
               vals <- c(min(epoch), max(epoch), col_breaks)
               labels <- if (inherits(col_obj, "Date"))
                  # as.numeric(Date) is whole days since 1970-01-01; days carry
                  # no timezone, so the origin idiom is unambiguous here.
                  format(as.Date(vals, origin = "1970-01-01"))
               else
                  # as.numeric(POSIXct) is seconds since the 1970 UTC epoch.
                  # .POSIXct() builds directly from those seconds (no origin
                  # string to be misparsed in local time), tagging the given
                  # tzone for display.
                  format(.POSIXct(vals, tz = if (nzchar(col_tz)) col_tz else ""))

               col_min_label <- labels[[1]]
               col_max_label <- labels[[2]]
               col_break_labels <- labels[-(1:2)]
            }
         }
         else if (inherits(x[[idx]], "integer64"))
         {
            col_search_type <- "character"
         }
         else if (is.character(val))
         {
            col_search_type <- "character"

            # category counts for the sidebar mini-plot. unlike factors this
            # costs a full hash of the column (table()), so very long columns
            # skip it and the sidebar shows no categorical summary. at most
            # maxCategoryBars distinct values draws one bar per value, most
            # frequent first (no natural order to preserve); above that only
            # the text summary fields (count of uniques, dominant value) are
            # shipped.
            #
            # gate on the column type, not val: list columns whose first
            # element is a character vector reach this branch, and table()
            # on a list either errors (ragged elements) or counts the wrong
            # thing. decorative only, so any failure just drops the fields
            # instead of failing the whole describe call
            status <- .rs.tryCatch({
               if (is.character(x[[idx]]) && length(x[[idx]]) <= maxCategorizeRows)
               {
                  counts <- table(x[[idx]], useNA = "no")
                  col_n_unique <- length(counts)
                  if (col_n_unique > 0 && col_n_unique <= maxCategoryBars)
                  {
                     counts <- sort(counts, decreasing = TRUE)
                     col_cat_vals <- names(counts)
                     col_cat_counts <- as.integer(counts)
                  }
                  else if (col_n_unique > 0)
                  {
                     top <- which.max(counts)
                     col_top_value <- names(counts)[[top]]
                     col_top_count <- as.integer(counts[[top]])
                  }
               }
            })

            if (inherits(status, "error"))
            {
               col_cat_vals <- NULL
               col_cat_counts <- NULL
               col_n_unique <- NULL
               col_top_value <- NULL
               col_top_count <- NULL
               .rs.logErrorMessage(
                  "Error computing category counts for column '%s': %s",
                  col_name,
                  conditionMessage(status))
            }
         }
         else if (is.logical(val))
         {
            col_search_type <- "boolean"
         }
      }
      # count NA values
      col_na_count <- sum(is.na(x[[idx]]))

      result <- list(
         col_name        = .rs.scalar(col_name),
         col_type        = .rs.scalar(col_type),
         col_breaks      = as.character(col_breaks),
         col_counts      = col_counts,
         col_search_type = .rs.scalar(col_search_type),
         col_label       = .rs.scalar(col_label),
         col_vals        = col_vals,
         col_class       = as.character(col_class),
         col_na_count    = .rs.scalar(col_na_count),
         col_index       = .rs.scalar(as.integer(colIndices[idx]))
      )

      # data range, present only for histogram-summarized numeric columns
      if (!is.null(col_min)) {
         result$col_min <- .rs.scalar(col_min)
         result$col_max <- .rs.scalar(col_max)
      }

      # Date/POSIXct display metadata: formatted break labels (parallel to the
      # numeric col_breaks), the formatted data range for the sidebar footer,
      # and the column's display timezone.
      if (!is.null(col_break_labels))
         result$col_break_labels <- as.character(col_break_labels)
      if (!is.null(col_min_label)) {
         result$col_min_label <- .rs.scalar(col_min_label)
         result$col_max_label <- .rs.scalar(col_max_label)
      }
      if (nzchar(col_tz))
         result$col_tz <- .rs.scalar(col_tz)

      # category metadata for the sidebar: bar values/counts at or below
      # the maxCategoryBars cutoff, dominant-value fields above it; the
      # distinct-value count ships whenever it was computed (character
      # columns within the row gate), regardless of the cutoff
      if (!is.null(col_cat_counts)) {
         result$col_cat_vals <- as.character(col_cat_vals)
         result$col_cat_counts <- as.integer(col_cat_counts)
      }
      if (!is.null(col_n_unique))
         result$col_n_unique <- .rs.scalar(as.integer(col_n_unique))
      if (!is.null(col_top_value)) {
         result$col_top_value <- .rs.scalar(col_top_value)
         result$col_top_count <- .rs.scalar(as.integer(col_top_count))
      }

      # Optionally include a cheap upper bound on displayed character count.
      # The client uses this for initial column width sizing instead of
      # sampling rows; absence of the field means "sample on the client."
      if (computeMaxChars) {
         maxCh <- colMaxChars(x[[idx]])
         if (!is.na(maxCh))
            result$col_max_chars <- .rs.scalar(as.integer(maxCh))
      }

      result
   })
   c(list(rowNameCol), colAttrs)
})

.rs.addFunction("describeColsByIndex", function(x, indices)
{
   totalCols <- ncol(x)
   if (is.null(totalCols) || totalCols == 0)
      return(NULL)

   # Keep only valid, in-range column indices (1-based). The client sends an
   # arbitrary ordered set -- pinned columns followed by the visible window --
   # so this is not necessarily contiguous. Fall back to the full frame if the
   # request is empty or entirely out of range.
   indices <- indices[!is.na(indices) & indices >= 1 & indices <= totalCols]
   if (length(indices) == 0)
      indices <- seq_len(totalCols)

   colSlice <- x[indices]

   # Make sure we preserve variable.labels if set.
   #
   # The structure of 'variable.labels' is not documented,
   # but it appears that the expectation is that it's a character
   # vector of the same length as 'x'.
   #
   # The vector can be optionally named (with names matching that of 'x'),
   # or it can be an unnamed vector -- in which case, the order of labels
   # needs to match the column order of 'x'.
   #
   # https://github.com/rstudio/rstudio/issues/14265
   colLabels <- attr(x, "variable.labels", exact = TRUE)
   if (!is.null(colLabels))
   {
      # Only subset 'variable.labels' if it's not named, since a named
      # attribute could potentially be in a different order than the
      # columns of 'x' itself. If 'variable.labels' is named, then all
      # we require is that it's a super-set of the names of 'x'.
      if (is.null(names(colLabels)))
         colLabels <- colLabels[indices]
      
      attr(colSlice, "variable.labels") <- colLabels
   }
      
   
   # Pass the fingerprint of the full frame so pagination doesn't fold
   # each page into a distinct fingerprint -- doing so would invalidate
   # saved UI state on every page change. The original (absolute) indices
   # are passed through as col_index so the client can track pinned/sorted/
   # filtered columns by identity rather than by their position in the slice.
   .rs.describeCols(
      x                = colSlice,
      maxRows          = -1,
      maxCols          = -1,
      maxFactors       = 64,
      totalCols        = totalCols,
      colIndices       = indices,
      colsFingerprint  = .rs.dataViewer.colsFingerprint(x)
   )
})

.rs.addFunction("summarizeColumn", function(x, columnIndex)
{
   # columnIndex is 1-based (R convention)
   if (columnIndex < 1 || columnIndex > ncol(x))
      return(list(error = .rs.scalar("Column index out of range")))

   col <- x[[columnIndex]]
   n <- length(col)

   # Each statistic is wrapped individually: exotic numerics that pass
   # is.numeric (difftime, S4 numerics, units::units, ...) and columns
   # whose is.na returns a non-logical can throw on a single call. With
   # error-surfacing in place at the C++ layer, a single throw would blank
   # the entire panel rather than degrading gracefully.
   nonNa <- .rs.tryOr(NULL, col[!is.na(col)])

   result <- list(
      n        = .rs.scalar(n),
      n_na     = .rs.scalar(.rs.tryOr(NA_integer_, sum(is.na(col)))),
      n_unique = .rs.scalar(.rs.tryOr(NA_integer_, length(unique(nonNa))))
   )

   if (is.numeric(col) && !is.factor(col))
   {
      if (!is.null(nonNa) && length(nonNa) > 0)
      {
         result$min    <- .rs.scalar(.rs.tryOr(NULL, min(nonNa)))
         result$max    <- .rs.scalar(.rs.tryOr(NULL, max(nonNa)))
         result$mean   <- .rs.scalar(.rs.tryOr(NULL, mean(nonNa)))
         result$median <- .rs.scalar(.rs.tryOr(NULL, median(nonNa)))
         result$sd     <- .rs.scalar(.rs.tryOr(NULL, sd(nonNa)))
      }
   }
   else if (is.character(col))
   {
      if (!is.null(nonNa) && length(nonNa) > 0)
      {
         lens <- .rs.tryOr(NULL, nchar(nonNa))
         if (!is.null(lens))
         {
            result$min_length <- .rs.scalar(.rs.tryOr(NULL, min(lens)))
            result$max_length <- .rs.scalar(.rs.tryOr(NULL, max(lens)))
            result$n_empty    <- .rs.scalar(.rs.tryOr(NULL, sum(lens == 0)))
         }
      }
   }
   else if (is.factor(col))
   {
      # Display levels in their R-encoded order to preserve ordered factors
      # and any user-set order. When the level count exceeds the cap we
      # truncate to the first N by encoding order, not the N most frequent.
      maxLevels <- 50L
      tbl <- .rs.tryOr(NULL, table(col, useNA = "no"))
      lvls <- .rs.tryOr(NULL, levels(col))
      if (!is.null(tbl) && !is.null(lvls))
      {
         if (length(lvls) > maxLevels)
         {
            lvls <- lvls[seq_len(maxLevels)]
            result$truncated <- .rs.scalar(TRUE)
         }
         result$top_levels  <- lvls
         result$top_counts  <- as.integer(tbl[lvls])
      }
   }
   else if (is.logical(col))
   {
      result$n_true  <- .rs.scalar(.rs.tryOr(NULL, sum(col == TRUE,  na.rm = TRUE)))
      result$n_false <- .rs.scalar(.rs.tryOr(NULL, sum(col == FALSE, na.rm = TRUE)))
   }
   else if (inherits(col, "Date") || inherits(col, "POSIXct"))
   {
      if (!is.null(nonNa) && length(nonNa) > 0)
      {
         result$min <- .rs.scalar(.rs.tryOr(NULL, as.character(min(nonNa))))
         result$max <- .rs.scalar(.rs.tryOr(NULL, as.character(max(nonNa))))
      }

      # Surface the display timezone (POSIXct only) so the expanded summary can
      # show it alongside min/max.
      tz <- .rs.dataViewer.columnTimezone(col)
      if (nzchar(tz))
         result$tz <- .rs.scalar(tz)
   }

   result
})

.rs.addFunction("formatRowNames", function(x, start, len) 
{
   # check for a data.frame with compact row names
   if (.rs.hasCompactRowNames(x))
   {
      # the second element indicates the number of rows, and
      # is negative if they're so-called "automatic" row names
      info <- .row_names_info(x, type = 0L)
      n <- abs(info[[2L]])
      range <- seq(from = start, to = min(n, start + len))
      return(as.character(range))
   }
   
   # retrieve row names; use .row_names_info for data.frame so
   # we can detect internal non-character row names
   rowNames <- if (is.data.frame(x))
   {
      .row_names_info(x, type = 0L)
   }
   else
   {
      row.names(x)
   }
   
   # subset the retrieved row names
   rowNames <- rowNames[start:min(length(rowNames), start + len)]
   
   # encode strings as JSON to force quoting + handle escaping
   # this also lets us differentiate numeric (automatic) row names
   # from explicitly-set row names
   if (is.character(rowNames))
   {
      .rs.mapChr(rowNames, .rs.toJSON, unbox = TRUE)
   }
   else
   {
      as.character(rowNames)
   }
})

# wrappers for nrow/ncol which will report the class of object for which we
# fail to get dimensions along with the original error
.rs.addFunction("nrow", function(x)
{
   rows <- 0
   tryCatch({
      rows <- NROW(x)
   }, error = function(e) {
      stop("Failed to determine rows for object of class '", class(x), "': ", 
           e$message)
   })
   if (is.null(rows))
      0
   else
      rows
})

.rs.addFunction("ncol", function(x)
{
   cols <- 0
   tryCatch({
      cols <- NCOL(x)
   }, error = function(e) {
      stop("Failed to determine columns for object of class '", class(x), "': ", 
           e$message)
   })
   if (is.null(cols))
      0
   else
      cols
})

.rs.addFunction("toDataFrame", function(x, name, flatten)
{
   # force a non-subclassed data.frame -- this is necessary to ensure
   # that row names (or row numbers) are not dropped when subsetting
   # data, since those row names are used when generating cell-specific
   # callbacks (e.g. for viewing a cell of a list column)
   if (is.data.frame(x))
   {
      class(x) <- "data.frame"
   }
   
   # if it's not already a frame, coerce it to a frame
   if (!is.data.frame(x))
   {
      frame <- NULL
      # attempt to coerce to a data frame--this can throw errors in the case
      # where we're watching a named object in an environment and the user
      # replaces an object that can be coerced to a data frame with one that
      # cannot
      tryCatch(
         {
            # create a temporary frame to hold the value; this is necessary because
            # "x" is a function argument and therefore a promise whose value won't
            # be bound via substitute() below. we use a random-looking name so we 
            # can spot it later when relabeling columns.
            `__RSTUDIO_VIEWER_COLUMN__` <- x
            
            # perform the actual coercion in the global environment; this is 
            # necessary because we want to honor as.data.frame overrides of packages
            # which are loaded after tools:rstudio in the search path
            frame <- eval(substitute(as.data.frame(`__RSTUDIO_VIEWER_COLUMN__`, 
                                                   optional = TRUE)), 
                          envir = globalenv())
         },
         error = function(e)
         {
         })
      
      # as.data.frame uses the name of its argument to label unlabeled columns,
      # so label these back to the original name
      if (!is.null(frame) && !is.null(names(frame)))
         names(frame)[names(frame) == "__RSTUDIO_VIEWER_COLUMN__"] <- name
      x <- frame 
   }
   
   # if coercion was successful (or we started with a frame), flatten the frame
   # if necessary and requested
   if (is.data.frame(x)) {
      
      # generate column names if we didn't have any to start
      if (is.null(names(x)))
         names(x) <- paste("V", seq_along(x), sep = "")
      
      if (flatten)
         x <- .rs.flattenFrame(x)
      
      return(x)
   }
})

.rs.addFunction("multiCols", function(x) {
   fun <- function(col) is.data.frame(col) || is.matrix(col)
   which(vapply(x, fun, TRUE))
})

# given a 'data.frame' containing columns which themselves have
# multiple columns (e.g. matrices, data.frames), expand those columns
# such that we have a 'data.frame' with the nested columns e
.rs.addFunction("flattenFrame", function(x)
{
   # skip if we don't have any rectangular columns;
   # in this case, we can return the data as-is
   isRectangular <- vapply(x, function(column) {
      is.data.frame(column) || is.matrix(column)
   }, FUN.VALUE = logical(1))
   
   if (!any(isRectangular))
      return(x)
   
   # split into separate data.frames
   stack <- .rs.stack()
   .rs.enumerate(x, .rs.dataViewer.flatten, stack = stack)
   parts <- stack$data()
   
   # pull out pieces we need
   keys <- vapply(parts, `[[`, "name", FUN.VALUE = "character")
   vals <- lapply(parts, `[[`, "value")
   
   # turn it into a data.frame
   names(vals) <- keys
   attr(vals, "row.names") <- .set_row_names(length(vals[[1L]]))
   class(vals) <- "data.frame"
   
   # all done
   vals
})

.rs.addFunction("dataViewer.flatten", function(name, value, stack)
{
   if (is.matrix(value)) {
      .rs.dataViewer.flattenMatrix(name, value, stack)
   } else if (is.data.frame(value)) {
      .rs.dataViewer.flattenDataFrame(name, value, stack)
   } else {
      stack$push(list(name = name, value = value))
   }
})

.rs.addFunction("dataViewer.flattenMatrix", function(name, value, stack)
{
   colNames <- if (is.null(colnames(value)))
      as.character(seq_len(ncol(value)))
   else
      encodeString(colnames(value), quote = '"')
   
   for (i in seq_len(ncol(value))) {
      .rs.dataViewer.flatten(
         name  = sprintf("%s[, %s]", name, colNames[[i]]),
         value = value[, i, drop = TRUE],
         stack = stack
      )
   }
})

.rs.addFunction("dataViewer.flattenDataFrame", function(name, value, stack)
{
   # a data.frame should almost always have names, but check just in case
   colNames <- names(value)
   if (is.null(colNames))
      colNames <- sprintf("<%i>", seq_along(value))
   
   for (i in seq_along(value)) {
      .rs.dataViewer.flatten(
         name  = paste(name, colNames[[i]], sep = "$"),
         value = value[[i]],
         stack = stack
      )
   }
})

.rs.addFunction("applyTransform", function(x, filtered, search, cols, dirs)
{
   # mark encoding on character inputs if not already marked
   filtered <- vapply(filtered, function(colfilter) {
      if (Encoding(colfilter) == "unknown") 
         Encoding(colfilter) <- "UTF-8"
      colfilter
   }, "")
   
   if (Encoding(search) == "unknown")
      Encoding(search) <- "UTF-8"
   
   # coerce argument to data frame--data.table objects (for example) report that
   # they're data frames, but don't actually support the subsetting operations
   # needed for search/sort/filter without an explicit cast
   #
   # similarly, we need to convert tibbles to regular data.frames so that we can
   # properly invoke the list / data viewer on filtered rows
   x <- .rs.toDataFrame(x, "transformed", TRUE)
   
   # apply columnwise filters
   for (i in seq_along(filtered))
   {
      if (nchar(filtered[i]) > 0 && length(x[[i]]) > 0)
      {
         # split filter--string format is "type|value" (e.g. "numeric|12-25") 
         filter <- strsplit(filtered[i], split = "|", fixed = TRUE)[[1]]
         if (length(filter) < 2) 
         {
            # no filter type information
            next
         }
         filtertype <- filter[1]
         filterval <- filter[2]
         
         # apply filter appropriate to type
         if (identical(filtertype, "factor")) 
         {
            # apply factor filter: convert to numeric values and discard missing
            filterval <- as.numeric(filterval)
            matches <- as.numeric(x[[i]]) == filterval
            matches[is.na(matches)] <- FALSE
            x <- x[matches, , drop = FALSE]
         }
         else if (identical(filtertype, "character"))
         {
            # apply character filter: non-case-sensitive prefix
            # use PCRE and the special \Q and \E escapes to ensure no characters in
            # the search expression are interpreted as regexes 
            x <- x[grepl(paste("\\Q", filterval, "\\E", sep = ""), x[[i]], 
                         perl = TRUE, ignore.case = TRUE), , 
                   drop = FALSE]
         } 
         else if (identical(filtertype, "numeric"))
         {
            # apply numeric filter, range ("2-32") or equality ("15")
            filterval <- as.numeric(strsplit(filterval, "_")[[1]])
            if (length(filterval) > 1)
               # range filter
               x <- x[is.finite(x[[i]]) & x[[i]] >= filterval[1] & x[[i]] <= filterval[2], , drop = FALSE]
            else
               # equality filter
               x <- x[is.finite(x[[i]]) & x[[i]] == filterval, , drop = FALSE]
         }
         else if (identical(filtertype, "date"))
         {
            # apply date/datetime range filter. The client sends two formatted
            # endpoints (the same ISO strings it displays) separated by "_";
            # parse them with the column's own class so comparison happens on
            # the native Date/POSIXct scale. A parse failure leaves x unchanged
            # rather than silently dropping every row.
            bounds <- strsplit(filterval, "_")[[1]]
            if (length(bounds) >= 2)
            {
               col <- x[[i]]
               parsed <- .rs.tryCatch({
                  if (inherits(col, "Date"))
                     as.Date(bounds[1:2])
                  else
                     as.POSIXct(bounds[1:2], tz = .rs.dataViewer.columnTimezone(col))
               })

               if (!inherits(parsed, "error") && !any(is.na(parsed)))
                  x <- x[!is.na(col) & col >= parsed[1] & col <= parsed[2], , drop = FALSE]
            }
         }
         else if (identical(filtertype, "boolean"))
         {
            filterval <- isTRUE(filterval == "TRUE")
            matches <- x[[i]] == filterval
            matches[is.na(matches)] <- FALSE
            x <- x[matches, , drop = FALSE]
         }
      }
   }
   
   # apply global search
   if (!is.null(search) && nchar(search) > 0)
   {
      # get columns for search
      searchColumns <- unclass(x)
      
      # also apply on row names if available
      if (is.data.frame(x))
      {
         info <- .row_names_info(x, type = 0L)
         if (is.character(info))
         {
            searchColumns[[length(searchColumns) + 1]] <- info
         }
      }
      
      # apply global search on data columns
      pattern <- paste0("\\Q", search, "\\E")
      matches <- lapply(searchColumns, function(column) {
         grepl(pattern, column, perl = TRUE, ignore.case = TRUE)
      })
      
      # collapse into single vector
      matches <- Reduce(`|`, matches)
      
      # update based on matches
      x <- x[matches, , drop = FALSE]
      
   }
   
   # apply sort
   if (length(cols) > 0)
   {
      vals <- list()
      # iterate every requested sort key; the previous "for (i in length(cols))"
      # ran the loop exactly once with i == length(cols), so multi-column sorts
      # silently applied only the last key.
      for (i in seq_along(cols))
      {
         idx <- cols[[i]]
         # list and data.frame columns are non-atomic; order() and xtfrm()
         # error out on them ("unimplemented type 'list'"), so skip them here
         # as a backstop in case the client requests a sort on such a column
         if (length(x[[idx]]) > 0 && !is.list(x[[idx]]))
         {
            if (identical(dirs[[i]], "asc"))
            {
               vals <- append(vals, list(x[[idx]]))
            }
            else
            {
               vals <- append(vals, list(-xtfrm(x[[idx]])))
            }
         }
      }
      
      if (length(vals) > 0)
      {
         x <- x[do.call(order, vals), , drop = FALSE]
      }
   }
   
   return(x)
})

# returns envName as an environment, or NULL if the conversion failed
.rs.addFunction("safeAsEnvironment", function(envName)
{
   env <- NULL
   tryCatch(
      {
         env <- as.environment(envName)
      }, 
      error = function(e) { })
   env
})

.rs.addFunction("findDataFrame", function(envName, objName, cacheKey, cacheDir) 
{
   env <- NULL
   
   # mark encoding on cache directory 
   if (Encoding(cacheDir) == "unknown")
      Encoding(cacheDir) <- "UTF-8"
   
   # do we have an object name? if so, check in a named environment
   if (!is.null(objName) && nchar(objName) > 0) 
   {
      if (is.null(envName) || identical(envName, "R_GlobalEnv") || 
          nchar(envName) == 0)
      {
         # global environment
         env <- globalenv()
      }
      else 
      {
         env <- .rs.safeAsEnvironment(envName)
         if (is.null(env))
            env <- emptyenv()
      }
      
      # if the object exists in this environment, return it (avoid creating a
      # temporary here)
      if (exists(objName, where = env, inherits = FALSE))
      {
         # attempt to coerce the object to a data frame--note that a null return
         # value here may indicate that the object exists in the environment but
         # is no longer a data frame (we want to fall back on the cache in this
         # case)
         dataFrame <- .rs.toDataFrame(get(objName, envir = env, inherits = FALSE), 
                                      objName, TRUE)
         if (!is.null(dataFrame)) 
            return(dataFrame)
      }
   }
   
   if (.rs.isNonEmptyScalarString(cacheKey))
   {
      # if the object exists in the cache environment, return it. objects
      # in the cache environment have already been coerced to data frames.
      if (exists(cacheKey, where = .rs.CachedDataEnv, inherits = FALSE))
         return(get(cacheKey, envir = .rs.CachedDataEnv, inherits = FALSE))
      
      # perhaps the object has been saved? attempt to load it into the
      # cached environment
      cacheFile <- file.path(cacheDir, paste(cacheKey, "Rdata", sep = "."))
      if (file.exists(cacheFile))
      {
         status <- try(load(cacheFile, envir = .rs.CachedDataEnv), silent = TRUE)
         if (inherits(status, "try-error"))
            return(NULL)
         
         if (exists(cacheKey, where = .rs.CachedDataEnv, inherits = FALSE))
            return(get(cacheKey, envir = .rs.CachedDataEnv, inherits = FALSE))
      }
   }
   
   # failure
   return(NULL)
})

# given a name, return the first environment on the search list that contains
# an object bearing that name. 
.rs.addFunction("findViewingEnv", function(name)
{
   # default to searching from the global environment
   env <- globalenv()
   
   # attempt to find a call frame from which View was invoked; this will allow
   # us to locate viewing environments further in the call stack
   # (e.g. in the debugger)
   for (i in seq_along(sys.calls()))
   {
      if (identical(deparse(sys.call(i)[[1]]), "View"))
      {
         env <- sys.frame(i - 1)
         break
      }
   }
   
   # NOTE: we previously looked through the parent environments of
   # the associated frame to find the actual environment hosting the
   # object being viewed, but this caused problems when attempting
   # to track object mutations. for example, the 'mtcars' dataset is
   # defined in 'package:datasets', but attempting to modify that
   # object would actually create that modified object in the R global
   # environment. for that reason, it's best to track objects from the
   # top-level environment where they were found, as that's where
   # "modified" versions of that object will be generated
   env
})

# attempts to determine whether the View(...) function the user has an 
# override (i.e. it's not the handler RStudio uses)
.rs.addFunction("isViewOverride", function()
{
   # check to see if View has been overridden: find the View() call in the 
   # stack and examine the function being evaluated there
   for (i in seq_along(sys.calls()))
   {
      if (identical(deparse(sys.call(i)[[1]]), "View"))
      {
         # the first statement in the override function should be a call to 
         # .rs.callAs
         return(!identical(deparse(body(sys.function(i))[[1]]), ".rs.callAs"))
      }
   }
   
   # if we can't find View on the callstack, presume the common case (no
   # override)
   FALSE
})

.rs.addFunction("viewHook", function(original, x, title) {
   
   # remember the expression from which the data was generated
   expr <- deparse(substitute(x), backtick = TRUE)
   
   # generate title if necessary (from deparsed expr)
   if (missing(title))
      title <- paste(expr[1])
   
   # collapse expr for serialization
   expr <- paste(expr, collapse = " ")
   
   name <- ""
   env <- emptyenv()
   
   if (.rs.isViewOverride()) 
   {
      # if the View() invoked wasn't our own, we have no way of knowing what's
      # been done to the data since the user invoked View() on it, so just view
      # a snapshot of the data
      name <- title
   }
   else if (is.name(substitute(x)))
   {
      # if the argument is the name of a variable, we can monitor it in its
      # environment, and don't need to make a copy for viewing
      name <- paste(deparse(substitute(x)))
      env <- .rs.findViewingEnv(name)
   }
   
   # is this a function? if it is, view as a function instead
   if (is.function(x)) 
   {
      # check the source refs to see if we can open the file itself instead of
      # opening a read-only source viewer
      srcref <- .rs.getSrcref(x)
      if (!is.null(srcref))
      {
         srcfile <- attr(srcref, "srcfile", exact = TRUE)
         filename <- .rs.nullCoalesce(srcfile$filename, "")
         if (!identical(filename, "~/.active-rstudio-document") &&
             file.exists(filename))
         {
            # the srcref points to a valid file--go there 
            .Call("rs_jumpToFunction",
                  normalizePath(filename, winslash = "/"),
                  srcref[[1]],
                  srcref[[5]],
                  TRUE,
                  PACKAGE = "(embedding)")
            
            return(invisible(NULL))
         }
      }
      
      # either this function doesn't have a source reference or its source
      # reference points to a file we can't locate on disk--show a deparsed
      # version of the function
      
      # remove package qualifiers from function name
      title <- sub("^[^:]+:::?", "", title)
      
      # infer environment location
      namespace <- .rs.environmentName(environment(x))
      if (identical(namespace, "R_EmptyEnv") || identical(namespace, ""))
         namespace <- "viewing"
      else if (identical(namespace, "R_GlobalEnv"))
         namespace <- ".GlobalEnv"
      invisible(.Call("rs_viewFunction", x, title, namespace, PACKAGE = "(embedding)"))
      return(invisible(NULL))
   }
   else if (inherits(x, "vignette"))
   {
      file.edit(file.path(x$Dir, "doc", x$File))
      return(invisible(NULL))
   }
   
   # delegate to object explorer if this is an 'explorable' object
   if (.rs.dataViewer.shouldUseObjectExplorer(x))
   {
      view <- .rs.explorer.viewObject(x, title = title, envir = env)
      return(invisible(view))
   }
   
   # convert Pandas DataFrames to R data.frames
   if (inherits(x, "pandas.core.frame.DataFrame"))
      x <- reticulate::py_to_r(x)
   
   # test for coercion to data frame--the goal of this expression is just to
   # raise an error early if the object can't be made into a frame; don't
   # require that we can generate row/col names
   coerced <- x
   eval(
      expr = substitute(as.data.frame(coerced, optional = TRUE)),
      envir = globalenv()
   )
   
   # save a copy into the cached environment
   cacheKey <- .rs.addCachedData(force(x), name)
   
   if (!.rs.isNonEmptyScalarString(cacheKey))
      return(invisible(NULL))
   
   # call viewData 
   invisible(.Call("rs_viewData", x, expr, title, name, env, cacheKey, FALSE))
})

.rs.registerReplaceHook("View", "utils", .rs.viewHook)

.rs.addFunction("dataViewer.shouldUseObjectExplorer", function(object)
{
   if (inherits(object, c("function", "vignette")))
      return(FALSE)
   
   # prefer data viewer for pandas DataFrames
   if (inherits(object, "pandas.core.frame.DataFrame"))
      return(FALSE)
   
   # don't explore regular data.frames
   isTabular <-
      is.data.frame(object) ||
      is.matrix(object) ||
      is.table(object)
   
   if (isTabular)
      return(FALSE)
   
   # other objects are worth using object explorer for
   TRUE
})

.rs.addFunction("viewDataFrame", function(x, title, preview) {
   cacheKey <- .rs.addCachedData(force(x), "")
   if (.rs.isNonEmptyScalarString(cacheKey))
      invisible(.Call("rs_viewData", x, "", title, "", emptyenv(), cacheKey, preview))
})

.rs.addFunction("initializeDataViewer", function(server) {
   if (server) {
      .rs.registerReplaceHook("edit", "utils", function(original, name, ...) {
         if (is.data.frame(name) || is.matrix(name))
            stop("Editing of data frames and matrixes is not supported in RStudio.", call. = FALSE)
         else
            original(name, ...)
      })
   }
})

.rs.addFunction("addCachedData", function(obj, objName) 
{
   cacheKey <- .Call("rs_generateShortUuid")
   .rs.assignCachedData(cacheKey, obj, objName)
   cacheKey
})

.rs.addFunction("assignCachedData", function(cacheKey, obj, objName) 
{
   # coerce to data frame before assigning, and don't assign if we can't coerce
   frame <- .rs.toDataFrame(obj, objName, TRUE)
   if (!is.null(frame) &&
       .rs.isNonEmptyScalarString(cacheKey))
      assign(cacheKey, frame, .rs.CachedDataEnv)
})

.rs.addFunction("removeCachedData", function(cacheKey, cacheDir)
{
   # mark encoding on cache directory 
   if (Encoding(cacheDir) == "unknown")
      Encoding(cacheDir) <- "UTF-8"
   
   if (.rs.isNonEmptyScalarString(cacheKey))
   {
      # remove data from the cache environment
      if (exists(".rs.CachedDataEnv") &&
          exists(cacheKey, where = .rs.CachedDataEnv, inherits = FALSE))
         rm(list = cacheKey, envir = .rs.CachedDataEnv, inherits = FALSE)
      
      # remove data from the cache directory
      cacheFile <- file.path(cacheDir, paste(cacheKey, "Rdata", sep = "."))
      if (file.exists(cacheFile))
         file.remove(cacheFile)
      
      # remove any working data
      .rs.removeWorkingData(cacheKey)
   }
   invisible(NULL)
})

.rs.addFunction("saveCachedData", function(cacheDir)
{
   # mark encoding on cache directory 
   if (Encoding(cacheDir) == "unknown")
      Encoding(cacheDir) <- "UTF-8"
   
   # no work to do if we have no cache
   if (!exists(".rs.CachedDataEnv")) 
      return(invisible(NULL))
   
   # save each active cache file from the cache environment
   lapply(ls(.rs.CachedDataEnv), function(cacheKey) {
      if (.rs.isNonEmptyScalarString(cacheKey))
         save(list = cacheKey, 
              file = file.path(cacheDir, paste(cacheKey, "Rdata", sep = ".")),
              envir = .rs.CachedDataEnv)
   })
   
   # clean the cache environment
   # can generate warnings if .rs.CachedDataEnv disappears (we call this on
   # shutdown); suppress these
   suppressWarnings(rm(list = ls(.rs.CachedDataEnv), where = .rs.CachedDataEnv))
   
   invisible(NULL)
})

.rs.addFunction("findWorkingData", function(cacheKey)
{
   if (.rs.isNonEmptyScalarString(cacheKey) &&
       exists(".rs.WorkingDataEnv") &&
       exists(cacheKey, where = .rs.WorkingDataEnv, inherits = FALSE))
      get(cacheKey, envir = .rs.WorkingDataEnv, inherits = FALSE)
   else
      NULL
})

.rs.addFunction("removeWorkingData", function(cacheKey)
{
   if (.rs.isNonEmptyScalarString(cacheKey) &&
       exists(".rs.WorkingDataEnv") &&
       exists(cacheKey, where = .rs.WorkingDataEnv, inherits = FALSE))
      rm(list = cacheKey, envir = .rs.WorkingDataEnv, inherits = FALSE)
   invisible(NULL)
})

.rs.addFunction("assignWorkingData", function(cacheKey, obj)
{
   if (.rs.isNonEmptyScalarString(cacheKey))
      assign(cacheKey, obj, .rs.WorkingDataEnv)
})

.rs.addFunction("findGlobalData", function(name)
{
   if (exists(name, envir = globalenv()))
   {
      if (inherits(get(name, envir = globalenv()), "data.frame"))
         return(name)
   }
   invisible("")
})

.rs.addFunction("isNonEmptyScalarString", function(x)
{
   is.character(x) && length(x) == 1 && nzchar(x)
})

