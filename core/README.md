# bupdater-core
Main functionality for updating a Super BOM with dependencies from multiple framework BOMs.
The code in this module is meant to be used either standalone via the [app](../app) or from within the Maven [plugin](../plugin).

It relies on the [MIni MAven](https://github.com/maveniverse/mima) (MIMA) library to resolve the dependencies
that should be pulled from the framework BOMs.
