# EDPS demo server

## Start mock server

```bash
python ./util/edps-mock-server/server.py
```

## Create Analysis Job
```bash
curl -X POST http://localhost:8081/v1/dataspace/analysisjob
```

## Post Job Data
```bash
curl -X POST -F "file=@data.csv" http://localhost:8081/v1/dataspace/analysisjob/40c70511-9427-43d1-811b-97231145cce1/data/data.csv
```

## Get Result
```bash
curl http://localhost:8081/v1/dataspace/analysisjob/8b5df83e-bf93-449c-af50-ae5766d57944/result -o result.zip
```


## Publish to Daseen
```bash
curl -X POST -F "file=@result-data.zip" http://localhost:8081/create-edp
```
