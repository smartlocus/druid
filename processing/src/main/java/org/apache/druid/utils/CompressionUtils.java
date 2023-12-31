/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.utils;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.druid.guice.annotations.PublicApi;
import org.apache.druid.java.util.common.FileUtils;
import org.apache.druid.java.util.common.IAE;
import org.apache.druid.java.util.common.IOE;
import org.apache.druid.java.util.common.ISE;
import org.apache.druid.java.util.common.RetryUtils;
import org.apache.druid.java.util.common.StreamUtils;
import org.apache.druid.java.util.common.io.NativeIO;
import org.apache.druid.java.util.common.logger.Logger;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@PublicApi
public class CompressionUtils
{

  public enum Format
  {
    BZ2(".bz2", "bz2"),
    GZ(".gz", "gz"),
    SNAPPY(".sz", "sz"),
    XZ(".xz", "xz"),
    ZIP(".zip", "zip"),
    ZSTD(".zst", "zst");

    private static final Map<String, Format> EXTENSION_TO_COMPRESSION_FORMAT;

    static {
      ImmutableMap.Builder<String, Format> builder = ImmutableMap.builder();
      builder.put(BZ2.getExtension(), BZ2);
      builder.put(GZ.getExtension(), GZ);
      builder.put(SNAPPY.getExtension(), SNAPPY);
      builder.put(XZ.getExtension(), XZ);
      builder.put(ZIP.getExtension(), ZIP);
      builder.put(ZSTD.getExtension(), ZSTD);
      EXTENSION_TO_COMPRESSION_FORMAT = builder.build();
    }

    private final String suffix;
    private final String extension;
    Format(String suffix, String extension)
    {
      this.suffix = suffix;
      this.extension = extension;
    }

    public String getSuffix()
    {
      return suffix;
    }

    public String getExtension()
    {
      return extension;
    }

    @Nullable
    public static Format fromFileName(@Nullable String fileName)
    {
      String extension = FileNameUtils.getExtension(fileName);
      if (null == extension) {
        return null;
      }
      return EXTENSION_TO_COMPRESSION_FORMAT.get(extension);
    }
  }

  public static final long COMPRESSED_TEXT_WEIGHT_FACTOR = 4L;
  private static final Logger log = new Logger(CompressionUtils.class);
  private static final int DEFAULT_RETRY_COUNT = 3;
  private static final int GZIP_BUFFER_SIZE = 8192; // Default is 512

  /**
   * Zip the contents of directory into the file indicated by outputZipFile. Sub directories are skipped
   *
   * @param directory     The directory whose contents should be added to the zip in the output stream.
   * @param outputZipFile The output file to write the zipped data to
   * @param fsync         True if the output file should be fsynced to disk
   *
   * @return The number of bytes (uncompressed) read from the input directory.
   *
   * @throws IOException
   */
  public static long zip(File directory, File outputZipFile, boolean fsync) throws IOException
  {
    if (!isZip(outputZipFile.getName())) {
      log.warn("No .zip suffix[%s], putting files from [%s] into it anyway.", outputZipFile, directory);
    }

    if (fsync) {
      return FileUtils.writeAtomically(outputZipFile, out -> zip(directory, out));
    } else {
      try (
          final FileChannel fileChannel = FileChannel.open(
              outputZipFile.toPath(),
              StandardOpenOption.WRITE,
              StandardOpenOption.CREATE
          );
          final OutputStream out = Channels.newOutputStream(fileChannel)
      ) {
        return zip(directory, out);
      }
    }
  }

  /**
   * Zip the contents of directory into the file indicated by outputZipFile. Sub directories are skipped
   *
   * @param directory     The directory whose contents should be added to the zip in the output stream.
   * @param outputZipFile The output file to write the zipped data to
   *
   * @return The number of bytes (uncompressed) read from the input directory.
   *
   * @throws IOException
   */
  public static long zip(File directory, File outputZipFile) throws IOException
  {
    return zip(directory, outputZipFile, false);
  }

  /**
   * Zips the contents of the input directory to the output stream. Sub directories are skipped
   *
   * @param directory The directory whose contents should be added to the zip in the output stream.
   * @param out       The output stream to write the zip data to. Caller is responsible for closing this stream.
   *
   * @return The number of bytes (uncompressed) read from the input directory.
   *
   * @throws IOException
   */
  public static long zip(File directory, OutputStream out) throws IOException
  {
    if (!directory.isDirectory()) {
      throw new IOE("directory[%s] is not a directory", directory);
    }

    final ZipOutputStream zipOut = new ZipOutputStream(out);

    long totalSize = 0;

    // Sort entries to make life easier when writing streaming-decompression unit tests.
    for (File file : Arrays.stream(directory.listFiles()).sorted().collect(Collectors.toList())) {
      log.debug("Adding file[%s] with size[%,d].  Total size so far[%,d]", file, file.length(), totalSize);
      if (file.length() > Integer.MAX_VALUE) {
        zipOut.finish();
        throw new IOE("file[%s] too large [%,d]", file, file.length());
      }
      zipOut.putNextEntry(new ZipEntry(file.getName()));
      totalSize += Files.asByteSource(file).copyTo(zipOut);
    }
    zipOut.closeEntry();
    // Workaround for http://hg.openjdk.java.net/jdk8/jdk8/jdk/rev/759aa847dcaf
    zipOut.flush();
    zipOut.finish();

    return totalSize;
  }

