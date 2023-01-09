KDP3 provides the functionality to apply transformations to incoming data.   
To start a project containing a custom transform, we’ll need to include the Koverse SDK.
In this example we’ll use the previously used Titanic dataset, parsing the ticket prefix into it's own column.

Transforms can be done in both Java/Scala and Python. In this case, we'll be using Python. 

First we’ll start off by creating a PySparkTransform class:

```python
from pyspark.sql.types import StructType
from pyspark.sql.functions import split
from pyspark.sql import functions as F

class PySparkTransform(object):
    '''
    Accepts a dataset from koverse context and returns that data as a pySpark DataFrame
    --------
    PARAMS
    None
    --------
    RETURNS
    pyspark.sql.dataframe.DataFrame
    '''
    def __init__(self, params):
        self.ticket_col = params.get("ticket_col", None)
```

We initialize the parameters in the class initialization, taking in user-defined parameters. Note there must be atleast 1 inputCollection type parameter for the source dataset.

```yaml
id: titanic_ticket_prefix
name: 'Titanic Ticket Transform'
description: 'Separate the titanic Ticket column into ticketPrefix and Ticket columns'
parameters:
  -
      displayName: 'Input Collection'
      parameterName: 'input_coll'
      required: true
      type: 'inputCollection'
  -
      defaultValue: "msgType"
      displayName: 'Ticket Column'
      parameterName: 'ticket_col'
      required: true
      type: 'string'
version: '1.0.0'
supportsIncrementalProcessing: true
```

The user defined parameters come from the description file.
Next, we define the execute method, with takes a spark context. Here, we can parse out the prefix of the ticket into it's own column. 
```python
    def execute(self, context):
        '''
        Returns all of the dataframes in the context as a single spark DataFrame,
        or if no data is included, returns an empty spark DataFrame
        --------
        PARAMS
        context: koverse.client.getTransformContext
        --------
        RETURNS
        output_df: pyspark.sql.dataframe.DataFrame
        '''
        output_df = None
        if len(context.inputDataFrames) > 0:
            for koverse_dataset_id, koverse_input_df in context.inputDataFrames.items():
                list_columns = koverse_input_df.columns
                if len(koverse_input_df.columns) > 0 and self.ticket_col in list_columns:
                    
                    unmodified_rows = koverse_input_df.where(~F.col(self.ticket_col).contains(" "))
                    modified_rows = koverse_input_df.where(F.col(self.ticket_col).contains(" "))

                    modified_rows = modified_rows.withColumn("ticketPrefix", split(modified_rows[self.ticket_col], " ").getItem(0))\
                        .withColumn(self.ticket_col, split(modified_rows[self.ticket_col], " ").getItem(1))
                    
                    unmodified_rows = unmodified_rows.withColumn("ticketPrefix", F.lit(""))
                    
                    koverse_input_df = modified_rows.union(unmodified_rows)

                    if output_df:
                        output_df = output_df.union(koverse_input_df)
                    
                    else:
                        output_df = koverse_input_df

                else:
                    output_df = koverse_input_df

        else:
            output_df = context.sqlContext.createDataFrame(
                context.sparkContext.emptyRDD(),
                StructType([])
            )
        return output_df
```

We have an assembly file that defines certain addon parameters about the python transform.

```xml
<assembly
        xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2 http://maven.apache.org/xsd/assembly-1.1.2.xsd">
    <id>bundle</id>

    <formats>
        <format>zip</format>
    </formats>

    <fileSets>
        <fileSet>
            <includes>
                <include>description.yaml</include>
                <include>transform.py</include>
            </includes>
        </fileSet>
    </fileSets>

    <includeBaseDirectory>false</includeBaseDirectory>
</assembly>
```
By compressing the assembly.xml, description.yaml, and transform.py into a single file, we can create a python transform and upload it to the KDP addons.
