import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

public class InvertedIndexingWithSort {

    public class CustomPartitioner extends Partitioner<KeyWordFilenamePair, Text> {
         @Override
         public int getPartition(KeyWordFilenamePair pair, Text text, int numberOfPartitions) {
             return Math.abs(pair.keyword.hashCode() % numberOfPartitions);
         }
    }

    public class CustomGroupingComparator extends WritableComparator {
         public CustomGroupingComparator() {
                 super(KeyWordFilenamePair.class, true);
         }
         /**
           * This comparator controls which keys are grouped
           * together into a single call to the reduce() method
          */
         @Override
         //here comparing the natural keys so that the keys are sorted accordingly.
         public int compare(WritableComparable wc1, WritableComparable wc2) {
             KeyWordFilenamePair pair = (KeyWordFilenamePair) wc1;
             KeyWordFilenamePair pair2 = (KeyWordFilenamePair) wc2;
                 return pair.keyword.compareTo(pair2.keyword);
         }
    }

    public class KeyWordFilenamePair implements Writable, WritableComparable<KeyWordFilenamePair>{
        private Text keyword = new Text();                 // natural key
        private IntWritable filename = new IntWritable(); // secondary key

        @Override
        public int compareTo(KeyWordFilenamePair pair) {
            int compareValue = this.keyword.compareTo(pair.keyword);
             if (compareValue == 0) {
                     compareValue = filename.compareTo(pair.filename);
             }
             return compareValue;
        }

        @Override
        public void write(DataOutput dataOutput) throws IOException {
            //do nothing
        }

        @Override
        public void readFields(DataInput dataInput) throws IOException {
            //do nothing
        }
    }

    public  static  class  Map  extends  MapReduceBase  implements  Mapper<LongWritable,  Text,  Text, Text> {
        JobConf conf;
        private Text compositeKey = new Text();
        private Text fileName = new Text();

        public void configure(JobConf job) {
            this.conf = job;
        }

        public void map(LongWritable docId, Text value, OutputCollector<Text, Text> output,
                        Reporter reporter) throws IOException {
            // retrieve # keywords from JobConf
            int argc = Integer.parseInt(conf.get("argc"));

            //adding all keywords to a HashSet
            Set<String> keywords = new HashSet<>();
            for(int i=0; i<argc; i++){
                keywords.add(conf.get("keyword"+i));
            }

            // get the current file name
            FileSplit fileSplit = (FileSplit) reporter.getInputSplit();
            String filename = "" + fileSplit.getPath().getName();

            //splitting each line into words
            String line = value.toString();
            StringTokenizer tokenizer = new StringTokenizer(line);
            while (tokenizer.hasMoreTokens()) {
                String curWord = tokenizer.nextToken();
                //collecting the word if it is one among the keywords
                if (keywords.contains(curWord)){
                    //here creating composite key
                    curWord = curWord + filename;
                    compositeKey.set(curWord);
                    fileName.set(filename);
                    output.collect(compositeKey, fileName);
                }
            }

        }
    }


    public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output,
                           Reporter reporter) throws IOException {
            //using a map to keep count of keywords for each file
            HashMap<String, Integer> docWordCount = new HashMap<String, Integer>();
            while(values.hasNext()){
                String curFileName = values.next().toString();
                int count = docWordCount.getOrDefault(curFileName, 0);
                docWordCount.put(curFileName, count+1);
            }
            // finally, print it out.
            StringBuilder sb = new StringBuilder();
            for (HashMap.Entry<String, Integer> entry : docWordCount.entrySet()) {
                sb.append(entry.getKey() + " ");
                sb.append(entry.getValue() + " ");
            }
            Text docListText = new Text(sb.toString());
            output.collect(key, docListText);

        }
    }


    public static void main(String[] args) throws Exception {
        // input format:
        // hadoop jar invertedindexes.jar InvertedIndexes input output keyword1 keyword2 ...
        JobConf conf = new JobConf(InvertedIndexing.class);  // AAAAA is this program’s file name
        conf.setJobName("invertedindexing");                 // BBBBB is a job name, whatever you like

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(Text.class);

        conf.setMapperClass(InvertedIndexing.Map.class);
        //conf.setCombinerClass(Reduce.class);
        conf.setReducerClass(InvertedIndexing.Reduce.class);
        conf.setPartitionerClass((Class<? extends org.apache.hadoop.mapred.Partitioner>) CustomPartitioner.class);
        conf.setOutputValueGroupingComparator(CustomGroupingComparator.class);

        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, new Path(args[0]));  // input directory name
        FileOutputFormat.setOutputPath(conf, new Path(args[1])); // output directory name

        conf.set( "argc", String.valueOf( args.length - 2 ) );   // argc maintains #keywords
        for ( int i = 0; i < args.length - 2; i++ )
            conf.set( "keyword" + i, args[i + 2] );              // keyword1, keyword2, ...

        long start = System.currentTimeMillis();
        JobClient.runJob(conf);
        long end = System.currentTimeMillis();
        long timeElapsed = end - start;
        System.out.println("Time elapsed: " + timeElapsed);

    }


}
