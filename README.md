# MDS Connector

NOTE: the `vault-filesystem` extension has been deprecated and it's not available anymore on EDC 0.6.2 on. Currently we're
using in memory implementation, to be replaced. 

## Build

Build project
```
./gradlew build
```

### In Memory connector

Build docker image
```
docker build --build-context runtime=connector-inmemory launchers
```

### Vault connector
```
docker build --build-context runtime=connector-vault launchers
```