  /**
   * Unzip the byteSource to the output directory. If cacheLocally is true, the byteSource is cached to local disk before unzipping.
   * This may cause more predictable behavior than trying to unzip a large file directly off a network stream, for example.
   * * @param byteSource The ByteSource which supplies the zip data
   *
   * @param byteSource   The ByteSource which supplies the zip data
   * @param outDir       The output directory to put the contents of the zip
   * @param shouldRetry  A predicate expression to determine if a new InputStream should be acquired from ByteSource
   *                     and the copy attempted again. If you want to retry on any exception, use
   *                     {@link FileUtils#IS_EXCEPTION}.
   * @param cacheLocally A boolean flag to indicate if the data should be cached locally
   *
   * @return A FileCopyResult containing the result of writing the zip entries to disk
   *
   * @throws IOException
   */
  public static FileUtils.FileCopyResult unzip(
      final ByteSource byteSource,
      final File outDir,
      final Predicate<Throwable> shouldRetry,
      boolean cacheLocally
  ) throws IOException
  {
    if (!cacheLocally) {
      try {
        return RetryUtils.retry(
            () -> unzip(byteSource.openStream(), outDir),
            shouldRetry,
            DEFAULT_RETRY_COUNT
        );
      }
      catch (IOException e) {
        throw e;
      }
      catch (Exception e) {
        throw Throwables.propagate(e);
      }
    } else {
      final File tmpFile = File.createTempFile("compressionUtilZipCache", Format.ZIP.getSuffix());
      try {
        FileUtils.retryCopy(
            byteSource,
            tmpFile,
            shouldRetry,
            DEFAULT_RETRY_COUNT
        );
        return unzip(tmpFile, outDir);
      }
      finally {
        if (!tmpFile.delete()) {
          log.warn("Could not delete zip cache at [%s]", tmpFile.toString());
        }
      }
    }
  }

  /**
   * Unzip the pulled file to an output directory. This is only expected to work on zips with lone files, and is not intended for zips with directory structures.
   *
   * @param pulledFile The file to unzip
   * @param outDir     The directory to store the contents of the file.
   *
   * @return a FileCopyResult of the files which were written to disk
   *
   * @throws IOException
   */
  public static FileUtils.FileCopyResult unzip(final File pulledFile, final File outDir) throws IOException
  {
    if (!(outDir.exists() && outDir.isDirectory())) {
      throw new ISE("outDir[%s] must exist and be a directory", outDir);
    }
    log.info("Unzipping file[%s] to [%s]", pulledFile, outDir);
    final FileUtils.FileCopyResult result = new FileUtils.FileCopyResult();
    try (final ZipFile zipFile = new ZipFile(pulledFile)) {
      final Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
      while (enumeration.hasMoreElements()) {
        final ZipEntry entry = enumeration.nextElement();
        final File outFile = new File(outDir, entry.getName());

        validateZipOutputFile(pulledFile.getCanonicalPath(), outFile, outDir);

        result.addFiles(
            FileUtils.retryCopy(
                new ByteSource()
                {
                  @Override
                  public InputStream openStream() throws IOException
                  {
                    return new BufferedInputStream(zipFile.getInputStream(entry));
                  }
                },
                outFile,
                FileUtils.IS_EXCEPTION,
                DEFAULT_RETRY_COUNT
            ).getFiles()
        );
      }
    }
    return result;
  }

  public static void validateZipOutputFile(
      String sourceFilename,
      final File outFile,
      final File outDir
  ) throws IOException
  {
    // check for evil zip exploit that allows writing output to arbitrary directories
    final File canonicalOutFile = outFile.getCanonicalFile();
    final String canonicalOutDir = outDir.getCanonicalPath();
    if (!canonicalOutFile.toPath().startsWith(canonicalOutDir)) {
      throw new ISE(
          "Unzipped output path[%s] of sourceFile[%s] does not start with outDir[%s].",
          canonicalOutFile,
          sourceFilename,
          canonicalOutDir
      );
    }
  }

