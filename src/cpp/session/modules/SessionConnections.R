#
# SessionConnections.R
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

.rs.addFunction("validateParams", function(obj, params, type, optional = FALSE) {
   for (param in params) {
      value <- obj[[param]]
      if (optional && is.null(value))
         next
      if (!inherits(value, type) || length(value) != 1)
         stop(param, " must be a single element of type '", type, "'", 
              call. = FALSE)
   }
})

.rs.addFunction("validateCharacterParams", function(params, optional = FALSE) {
   .rs.validateParams(params, names(params), "character", optional)
})

.rs.addFunction("validateConnection", function(connection) {
   .rs.validateParams(connection, 
       c("type", "host", "displayName", "connectCode"),
       "character")
   .rs.validateParams(connection, "icon", "character", optional = TRUE)
   .rs.validateParams(connection, 
       c("disconnect", "listObjects", "listColumns", "previewObject"),
       "function")
})

# create an environment which will host the known active connections
assign(".rs.activeConnections", 
       value = new.env(parent = emptyenv()), 
       envir = .rs.toolsEnv())

# given a connection type and host, find a matching active connection name, or
# NULL if no connection was found
.rs.addFunction("findConnectionName", function(type, host) {
   connections <- ls(.rs.activeConnections)
   for (name in connections) {
      connection <- get(name, envir = .rs.activeConnections)
      if (identical(connection$type, type) && 
          identical(connection$host, host)) {
         return(name)
      }
   }
   # indicates no connection was found
   NULL
})

# given a connection type and host, find an active connection object, or NULL if
# no connection was found
.rs.addFunction("findActiveConnection", function(type, host) {
   name <- .rs.findConnectionName(type, host)
   if (is.null(name))
      return(NULL)
   return(get(name, envir = .rs.activeConnections))
})

options(connectionObserver = list(
   connectionOpened = function(type, host, displayName, icon = NULL, 
                               connectCode, disconnect, listObjectTypes,
                               listObjects, listColumns, previewObject, 
                               connectionObject, actions = NULL) {

      # execute the object types function once to get the list of known 
      # object types; this is presumed to be static over the lifetime of the
      # connection
      if (!inherits(listObjectTypes, "function")) {
         stop("listObjectTypes must be a function returning a list of object types", 
              call. = FALSE)
      }

      # function to flatten the tree of object types for more convenient storage
      promote <- function(name, l) {
        if (length(l) == 0)
          return(list())
        if (is.null(l$contains)) {
          # plain data
          return(list(list(name = name,
                      icon = l$icon,
                      contains = "data")))
        } else {
          # subtypes
          return(unlist(append(list(list(list(
            name = name, 
            icon = l$icon, 
            contains = names(l$contains)))),
            lapply(names(l$contains), function(name) {
              promote(name, l$contains[[name]])
            })), recursive = FALSE))
        }
        return(list())
      }
      
      # apply tree flattener to provided object tree
      objectTree <- listObjectTypes()
      objectTypes <- lapply(names(objectTree), function(name) {
         promote(name, objectTree[[name]])
      })[[1]]
      
      # manufacture and validate object representing this connection
      connection <- list(
         type             = type,            # the type of the connection
         host             = host,            # the host being connected to
         displayName      = displayName,     # the name to display 
         icon             = icon,            # an icon representing the connection
         connectCode      = connectCode,     # code to (re)establish connection
         disconnect       = disconnect,      # function that disconnects
         objectTypes      = objectTypes,     # list of object types known 
         listObjects      = listObjects,     # list objects (all or in container)
         listColumns      = listColumns,     # list columns of a data object
         previewObject    = previewObject,   # preview an object
         actions          = actions,         # list of actions possible on conn
         connectionObject = connectionObject # raw connection object
      )
      class(connection) <- "rstudioConnection"
      .rs.validateConnection(connection)

      # generate an internal key for this connection in the local cache 
      cacheKey <- paste(connection$type, connection$host, 
                        .Call("rs_generateShortUuid"), 
                        sep = "_")
      assign(cacheKey, value = connection, envir = .rs.activeConnections)

      # serialize and generate client events
      invisible(.Call("rs_connectionOpened", connection))
   },
   connectionClosed = function(type, host, ...) {
      .rs.validateCharacterParams(list(type = type, host = host))

      # clean up reference in environment
      name <- .rs.findConnectionName(type, host)
      if (!is.null(name))
         rm(list = name, envir = .rs.activeConnections)

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

.rs.addFunction("getConnectionObject", function(type, host) {
   name <- .rs.getConnectionObjectName(type, host)
   get(name, envir = globalenv())
})

.rs.addFunction("connectionDisconnect", function(type, host) {
   connection <- .rs.findActiveConnection(type, host)
   if (!is.null(connection))
      connection$disconnect()
})

.rs.addFunction("connectionListObjects", function(type, host, ...) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection)) 
      connection$listObjects(...)
   else
      character()
})

