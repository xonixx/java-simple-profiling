package com.cmlteam.measure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/** Very simple API used to runtime(production-time) profiling of the code. */
public class Measure {

  private static final Logger log = LoggerFactory.getLogger(Measure.class);

  private static ThreadLocal<MeasureTransaction> measureStateTL = new ThreadLocal<>();

  /**
   * Start measurements tx. Transaction will consist of multiple actions measured. Optionaly
   * transaction will include some methods run time, if methods {@link #methodEnter(String)}/{@link
   * #methodExit(String)} used.
   *
   * @param transactionName tx name
   */
  public static void start(String transactionName) {
    if (!MeasureFramework.configured) return;

    MeasureTransaction currentTx = getMeasureTransaction();

    if (currentTx != null) {
      log.warn("Abandoned tx: {}", currentTx.name);
    }

    try {
      MeasureTransaction measureTransaction = new MeasureTransaction(transactionName);
      measureStateTL.set(measureTransaction);
      measureTransaction.t = System.currentTimeMillis();
      measureTransaction.t0 = measureTransaction.t;
    } catch (Throwable t) {
      log.warn(t.getMessage());
    }
  }

  private static MeasureTransaction getMeasureTransaction() {
    if (!MeasureFramework.configured) return null;

    return measureStateTL.get();
  }

  /** Start next measurement (action) */
  public static void startMeasure() {
    try {
      MeasureTransaction measureTransaction = getMeasureTransaction();
      if (measureTransaction == null) return;

      measureTransaction.t = System.currentTimeMillis();
    } catch (Throwable t) {
      log.warn(t.getMessage());
    }
  }

  public static void increment(String prop) {
    increment(prop, 1);
  }

  public static void increment(String prop, int increment) {
    try {
      MeasureTransaction measureTransaction = getMeasureTransaction();
      if (measureTransaction == null) return;

      Map<String, Integer> increments = measureTransaction.increments;
      Integer val = increments.get(prop);
      if (val == null) {
        val = 0;
      }
      val += increment;
      increments.put(prop, val);
    } catch (Throwable t) {
      log.warn(t.getMessage());
    }
  }

  /**
   * Record executed measurement (action). This is time passed after previous {@link
   * #startMeasure()} or this method invocation.
   *
   * @param actionName action name, this is named piece of code.
   */
  public static void recordMeasure(String actionName) {
    try {
      MeasureTransaction measureTransaction = getMeasureTransaction();
      if (measureTransaction == null) return;

      measureTransaction.records.add(
          new ActionRecord(actionName, System.currentTimeMillis() - measureTransaction.t));
      measureTransaction.t = System.currentTimeMillis();
    } catch (Throwable t) {
      log.warn(t.getMessage());
    }
  }

  public static void methodEnter(String methodName) {
    MeasureTransaction measureTransaction = getMeasureTransaction();
    if (measureTransaction == null) return;

    MethodCallStat methodCallStat = measureTransaction.methodCalls.get(methodName);

    if (methodCallStat == null) {
      methodCallStat = new MethodCallStat(methodName);
      measureTransaction.methodCalls.put(methodName, methodCallStat);
    }
    if (methodCallStat.depth == 0) {
      methodCallStat.t0 = System.currentTimeMillis();
    }
    methodCallStat.callsCount++;
    methodCallStat.depth++;
  }

  public static void methodExit(String methodName) {
    MeasureTransaction measureTransaction = getMeasureTransaction();
    if (measureTransaction == null) return;

    MethodCallStat methodCallStat = measureTransaction.methodCalls.get(methodName);

    if (methodCallStat == null) {
      throw new IllegalStateException("method exit without enter");
    } else {
      methodCallStat.depth--;
      if (methodCallStat.depth == 0) {
        methodCallStat.durationMillis += System.currentTimeMillis() - methodCallStat.t0;
      }
    }
  }

  public static void finish() {
    try {
      MeasureTransaction measureTransaction = getMeasureTransaction();
      if (measureTransaction == null) return;

      measureTransaction.finish();
      MeasureFramework.submitTransaction(measureTransaction);
      measureStateTL.set(null);
    } catch (Throwable t) {
      log.warn(t.getMessage());
    }
  }
}
