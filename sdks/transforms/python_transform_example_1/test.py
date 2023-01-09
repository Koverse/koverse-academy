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
                'replacementValue': 'NA',
                'replacementColumn': 'Embarked'
            },
            PySparkTransform
        )

    def testSimpleRun(self):

        output = self.runner.testOnLocalData(self.test_data).select(['Embarked']).collect()

        output = [r.asDict()["Embarked"] for r in output if (r.asDict()["Embarked"] == "NA")]
    
        self.assertTrue( ((len(output) > 1) and (output[0] == "NA")) )
        

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
