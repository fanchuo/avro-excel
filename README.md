# AvroExcel

## Context: csv or xlsx ?

When contributing to a project that involves data exchange between different stakeholders,
you eventually will end up to choose many encoding technologies. The CSV format is one of
those options. The big plus about CSV are its simplicity and the fact it could be read and
edited using Excel.

So picking CSV means:
* Your data is well represented in tabular format
* It holds in Excel's 1 048 576 lines limit
* It is likely to be handled through Excel by human operators

However, there would be several pros using xlsx format instead of CSV:
* Date and numbers are natively supported
* Character encoding is no more a question
* Layout and formatting can be kept
* You do not need to operate _Export to CSV_ or _Import from CSV_

Also, the only aspects on which CSV is better than xlsx are not aspects on which CSV is really
good, such as compacity and stream ability. In fact, beyond prototyping, CSV is a disappointing solution.

So let's encode and decode data using directly xlsx files rather than csv files, with the help of
the [POI project](https://github.com/apache/poi).

## Features provided

You can use the [CLI module](./cli/README.md) or directly the [Excel API](./excel/README.md).

### Conversion xlsx/avro/parquet

Though xlsx is a good format for exchange, once you integrate it, you are more likely to keep
it as a avro or parquet. You can also obtain a well formatted Excel file from your avro or parquet.

### Schema validation

Let's get back to the data exchange situation. A critical operation to improve reliability of
your pipelines is to provide fast and explicit schema validation of the exchanged data. However,
there is no standard technology for CSV or xlsx schema validation. In the scope of this project,
I'll choose to integrate Avro schema to provide this, as it's a generic and complete solution.

#### Schema inference

Providing a schema is not mandatory if you just want to convert your Excel file. In this case
the schema is inferred. You can also just obtain the schema inferred from your spreadsheet.

#### Composite data structure

You can think of a way to encode composite data structures within CSV, but unless it remains
very simple the result is rarely visual and simple (that were the reason you picked CSV in the
first place). If you stick with Excel files, you can make usage of merged cells, that could be
used to provide a better visual representation of composite values.

#### Typical usage

##### Simple tabular data, with schema validation

Here is a spreadsheet:
|field_txt|field_num|field_bool |field_date|field_time    |
|---------|---------|-----------|----------|--------------|
|a        |1.2      |TRUE       |1/10/26   |1/10/26 18:50 |
|2        |1.2      |FALSE      |1/12/26   |2/10/26 15:40 |
|a        |b        |NOT_A_BOOL |1/10/27   |3/10/26 18:51 |

Consider this schema description:
```json
{
  "type": "record",
  "name": "test",
  "fields": [
    { "name": "field_txt", "type": "string" },
    { "name": "field_num", "type": "double" },
    { "name": "field_bool", "type": "boolean" },
    { "name": "field_date", "type": { "type": "int", "logicalType": "date" } },
    { "name": "field_time", "type": { "type": "long", "logicalType": "local-timestamp-millis" } }
  ]
}
```

The validation process is likely to raise the following error:
```
Caused by:
  [A4] Cannot match schema RECORD test [field_txt, field_num, field_bool, field_date, field_time]
  Caused by:
    [A4] Failed to match record for field field_num
    [B4] Cell type 'STRING' is not NUMERIC
```
In a spreadsheet, A4 is the cell at the first column, 4th line. So B4 is the cell that contains the value "b".
And as it's in the field_num column, it was expected to be a numeric value.

##### Composite data structure

Let's take advantage of merged cells for composite data structures.

<table>
  <tr>
    <th rowspan="2">name</th>
    <th colspan="2">favorite</th>
    <th rowspan="2">creation_date</th>
  </tr>
  <tr>
    <th>number</th>
    <th>color</th>
  </tr>
  <tr>
    <td>Alyssa</td>
    <td>256</td>
    <td>&nbsp;</td>
    <td>2025-01-01T00:00:00Z</td>
  </tr>
  <tr>
    <td>Ben</td>
    <td>7</td>
    <td>red</td>
    <td>2025-01-02T00:00:00Z</td>
  </tr>
  <tr>
    <td>Charlie</td>
    <td>&nbsp;</td>
    <td>blue</td>
    <td>2025-01-03T00:00:00Z</td>
  </tr>
</table>

This can be encoded using the following schema.
```json
{
    "type": "record",
    "name": "User",
    "fields": [
        {"name": "name", "type": "string"},
        {
          "name": "favorite",
          "type": {
            "type": "record",
            "name": "Favorite",
            "fields": [
              {"name": "number",  "type": ["int", "null"]},
              {"name": "color", "type": ["string", "null"]}
            ]
          }
        },
        {
          "name": "creation_date",
          "type": {
            "type": "long",
            "logicalType": "timestamp-millis"
          }
        }
    ]
}
```
