# flink-format-maxwellx

Enrichment for Flink [Maxwell Format](https://nightlies.apache.org/flink/flink-docs-release-1.12/dev/table/connectors/formats/maxwell.html) 1.12.2

## Dependencies

### Maven dependency(Incomplete)

```xml
<dependency>
  <groupId>com.github</groupId>
  <artifactId>flink-format-maxwellx</artifactId>
  <version>1.12.2</version>
</dependency>
```

### SQL Client JAR

Complie and package, put JAR file `flink-format-maxwellx-1.12.2.jar` into path  <FLINK_HOME>/lib/.

## How to use Maxwell format

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


SELECT * FROM product ;
```

## Format Opstions

| Option                                  | Required | Default | Type    | Description                                                                                                                                                |
| --------------------------------------- | -------- | ------- | ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| format                                  | required | (none)  | String  | Specify what format to use, here should be 'maxwellx-json'.                                                                                                |
| maxwellx-json.ignore-parse-errors       | optional | false   | Boolean | Skip fields and rows with parse errors instead of failing. Fields are set to null in case of errors.                                                       |
| maxwellx-json.timestamp-format.standard | optional | 'SQL'   | String  |                                                                                                                                                            |
| maxwellx-json.map-null-key.mode         | optional | 'FAIL'  | String  |                                                                                                                                                            |
| maxwellx-json.map-null-key.literal      | optional | 'null'  | String  |                                                                                                                                                            |
| maxwellx-json.database.include          | optional | (none)  | String  | Only read changelog rows which match the specific database (by comparing the "database" meta field in the Canal record).                                   |
| maxwellx-json.table.include             | optional | (none)  | String  | Only read changelog rows which match the specific table (by comparing the "table" meta field in the Canal record).                                         |
| maxwellx-json.delete.contains.old-field | optional | false   | Boolean | Optional flag to specify the deleted rows contained "old" field, instead of "data" field(In TiCDC, the deleted rows of maxwell message return "old" field) |
