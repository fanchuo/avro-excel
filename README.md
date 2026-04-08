# AvroExcel

## Context: csv or xlsx ?

When contributing to a project that involves data exchange between different stakeholders,
you eventually will end up to choose several encoding technologies. The CSV format is one of
those options, and its big plus are its simplicity and the fact it could be read and
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

You can use the [CLI module](./cli) or directly the [Excel API](./excel).

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


| field_txt | field_num | field_bool | field_date | field_time    |
| --------- | --------- | ---------- | ---------- | ------------- |
| a         | 1.2       | TRUE       | 1/10/26    | 1/10/26 18:50 |
| 2         | 1.2       | FALSE      | 1/12/26    | 2/10/26 15:40 |
| a         | b         | NOT_A_BOOL | 1/10/27    | 3/10/26 18:51 |

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
        { "name": "creation_date", "type": { "type": "long", "logicalType": "timestamp-millis" } }
    ]
}
```

##### Collections

<table>
  <tr>
    <th rowspan="2">name</th>
    <th colspan="2">some_list</th>
  </tr>
  <tr>
    <th>*size</th>
    <th>*</th>
  </tr>
  <tr>
    <td>Adam</td>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>
  <tr>
    <td>Ben</td>
    <td>*</td>
    <td>&nbsp;</td>
  </tr>
  <tr>
    <td>Charlie</td>
    <td>*</td>
    <td>123.4</td>
  </tr>
  <tr>
    <td rowspan="2">Daniel</td>
    <td rowspan="2">*</td>
    <td>123.4</td>
  </tr>
  <tr>
    <td>3.14</td>
  </tr>
</table>

This can be validated with the following schema:

```json
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "name", "type": "string"},
    {
      "name": "some_list",
      "type": {
        "type": "array",
        "items": ["double", "null"]
      }
    }
  ]
}
```

To represent a list, let's use 2 special columns: '\*' and '\*size'. The '\*size' column is here to give
the size of the embedded collection, and '\*' the items in the list.

The absence of value in column '\*size' means the list is empty for record 'Adam'. Then to make it visual,
the cell spreads over all the elements of the list. Here the values are scalars, but it could even be a struct
or another list

This is a 2x2 matrix

<table>
  <tr>
    <th rowspan="3">name</th>
    <th colspan="3">matrix</th>
  </tr>
  <tr>
    <th rowspan="2">*size</th>
    <th colspan="2">*</th>
  </tr>
  <tr>
    <th>*size</th>
    <th>*</th>
  </tr>
  <tr>
    <td>Some matrix</td>
    <td rowspan="4">*</td>
    <td rowspan="2">*</td>
    <td>1</td>
  </tr>
  <tr>
    <td></td>
    <td>2</td>
  </tr>
  <tr>
    <td></td>
    <td rowspan="2">*</td>
    <td>3</td>
  </tr>
  <tr>
    <td></td>
    <td>4</td>
  </tr>
</table>

And this is a list of points

<table>
  <tr>
    <th rowspan="3">name</th>
    <th colspan="3">points_list</th>
  </tr>
  <tr>
    <th rowspan="2">*size</th>
    <th colspan="2">*</th>
  </tr>
  <tr>
    <th>x</th>
    <th>y</th>
  </tr>
  <tr>
    <td>Some polygon</td>
    <td rowspan="2">*</td>
    <td>123</td>
    <td>456</td>
  </tr>
  <tr>
    <td></td>
    <td>98</td>
    <td>765</td>
  </tr>
</table>

For dictionaries, this is likely, but the column names are '\#size' '\#k' and '\#v'.

<table>
  <tr>
    <th rowspan="2">name</th>
    <th colspan="3">map_example</th>
  </tr>
  <tr>
    <th>#size</th>
    <th>#k</th>
    <th>#v</th>
  </tr>
  <tr>
    <td>Some map</td>
    <td rowspan="2">#</td>
    <td>a</td>
    <td>b</td>
  </tr>
  <tr>
    <td></td>
    <td>c</td>
    <td>d</td>
  </tr>
</table>
