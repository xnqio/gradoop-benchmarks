/*
 * Copyright © 2014 - 2019 Leipzig University (Database Research Group)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradoop.benchmarks.grouping;

import com.google.common.collect.Lists;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.gradoop.benchmarks.AbstractRunner;
import org.gradoop.flink.model.api.functions.AggregateFunction;
import org.gradoop.flink.model.api.operators.UnaryBaseGraphToBaseGraphOperator;
import org.gradoop.flink.model.impl.epgm.LogicalGraph;
import org.gradoop.flink.model.impl.operators.aggregation.functions.count.Count;
import org.gradoop.flink.model.impl.operators.aggregation.functions.max.MaxProperty;
import org.gradoop.flink.model.impl.operators.aggregation.functions.min.MinProperty;
import org.gradoop.flink.model.impl.operators.grouping.Grouping;
import org.gradoop.flink.model.impl.operators.grouping.GroupingStrategy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * A dedicated program for parametrized graph grouping benchmark.
 */
public class GroupingBenchmark extends AbstractRunner {

  /**
   * Option to declare path to input graph
   */
  private static final String OPTION_INPUT_PATH = "i";
  /**
   * Option to declare input graph format (csv, indexed, json)
   */
  private static final String OPTION_INPUT_FORMAT = "f";
  /**
   * Option to declare path to output graph
   */
  private static final String OPTION_OUTPUT_PATH = "o";
  /**
   * Option to set the grouping strategy
   */
  private static final String OPTION_GROUPING_STRATEGY = "s";
  /**
   * Vertex grouping key option
   */
  private static final String OPTION_VERTEX_GROUPING_KEY = "vgk";
  /**
   * EPGMEdge grouping key option
   */
  private static final String OPTION_EDGE_GROUPING_KEY = "egk";
  /**
   * Use vertex label option
   */
  private static final String OPTION_USE_VERTEX_LABELS = "uvl";
  /**
   * Use edge label option
   */
  private static final String OPTION_USE_EDGE_LABELS = "uel";
  /**
   * Path to CSV log file
   */
  private static final String OPTION_CSV_PATH = "csv";
  /**
   * Used vertex aggregator functions (min, max, count, none)
   */
  private static final String OPTION_VERTEX_AGGREGATION_FUNCS = "vagg";
  /**
   * Used vertex aggregation keys
   */
  private static final String OPTION_VERTEX_AGGREGATION_KEYS = "vak";
  /**
   * Used EPGMEdge aggregator functions (min, max, count, none)
   */
  private static final String OPTION_EDGE_AGGREGATION_FUNCS = "eagg";
  /**
   * Used vertex aggregation keys
   */
  private static final String OPTION_EDGE_AGGREGATION_KEYS = "eak";
  /**
   * Grouping strategy
   */
  private static GroupingStrategy STRATEGY = GroupingStrategy.GROUP_REDUCE;
  /**
   * Used VertexKey for grouping
   */
  private static String VERTEX_GROUPING_KEYS;
  /**
   * Used EdgeKey for grouping
   */
  private static String EDGE_GROUPING_KEYS;
  /**
   * Used csv path
   */
  private static String CSV_PATH;
  /**
   * Used hdfs INPUT_PATH
   */
  private static String INPUT_PATH;
  /**
   * Used INPUT_FORMAT
   */
  private static String INPUT_FORMAT;
  /**
   * Used hdfs OUTPUT_PATH
   */
  private static String OUTPUT_PATH;
  /**
   * Used vertex aggregators
   */
  private static String VERTEX_AGGREGATORS;
  /**
   * Used vertex aggregator keys
   */
  private static String VERTEX_AGGREGATOR_KEYS;
  /**
   * Used edge aggregators
   */
  private static String EDGE_AGGREGATORS;
  /**
   * Used edge aggregator keys
   */
  private static String EDGE_AGGREGATOR_KEYS;
  /**
   * Uses VertexLabels
   */
  private static boolean USE_VERTEX_LABELS;
  /**
   * Uses EdgeLabels
   */
  private static boolean USE_EDGE_LABELS;
  /**
   * Token separator for input strings
   */
  private static final Pattern TOKEN_SEPARATOR = Pattern.compile(",");


