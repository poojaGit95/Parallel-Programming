import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

public class InvertedIndexing{
    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
	JobConf conf;
	private Text word;
	private Text fileName;
		
		public void configure(JobConf job) {
			this.conf = job;
		}

	public void map(LongWritable docId, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
		// retrieve # keywords from JobConf
			int argc = Integer.parseInt(conf.get("argc"));
			// put args into a String array
			Set<String> set = new HashSet();
			// retrieve keywords
			for (int i = 0; i < argc; i++) {
				set.add(conf.get("keyword" + i));
			}
			// get the current file name
			FileSplit fileSplit = (FileSplit)reporter.getInputSplit();
			String filename = "" + fileSplit.getPath().getName();
			String line = value.toString();
			StringTokenizer tokenizer = new StringTokenizer(line);
			//collect if next token match one of the args
			while (tokenizer.hasMoreTokens()) {
				String curr = tokenizer.nextToken();
				if (set.contains(curr)) {
					word = new Text(curr);
					fileName = new Text(filename);
					output.collect(word, fileName);
				}
			}
    	}
	}
	 
    public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
	public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {

		//HashMap to store the file name and the occurence of the keyword
		HashMap<String, Integer> map = new HashMap<String, Integer>();

		while(values.hasNext()) {
			String fileName = values.next().toString();
			if(map.containsKey(fileName)) {
				map.put(fileName, map.get(fileName) + 1);
			}
			else{
				map.put(fileName, 1);
			}
		}

		//Custom Comparator to sort files in ascending order of the word count
		Comparator<HashMap.Entry<String, Integer>> mapComparator = new Comparator<HashMap.Entry<String, Integer>>(){
			@Override
			public int compare(HashMap.Entry<String, Integer> a , HashMap.Entry<String, Integer> b) {
				int first = a.getValue();
				int second = b.getValue();
				if(first > second){
					return 1;
				}
				else{
					return -1;
				}
			}
		};

		List<HashMap.Entry<String, Integer>> list = new ArrayList<HashMap.Entry<String, Integer>>(map.entrySet());

		//Sorting the list
		Collections.sort(list, mapComparator);

		//Appending the output to a string
		StringBuilder sb = new StringBuilder();
		for(HashMap.Entry<String, Integer> entry : list) {
			sb.append(entry.getKey());
			sb.append(" ");
			sb.append(entry.getValue());
			sb.append(" ");
		}
		Text docListText = new Text(sb.toString());
		output.collect(key, docListText);
	}
    }
    
    public static void main(String[] args) throws Exception {
	JobConf conf = new JobConf(InvertedIndexing.class);
	conf.setJobName("invertedIndex");
	
	conf.setOutputKeyClass(Text.class);
	conf.setOutputValueClass(Text.class);
	
	conf.setMapperClass(Map.class);
	conf.setReducerClass(Reduce.class);
	
	conf.setInputFormat(TextInputFormat.class);
	conf.setOutputFormat(TextOutputFormat.class);
	
	FileInputFormat.setInputPaths(conf, new Path(args[0]));
	FileOutputFormat.setOutputPath(conf, new Path(args[1]));
	
    conf.set("argc", String.valueOf(args.length-2));
    for(int i = 0; i < args.length-2; i++){
        conf.set("keyword" + i, args[i+2]);
    }
	JobClient.runJob(conf);
    }
}