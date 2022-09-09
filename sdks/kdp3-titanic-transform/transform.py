from pyspark.sql.types import StructType


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
        pass

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