  /**
   * Unzip from the input stream to the output directory, using the entry's file name as the file name in the output directory.
   * The behavior of directories in the input stream's zip is undefined.
   * If possible, it is recommended to use unzip(ByteStream, File) instead
   *
   * @param in     The input stream of the zip data. This stream is closed
   * @param outDir The directory to copy the unzipped data to
   *
   * @return The FileUtils.FileCopyResult containing information on all the files which were written
   *
   * @throws IOException
   */
  public static FileUtils.FileCopyResult unzip(InputStream in, File outDir) throws IOException
  {
    try (final ZipInputStream zipIn = new ZipInputStream(in)) {
      final FileUtils.FileCopyResult result = new FileUtils.FileCopyResult();
      ZipEntry entry;
      while ((entry = zipIn.getNextEntry()) != null) {
        final File file = new File(outDir, entry.getName());

        validateZipOutputFile("", file, outDir);

        NativeIO.chunkedCopy(zipIn, file);

        result.addFile(file);
        zipIn.closeEntry();
      }

      // Skip the rest of the zip file to work around https://github.com/apache/druid/issues/6905
      final byte[] buf = new byte[512];
      while (in.read(buf) != -1) {
        // Intentionally left empty.
      }

      return result;
    }
  }

  /**
   * gunzip the file to the output file.
   *
   * @param pulledFile The source of the gz data
   * @param outFile    A target file to put the contents
   *
   * @return The result of the file copy
   *
   * @throws IOException
   */
  public static FileUtils.FileCopyResult gunzip(final File pulledFile, File outFile)
  {
    return gunzip(Files.asByteSource(pulledFile), outFile);
  }

  /**
   * Unzips the input stream via a gzip filter. use gunzip(ByteSource, File, Predicate) if possible
   *
   * @param in      The input stream to run through the gunzip filter. This stream is closed
   * @param outFile The file to output to
   *
   * @throws IOException
   */
  public static FileUtils.FileCopyResult gunzip(InputStream in, File outFile) throws IOException
  {
    try (GZIPInputStream gzipInputStream = gzipInputStream(in)) {
      NativeIO.chunkedCopy(gzipInputStream, outFile);
      return new FileUtils.FileCopyResult(outFile);
    }
  }

  /**
   * Fixes java bug 7036144 http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7036144 which affects concatenated GZip
   *
   * @param in The raw input stream
   *
   * @return A GZIPInputStream that can handle concatenated gzip streams in the input
   *
   * @see #decompress(InputStream, String) which should be used instead for streams coming from files
   */
  public static GZIPInputStream gzipInputStream(final InputStream in) throws IOException
  {
    return new GZIPInputStream(
        new FilterInputStream(in)
        {
          @Override
          public int available() throws IOException
          {
            final int otherAvailable = super.available();
            // Hack. Docs say available() should return an estimate,
            // so we estimate about 1KiB to work around available == 0 bug in GZIPInputStream
            return otherAvailable == 0 ? 1 << 10 : otherAvailable;
          }
        },
        GZIP_BUFFER_SIZE
    );
  }

  /**
   * gunzip from the source stream to the destination stream.
   *
   * @param in  The input stream which is to be decompressed. This stream is closed.
   * @param out The output stream to write to. This stream is closed
   *
   * @return The number of bytes written to the output stream.
   *
   * @throws IOException
   */
  public static long gunzip(InputStream in, OutputStream out) throws IOException
  {
    try (GZIPInputStream gzipInputStream = gzipInputStream(in)) {
      final long result = ByteStreams.copy(gzipInputStream, out);
      out.flush();
      return result;
    }
    finally {
      out.close();
    }
  }

  /**
   * A gunzip function to store locally
   *
   * @param in          The factory to produce input streams
   * @param outFile     The file to store the result into
   * @param shouldRetry A predicate to indicate if the Throwable is recoverable
   *
   * @return The count of bytes written to outFile
   */
  public static FileUtils.FileCopyResult gunzip(
      final ByteSource in,
      final File outFile,
      Predicate<Throwable> shouldRetry
  )
  {
    return FileUtils.retryCopy(
        new ByteSource()
        {
          @Override
          public InputStream openStream() throws IOException
          {
            return gzipInputStream(in.openStream());
          }
        },
        outFile,
        shouldRetry,
        DEFAULT_RETRY_COUNT
    );
  }


  /**
   * Gunzip from the input stream to the output file
   *
   * @param in      The compressed input stream to read from
   * @param outFile The file to write the uncompressed results to
   *
   * @return A FileCopyResult of the file written
   */
  public static FileUtils.FileCopyResult gunzip(final ByteSource in, File outFile)
  {
    return gunzip(in, outFile, FileUtils.IS_EXCEPTION);
  }

