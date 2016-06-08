#
# SessionConnections.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
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

.rs.addFunction("validateCharacterParams", function(params, optional = FALSE) {
   paramNames <- names(params)
   for (param in paramNames) {
      value <- params[[param]]
      if (optional && is.null(value))
         next
      if (!is.character(value) || length(value) != 1)
         stop(param, " must be a single element character vector", call. = FALSE)
   }
})

options(connectionViewer = list(
   connectionOpened = function(type, host, finder, connectCode, disconnectCode,
                               listTablesCode = NULL, listColumnsCode = NULL,
                               previewTableCode = NULL, ...) {
      .rs.validateCharacterParams(list(
         type = type, host = host, connectCode = connectCode,
         disconnectCode = disconnectCode
      ))
      .rs.validateCharacterParams(list(
         listTablesCode = listTablesCode,
         listColumnsCode = listColumnsCode,
         previewTableCode = previewTableCode
      ), optional = TRUE)
      if (!is.function(finder))
         stop("finder must be a function", call. = FALSE)
      finder <- paste(deparse(finder), collapse = "\n")
      invisible(.Call("rs_connectionOpened", type, host, finder, 
                      connectCode, disconnectCode,
                      listTablesCode, listColumnsCode,
                      previewTableCode))
   },
   connectionClosed = function(type, host, ...) {
      .rs.validateCharacterParams(list(type = type, host = host))
      invisible(.Call("rs_connectionClosed", type, host))
   },
   connectionUpdated = function(type, host, hint, ...) {
      .rs.validateCharacterParams(list(type = type, host = host, hint = hint))
      invisible(.Call("rs_connectionUpdated", type, host, hint))
   }
))


.rs.addFunction("getConnectionObjectName", function(finder, host) {
   finderFunc <- eval(parse(text = finder))
   finderFunc(globalenv(), host)
})

.rs.addFunction("getConnectionObject", function(finder, host) {
   name <- .rs.getConnectionObjectName(finder, host)
   get(name, envir = globalenv())
})

.rs.addFunction("getDisconnectCode", function(finder, host, template) {
   name <- .rs.getConnectionObjectName(finder, host)
   if (!is.null(name))
      sprintf(template, name)
   else
      ""
})

.rs.addFunction("getSparkWebUrl", function(finder, host) {
   sc <- .rs.getConnectionObject(finder, host)
   rspark::spark_web(sc)
})

.rs.addFunction("getSparkLogFile", function(finder, host) {
   sc <- .rs.getConnectionObject(finder, host)
   rspark::spark_log_file(sc)
})

.rs.addJsonRpcHandler("get_new_spark_connection_context", function() {

  context <- list()

  # all previously connected to remote servers
  context$remote_servers <- .Call("rs_availableRemoteServers")

  # available cores on this machine
  context$cores <- parallel::detectCores()
  if (is.na(context$cores))
    context$cores <- 1
  context$cores <- .rs.scalar(context$cores)

  # can we install new versions
  context$can_install_spark_versions <- .rs.scalar(rspark:::spark_can_install())

  # available spark versions (filter by installed if we can't install
  # new versions of spark)
  spark_versions <- rspark:::spark_versions()
  if (!context$can_install_spark_versions)
    spark_versions <- subset(spark_versions, spark_versions$installed)
  context$spark_versions <- spark_versions

  # is java installed?
  context$java_installed <- .rs.scalar(rspark:::is_java_available())
  context$java_install_url <- .rs.scalar(rspark:::java_install_url())

  context
})


