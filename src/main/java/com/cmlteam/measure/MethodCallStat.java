package com.cmlteam.measure;

/** Piece of profiling data collected for a method call being profiled. */
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
