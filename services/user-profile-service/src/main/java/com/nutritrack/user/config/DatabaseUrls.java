package com.nutritrack.user.config;

/**
 * Converts Railway-style {@code postgres://} / {@code postgresql://} URLs to JDBC URLs.
 * Compose and local runs should prefer {@code SPRING_DATASOURCE_URL}; Railway may inject
 * {@code DATABASE_URL} which callers can convert with {@link #toJdbcUrl(String)}.
 */
public final class DatabaseUrls {

  private DatabaseUrls() {}

  public static String toJdbcUrl(String databaseUrl) {
    if (databaseUrl == null || databaseUrl.startsWith("jdbc:")) {
      return databaseUrl;
    }
    java.net.URI uri = java.net.URI.create(databaseUrl);
    String scheme = uri.getScheme();
    if ("postgres".equals(scheme) || "postgresql".equals(scheme)) {
      String path = uri.getPath() == null ? "" : uri.getPath();
      String query = uri.getQuery() == null ? "" : "?" + uri.getQuery();
      return "jdbc:postgresql://"
          + uri.getHost()
          + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
          + path
          + query;
    }
    return databaseUrl;
  }
}