.rs.addFunction("connectionListColumns", function(type, host, ...) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection))
      listColumnsCode <- connection$listColumns(...)
   else
      NULL
})

.rs.addFunction("connectionPreviewObject", function(type, host, limit, ...) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection)) {
      df <- connection$previewObject(limit, ...)

      # use the last element of the specifier to caption the frame
      args <- list(...)
      .rs.viewDataFrame(df, args[[length(args)]], TRUE)
   }

   NULL
})

.rs.addFunction("connectionExecuteAction", function(type, host, action) {

   connection <- .rs.findActiveConnection(type, host)

   if (!is.null(connection) && action %in% names(connection$actions)) {
      connection$actions[[action]]$callback()
   }

   NULL
})

.rs.addFunction("connectionFilesPath", function() {
   snippetsPath <- getOption("connections-path", "/etc/rstudio/connections/")

   if (!is.null(getOption("connections-path")) && !dir.exists(snippetsPath)) {
      warning(
         "Path '", snippetsPath, "' does not exist. ",
         "Configure the connections-path option appropriately.")
   }

   snippetsPath
})

.rs.addFunction("connectionOdbcInstallerPath", function() {
   normalizePath(
      file.path(
         .Call("rs_connectionOdbcInstallPath"),
         "odbc",
         "installers"),
      mustWork = FALSE
   )
})

.rs.addFunction("connectionFiles", function(include, defaultPath) {
   connectionFiles <- list()
   
   if (!is.null(defaultPath)) {
      connectionFiles <- list.files(defaultPath)
   }

   files <- lapply(connectionFiles, function(file) {
      fullPath <- file.path(defaultPath, file)
   })

   names(files) <- gsub(include, "", connectionFiles)

   files <- files[grepl(include, files)]
   sapply(files, normalizePath)
})

.rs.addFunction("connectionHasInstaller", function(name) {
   installerName <- paste(name, "dcf", sep = ".")
   connectionFiles <- as.character(.rs.connectionFiles("\\.dcf$", .rs.connectionOdbcInstallerPath()))
   
   any(basename(connectionFiles) == installerName)
})

.rs.addFunction("connectionInstallerInfo", function(name) {
   installerName <- paste(name, "dcf", sep = ".")
   installerFile <- as.character(.rs.connectionFiles(installerName, .rs.connectionOdbcInstallerPath()))

   fileContents <- read.dcf(installerFile)
   list(
      name = if ("Name" %in% colnames(fileContents)) fileContents[,"Name"][[1]] else NULL,
      version = if ("Version" %in% colnames(fileContents)) fileContents[,"Version"][[1]] else NULL
   )
})

.rs.addFunction("connectionReadSnippets", function() {
   snippetsPaths <- .rs.connectionFiles("\\.R$", .rs.connectionFilesPath())

   snippets <- lapply(snippetsPaths, function(fullPath) {
      paste(readLines(fullPath), collapse = "\n")
   })

   lapply(names(snippets), function(snippetName) {
      tryCatch({
         snippet <- snippets[[snippetName]]

         list(
            package = .rs.scalar(NULL),
            version = .rs.scalar(NULL),
            name = .rs.scalar(snippetName),
            type = .rs.scalar("Snippet"),
            snippet = .rs.scalar(snippet),
            help = .rs.scalar(NULL),
            iconData = .rs.scalar(.Call("rs_connectionIcon", snippetName)),
            licensed = .rs.scalar(FALSE),
            source = .rs.scalar("Snippet"),
            hasInstaller = .rs.scalar(FALSE)
         )
      }, error = function(e) {
         warning(e$message)
         NULL
      })
   })
})