  static {
    OPTIONS.addOption(OPTION_INPUT_PATH, "graph-input-path", true,
      "Directory of the input graph");
    OPTIONS.addOption(OPTION_INPUT_FORMAT, "input-format", true,
      "Format of the input [csv, indexed] Default: " + DEFAULT_FORMAT);
    OPTIONS.addOption(OPTION_OUTPUT_PATH, "output-path", true,
      "Path to the output graph");
    OPTIONS.addOption(OPTION_GROUPING_STRATEGY, "strategy", true,
      "Grouping strategy (GR, GC)");
    OPTIONS.addOption(OPTION_USE_VERTEX_LABELS, "use-vertex-labels", false,
      "Group on vertex labels");
    OPTIONS.addOption(OPTION_USE_EDGE_LABELS, "use-edge-labels", false,
      "Group on edge labels");
    OPTIONS.addOption(OPTION_VERTEX_GROUPING_KEY, "vertex-grouping-key", true,
      "EPGMProperty key to group vertices on");
    OPTIONS.addOption(OPTION_EDGE_GROUPING_KEY, "edge-grouping-key", true,
      "EPGMProperty key to group edges on");
    OPTIONS.addOption(OPTION_CSV_PATH, "csv-path", true, "Path of the " +
      "generated CSV-File");
    OPTIONS.addOption(OPTION_VERTEX_AGGREGATION_FUNCS, "vertex-aggregator",
      true, "Applied aggregation functions on vertices (min, max, count, none or" +
        " list of these)");
    OPTIONS.addOption(OPTION_VERTEX_AGGREGATION_KEYS,
      "vertex-aggregation-keys", true, "Keys for the vertex aggregation" +
        "functions (needed by max and min aggregation)");
    OPTIONS.addOption(OPTION_EDGE_AGGREGATION_FUNCS, "edge-aggregator", true,
      "Applied aggregation functions on edges (min, max, count, none or list of these)");
    OPTIONS.addOption(OPTION_EDGE_AGGREGATION_KEYS, "edge-aggregation-keys",
      true, "keys for the edge aggregation functions (needed by max and min " +
        "aggregation)");
  }

  /**
   * Main program to run the benchmark. Arguments are the available options.
   *
   * @param args program arguments
   * @throws Exception on failure
   */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    CommandLine cmd = parseArguments(args, GroupingBenchmark.class.getName());
    if (cmd == null) {
      return;
    }
    // test if minimum arguments are set
    performSanityCheck(cmd);

    // read cmd arguments
    readCMDArguments(cmd);

    // initialize EPGM database
    LogicalGraph graphDatabase = readLogicalGraph(INPUT_PATH, INPUT_FORMAT);

    // initialize grouping keys
    List<String> vertexKeys = Lists.newArrayList();
    if (VERTEX_GROUPING_KEYS != null) {
      vertexKeys = getKeys(VERTEX_GROUPING_KEYS);
    }

    List<String> edgeKeys = Lists.newArrayList();
    if (EDGE_GROUPING_KEYS != null) {
      edgeKeys = getKeys(EDGE_GROUPING_KEYS);
    }

    // initialize aggregators
    List<AggregateFunction> vAggregators = Lists.newArrayList();
    List<AggregateFunction> eAggregators = Lists.newArrayList();

    if (cmd.hasOption(OPTION_VERTEX_AGGREGATION_FUNCS)) {
      vAggregators =
        getAggregators(VERTEX_AGGREGATORS, VERTEX_AGGREGATOR_KEYS);
    }

    if (cmd.hasOption(OPTION_EDGE_AGGREGATION_FUNCS)) {
      eAggregators = getAggregators(EDGE_AGGREGATORS, EDGE_AGGREGATOR_KEYS);
    }
    // build grouping operator
    Grouping grouping = (Grouping) getOperator(STRATEGY,
      vertexKeys, edgeKeys, USE_VERTEX_LABELS, USE_EDGE_LABELS, vAggregators,
      eAggregators);

