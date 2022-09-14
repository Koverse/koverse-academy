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
