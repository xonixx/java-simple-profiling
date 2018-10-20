package com.cmlteam.measure;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Represents the set of performance metrics and stats collected during single transaction
 * (typically web request or job run).
 */
class MeasureTransaction {

  final String name;
  long start;
  long durationMillis;
  long t0;
  long t;

  List<ActionRecord> records = new ArrayList<>();
  Map<String, MethodCallStat> methodCalls = new LinkedHashMap<>();
  Map<String, Integer> increments = new HashMap<>();

  public MeasureTransaction(String name) {
    this.name = name;
    start = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    double totalSec = durationMillis / 1000.;

    StringBuilder sb =
        new StringBuilder("======{{ ")
            .append(name)
            .append(' ')
            .append(totalSec)
            .append("s ")
            .append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date()))
            .append('\n');

    for (ActionRecord record : records) {
      sb.append("M | ")
          .append(record.durationMillis / 1000.)
          .append("s - ")
          .append(record.actionName)
          .append('\n');
    }

    if (methodCalls.size() > 0) {
      for (Map.Entry<String, MethodCallStat> entry : methodCalls.entrySet()) {
        MethodCallStat methodCallStat = entry.getValue();
        sb.append("C | ")
            .append(methodCallStat.durationMillis / 1000.)
            .append("s - ")
            .append(methodCallStat.callsCount)
            .append(" calls - ")
            .append(methodCallStat.name)
            .append('\n');
      }
    }

    sb.append("T | ").append(totalSec).append("s - TOTAL\n");

    for (Map.Entry<String, Integer> entry : increments.entrySet()) {
      sb.append("I | ").append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
    }

    sb.append("======}}");

    return sb.toString();
  }

  public void finish() {
    durationMillis = System.currentTimeMillis() - start;
  }
}
