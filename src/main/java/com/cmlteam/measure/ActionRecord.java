package com.cmlteam.measure;

class ActionRecord {
  final String actionName;
  final long durationMillis;

  public ActionRecord(String actionName, long durationMillis) {
    this.actionName = actionName;
    this.durationMillis = durationMillis;
  }
}
