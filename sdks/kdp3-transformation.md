KDP3 provides the functionality to apply transformations to incoming data.   
To start a project containing a custom transform, we’ll need to include the Koverse SDK.
In this example we’ll use the previously used Titanic dataset, parsing the ticket prefix into it's own column.

Transforms can be done in both Java/Scala and Python. In this case, we'll be using Python. 

First we’ll start off by creating a PySparkTransform class:

```python
from pyspark.sql.types import StructType


class PySparkTransform(object):
```

We don't need to pass in any user parameters or save class parameters, so we initialize the class with nothing.

```python
def __init__(self, params):
    pass
```
Next, we define the execute method, with takes a spark context. Here, we can parse out the prefix of the ticket into it's own column.
```python
def execute(self, context):

    output_df = None
    if len(context.inputDataFrames) > 0:
        for koverse_dataset_id, koverse_input_df in context.inputDataFrames.items():
            if len(koverse_input_df.columns) > 0:
                if output_df:
                    output_df = output_df.union(koverse_input_df)
                else:
                    output_df = koverse_input_df
    else:
        output_df = context.sqlContext.createDataFrame(
            context.sparkContext.emptyRDD(),
            StructType([])
        )
    return output_df
```

