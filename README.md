# flink-format-maxwellx

**flink-format-maxwellx** is an enhanced extension of the original [Maxwell Format](https://nightlies.apache.org/flink/flink-docs-release-1.12/dev/table/connectors/formats/maxwell.html) used in Apache Flink, specifically designed for version 1.12.2. This plugin builds upon the standard Maxwell format, adding additional capabilities to better handle database changelogs and more flexible data parsing for real-time data streaming applications.

## Overview

Maxwell is a CDC (Change Data Capture) tool that streams changes from a MySQL database in the form of JSON. Flink’s Maxwell format allows Flink SQL to process those change events as part of a stream processing job. The `flink-format-maxwellx` plugin enhances the original Maxwell format by providing more configuration options, better error handling, and additional features like advanced filtering based on database and table names.

This plugin is useful in scenarios where users need more control over how database changes are captured, parsed, and handled within a Flink application, such as in environments where multiple databases and tables are being streamed, or where specific error handling behaviors are required.

## Features

- **Extended Configuration Options**: Additional options for fine-tuning how Maxwell JSON data is processed, including database/table filters and null-key handling.
- **Improved Error Handling**: More robust error handling options, allowing users to skip or nullify problematic rows instead of failing the entire stream.
- **Compatibility with TiCDC**: Provides support for use with TiCDC by correctly handling deleted rows with "old" fields in Maxwell messages.
- **Supports Complex Data Filtering**: Allows selective streaming of data based on specific database and table names, improving efficiency in scenarios where only a subset of data is needed.

## Dependencies

### Maven Dependency (Incomplete)

To include this plugin in your Maven project, add the following to your `pom.xml`:

```xml
<dependency>
  <groupId>com.github</groupId>
  <artifactId>flink-format-maxwellx</artifactId>
  <version>1.12.2</version>
</dependency>
```

### Flink SQL Client JAR

If you are using the Flink SQL Client, you need to compile and package the project. Once compiled, place the generated JAR file (`flink-format-maxwellx-1.12.2.jar`) into the `<FLINK_HOME>/lib/` directory to make it available for use.

## Usage Example: Using the MaxwellX Format in Flink SQL

Here is an example of how to define a Flink table using the MaxwellX format in a Flink SQL environment:

```sql
CREATE TABLE product (
  id BIGINT,
  name STRING,
  PRIMARY KEY (id) NOT ENFORCED
) WITH (
  'connector' = 'kafka',
  'topic' = 'tffi_maxwell_product',
  'properties.bootstrap.servers' = 'kafka:9092',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'maxwellx-json',
  'maxwellx-json.database.include' = 'tffi',
  'maxwellx-json.table.include' = 'product'
);

SELECT * FROM product;
```

In this example:
- The data is consumed from a Kafka topic (`tffi_maxwell_product`).
- The `maxwellx-json` format is used to parse the changelog.
- The `maxwellx-json.database.include` option ensures that only changes from the `tffi` database are processed.
- The `maxwellx-json.table.include` option filters the changes to only include rows from the `product` table.

## Format Options

The following options can be used to configure the `maxwellx-json` format. These options provide flexibility in error handling, filtering, and how null keys are managed.

| Option                                  | Required | Default | Type    | Description                                                                                                                                                |
| --------------------------------------- | -------- | ------- | ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `format`                                | Yes      | (none)  | String  | Specifies the format to use. This must be set to `'maxwellx-json'`.                                                                                         |
| `maxwellx-json.ignore-parse-errors`     | No       | `false` | Boolean | If `true`, rows with parsing errors will be skipped, and fields with errors will be set to `null`. If `false`, parsing errors will cause the job to fail.    |
| `maxwellx-json.timestamp-format.standard`| No      | `SQL`   | String  | Specifies the timestamp format standard. The default is `SQL`, which is compatible with the SQL standard for date and time formatting.                      |
| `maxwellx-json.map-null-key.mode`       | No       | `FAIL`  | String  | Defines how null keys in a map should be handled. Can be set to `FAIL` (fail on null keys) or `DROP` (drop rows with null keys).                            |
| `maxwellx-json.map-null-key.literal`    | No       | `null`  | String  | Defines the literal value to use when a map key is `null`. This is useful when you want to replace `null` map keys with a specific literal string.          |
| `maxwellx-json.database.include`        | No       | (none)  | String  | Filters the changelog to only include rows from the specified database. This is done by comparing the "database" field in the Maxwell JSON record.           |
| `maxwellx-json.table.include`           | No       | (none)  | String  | Filters the changelog to only include rows from the specified table. This is done by comparing the "table" field in the Maxwell JSON record.                 |
| `maxwellx-json.delete.contains.old-field`| No      | `false` | Boolean | If `true`, the deleted rows in the changelog contain an "old" field instead of a "data" field. This is useful for compatibility with TiCDC, where deleted rows have an "old" field. |

### Key Options Explained

- **`maxwellx-json.ignore-parse-errors`**: 
  - When set to `true`, Flink will skip rows that cannot be parsed correctly due to data issues, preventing the job from failing. This is useful in production environments where data quality may vary.
  - When set to `false` (default), parsing errors will cause the job to fail, ensuring strict data integrity.

- **`maxwellx-json.database.include` and `maxwellx-json.table.include`**:
  - These options allow you to filter the changelog stream so that only changes from specific databases and tables are processed. This can significantly reduce the amount of data being processed when dealing with large databases with multiple tables.
  
- **`maxwellx-json.delete.contains.old-field`**:
  - This option is crucial when working with TiCDC or similar CDC tools where deleted rows in Maxwell contain an "old" field. Setting this to `true` allows for proper handling of such records.

## Compatibility

This plugin is compatible with Flink 1.12.2 and works with the Maxwell format for JSON-encoded changelog data. It is especially useful in environments where Flink is used for CDC processing with MySQL, TiCDC, or other tools that rely on the Maxwell format.

## Building the Project

To build this project, ensure you have Maven installed. Run the following command to compile and package the JAR:

```bash
mvn clean package
```

After a successful build, the JAR file can be found in the `target/` directory. Place the JAR into your `<FLINK_HOME>/lib/` directory to make it available for Flink SQL jobs.