  /**
   * Copy inputStream to out while wrapping out in a GZIPOutputStream
   * Closes both input and output
   *
   * @param inputStream The input stream to copy data from. This stream is closed
   * @param out         The output stream to wrap in a GZIPOutputStream before copying. This stream is closed
   *
   * @return The size of the data copied
   *
   * @throws IOException
   */
  public static long gzip(InputStream inputStream, OutputStream out) throws IOException
  {
    try (GZIPOutputStream outputStream = new GZIPOutputStream(out)) {
      final long result = ByteStreams.copy(inputStream, outputStream);
      out.flush();
      return result;
    }
    finally {
      inputStream.close();
    }
  }

  /**
   * Gzips the input file to the output
   *
   * @param inFile      The file to gzip
   * @param outFile     A target file to copy the uncompressed contents of inFile to
   * @param shouldRetry Predicate on a potential throwable to determine if the copy should be attempted again.
   *
   * @return The result of the file copy
   *
   * @throws IOException
   */
  public static FileUtils.FileCopyResult gzip(final File inFile, final File outFile, Predicate<Throwable> shouldRetry)
  {
    gzip(Files.asByteSource(inFile), Files.asByteSink(outFile), shouldRetry);
    return new FileUtils.FileCopyResult(outFile);
  }

  public static long gzip(final ByteSource in, final ByteSink out, Predicate<Throwable> shouldRetry)
  {
    return StreamUtils.retryCopy(
        in,
        new ByteSink()
        {
          @Override
          public OutputStream openStream() throws IOException
          {
            return new GZIPOutputStream(out.openStream());
          }
        },
        shouldRetry,
        DEFAULT_RETRY_COUNT
    );
  }


  /**
   * GZip compress the contents of inFile into outFile
   *
   * @param inFile  The source of data
   * @param outFile The destination for compressed data
   *
   * @return A FileCopyResult of the resulting file at outFile
   *
   * @throws IOException
   */
  public static FileUtils.FileCopyResult gzip(final File inFile, final File outFile)
  {
    return gzip(inFile, outFile, FileUtils.IS_EXCEPTION);
  }

  /**
   * Checks to see if fName is a valid name for a "*.zip" file
   *
   * @param fName The name of the file in question
   *
   * @return True if fName is properly named for a .zip file, false otherwise
   */
  public static boolean isZip(String fName)
  {
    if (Strings.isNullOrEmpty(fName)) {
      return false;
    }
    return fName.endsWith(Format.ZIP.getSuffix()); // Technically a file named `.zip` would be fine
  }

  /**
   * Checks to see if fName is a valid name for a "*.gz" file
   *
   * @param fName The name of the file in question
   *
   * @return True if fName is a properly named .gz file, false otherwise
   */
  public static boolean isGz(String fName)
  {
    if (Strings.isNullOrEmpty(fName)) {
      return false;
    }
    return fName.endsWith(Format.GZ.getSuffix()) && fName.length() > Format.GZ.getSuffix().length();
  }

  /**
   * Get the file name without the .gz extension
   *
   * @param fname The name of the gzip file
   *
   * @return fname without the ".gz" extension
   *
   * @throws IAE if fname is not a valid "*.gz" file name
   */
  public static String getGzBaseName(String fname)
  {
    final String reducedFname = Files.getNameWithoutExtension(fname);
    if (isGz(fname) && !reducedFname.isEmpty()) {
      return reducedFname;
    }
    throw new IAE("[%s] is not a valid gz file name", fname);
  }

  /**
   * Decompress an input stream from a file, based on the filename.
   */
  public static InputStream decompress(final InputStream in, final String fileName) throws IOException
  {
    if (fileName.endsWith(Format.GZ.getSuffix())) {
      return gzipInputStream(in);
    } else if (fileName.endsWith(Format.BZ2.getSuffix())) {
      return new BZip2CompressorInputStream(in, true);
    } else if (fileName.endsWith(Format.XZ.getSuffix())) {
      return new XZCompressorInputStream(in, true);
    } else if (fileName.endsWith(Format.SNAPPY.getSuffix())) {
      return new FramedSnappyCompressorInputStream(in);
    } else if (fileName.endsWith(Format.ZSTD.getSuffix())) {
      return new ZstdCompressorInputStream(in);
    } else if (fileName.endsWith(Format.ZIP.getSuffix())) {
      // This reads the first file in the archive.
      final ZipInputStream zipIn = new ZipInputStream(in, StandardCharsets.UTF_8);
      try {
        final ZipEntry nextEntry = zipIn.getNextEntry();
        if (nextEntry == null) {
          zipIn.close();

          // No files in the archive - return an empty stream.
          return new ByteArrayInputStream(new byte[0]);
        }
        return zipIn;
      }
      catch (IOException e) {
        try {
          zipIn.close();
        }
        catch (IOException e2) {
          e.addSuppressed(e2);
        }
        throw e;
      }
    } else {
      return in;
    }
  }
}