.rs.addFunction("connectionOdbcInstallPath", function()
{
   normalizePath(
      file.path(
         .Call("rs_connectionOdbcInstallPath"),
         "odbc",
         "drivers"),
      mustWork = FALSE
   )
})

.rs.addFunction("connectionReadInstallers", function() {
   if (!.rs.isDesktop()) return(list())
   
   installerPaths <- .rs.connectionFiles("\\.dcf$", .rs.connectionOdbcInstallerPath())

   installers <- lapply(installerPaths, function(fullPath) {
      read.dcf(fullPath)
   })

   valueOrDefault <- function(name, data, default) {
      cols <- colnames(data)
      ifelse(name %in% cols, data[,name], default)
   }

   valueOrEmpty <- function(name, data) {
      cols <- colnames(data)
      ifelse(name %in% cols, data[,name], "")
   }

   lapply(names(installers), function(installerName) {
      tryCatch({
         installer <- installers[[installerName]]
         cols <- colnames(installer)

         warning <- gsub(
            "\n",
            " ",
            valueOrDefault(
               paste("Warning", .Platform$OS.type, sep = "."),
               installer,
               valueOrEmpty("Warning", installer)
            )
         )

         list(
            package = .rs.scalar(NULL),
            version = .rs.scalar(NULL),
            name = .rs.scalar(installerName),
            type = .rs.scalar("Install"),
            subtype = .rs.scalar("Odbc"),
            help = .rs.scalar(NULL),
            iconData = .rs.scalar(.Call("rs_connectionIcon", installerName)),
            licensed = .rs.scalar("Licensed" %in% colnames(installer)),
            source = .rs.scalar("Snippet"),
            snippet = .rs.scalar(""),
            # odbc installer dcf fields
            odbcVersion = .rs.scalar(valueOrEmpty("Version", installer)),
            odbcLicense = .rs.scalar(gsub("\n", " ", valueOrEmpty("License", installer))),
            odbcDownload = .rs.scalar(installer[,"Download"]),
            odbcFile = .rs.scalar(valueOrEmpty("File", installer)),
            odbcLibrary = .rs.scalar(valueOrEmpty("Library", installer)),
            odbcWarning = .rs.scalar(warning),
            odbcInstallPath = .rs.scalar(.rs.connectionOdbcInstallPath()),
            odbcMD5 = .rs.scalar(gsub("\n", " ", valueOrEmpty("MD5", installer))),
            hasInstaller = .rs.scalar(TRUE)
         )
      }, error = function(e) {
         warning(e$message)
         NULL
      })
   })
})

.rs.addFunction("connectionSupportedPackages", function() {
   list(
      list(
         name = "ODBC",
         package = "odbc",
         version = "1.1.1"
      ),
      list(
         name = "Spark",
         package = "sparklyr",
         version = "0.5.6"
      )
   )
})

.rs.addFunction("connectionReadWindowsRegistry", function() {
   registryOdbcPath <- "SOFTWARE\\ODBC\\ODBCINST.INI\\"

   registryEntries <- lapply(names(readRegistry(registryOdbcPath)), function(driver) {
     driverPath <- readRegistry(paste(registryOdbcPath, driver, sep = ""))$Driver
     list(name = driver, attribute = "Driver", value = driverPath)
   })

   registryEntriesValue <- Filter(function(e) !is.null(e$value), registryEntries)

   do.call(rbind, lapply(registryEntriesValue, function(e) data.frame(e, stringsAsFactors = FALSE)))
})

