package com.cmlteam.measure;

public class MethodCallStat {

  final String name;
  long t0;
  int depth;
  long durationMillis;
  int callsCount;

  public MethodCallStat(String name) {
    this.name = name;
  }
}
