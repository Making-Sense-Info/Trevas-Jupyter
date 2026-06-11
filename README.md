# Trevas Jupyter

Jupyter notebook providing VTL support through Trevas engine

[![Build Status](https://github.com/Making-Sense-Info/Trevas-Jupyter/actions/workflows/ci.yml/badge.svg)](https://github.com/Making-Sense-Info/Trevas-Jupyter/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

## Usage

Run `mvn package` to bundle the VTL Kernel.

### Without Docker

TODO

### With Docker

```shell
docker build . -t jupyter_vtl
docker run -p 8888:8888 jupyter_vtl
```

## Custom functions

Custom functions have been introduced into the Trevas engine.

| Name                  | Arguments                                               | Returned type | Description                                                                             |
| --------------------- | ------------------------------------------------------- | ------------- | --------------------------------------------------------------------------------------- |
| loadParquet           | String url                                              | Dataset       | Load Parquet dataset                                                                    |
| loadCSV (1)           | String url                                              | Dataset       | Load CSV dataset                                                                        |
| loadSas               | String url                                              | Dataset       | Load Sas dataset                                                                        |
| loadSDMXEmptySource   | (String sdmxMesUrl, String structureId)                 | Dataset       | Load SDMX empty source                                                                  |
| loadSDMXSource (2)    | (String sdmxMesUrl, String structureId, String dataUrl) | Dataset       | Load SDMX source                                                                        |
| writeParquet          | (String url, Dataset ds)                                | String        | Write given dataset in Parquet                                                          |
| writeCSV (3)          | (String url, Dataset ds)                                | String        | Write given dataset in CSV                                                              |
| show                  | Dataset ds                                              | DisplayData   | Display firt rows of a given dataset                                                    |
| showMetadata          | Dataset ds                                              | DisplayData   | Display metadata of a given dataset                                                     |
| runSDMXPreview        | String sdmxMesUrl                                       | DisplayData   | Run SDMX VTL transformations, with empty datasets, to obtain Persitent defined datasets |
| runSDMX (4)           | (String sdmxMesUrl, String dataLocations )              | DisplayData   | Run SDMX VTL transformations, with sources, to obtain Persitent defined datasets        |
| getTransformationsVTL | String sdmxMesUrl                                       | String        | Display VTL transformations defined in the SDMX Message file                            |
| getRulesetsVTL        | String sdmxMesUrl                                       | String        | Display VTL rulesets defined in the SDMX Message file                                   |
| size                  | Dataset ds                                              | String        | Display size of a given dataset                                                         |

### Data locations

`loadCSV`, `writeCSV`, `loadParquet` and `writeParquet` accept the following location formats:

| Format | Example | Notes |
| ------ | ------- | ----- |
| Local path (relative) | `./data/file.csv` | Resolved from the current working directory of the Trevas kernel process |
| Local path (absolute) | `/home/jovyan/work/file.csv` | Typical path inside the Jupyter container |
| HTTPS | `https://example.com/data/file.csv` | Passed through to Spark |
| S3 | `s3://my-bucket/path/file.csv` | Requires AWS/S3 credentials in the kernel environment (see below) |

CSV options (`delimiter`, `quote`, `header`, …) can still be appended as URL query parameters on any of these formats (values must be URL-encoded), for example:

`loadCSV("s3://my-bucket/data.csv?delimiter=%7C")`

For `s3://` and `s3a://` URLs, credentials must be available to the **Trevas JVM process** (not only the Python kernel), for example via environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`) or Spark/Hadoop configuration (`fs.s3a.*`) provided by the platform (e.g. Onyxia).

### (1) loadCSV

Default option values:

|    Name     |  Value  |
|:-----------:|:-------:|
|   header    |  true   |
|  delimiter  |    ;    |
|    quote    |    "    |

Any CSV option can be defined or overridden thanks to url parameters (values have to be encoded).

For instance, to read a CSV content where delimiter is `|` and quote is `'`:

`loadCSV("s3://my-bucket/data.csv?delimiter=%7C&quote=%27")`

Or from a local file in the container:

`loadCSV("./data/file.csv")`

### (2) loadSDMXSource

Sources has to be `.csv` files for now.

### (3) writeCSV

Default option values:

|    Name     |  Value  |
|:-----------:|:-------:|
|   header    |  true   |
|  delimiter  |    ;    |
|    quote    |    "    |

Any CSV option can be defined or overridden thanks to url parameters (values have to be encoded).

For instance, to write a CSV with a content delimited by `|` and quoted by `'`:

- `writeCSV(...?delimiter=%7C&quote=%27)`
- `writeCSV("s3://my-bucket/out.csv?delimiter=%7C&quote=%27", ds)`

### (4) runSDMX

Sources has to be `.csv` files for now.

Second argument, `dataLocations` has to be a string separated field containing SDMX structure id and source location (ex: `structId1,dataLocation1,structId2,dataLocation2`)

## Launch with demo project

`INIT_PROJECT_URL` docker environment variable enable to load a default project in your Trevas Jupyter instance.

Have a look to [this project definition](https://github.com/Making-Sense-Info/Trevas-Jupyter-Training) for instance.

Fill the `INIT_PROJECT_URL` environment variable with your script adress and run:

```bash
docker pull makingsenseinfo/trevas-jupyter:latest
docker run -p 8888:8888 -e INIT_PROJECT_URL="https://raw.githubusercontent.com/Making-Sense-Info/Trevas-Jupyter-Training/main/init-notebook.sh" makingsenseinfo/trevas-jupyter:latest
```
