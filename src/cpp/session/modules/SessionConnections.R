#
# SessionConnections.R
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


# install the S4 classes which represent active connections into the tools
# environment
setClass("rstudioConnectionAction", representation(
  name = "character",
  icon = "character",
  callback = "function"
), where = .rs.toolsEnv())

setClass("rstudioConnection", representation(
  type = "character",
  host = "character",
  displayName = "character",
  icon = "character",
  connectCode = "character",
  disconnectCode = "function",
  listTables = "function",
  listColumns = "function",
  previewTable = "function",
  actions = "list"
), where = .rs.toolsEnv())

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

# create an environment which will host the known active connections
assign(".rs.activeConnections", 
       value = new.env(parent = emptyenv()), 
       envir = .rs.toolsEnv())

# given a connection type and host, find a matching active connection, or NULL
# if no connection was found
.rs.addFunction("findActiveConnection", function(type, host) {
   connections <- ls(.rs.activeConnections)
   for (name in connections) {
      connection <- get(name, envir = .rs.activeConnections)
      if (identical(connection@type, type) && 
          identical(connection@host, host)) {
         return(connection)
      }
   }
   # indicates no connection was found
   NULL
})

options(connectionViewer = list(
   connectionOpened = function(connection) {
      # validate connection object
      if (!inherits(connection, "rstudioConnection"))
         stop("argument must be an object of class rstudioConnection")

      # generate an internal key for this connection in the local cache 
      cacheKey <- paste(connection@type, connection@host, 
                        .Call("rs_generateShortUuid"), 
                        sep = "_")
      assign(cacheKey, value = connection, envir = .rs.activeConnections)

      # serialize and generate client events
      invisible(.Call("rs_connectionOpened", connection))
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
   connection <- .rs.findActiveConnection(type, host)
   if (!is.null(name))
      connection@disconnectCode()
   else
      ""
})

.rs.addFunction("getSparkWebUrl", function(finder, host) {
   sc <- .rs.getConnectionObject(finder, host)
   sparklyr:::spark_web(sc)
})

.rs.addFunction("getSparkLogFile", function(finder, host) {
   sc <- .rs.getConnectionObject(finder, host)
   sparklyr:::spark_log_file(sc)
})

.rs.addFunction("connectionListTables", function(type, host) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection)) 
      connection@listTables()
   else
      character()
})

.rs.addFunction("connectionListColumns", function(type, host, table) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(name)) {
      listColumnsCode <- connection@listColumns(table)
      eval(parse(text = listColumnsCode), envir = globalenv())
   }
   else
      NULL
})

.rs.addFunction("connectionPreviewTable", function(type,
                                                   host,
                                                   table,
                                                   limit) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(name)) {
      previewTableCode <- connection@previewTable(table, limit)
      df <- eval(parse(text = previewTableCode), envir = globalenv())
      .rs.viewDataFrame(df, table, TRUE)
   }

   NULL
})


.rs.addJsonRpcHandler("get_new_spark_connection_context", function() {

  context <- list()

  # all previously connected to remote servers
  context$remote_servers <- .Call("rs_availableRemoteServers")

  # is there a spark home option
  context$spark_home <- sparklyr:::spark_home()

  # can we install new versions
  canInstall <- sparklyr:::spark_can_install()
  context$can_install_spark_versions <- .rs.scalar(canInstall)

  # available spark versions (filter by installed if we can't install
  # new versions of spark)
  spark_versions <- sparklyr:::spark_versions()
  if (!context$can_install_spark_versions)
    spark_versions <- subset(spark_versions, spark_versions$installed)
  context$spark_versions <- spark_versions

  # default spark and hadoop version
  defaultVersion <- sparklyr:::spark_default_version()
  context$spark_default <- .rs.scalar(defaultVersion$spark);
  context$hadoop_default <- .rs.scalar(defaultVersion$hadoop);

  # option defining what connections are supported
  defaultConnections <- c("local", "cluster")
  context$connections_option <- tryCatch({
      connections <- getOption("rstudio.spark.connections", defaultConnections)
      if (is.character(connections))
         connections
      else
         defaultConnections
    },
    error = function(e) defaultConnections
  )

  # default spark cluster url
  context$default_cluster_url <- .rs.scalar(.Call("rs_defaultSparkClusterUrl"))

  # is java installed?
  context$java_installed <- .rs.scalar(sparklyr:::is_java_available())
  context$java_install_url <- .rs.scalar(sparklyr:::java_install_url())

  context
})


