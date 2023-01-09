from pyspark.sql.types import StructType
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
        # self.replacement_param = params.get("replacementValue", None)
        self.replacement_param = params["replacementValue"]
        self.replacement_col = params.get("replacementColumn", None)

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
                if not output_df:
                    output_df = koverse_input_df.withColumn(self.replacement_col, F.when(F.col(self.replacement_col) == "", None).otherwise(F.col(self.replacement_col)))
                    output_df = output_df.withColumn(self.replacement_col, F.when(F.col(self.replacement_col) == "NULL", None).otherwise(F.col(self.replacement_col)))
                    output_df = output_df.fillna(value=self.replacement_param, subset=[self.replacement_col])
                else:
                    output_df = output_df.union(koverse_input_df.fillna(value=self.replacement_param, subset=[self.replacement_col]))

        else:
            output_df = context.sqlContext.createDataFrame(
                context.sparkContext.emptyRDD(),
                StructType([])
            )
        return output_df
