package com.cmlteam.measure;

/**
 * Represents single profiled record captured. Every MeasureTransaction contains multiple such
 * records.
 */
class ActionRecord {
  final String actionName;
  final long durationMillis;

  public ActionRecord(String actionName, long durationMillis) {
    this.actionName = actionName;
    this.durationMillis = durationMillis;
  }
}
