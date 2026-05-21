/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.roda.core.common.iterables.CloseableIterable;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.storage.Binary;
import org.roda.core.storage.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers bundled XSLT stylesheets in AIP storage.
 * <p>
 * Lives in {@code roda-core} so that {@code roda-ui} can run XSLT discovery
 * without importing {@link ModelUtils}, {@link StoragePath} or
 * {@link Resource} directly.
 */
public final class XsltDiscoveryHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(XsltDiscoveryHelper.class);

  private XsltDiscoveryHelper() {
    // utility class
  }

  /**
   * Returns the direct .xsl/.xslt siblings of the given XML file (non-recursive,
   * in the same {@code data/} folder).
   */
  public static List<Binary> searchXsltsBesideXmlFile(ModelService model, IndexedFile indexedFile) {
    try {
      // null fileId resolves to the parent directory
      StoragePath parentDir = ModelUtils.getFileStoragePath(indexedFile.getAipId(),
        indexedFile.getRepresentationId(), indexedFile.getPath(), null);
      try (CloseableIterable<Resource> resources = model.getStorage().listResourcesUnderDirectory(parentDir, false)) {
        return collectXsltBinaries(model, resources, xmlBaseName(indexedFile.getId()), false);
      }
    } catch (Exception e) {
      LOGGER.debug("Could not list XSLT siblings for file {} in rep {}: {}", indexedFile.getId(),
        indexedFile.getRepresentationId(), e.getMessage());
    }
    return Collections.emptyList();
  }

  /**
   * Returns all .xsl/.xslt files under the AIP- or representation-level
   * documentation folder (recursive).
   */
  public static List<Binary> searchAllXsltsInDocumentation(ModelService model, String aipId, String representationId,
    String xmlFileName) {
    try {
      StoragePath docPath = representationId != null
        ? ModelUtils.getDocumentationStoragePath(aipId, representationId)
        : ModelUtils.getDocumentationStoragePath(aipId);
      try (CloseableIterable<Resource> resources = model.getStorage().listResourcesUnderDirectory(docPath, true)) {
        return collectXsltBinaries(model, resources, xmlBaseName(xmlFileName), false);
      }
    } catch (Exception e) {
      LOGGER.debug("Could not list all XSLTs in documentation for aip={}, rep={}: {}", aipId, representationId,
        e.getMessage());
    }
    return Collections.emptyList();
  }

  /** Returns true if {@code name} has a .xsl or .xslt extension (case-insensitive). */
  public static boolean isXsltFilename(String name) {
    if (name == null) {
      return false;
    }
    String lower = name.toLowerCase(Locale.ROOT);
    return lower.endsWith(".xslt") || lower.endsWith(".xsl");
  }

  /** Strips a trailing .xsl/.xslt extension; returns the input unchanged otherwise. */
  public static String stripXsltExtension(String name) {
    if (name == null) {
      return null;
    }
    String lower = name.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".xslt")) {
      return name.substring(0, name.length() - 5);
    }
    if (lower.endsWith(".xsl")) {
      return name.substring(0, name.length() - 4);
    }
    return name;
  }

  /** Returns true if the XSLT's basename equals the XML's basename (case-insensitive). */
  public static boolean isXsltMatchForXml(String xsltName, String xmlBaseName) {
    if (xsltName == null || xmlBaseName == null) {
      return false;
    }
    String xsltBase = stripXsltExtension(xsltName);
    return xsltBase != null && xsltBase.equalsIgnoreCase(xmlBaseName);
  }

  /** Returns the XML basename (without .xml), or {@code null} if not an XML file. */
  public static String xmlBaseName(String xmlFileName) {
    if (xmlFileName != null && xmlFileName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
      return xmlFileName.substring(0, xmlFileName.length() - 4);
    }
    return null;
  }

  // Filename-matched XSLTs first, others alphabetically. matchedOnly=true drops the others entirely.
  private static List<Binary> collectXsltBinaries(ModelService model, Iterable<Resource> resources,
    String xmlBaseName, boolean matchedOnly) throws Exception {
    List<Binary> matched = new ArrayList<>();
    List<Binary> others = new ArrayList<>();
    for (Resource resource : resources) {
      String name = resource.getStoragePath().getName();
      if (isXsltFilename(name)) {
        Binary b = model.getStorage().getBinary(resource.getStoragePath());
        if (xmlBaseName != null && isXsltMatchForXml(name, xmlBaseName)) {
          matched.add(b);
        } else if (!matchedOnly) {
          others.add(b);
        }
      }
    }
    others.sort((a, b) -> a.getStoragePath().getName().compareToIgnoreCase(b.getStoragePath().getName()));
    matched.addAll(others);
    return matched;
  }
}
