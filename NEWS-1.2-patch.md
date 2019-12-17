
## RStudio v1.2 Patch 3 "Orange Blossom"


### Misc

* Add compatibility with recent versions of the `shinytest` package (#5677, #5703)
* Eliminate warnings when using `_R_CHECK_LENGTH_1_CONDITION_` (#5268, #5363)
* Ensure compatibility with Boost 1.72.0 (#5742)
* Set Server Name Indication (SNI) on outbound HTTPS requests for compatibility with TLS 1.3 (#5757)
* Session configuration option to customize environment variables saved when session suspends (#5769)

### Server Pro

* Add ability to configure Slurm command line tools location for the Slurm Launcher Plugin (#1298)
* Fix an issue where the Slurm service user must have a home directory for the Slurm Launcher Plugin (#1286)
* Fix an issue where errors are ignored when validating the version of Slurm for the Slurm Launcher Plugin (#1287)
* Add documentation for load balancing limitations, module support, and support for multiple versions of R with the Slurm Launcher Plugin (#1296)
* Fix an issue where sessions would sometimes stall while executing commands due to heavy processing in rserver-monitor (#1338)