.rs.addFunction("connectionReadOdbcEntry", function(drivers, uniqueDriverNames, driver) {
   tryCatch({
      currentDriver <- drivers[drivers$attribute == "Driver" & drivers$name == driver, ]
      driverInstaller <- drivers[drivers$attribute == "Installer" & drivers$name == driver, ]
      driverId <- gsub(.rs.connectionOdbcRStudioDriver(), "", driver)

      basePath <- sub(paste(tolower(driver), ".*$", sep = ""), "", currentDriver$value)
      snippetsFile <- file.path(
         basePath,
         tolower(driver),
         "snippets",
         paste(tolower(driverId), ".R", sep = "")
      )
      
      if (identical(file.exists(snippetsFile), TRUE)) {
         snippet <- paste(readLines(snippetsFile), collapse = "\n")
      }
      else {
         snippet <- paste(
            "library(DBI)\n",
            "con <- dbConnect(odbc::odbc(), .connection_string = \"", 
            "Driver={", driver, "};${1:Parameters}\", timeout = 10)",
            sep = "")
      }

      licenseFile <- file.path(dirname(currentDriver$value), "license.lock")

      iconData <- .Call("rs_connectionIcon", driverId)
      if (nchar(iconData) == 0)
         iconData <- .Call("rs_connectionIcon", "ODBC")

      hasInstaller <- identical(driverInstaller$value, "RStudio")
      warningMessage <- NULL

      if (hasInstaller) {
         installerVersion <- .rs.connectionInstallerInfo(driverId)$version

         currentVersion <- drivers[drivers$attribute == "Version" & drivers$name == driver, ]
         if (nrow(currentVersion) == 1) {
            if (compareVersion(installerVersion, currentVersion$value) > 0) {
               warningMessage <- "A new driver version is available, to upgrade, uninstall and then reinstall."
            }
         }
      }

      list(
         package = .rs.scalar(NULL),
         version = .rs.scalar(NULL),
         name = .rs.scalar(driver),
         type = .rs.scalar("Snippet"),
         snippet = .rs.scalar(snippet),
         help = .rs.scalar(NULL),
         iconData = .rs.scalar(iconData),
         licensed = .rs.scalar(identical(file.exists(licenseFile), TRUE)),
         source = .rs.scalar("ODBC"),
         hasInstaller = .rs.scalar(hasInstaller),
         warning = .rs.scalar(warningMessage),
         installer = .rs.scalar(driverInstaller$value)
      )
   }, error = function(e) {
      warning(e$message)
      NULL
   })
})

.rs.addFunction("connectionReadOdbc", function() {
   if (.rs.isPackageInstalled("odbc")) {
      drivers <- data.frame()

      tryCatch({
         drivers <- get("odbcListDrivers", envir = asNamespace("odbc"))()
         
         if (.Platform$OS.type == "windows") {
            drivers <- rbind(drivers, .rs.connectionReadWindowsRegistry())
         }
      }, error = function(e) warning(e$message))

      uniqueDriverNames <- unique(drivers$name)

      lapply(uniqueDriverNames, function(driver) {
         .rs.connectionReadOdbcEntry(drivers, uniqueDriverNames, driver)
      })
   }
})

.rs.addFunction("connectionReadPackages", function() {
   rawConnections <- .rs.fromJSON(.Call("rs_availableConnections"))

   pacakgeConnections <- lapply(rawConnections, function(con) {
      tryCatch({
         ns <- asNamespace(con$package)

         connectionType <- if (nchar(con$shinyapp) == 0) "Snippet" else "Shiny"
         snippetFile <- file.path("rstudio", "connections", paste(con$name, ".R", sep = ""))
         snippet <- ""

         if (nchar(con$shinyapp) == 0) {
            snippetPath <- system.file(snippetFile, package = con$package)
            if (!file.exists(snippetPath)) {
               warning(
                  "The file \"", con$name, ".R\" does not exist under \"rstudio/connections\" for ",
                  "package \"", con$package , "\".")
            }
            else {
               snippet <- paste(readLines(snippetPath), collapse = "\n")
            }
         }
         else {
            if (!exists(con$shinyapp, envir = ns, mode="function")) {
               warning(
                  "The function \"", con$shinyapp, "\" does not exist. ",
                  "Check the ShinyApp DCF field in the ", con$package, " package.")
            }
         }

         iconData <- if (nchar(con$icon) > 0) {
            iconPath <- system.file(con$icon, package = con$package)
            if (file.exists(iconPath)) {
               paste0("data:image/png;base64,", .rs.base64encodeFile(iconPath));
            }
         }
         else {
            .Call("rs_connectionIcon", con$name)
         }

         list(
            package = .rs.scalar(con$package),
            version = .rs.scalar(NULL),
            name = .rs.scalar(con$name),
            type = .rs.scalar(connectionType),
            newConnection = .rs.scalar(paste(con$package, "::", con$shinyapp, "()", sep = "")),
            snippet = .rs.scalar(snippet),
            help = .rs.scalar(con$help),
            iconData = .rs.scalar(iconData),
            licensed = .rs.scalar(FALSE),
            source = .rs.scalar("Package"),
            hasInstaller = .rs.scalar(FALSE)
         )
      }, error = function(e) {
         warning(e$message)
         NULL
      })
   })

   names(pacakgeConnections) <- NULL
   pacakgeConnections
})

