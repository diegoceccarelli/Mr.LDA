package cc.mrlda;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.google.common.base.Preconditions;

import edu.umd.cloud9.io.map.HMapIDW;
import edu.umd.cloud9.io.pair.PairOfIntFloat;

public class DisplayTopic2 extends Configured implements Tool {
  public static final String TOP_DISPLAY_OPTION = "topdisplay";
  public static final int TOP_DISPLAY = 10;

  @SuppressWarnings("unchecked")
  public int run(String[] args) throws Exception {
    Options options = new Options();

    options.addOption(Settings.HELP_OPTION, false, "print the help message");
    options.addOption(OptionBuilder.withArgName(Settings.PATH_INDICATOR).hasArg()
        .withDescription("input beta file").create(Settings.INPUT_OPTION));
    options.addOption(OptionBuilder.withArgName(Settings.PATH_INDICATOR).hasArg()
        .withDescription("term index file").create(ParseCorpusOptions.INDEX));
    options.addOption(OptionBuilder.withArgName(Settings.INTEGER_INDICATOR).hasArg()
        .withDescription("display top terms only (default - 10)").create(TOP_DISPLAY_OPTION));

    String betaString = null;
    String indexString = null;
    int topDisplay = TOP_DISPLAY;

    CommandLineParser parser = new GnuParser();
    HelpFormatter formatter = new HelpFormatter();
    try {
      CommandLine line = parser.parse(options, args);

      if (line.hasOption(Settings.HELP_OPTION)) {
        formatter.printHelp(ParseCorpus.class.getName(), options);
        System.exit(0);
      }

      if (line.hasOption(Settings.INPUT_OPTION)) {
        betaString = line.getOptionValue(Settings.INPUT_OPTION);
      } else {
        throw new ParseException("Parsing failed due to " + Settings.INPUT_OPTION
            + " not initialized...");
      }

      if (line.hasOption(ParseCorpusOptions.INDEX)) {
        indexString = line.getOptionValue(ParseCorpusOptions.INDEX);
      } else {
        throw new ParseException("Parsing failed due to " + ParseCorpusOptions.INDEX
            + " not initialized...");
      }
      if (line.hasOption(TOP_DISPLAY_OPTION)) {
        topDisplay = Integer.parseInt(line.getOptionValue(TOP_DISPLAY_OPTION));
      }
    } catch (ParseException pe) {
      System.err.println(pe.getMessage());
      formatter.printHelp(ParseCorpus.class.getName(), options);
      System.exit(0);
    } catch (NumberFormatException nfe) {
      System.err.println(nfe.getMessage());
      System.exit(0);
    }

    JobConf conf = new JobConf(DisplayTopic2.class);
    FileSystem fs = FileSystem.get(conf);

    Path indexPath = null;
    indexPath = new Path(indexString);
    Preconditions.checkArgument(fs.exists(indexPath) && fs.isFile(indexPath),
        "Invalid index path...");
    Path betaPath = new Path(betaString);
    Preconditions.checkArgument(fs.exists(betaPath) && fs.isFile(betaPath), "Invalid beta path...");

    SequenceFile.Reader sequenceFileReader = null;
    try {
      IntWritable intWritable = new IntWritable();
      Text text = new Text();
      Map<Integer, String> termIndex = new HashMap<Integer, String>();
      sequenceFileReader = new SequenceFile.Reader(fs, indexPath, conf);
      while (sequenceFileReader.next(intWritable, text)) {
        termIndex.put(intWritable.get(), text.toString());
      }
      PairOfIntFloat pairOfIntFloat = new PairOfIntFloat();
      // HMapIFW hmap = new HMapIFW();
      HMapIDW hmap = new HMapIDW();
      TreeMap<Double, Integer> treeMap = new TreeMap<Double, Integer>();
      sequenceFileReader = new SequenceFile.Reader(fs, betaPath, conf);
      
      
      // read the beta file:
      // id topic -> map with id->prob, for each term-id
      while (sequenceFileReader.next(pairOfIntFloat, hmap)) {
        treeMap.clear();

        System.out.println("==============================");
        System.out.println("Top ranked " + topDisplay + " terms for Topic "
            + pairOfIntFloat.getLeftElement());
        System.out.println("==============================");

        Iterator<Integer> itr1 = hmap.keySet().iterator();
        int temp1 = 0;
        while (itr1.hasNext()) {
          
          int id = itr1.next();
          double d = hmap.get(temp1);
          if (termIndex.containsKey(id)) {
        	  System.out.println(termIndex.get(id) + "\t" + d);
        	  
          }
        }

       
      }
    } finally {
      IOUtils.closeStream(sequenceFileReader);
    }

    return 0;
  }

  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new DisplayTopic2(), args);
    System.exit(res);
  }
}