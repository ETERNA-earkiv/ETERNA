/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.tuple.Triple;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.storage.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmMap;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

public class RodaUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(RodaUtils.class);

  private static final long XSLT_TIMEOUT_SECONDS = 30;
  private static final int MAX_OUTPUT_SIZE = 10 * 1024 * 1024; // 10 MB

  private static final Processor PROCESSOR = createSecuredProcessor();

  private static Processor createSecuredProcessor() {
    Configuration config = new Configuration();
    config.setBooleanProperty(Feature.ALLOW_EXTERNAL_FUNCTIONS, false);
    return new Processor(config);
  }

  private static final LoadingCache<Triple<String, String, String>, XsltExecutable> CACHE = CacheBuilder.newBuilder()
    .expireAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<Triple<String, String, String>, XsltExecutable>() {

      @Override
      public XsltExecutable load(Triple<String, String, String> key) throws Exception {
        String basePath = key.getLeft();
        String metadataType = key.getMiddle();
        String metadataVersion = key.getRight();
        return createMetadataTransformer(basePath, metadataType, metadataVersion);
      }

    });

  private static final LoadingCache<String, XsltExecutable> EVENT_CACHE = CacheBuilder.newBuilder()
    .expireAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<String, XsltExecutable>() {
      @Override
      public XsltExecutable load(String path) throws Exception {
        return createEventTransformer(path);
      }
    });

  /**
   * CharArrayWriter with a size limit to prevent output-based resource exhaustion.
   */
  private static class LimitedCharArrayWriter extends CharArrayWriter {
    private final int limit;

    LimitedCharArrayWriter(int limit) {
      this.limit = limit;
    }

    @Override
    public void write(int c) {
      checkLimit(1);
      super.write(c);
    }

    @Override
    public void write(char[] c, int off, int len) {
      checkLimit(len);
      super.write(c, off, len);
    }

    @Override
    public void write(String str, int off, int len) {
      checkLimit(len);
      super.write(str, off, len);
    }

    private void checkLimit(int additional) {
      if (size() + additional > limit) {
        throw new RuntimeException("XSLT output exceeded maximum size of " + limit + " bytes");
      }
    }
  }

  /**
   * Best-effort timeout for XSLT transformations.
   * <p>
   * Saxon-HE does not poll the thread interrupt flag during transformation, so
   * {@code future.cancel(true)} on a runaway transform may not abort the worker
   * immediately — the request returns a timeout error but the worker can keep
   * running until it finishes on its own. To prevent that from saturating the
   * pool we use:
   * <ul>
   *   <li>a bounded fixed-size daemon pool (scales with CPU cores; daemon
   *       threads die with the JVM)</li>
   *   <li>{@link LimitedCharArrayWriter} with {@link #MAX_OUTPUT_SIZE} as an
   *       upper bound on work — a transform that exceeds it throws and the
   *       worker exits</li>
   *   <li>uploaded XSLTs capped at 1&nbsp;MB by the controller</li>
   * </ul>
   * True in-process termination would require a Saxon cooperative-abort hook
   * (e.g. throwing {@code XmlProcessingAbort} from a checkpoint) which is out
   * of scope here.
   */
  private static final int XSLT_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors());
  // Bounded queue + CallerRunsPolicy gives backpressure: when both workers and
  // queue are saturated, the submitting HTTP thread runs the transform itself
  // (still capped by XSLT_TIMEOUT_SECONDS and MAX_OUTPUT_SIZE), so callers
  // experience natural slowdown instead of unbounded memory growth.
  private static final int XSLT_QUEUE_CAPACITY = Math.max(8, XSLT_POOL_SIZE * 4);
  private static final ExecutorService XSLT_EXECUTOR = new ThreadPoolExecutor(
    XSLT_POOL_SIZE, XSLT_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
    new ArrayBlockingQueue<>(XSLT_QUEUE_CAPACITY),
    r -> { Thread t = new Thread(r, "xslt-transform"); t.setDaemon(true); return t; },
    new ThreadPoolExecutor.CallerRunsPolicy());

  private static void transformWithTimeout(XsltTransformer transformer) throws GenericException {
    Future<?> future = XSLT_EXECUTOR.submit(() -> {
      try {
        transformer.transform();
      } catch (SaxonApiException e) {
        throw new RuntimeException(e);
      }
    });
    try {
      future.get(XSLT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      // Best-effort cancel — see XSLT_EXECUTOR docs. The request fails fast even
      // if the worker keeps churning until MAX_OUTPUT_SIZE or natural completion.
      future.cancel(true);
      throw new GenericException("XSLT transformation timed out after " + XSLT_TIMEOUT_SECONDS + " seconds");
    } catch (java.util.concurrent.ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException && cause.getCause() instanceof SaxonApiException) {
        throw new GenericException("XSLT transformation failed", cause.getCause());
      }
      throw new GenericException("XSLT transformation failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new GenericException("XSLT transformation interrupted", e);
    }
  }

  /** Private empty constructor */
  private RodaUtils() {
    // do nothing
  }

  /**
   * Closes an input stream quietly (i.e. no exception is thrown) while inspecting
   * its class. The inspection is done to avoid closing streams associates with
   * jar files that may cause later errors like
   *
   * <pre>
   * Caused by: java.io.IOException: Stream closed
   * </pre>
   *
   * So, all streams whose class name does not start with
   * <code>sun.net.www.protocol.jar.JarURLConnection</code> will be closed
   *
   * @since 2016-09-20
   */
  public static void closeQuietly(InputStream inputstream) {
    try {
      close(inputstream);
    } catch (IOException e) {
      // do nothing as we should be quiet
    }
  }

  /**
   * Closes an input stream while inspecting its class. The inspection is done to
   * avoid closing streams associates with jar files that may cause later errors
   * like
   *
   * <pre>
   * Caused by: java.io.IOException: Stream closed
   * </pre>
   *
   * So, all streams whose class name does not start with
   * <code>sun.net.www.protocol.jar.JarURLConnection</code> will be closed
   *
   * @throws IOException
   *
   * @since 2016-09-20
   */
  public static void close(InputStream inputstream) throws IOException {
    if (inputstream != null) {
      String inputstreamClassName = inputstream.getClass().getName();
      if (!inputstreamClassName.startsWith("sun.net.www.protocol.jar.JarURLConnection")) {
        inputstream.close();
      }
    }
  }

  public static Map<String, Object> copyMap(Object object) {
    if (!(object instanceof Map)) {
      return null;
    }
    Map<?, ?> map = (Map<?, ?>) object;
    Map<String, Object> temp = new HashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (entry.getKey() instanceof String) {
        temp.put((String) entry.getKey(), entry.getValue());
      } else {
        return null;
      }
    }
    return temp;
  }

  public static List<String> copyList(Object object) {
    if (!(object instanceof List)) {
      return new ArrayList<>();
    }
    List<?> list = (List<?>) object;
    List<String> temp = new ArrayList<>();
    for (Object ob : list) {
      if (ob instanceof String) {
        temp.add((String) ob);
      } else if (ob == null) {
        temp.add(null);
      } else {
        return new ArrayList<>();
      }
    }
    return temp;
  }

  /**
   * INFO 20160711 this method does not cache stylesheet related resources
   */
  public static void applyStylesheet(Reader xsltReader, Reader fileReader, Map<String, String> parameters,
    Writer result) throws IOException, TransformerException {

    TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
    factory.setURIResolver(new RodaURIFileResolver());
    Source xsltSource = new StreamSource(xsltReader);
    Transformer transformer = factory.newTransformer(xsltSource);
    for (Entry<String, String> parameter : parameters.entrySet()) {
      transformer.setParameter(parameter.getKey(), parameter.getValue());
    }
    try {
      XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      xmlReader.setEntityResolver(new RodaEntityResolver());
      InputSource source = new InputSource(fileReader);
      Source text = new SAXSource(xmlReader, source);
      transformer.transform(text, new StreamResult(result));
    } catch (SAXException se) {
      LOGGER.error(se.getMessage(), se);
    }
  }

  public static Reader applyMetadataStylesheet(Binary binary, String basePath, String metadataType,
    String metadataVersion, Map<String, String> parameters) throws GenericException {
    try (
      Reader descMetadataReader = new InputStreamReader(new BOMInputStream(binary.getContent().createInputStream()))) {

      XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      xmlReader.setEntityResolver(new RodaEntityResolver());
      InputSource source = new InputSource(descMetadataReader);
      Source text = new SAXSource(xmlReader, source);

      XsltExecutable xsltExecutable = CACHE.get(Triple.of(basePath, metadataType, metadataVersion));

      XsltTransformer transformer = xsltExecutable.load();
      LimitedCharArrayWriter transformerResult = new LimitedCharArrayWriter(MAX_OUTPUT_SIZE);

      transformer.setSource(text);
      transformer.setDestination(PROCESSOR.newSerializer(transformerResult));

      for (Entry<String, String> parameter : parameters.entrySet()) {
        QName qName = new QName(parameter.getKey());
        XdmValue xdmValue = new XdmAtomicValue(parameter.getValue());
        transformer.setParameter(qName, xdmValue);
      }

      QName qNameMap = new QName("i18n");
      XdmMap xdmMap = XdmMap.makeMap(parameters);
      transformer.setParameter(qNameMap, xdmMap);

      transformWithTimeout(transformer);

      return new CharArrayReader(transformerResult.toCharArray());

    } catch (IOException | SAXException | ExecutionException e) {
      throw new GenericException("Could not process descriptive metadata binary " + binary.getStoragePath()
        + " metadata type " + metadataType + " and version " + metadataVersion, e);
    }
  }

  /**
   * Applies a user-supplied XSLT stylesheet to the given XML binary.
   * <p>
   * The transformation runs through the shared, secured {@code PROCESSOR} with
   * external functions disabled and an output-size cap; execution is wrapped in
   * a timeout (see {@link #transformWithTimeout(XsltTransformer)}).
   *
   * @param binary
   *          the source XML binary to transform.
   * @param xsltInputStream
   *          the XSLT stylesheet to apply. Consumed and closed by this method.
   * @param parameters
   *          stylesheet parameters; also exposed as an {@code i18n} XdmMap.
   * @return a {@link Reader} over the transformation output.
   * @throws GenericException
   *           if reading the source, compiling the stylesheet, or running the
   *           transformation fails or exceeds the configured limits.
   */
  public static Reader applyCustomStylesheet(Binary binary, InputStream xsltInputStream,
    Map<String, String> parameters) throws GenericException {
    try (
      // Read as bytes so SAX honours the encoding declared in the XML prolog
      // (a Reader would already have applied the JVM default charset).
      InputStream xmlByteStream = new BOMInputStream(binary.getContent().createInputStream());
      InputStream xsltStream = xsltInputStream) {

      XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      xmlReader.setEntityResolver(new RodaEntityResolver());
      InputSource source = new InputSource(xmlByteStream);
      Source text = new SAXSource(xmlReader, source);

      XsltCompiler compiler = PROCESSOR.newXsltCompiler();
      compiler.setURIResolver(new RodaURIFileResolver());
      XsltExecutable xsltExecutable = compiler.compile(new StreamSource(xsltStream));

      XsltTransformer transformer = xsltExecutable.load();
      LimitedCharArrayWriter transformerResult = new LimitedCharArrayWriter(MAX_OUTPUT_SIZE);

      transformer.setSource(text);
      transformer.setDestination(PROCESSOR.newSerializer(transformerResult));

      for (Entry<String, String> parameter : parameters.entrySet()) {
        QName qName = new QName(parameter.getKey());
        XdmValue xdmValue = new XdmAtomicValue(parameter.getValue());
        transformer.setParameter(qName, xdmValue);
      }

      QName qNameMap = new QName("i18n");
      XdmMap xdmMap = XdmMap.makeMap(parameters);
      transformer.setParameter(qNameMap, xdmMap);

      transformWithTimeout(transformer);

      return new CharArrayReader(transformerResult.toCharArray());

    } catch (IOException | SAXException | SaxonApiException e) {
      throw new GenericException("Could not apply custom XSLT stylesheet to binary " + binary.getStoragePath(), e);
    }
  }


  public static Reader applyEventStylesheet(Binary binary, boolean onlyDetails, Map<String, String> translations,
    String path) throws GenericException {
    try (
      Reader descMetadataReader = new InputStreamReader(new BOMInputStream(binary.getContent().createInputStream()))) {

      XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      xmlReader.setEntityResolver(new RodaEntityResolver());
      InputSource source = new InputSource(descMetadataReader);
      Source text = new SAXSource(xmlReader, source);

      XsltExecutable xsltExecutable = EVENT_CACHE.get(path);

      XsltTransformer transformer = xsltExecutable.load();
      LimitedCharArrayWriter transformerResult = new LimitedCharArrayWriter(MAX_OUTPUT_SIZE);

      transformer.setSource(text);
      transformer.setDestination(PROCESSOR.newSerializer(transformerResult));

      // send param to filter stylesheet work
      transformer.setParameter(new QName("onlyDetails"), new XdmAtomicValue(Boolean.toString(onlyDetails)));

      for (Entry<String, String> parameter : translations.entrySet()) {
        QName qName = new QName(parameter.getKey());
        XdmValue xdmValue = new XdmAtomicValue(parameter.getValue());
        transformer.setParameter(qName, xdmValue);
      }

      transformWithTimeout(transformer);
      return new CharArrayReader(transformerResult.toCharArray());
    } catch (IOException | SAXException | ExecutionException e) {
      LOGGER.error(e.getMessage(), e);
      throw new GenericException("Could not process event binary " + binary.getStoragePath(), e);
    }
  }

  protected static XsltExecutable createMetadataTransformer(String basePath, String metadataType,
    String metadataVersion) throws SaxonApiException, GenericException {
    InputStream transformerStream = null;

    try {
      // get xslt from metadata type and version if defined
      if (metadataType != null) {
        String lowerCaseMetadataType = metadataType.toLowerCase();
        if (metadataVersion != null) {
          String lowerCaseMetadataTypeWithVersion = lowerCaseMetadataType + RodaConstants.METADATA_VERSION_SEPARATOR
            + metadataVersion;
          transformerStream = RodaCoreFactory
            .getConfigurationFileAsStream(basePath + lowerCaseMetadataTypeWithVersion + ".xslt");
        }
        if (transformerStream == null) {
          transformerStream = RodaCoreFactory.getConfigurationFileAsStream(basePath + lowerCaseMetadataType + ".xslt");
        }
      }

      // fallback
      if (transformerStream == null) {
        // TODO change plain to default
        transformerStream = RodaCoreFactory.getConfigurationFileAsStream(basePath + "plain.xslt");
      }

      if (transformerStream == null) {
        throw new GenericException("Could not find stylesheet nor fallback at basePath=" + basePath + ", metadataType="
          + metadataType + ", metadataVersion=" + metadataVersion);
      }

      XsltCompiler compiler = PROCESSOR.newXsltCompiler();
      compiler.setURIResolver(new RodaURIFileResolver());
      // compiler.setSchemaAware(false);
      return compiler.compile(new StreamSource(transformerStream));

    } finally {
      IOUtils.closeQuietly(transformerStream);
    }
  }

  protected static XsltExecutable createEventTransformer(String path) throws SaxonApiException, GenericException {
    try (InputStream transformerStream = RodaCoreFactory.getConfigurationFileAsStream(path)) {
      if (transformerStream == null) {
        throw new GenericException("Could not find stylesheet nor fallback at path=" + path);
      }

      XsltCompiler compiler = PROCESSOR.newXsltCompiler();
      compiler.setURIResolver(new RodaURIFileResolver());
      return compiler.compile(new StreamSource(transformerStream));
    } catch (IOException e) {
      throw new GenericException(e);
    }
  }

  public static void copyFilesFromClasspath(String classpathPrefix, Path destinationDirectory) throws IOException {
    copyFilesFromClasspath(classpathPrefix, destinationDirectory, false);
  }

  public static void copyFilesFromClasspath(String classpathPrefix, Path destinationDirectory,
    boolean removeClasspathPrefixFromFinalPath) throws IOException {
    copyFilesFromClasspath(classpathPrefix, destinationDirectory, removeClasspathPrefixFromFinalPath,
      Collections.emptyList());
  }

  public static void copyFilesFromClasspath(String classpathPrefix, Path destinationDirectory,
    boolean removeClasspathPrefixFromFinalPath, List<String> excludePaths) throws IOException {

    List<ClassLoader> classLoadersList = new LinkedList<>();
    classLoadersList.add(ClasspathHelper.contextClassLoader());

    Reflections reflections = new Reflections(new ConfigurationBuilder()
      .forPackage(classpathPrefix, ClasspathHelper.contextClassLoader(), ClasspathHelper.staticClassLoader())
      .setScanners(Scanners.Resources));

    Set<String> resources = reflections.getResources(Pattern.compile(".*"));
    resources = resources.stream().filter(r -> !shouldExclude(r, classpathPrefix, excludePaths))
      .collect(Collectors.toSet());

    LOGGER.info("Copying files from classpath prefix={}, destination={}, removePrefix={}, excludePaths={}",
      classpathPrefix, destinationDirectory, removeClasspathPrefixFromFinalPath, excludePaths);

    for (String resource : resources) {

      InputStream originStream = RodaCoreFactory.class.getClassLoader().getResourceAsStream(resource);
      Path destinyPath;

      // 20240619 gbarros: this is needed in order to avoid npe for security plugins that already loaded on classpath during the bootstrap
      if(originStream == null){
        continue;
      }
      String resourceFileName = resource;

      // Removing ":" escape
      resourceFileName = resourceFileName.replace("::", ":");

      if (removeClasspathPrefixFromFinalPath) {
        destinyPath = destinationDirectory.resolve(resourceFileName.replaceFirst(classpathPrefix, ""));
      } else {
        destinyPath = destinationDirectory.resolve(resourceFileName);
      }

      try {
        // create all parent directories
        Files.createDirectories(destinyPath.getParent());
        // copy file
        Files.copy(originStream, destinyPath, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        LOGGER.error("Error copying file from classpath: {} to {} (reason: {})", originStream, destinyPath,
          e.getMessage());
        throw e;
      } finally {
        RodaUtils.closeQuietly(originStream);
      }

    }
  }

  private static boolean shouldExclude(String resource, String classpathPrefix, List<String> excludePaths) {
    boolean exclude = false;

    if (resource.startsWith(classpathPrefix)) {

      for (String excludePath : excludePaths) {
        if (resource.startsWith(excludePath)) {
          exclude = true;
          break;
        }
      }
    } else {
      exclude = true;
    }
    return exclude;
  }
}
