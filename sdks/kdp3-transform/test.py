import os
from unittest import TestCase

from koverse.transformTest import PySparkTransformTestRunner

from transform import PySparkTransform

class TestFeaturesFromShellTransform(TestCase):
    TEST_DATA_DIRECTORY = 'test_data'

    def setUp(self):
        self.test_data = [
            self.make_csv_json(TestFeaturesFromShellTransform.TEST_DATA_DIRECTORY + '/' + test_file)
            for test_file in os.listdir(
                TestFeaturesFromShellTransform.TEST_DATA_DIRECTORY
            )
        ]
        self.runner = PySparkTransformTestRunner(
            {
                'ticket_col': 'Ticket'
            },
            PySparkTransform
        )

    def testSimpleRun(self):
        '''
        Test transform.py ( which currently just returns the dataframe as is )
        '''
        print("running tests")
        data_frame = self.runner.testOnLocalData(
            inputDatasets=self.test_data
        )

        list_columns = data_frame.columns

        data_frame = data_frame.collect()

        self.assertIsNotNone(data_frame)
        self.assertGreater(len(data_frame), 0)
        self.assertTrue("ticketPrefix" in list_columns)

    @staticmethod
    def make_csv_json(filename):
        '''
        convert csv into list of dicts for conversion to dataframe
        '''
        out_lst = []

        with open(filename) as fopen:
            fh = fopen.read().split('\n')
        keys = fh[0].split(",")
        for line in fh[1:]:
            out_lst.append({k: v for k, v in zip(keys, line.split(","))})
        return out_lst