.rs.addFunction("connectionReadDSN", function() {
   if (.rs.isPackageInstalled("odbc")) {
      dataSources <- data.frame()

      tryCatch({
         if (exists("list_data_sources", envir = asNamespace("odbc"))) {
            listSources <- get("list_data_sources", envir = asNamespace("odbc"))
         }
         else {
            listSources <- get("odbcListDataSources", envir = asNamespace("odbc"))
         }
         dataSources <- listSources()
      }, error = function(e) warning(e$message))

      lapply(dataSources$name, function(dataSourceName) {
         tryCatch({

            dataSource <- dataSources[dataSources$name == dataSourceName, ]

            snippet <- paste(
               "library(DBI)\n",
               "con <- dbConnect(odbc::odbc(), \"${1:Data Source Name=", 
               dataSource$name,
               "}\", timeout = 10)",
               sep = "")

            iconData <- .Call("rs_connectionIcon", dataSource$name)
            if (nchar(iconData) == 0)
               iconData <- .Call("rs_connectionIcon", "ODBC")

            list(
               package = .rs.scalar(NULL),
               version = .rs.scalar(NULL),
               name = .rs.scalar(dataSource$name),
               type = .rs.scalar("Snippet"),
               snippet = .rs.scalar(snippet),
               help = .rs.scalar(NULL),
               iconData = .rs.scalar(iconData),
               licensed = .rs.scalar(FALSE),
               source = .rs.scalar("DSN"),
               hasInstaller = .rs.scalar(FALSE)
            )
         }, error = function(e) {
            warning(e$message)
            NULL
         })
      })
   }
})

.rs.addFunction("connectionReadPackageInstallers", function() {

   supportedNotInstsalled <- Filter(function(e) {
      !.rs.isPackageVersionInstalled(e$package, e$version)
   }, .rs.connectionSupportedPackages())

   lapply(supportedNotInstsalled, function(supportedPackage) {
      iconData <- .Call("rs_connectionIcon", supportedPackage$name)
      list(
         package = .rs.scalar(supportedPackage$package),
         version = .rs.scalar(supportedPackage$version),
         name = .rs.scalar(supportedPackage$name),
         type = .rs.scalar("Install"),
         subtype = .rs.scalar("Package"),
         newConnection = .rs.scalar(NULL),
         snippet = .rs.scalar(NULL),
         help = .rs.scalar(NULL),
         iconData = .rs.scalar(iconData),
         licensed = .rs.scalar(FALSE),
         hasInstaller = .rs.scalar(FALSE)
      )
   })
})

