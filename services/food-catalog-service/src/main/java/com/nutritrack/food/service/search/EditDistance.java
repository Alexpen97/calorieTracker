package com.nutritrack.food.service.search;

public final class EditDistance {

  private EditDistance() {}

  public static int levenshtein(String a, String b) {
    if (a == null) {
      a = "";
    }
    if (b == null) {
      b = "";
    }
    int m = a.length();
    int n = b.length();
    int[] prev = new int[n + 1];
    int[] curr = new int[n + 1];

    for (int j = 0; j <= n; j++) {
      prev[j] = j;
    }

    for (int i = 1; i <= m; i++) {
      curr[0] = i;
      for (int j = 1; j <= n; j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
      }
      int[] swap = prev;
      prev = curr;
      curr = swap;
    }

    return prev[n];
  }

  public static double normalizedSimilarity(String a, String b) {
    if (a == null) {
      a = "";
    }
    if (b == null) {
      b = "";
    }
    int maxLen = Math.max(a.length(), b.length());
    if (maxLen == 0) {
      return 1.0;
    }
    return 1.0 - (double) levenshtein(a, b) / maxLen;
  }
}
