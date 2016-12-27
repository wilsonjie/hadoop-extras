package com.sogou.hadoop.extras.tools.statistics;

import com.sogou.hadoop.extras.common.HiveUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.thrift.TException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lauo on 16/12/9.
 */
public class Statistics {
  private final static Log log = LogFactory.getLog(Statistics.class);

  private static Configuration conf = new Configuration();

  public static long[] dus(String db, String table, List<String> partitionVals)
      throws TException, IOException {
    String dirPattern = getLocationOnlyPath(
        HiveUtils.getLocation(db, table, partitionVals));
    return dus(dirPattern, "*");
  }

  public static long[] dus(String pathPattern) throws IOException {
    Path path = new Path(pathPattern);
    FileSystem fs = path.getFileSystem(conf);

    long sumFileSize = 0L;
    long sumFileCnt = 0L;
    FileStatus[] fileStatuses = fs.globStatus(path);
    if (fileStatuses.length == 0) {
      throw new IOException(
          String.format("no path match pattern: %s", pathPattern));
    }

    for (FileStatus fileStatus : fileStatuses) {
      if (fileStatus.isFile()) {
        sumFileSize += fileStatus.getLen();
        sumFileCnt++;
      }
    }

    return new long[]{sumFileSize, sumFileCnt};
  }

  public static long[] dus(String dirPattern, String filePattern)
      throws IOException {
    if (!dirPattern.endsWith("/")) {
      dirPattern += "/";
    }
    return dus(dirPattern + filePattern);
  }

  private static String getLocationOnlyPath(String location) {
    return new Path(location).toUri().getPath();
  }

  public static void main(String[] args) {
    if (args.length != 1 + 2 && args.length != 1 + 3) {
      log.error(
          "need args: " +
              "<type> <dirPattern> <filePattern>" +
              " or <type> <db> <table> <partitionValStrs>");
      System.exit(1);
    }

    String type = args[0];
    if (!"HDFS".equals(type) && !"Hive".equals(type)) {
      log.error(String.format("type not exists: %s", type));
      System.exit(1);
    }

    try {
      long[] rs = null;
      if ("HDFS".equals(type) && args.length == 1 + 2) {
        String dirPattern = args[1];
        String filePattern = args[2];
        rs = Statistics.dus(dirPattern, filePattern);
      } else {//if ("Hive".equals(type) && args.length == 1 + 3) {
        String db = args[1];
        String table = args[2];
        String partitionValStrs = args[3];
        List<String> partitionVals = Arrays.asList(
            partitionValStrs.split("\\s+/\\s+"));
        rs = Statistics.dus(db, table, partitionVals);
      }
      System.out.println(rs[0] + "," + rs[1]);
      System.exit(0);
    } catch (IOException | TException e) {
      log.error(e);
      System.exit(1);
    }
  }

}