.rs.addJsonRpcHandler("get_new_connection_context", function() {
   connectionList <- c(
      list(),
      .rs.connectionReadSnippets(),         # add snippets to connections list
      .rs.connectionReadDSN(),              # add ODBC DSNs to connections list
      .rs.connectionReadPackages(),         # add packages to connections list
      .rs.connectionReadOdbc(),             # add ODBC drivers to connections list
      .rs.connectionReadInstallers(),       # add installers to connections list
      .rs.connectionReadPackageInstallers() # add package installers to connection list
   )
   
   connectionList <- Filter(function(e) !is.null(e), connectionList)

   # remove duplicate names, in order
   connectionNames <- list()
   for (i in seq_along(connectionList)) {
      entryName <- connectionList[[i]]$name
      if (!is.null(connectionNames[[entryName]])) {
         existingDriver <- connectionNames[[entryName]]
         withRStudioName <- paste(entryName, .rs.connectionOdbcRStudioDriver(), sep = "")

         if (identical(as.character(connectionList[[i]]$type), "Install") &&
             !identical(as.character(existingDriver$installer), "RStudio") &&
             is.null(connectionNames[[withRStudioName]])) {
            connectionList[[i]]$name <- entryName <- .rs.scalar(withRStudioName)
         }
         else {
            connectionList[[i]]$remove <- TRUE
         }
      }

      if (is.null(connectionNames[[entryName]])) {
         connectionNames[[entryName]] <- connectionList[[i]]
      }
   }
   
   connectionList <- Filter(function(e) !identical(e$remove, TRUE), connectionList)

   context <- list(
      connectionsList = unname(connectionList)
   )

   context
})

.rs.addJsonRpcHandler("get_new_odbc_connection_context", function(name, retries = 1) {
   singleEntryFilter <- function(e) {
      identical(as.character(e$name), name)
   }

   connectionContext <- Filter(singleEntryFilter, .rs.connectionReadOdbc())

   while (length(connectionContext) != 1 && (retries <- retries - 1) >= 0)
      Sys.sleep(1)

   if (length(connectionContext) != 1)
      list(
         error = .rs.scalar(
            paste("The", name, "driver is not registered.")
         )
      )
   else {
      connectionContext[[1]]
   }
})

.rs.addFunction("embeddedViewer", function(url)
{
   .Call("rs_embeddedViewer", url)
})

.rs.addJsonRpcHandler("launch_embedded_shiny_connection_ui", function(package, name)
{
   if (package == "sparklyr" && packageVersion("sparklyr") <= "0.5.4") {
      return(.rs.error(
         "sparklyr ", packageVersion("sparklyr"), " does not support this functionality. ",
         "Please upgrade to sparklyr 0.5.5 or newer."
      ))
   }

   connectionContext <- .rs.rpc.get_new_connection_context()$connectionsList
   connectionInfo <- Filter(
      function(e)
        identical(as.character(e$package), as.character(package)) &
        identical(as.character(e$name), as.character(name)),
      connectionContext
   )

   if (length(connectionInfo) != 1) {
      return(.rs.error(
         "Connection for package ", package, " and name ", name, " is not registered"
      ))
   }

   connectionInfo <- connectionInfo[[1]]

   consoleCommand <- paste(
      "shiny::runGadget(",
      connectionInfo$newConnection,
      ", viewer = .rs.embeddedViewer)",
      sep = ""
   )

   .rs.api.sendToConsole(consoleCommand, echo = FALSE, execute = TRUE, focus = FALSE)

   .rs.success()
})

.rs.addJsonRpcHandler("connection_test", function(code) {
   error <- ""

   oldConnectionObserver <- getOption("connectionObserver")
   on.exit(options(connectionObserver = oldConnectionObserver))

   disconnectCalls <- list()

   options(connectionObserver = list(
      connectionOpened = function(type, host, displayName, icon = NULL, 
                                  connectCode, disconnect, listObjectTypes,
                                  listObjects, listColumns, previewObject, 
                                  connectionObject, actions = NULL) {
         disconnectCalls <<- c(disconnectCalls, disconnect)
      },
      connectionClosed = function(type, host, ...) {

      },
      connectionUpdated = function(type, host, hint, ...) {
      }
   ))

   .envir <- .rs.getActiveFrame()
   tryCatch({
      eval(parse(text = code), envir = .envir)
   }, error = function(e) {
      error <<- e$message
   })

   lapply(disconnectCalls, function(e) e())

   .rs.scalar(error)
})

.rs.addJsonRpcHandler("connection_add_package", function(package) {
   extensionPath <- system.file("rstudio/connections.dcf", package = package)
   invisible(.Call("rs_connectionAddPackage", package, extensionPath))
})

