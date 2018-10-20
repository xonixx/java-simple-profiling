package com.cmlteam.measure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MeasureFramework {
  private static final Logger log = LoggerFactory.getLogger(MeasureFramework.class);

  public static final int AGGREGATE_NUM = 20;
  public static final long ROTATE_AFTER_EACH = 100_000;
  static boolean configured = false;
  private static boolean exiting;
  private static File folder;
  private static Map<String, PrintStream> outs = new HashMap<>();
  private static Map<String, Long> logCountsInFile = new HashMap<>();
  private static Map<String, List<MeasureTransaction>> transactionLists = new HashMap<>();

  /** @param folderName folder name for measure logs or null = console */
  public static void configure(String folderName) {
    if (folderName != null) {
      folder = new File(folderName);
      if (!folder.exists()) {
        if (!folder.mkdirs()) throw new RuntimeException("Can't create folder: " + folderName);
      }
    }
    new LogTread().start();
    Runtime.getRuntime().addShutdownHook(new LogStop());
    configured = true;
  }

  static BlockingQueue<MeasureTransaction> queue = new ArrayBlockingQueue<>(1000, true);

  static void submitTransaction(MeasureTransaction measureTransaction) {
    queue.add(measureTransaction);
  }

  private static PrintStream getPrintStream(String transactionName, String suffix) {
    if (folder == null) {
      return System.out;
    }
    String fileName = transactionName.replaceAll("\\W+", "_"); // safe file name
    if (suffix != null) fileName += "." + suffix;
    fileName += ".log";

    PrintStream printStream = outs.get(fileName);

    Long cntInFile = logCountsInFile.get(fileName);
    if (cntInFile == null) {
      cntInFile = 0L;
    }
    logCountsInFile.put(fileName, ++cntInFile);

    if (printStream != null) {
      if (cntInFile <= ROTATE_AFTER_EACH) return printStream;

      // do rotate
      log.debug("Rotating '" + fileName + "' after " + ROTATE_AFTER_EACH + " records...");
      printStream.flush();
      printStream.close();
      outs.put(fileName, null);
      logCountsInFile.put(fileName, 0L);
      File from = new File(folder, fileName);
      File to =
          new File(
              folder, fileName + "." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
      try {
        Files.move(from.toPath(), to.toPath());
      } catch (IOException e) {
        log.error("Can't move " + from.getAbsolutePath() + " to " + to.getAbsolutePath(), e);
      }
    }

    try {
      File file = new File(folder, fileName);
      if (!file.exists()) file.createNewFile();
      printStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(file, true)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    outs.put(fileName, printStream);

    return printStream;
  }

  private static Map<String, Float> calcAggregation(List<MeasureTransaction> transactions) {
    Map<String, Long> agg = new LinkedHashMap<>();
    Map<String, Float> percents = new LinkedHashMap<>();
    long total = 0;
    for (MeasureTransaction tx : transactions) {
      for (ActionRecord record : tx.records) {
        Long value = agg.get(record.actionName);
        if (value == null) value = 0L;
        agg.put(record.actionName, value + record.durationMillis);
      }
      total += tx.durationMillis;
    }

    for (Map.Entry<String, Long> entry : agg.entrySet()) {
      percents.put(entry.getKey(), entry.getValue() * 100F / total);
    }
    return percents;
  }

  private static class LogTread extends Thread {
    @Override
    public void run() {
      try {
        while (!exiting) {
          try {
            MeasureTransaction measureTransaction = queue.take();
            String txName = measureTransaction.name;

            PrintStream printStream = getPrintStream(txName, null);
            printStream.println(measureTransaction);
            printStream.flush();

            List<MeasureTransaction> transactions = transactionLists.get(txName);
            if (transactions == null) {
              transactions = new ArrayList<>(AGGREGATE_NUM);
              transactionLists.put(txName, transactions);
            }

            transactions.add(measureTransaction);

            if (transactions.size() == AGGREGATE_NUM) {
              transactionLists.remove(txName);
              Map<String, Float> aggregations = calcAggregation(transactions);
              printStream = getPrintStream(txName, "percent");
              printStream.println(
                  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
              printStream.println("------{{ AGG: " + txName);
              for (Map.Entry<String, Float> entry : aggregations.entrySet()) {
                printStream.println(entry.getValue() + "% - " + entry.getKey());
              }
              printStream.println("------}}");
              printStream.flush();
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      } catch (Throwable t) {
        log.error("Unable to write performance data", t);
        configured = false;
      }
      for (PrintStream printStream : outs.values()) {
        printStream.flush();
      }
    }
  }

  private static class LogStop extends Thread {
    @Override
    public void run() {
      log.debug("Measure framework stopping...");
      exiting = true;
    }
  }
}