    // call grouping on whole database graph
    LogicalGraph summarizedGraph = graphDatabase.callForGraph(grouping);
    if (summarizedGraph != null) {
      writeLogicalGraph(summarizedGraph, OUTPUT_PATH);
      writeCSV();
    } else {
      System.err.println("wrong parameter constellation");
    }
  }


  /**
   * Checks if the minimum of arguments is provided
   *
   * @param cmd command line
   */
  private static void performSanityCheck(final CommandLine cmd) {
    if (!cmd.hasOption(OPTION_INPUT_PATH)) {
      throw new IllegalArgumentException("Define a graph input directory.");
    }
    if (!cmd.hasOption(OPTION_CSV_PATH)) {
      throw new IllegalArgumentException("Path to CSV-File need to be set");
    }
    if (!cmd.hasOption(OPTION_VERTEX_GROUPING_KEY) &&
      !cmd.hasOption(OPTION_USE_VERTEX_LABELS)) {
      throw new IllegalArgumentException(
        "Chose at least one vertex grouping key or use vertex labels.");
    }
    if (!cmd.hasOption(OPTION_VERTEX_AGGREGATION_FUNCS)) {
      throw new IllegalArgumentException("Vertex aggregator need to be set! " +
        "(max, min, count, none (or list of these)");
    }
    if (!cmd.hasOption(OPTION_EDGE_AGGREGATION_FUNCS)) {
      throw new IllegalArgumentException("Edge aggregator need to be set! " +
        "(max, min, count, none (or list of these)");
    }

  }

  /**
   * Reads the given arguments from command line
   *
   * @param cmd command line
   */
  private static void readCMDArguments(final CommandLine cmd) {
    // read input output paths
    INPUT_PATH = cmd.getOptionValue(OPTION_INPUT_PATH);
    OUTPUT_PATH = cmd.getOptionValue(OPTION_OUTPUT_PATH);
    CSV_PATH = cmd.getOptionValue(OPTION_CSV_PATH);

    // input format
    INPUT_FORMAT = cmd.hasOption(OPTION_INPUT_FORMAT) ?
      cmd.getOptionValue(OPTION_INPUT_FORMAT).toLowerCase() : DEFAULT_FORMAT;

    // initialize grouping strategy
    if (cmd.hasOption(OPTION_GROUPING_STRATEGY)) {
      String value = cmd.getOptionValue(OPTION_GROUPING_STRATEGY);
      if (value.toUpperCase().equals("GC")) {
        STRATEGY = GroupingStrategy.GROUP_COMBINE;
      }
    }

    // read if vertex or edge keys should be used
    boolean useVertexKey = cmd.hasOption(OPTION_VERTEX_GROUPING_KEY);
    VERTEX_GROUPING_KEYS =
      useVertexKey ? cmd.getOptionValue(OPTION_VERTEX_GROUPING_KEY) : null;
    boolean useEdgeKey = cmd.hasOption(OPTION_EDGE_GROUPING_KEY);
    EDGE_GROUPING_KEYS =
      useEdgeKey ? cmd.getOptionValue(OPTION_EDGE_GROUPING_KEY) : null;

    // read vertex and edge labels
    USE_VERTEX_LABELS = cmd.hasOption(OPTION_USE_VERTEX_LABELS);
    USE_EDGE_LABELS = cmd.hasOption(OPTION_USE_EDGE_LABELS);

    // read aggregators
    VERTEX_AGGREGATORS = cmd.getOptionValue(OPTION_VERTEX_AGGREGATION_FUNCS);
    boolean vertexAggKeys = cmd.hasOption(OPTION_VERTEX_AGGREGATION_KEYS);
    if (vertexAggKeys) {
      VERTEX_AGGREGATOR_KEYS =
        cmd.getOptionValue(OPTION_VERTEX_AGGREGATION_KEYS);
    }

    EDGE_AGGREGATORS = cmd.getOptionValue(OPTION_EDGE_AGGREGATION_FUNCS);
    boolean edgeAggKeys = cmd.hasOption(OPTION_EDGE_AGGREGATION_KEYS);
    if (edgeAggKeys) {
      EDGE_AGGREGATOR_KEYS = cmd.getOptionValue(OPTION_EDGE_AGGREGATION_KEYS);
    }
  }

  /**
   * Method to get keys as list
   *
   * @param keys keys string
   * @return keys as list
   */
  private static List<String> getKeys(String keys) {
    keys = keys.replace("\\s", "");
    return Arrays.asList(TOKEN_SEPARATOR.split(keys));
  }

  /**
   * Method to build aggregators
   *
   * @param aggs        aggregators as whole string
   * @param keys        aggregator keys as whole string
   * @return List of PropertyValueAggregators
   */
  private static List<AggregateFunction> getAggregators(String
    aggs, String keys) {

    checkIfEnoughKeys(aggs,keys);
    if(keys==null){
      keys = " ";
    }

    List<AggregateFunction> aggregatorList = Lists.newArrayList();
    List<String> aggsList = getKeys(aggs);
    List<String> keyList = getKeys(keys);

    //index variable j makes sure that count aggregate functions do not need an aggregator key
    int j = 0;
    for (int i = 0; i < aggsList.size(); i++) {
      switch (aggsList.get(i)) {
      case "count":
        aggregatorList.add(new Count());
        break;
      case "max":
        aggregatorList.add(new MaxProperty(keyList.get(j)));
        j++;
        break;
      case "min":
        aggregatorList.add(new MinProperty(keyList.get(j)));
        j++;
        break;
      default:
        aggregatorList.add(null);
        break;
      }
    }
    return aggregatorList;
  }

  /**
   * Returns the grouping operator implementation based on the given strategy.
   *
   * @param strategy        grouping strategy to use
   * @param vertexKeys      vertex property keys used for grouping
   * @param edgeKeys        edge property keys used for grouping
   * @param useVertexLabels use vertex label for grouping, true/false
   * @param useEdgeLabels   use edge label for grouping, true/false
   * @param vAggs           used vertex aggregators
   * @param eAggs           used edge aggregators
   * @return grouping operator implementation
   */
  private static UnaryBaseGraphToBaseGraphOperator<LogicalGraph> getOperator(GroupingStrategy strategy,
    List<String> vertexKeys, List<String> edgeKeys,
    boolean useVertexLabels, boolean useEdgeLabels,
    List<AggregateFunction> vAggs, List<AggregateFunction> eAggs) {

    Grouping.GroupingBuilder builder =
      new Grouping.GroupingBuilder()
        .setStrategy(strategy)
        .useVertexLabel(useVertexLabels)
        .useEdgeLabel(useEdgeLabels);

    if (vAggs.size() > 0) {
      for (AggregateFunction agg:vAggs) {
        if (agg != null) {
          builder.addVertexAggregateFunction(agg);
        }
      }
    }

    if (eAggs.size() > 0) {
      for (AggregateFunction agg: eAggs) {
        if (agg != null) {
          builder.addEdgeAggregateFunction(agg);
        }
      }
    }

    if (vertexKeys.size() > 0) {
      for (String vKey : vertexKeys) {
        builder.addVertexGroupingKey(vKey);
      }
    }

    if (edgeKeys.size() > 0) {
      for (String eKey : edgeKeys) {
        builder.addEdgeGroupingKey(eKey);
      }
    }
    return builder.build();
  }

  /**
   * Method to create and add lines to a csv-file
   * @throws IOException on failure
   */
  private static void writeCSV() throws IOException {

    String head = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s%n",
      "Parallelism", "dataset", "vertexKeys", "edgeKeys", "USE_VERTEX_LABELS",
      "USE_EDGE_LABELS", "Vertex Aggregators", "Vertex-Aggregator-Keys",
      "EPGMEdge-Aggregators", "EPGMEdge-Aggregator-Keys", "Runtime(s)");

    String tail = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s%n",
      getExecutionEnvironment().getParallelism(), INPUT_PATH,
      VERTEX_GROUPING_KEYS, EDGE_GROUPING_KEYS, USE_VERTEX_LABELS,
      USE_EDGE_LABELS, VERTEX_AGGREGATORS, VERTEX_AGGREGATOR_KEYS,
      EDGE_AGGREGATORS, EDGE_AGGREGATOR_KEYS,
      getExecutionEnvironment().getLastJobExecutionResult()
        .getNetRuntime(TimeUnit.SECONDS));

    File f = new File(CSV_PATH);
    if (f.exists() && !f.isDirectory()) {
      FileUtils.writeStringToFile(f, tail, true);
    } else {
      PrintWriter writer = new PrintWriter(CSV_PATH, "UTF-8");
      writer.print(head);
      writer.print(tail);
      writer.close();
    }
  }

  /**
   * Method to make sure every min or max aggregation function gets a propertyKey
   *
   * @param aggs        aggregators as whole string
   * @param keys        aggregator keys as whole string
   * @return true if there are enough aggregation keys
   */
  private static boolean checkIfEnoughKeys(String aggs, String keys ) {

    List<String> aggsList = getKeys(aggs);
    if (keys == null) {
      if (aggsList.contains("max") || aggsList.contains("min")) {
        throw new IllegalArgumentException(
          "A propertyKey is needed for every min/max aggregation function. Use -vak or -eak options when " +
            "starting the program!");
      } else { return true; }
    } else {
      List<String> keyList = getKeys(keys);
      int i = 0;
      for (String s : aggsList) {
        if (s.equals("count")) {
          i++;
        }
      }
      if (aggsList.size() - i > keyList.size()) {
        throw new IllegalArgumentException(
          "A propertyKey is needed for every min/max aggregation function. Use -vak or -eak options when " +
            "starting the program!");
      }
      return true;
    }
  }
}