.rs.addFunction("connectionInstallerCommand", function(driverName, installationPath) {
   connectionContext <- Filter(function(e) {
      identical(
         as.character(e$name),
         gsub(.rs.connectionOdbcRStudioDriver(), "", driverName)
      )
   }, .rs.connectionReadInstallers())[[1]]

   placeholder <-  connectionContext$odbcFile
   driverUrl <- connectionContext$odbcDownload
   libraryPattern <- connectionContext$odbcLibrary
   targetMD5 <- connectionContext$odbcMD5
   driverVersion <- connectionContext$odbcVersion

   if (any(grepl("'", c(driverName, driverUrl, placeholder, installationPath, libraryPattern, targetMD5, driverVersion)))) {
      stop("Single quote can't be used in installer definitions.")
   }

   paste(
      ".rs.odbcBundleInstall(",
      "name = '", driverName, "', ",
      "url = '", driverUrl, "', ",
      "placeholder = '", placeholder, "', ",
      "installPath = '", normalizePath(installationPath, winslash = "/"), "', ",
      "libraryPattern = '", libraryPattern, "', ",
      "md5 = '", targetMD5, "', ",
      "version = '", driverVersion, "'",
      ")",
      sep = ""
   )
})

.rs.addFunction("connectionUnregisterOdbcinstDriver", function(driverName) {
   odbcinstPath <- .rs.odbcBundleOdbcinstPath()
   odbcinstData <- .rs.odbcBundleReadIni(odbcinstPath)

   if (driverName %in% names(odbcinstData)) {
      odbcinstData[[driverName]] <- NULL

      .rs.odbcBundleWriteIni(odbcinstPath, odbcinstData)
   }
})

.rs.addFunction("connectionUnregisterWindowsDriver", function(driverName) {
   .rs.odbcBundleRegistryRemove(
      list(
         list(
            path = file.path("SOFTWARE", "ODBC", "ODBCINST.INI", "ODBC Drivers", fsep = "\\"),
            key = driverName
         ),
         list(
            path = file.path("SOFTWARE", "ODBC", "ODBCINST.INI", driverName, fsep = "\\")
         )
      )
   )
})

.rs.addJsonRpcHandler("uninstall_odbc_driver", function(driverName) {
   tryCatch({
      defaultInstallPath <- file.path(.rs.connectionOdbcInstallPath(), tolower(driverName))
      defaultInstallExists <- dir.exists(defaultInstallPath)

      # delete the driver
      if (defaultInstallExists) {
         unlink(defaultInstallPath, recursive = TRUE)
      }

      # unregister driver
      if (identical(tolower(Sys.info()["sysname"][[1]]), "windows")) {
         .rs.connectionUnregisterWindowsDriver(driverName)
      }
      else {
         .rs.connectionUnregisterOdbcinstDriver(driverName)
      }

      # if driver was not installed in default location
      if (!defaultInstallExists) {
         list(
            message = .rs.scalar(
               paste(
                  "The", driverName, "driver was not found in the default installation path;",
                  "if appropriate, please manually remove this driver."
               )
            )
         )
      }
      else {
         list(
         )
      }
   }, error = function(e) {
      list(
         error = .rs.scalar(e$message)
      )
   })
})

.rs.addJsonRpcHandler("update_odbc_installers", function() {
   installerUrl <- getOption("connections-installer")
   connectionsWarning <- NULL

   if (!.rs.isDesktop()) return(list())

   # once per session, attempt to download driver updates
   if (!is.null(installerUrl) && nchar(installerUrl) > 0) {
      installerHostName <- gsub("https?://|/[^:].+$", "", installerUrl)

      connectionsWarning <- tryCatch({
         installersFile <- file.path(tempdir(), basename(installerUrl))
         download.file(installerUrl, installersFile, quiet = TRUE)

         untar(installersFile, exdir = .rs.connectionOdbcInstallerPath())

         NULL
      }, error = function(e) {
         paste(
            "Could not retrieve driver updates from ",
            installerHostName,
            sep = ""
         )
      })
   }

   list(
      warning = connectionsWarning
   )
})
