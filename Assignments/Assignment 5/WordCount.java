
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;  
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.io.IOException;
import java.util.regex.Pattern;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.StringUtils; 


public class WordCount extends Configured implements Tool {

  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new WordCount(), args);
    System.exit(res);
  }

  public int run(String[] args) throws Exception {
    Job job = Job.getInstance(getConf(), "wordcount");

    job.addCacheFile(new Path(args[2]).toUri());

    job.setJarByClass(this.getClass());
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    job.setMapperClass(Map.class);
    job.setCombinerClass(Reduce.class);
    job.setReducerClass(Reduce.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    return job.waitForCompletion(true) ? 0 : 1;
  }

  public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();
    private long numRecords = 0;
    private String input;
    private Set<String> stopWordList = new HashSet<String>();
    private static final Pattern Word_Boundary = Pattern.compile("\\s*\\b\\s*");

    protected void setup(Mapper.Context context)throws IOException, InterruptedException {
      
      if (context.getInputSplit() instanceof FileSplit) {
        this.input = ((FileSplit) context.getInputSplit()).getPath().toString();
      } else {
        this.input = context.getInputSplit().toString();
      }

      URI[] localPaths = context.getCacheFiles();
      parseStopWordsFile(localPaths[0]);
    }


    private void parseStopWordsFile(URI patternsURI) {
      try {
        BufferedReader fis = new BufferedReader(new FileReader(new File(patternsURI.getPath()).getName()));
        String pattern;
        while ((pattern = fis.readLine()) != null) {
          stopWordList.add(pattern);
        }
      } catch (IOException ioe) {
        
      }
    }

    public void map(LongWritable offset, Text lineText, Context context) throws IOException, InterruptedException {
      String line = lineText.toString();
      
      line = line.toLowerCase();

      Text currentWord = new Text();
      for (String word : Word_Boundary.split(line)) {
        if (word.isEmpty() || stopWordList.contains(word)) {
            continue;
        }
            currentWord = new Text(word);
            context.write(currentWord,one);
        }             
    }
  }


  public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {
    
    @Override
    public void reduce(Text word, Iterable<IntWritable> counts, Context context)
        throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable count : counts) {
        sum += count.get();
      }
      context.write(word, new IntWritable(sum));
    }
  }
}

