# Watchdog

Inspired by [watchdog timers](https://en.wikipedia.org/wiki/Watchdog_timer) in embedded systems, the Watchdog project is a Java toolkit that allows you to monitor your worker threads and potentially take corrective action when problems arise. Specially, watchdog provides 2 distinct features:

* Break out of hung worker threads (e.g. tight loops, hung I/O, etc..)
* Monitor worker thread activity (new objects, method entry, and branching operations)
