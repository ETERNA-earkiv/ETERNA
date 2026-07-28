/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.roda.core.RodaCoreFactory;
import org.roda.core.common.tools.ZipEntryInfo;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.LiteRODAObject;
import org.roda.core.data.v2.StreamResponse;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.select.SelectedItems;
import org.roda.core.data.v2.index.select.SelectedItemsFilter;
import org.roda.core.data.v2.index.select.SelectedItemsList;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.core.index.IndexService;
import org.roda.core.index.utils.IterableIndexResult;
import org.roda.core.model.LiteRODAObjectFactory;
import org.roda.core.model.ModelService;
import org.roda.core.protocols.Protocol;
import org.roda.core.storage.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns a selection of files into a validated, materialized file list and
 * packages it as a zip of file content only.
 * <p>
 * This serves disclosure ("utlämnande"): the recipient is a person who asked
 * for records and wants readable files, not an archival package. Hence no
 * {@code metadata/}, no METS, and paths relative to the representation's
 * {@code data/}.
 * <p>
 * Validation happens before anything is streamed, because zip streaming cannot
 * report errors: status line and headers are sent before the first file is
 * read, so a mid-stream failure delivers a truncated zip with status 200 —
 * which zip tools often open without complaint, since the table of contents
 * sits at the end.
 */
public final class DownloadSelection {
  private static final Logger LOGGER = LoggerFactory.getLogger(DownloadSelection.class);

  private static final int MAX_ZIP_NAME_LENGTH = 80;
  private static final String FALLBACK_ZIP_NAME = "files";

  /**
   * Everything the expansion, the validation, the zip entries and the zip name
   * need — and no more, since a selection may run into the thousands.
   */
  private static final List<String> FILE_FIELDS = List.of(RodaConstants.INDEX_UUID, RodaConstants.INDEX_ID,
    RodaConstants.FILE_PATH, RodaConstants.FILE_AIP_ID, RodaConstants.FILE_REPRESENTATION_ID,
    RodaConstants.FILE_REPRESENTATION_UUID, RodaConstants.FILE_ISDIRECTORY, RodaConstants.FILE_ISREFERENCE,
    RodaConstants.FILE_REFERENCE_URL, RodaConstants.FILE_SIZE);

  private DownloadSelection() {
    // do nothing
  }

  /**
   * Resolves a selection into the actual bitstreams it stands for: filters are
   * resolved against the index and selected folders contribute their whole
   * contents, recursively.
   */
  public static List<IndexedFile> expand(IndexService index, SelectedItems<IndexedFile> selection)
    throws GenericException, RequestNotValidException, NotFoundException {
    // insertion-ordered, so an explicit list keeps the order the user sees;
    // keyed by UUID so an overlapping selection (a folder plus a file inside
    // it) does not produce duplicate zip entries
    Map<String, IndexedFile> expanded = new LinkedHashMap<>();

    switch (selection) {
      case SelectedItemsList<IndexedFile> list -> {
        for (String uuid : list.getIds()) {
          collect(index, index.retrieve(IndexedFile.class, uuid, FILE_FIELDS), expanded);
        }
      }
      case SelectedItemsFilter<IndexedFile> filter ->
        collectAll(index, filter.getFilter(), filter.justActive(), expanded);
      default -> throw new RequestNotValidException(
        "Selected items implementation not supported: " + selection.getClass().getName());
    }

    return new ArrayList<>(expanded.values());
  }

  private static void collectAll(IndexService index, Filter filter, boolean justActive,
    Map<String, IndexedFile> expanded) throws GenericException, RequestNotValidException {
    try (IterableIndexResult<IndexedFile> result = index.findAll(IndexedFile.class, filter, justActive, FILE_FIELDS)) {
      for (IndexedFile file : result) {
        collect(index, file, expanded);
      }
    } catch (IOException e) {
      throw new GenericException("Could not expand the selected files", e);
    }
  }

  private static void collect(IndexService index, IndexedFile file, Map<String, IndexedFile> expanded)
    throws GenericException, RequestNotValidException {
    if (!file.isDirectory()) {
      expanded.putIfAbsent(file.getUUID(), file);
      return;
    }

    // every descendant carries the folder's UUID in ancestorsPath, at any
    // depth, so a single query covers the whole subtree.
    // justActive is deliberately false: the folder itself was already
    // resolved, and dropping part of its contents because of AIP state would
    // silently deliver an incomplete disclosure. Permissions are checked on
    // the expanded result, so nothing escapes the check by this route.
    Filter descendants = new Filter(new SimpleFilterParameter(RodaConstants.FILE_ANCESTORS_PATH, file.getUUID()));
    try (IterableIndexResult<IndexedFile> result = index.findAll(IndexedFile.class, descendants, false, FILE_FIELDS)) {
      for (IndexedFile descendant : result) {
        if (!descendant.isDirectory()) {
          expanded.putIfAbsent(descendant.getUUID(), descendant);
        }
      }
    } catch (IOException e) {
      throw new GenericException("Could not expand the contents of folder " + file.getUUID(), e);
    }
  }

  /**
   * Rejects the request when the selection is larger than
   * {@code core.download.max_files}. The limit is off by default: the
   * confirmation dialog, not this check, is what protects against
   * accidentally large selections — an archivist with a legitimately large
   * disclosure should be able to confirm and proceed.
   */
  public static void validateFileCount(List<IndexedFile> files) throws RequestNotValidException {
    int maxFiles = RodaCoreFactory.getRodaConfigurationAsInt(RodaConstants.DEFAULT_CORE_DOWNLOAD_MAX_FILES,
      RodaConstants.CORE_DOWNLOAD_MAX_FILES);

    if (maxFiles > 0 && files.size() > maxFiles) {
      throw new RequestNotValidException("The selection contains " + files.size()
        + " files, which exceeds the configured maximum of " + maxFiles + " files per download");
    }
  }

  /**
   * Verifies that referenced (shallow) content can actually be fetched. The
   * availability check costs a network round trip, so it is made once per
   * unique protocol and host and reused for every file pointing there — a
   * selection of hundreds of reference files against one host must not become
   * hundreds of requests.
   */
  public static void validateDeliverability(List<IndexedFile> files) throws RequestNotValidException {
    validateDeliverability(files, DownloadSelection::isAvailable);
  }

  /**
   * Seam for the tests, which need to count how many probes are actually made.
   */
  static void validateDeliverability(List<IndexedFile> files, Predicate<URI> availabilityProbe)
    throws RequestNotValidException {
    Map<String, Boolean> availabilityByEndpoint = new HashMap<>();
    int undeliverable = 0;

    for (IndexedFile file : files) {
      if (!file.isReference()) {
        continue;
      }

      URI referenceURI = parseReferenceURI(file);
      if (referenceURI == null) {
        undeliverable++;
        continue;
      }

      if (!availabilityByEndpoint.computeIfAbsent(endpointKey(referenceURI),
        key -> availabilityProbe.test(referenceURI))) {
        undeliverable++;
      }
    }

    if (undeliverable > 0) {
      throw new RequestNotValidException(
        "The content of " + undeliverable + " of the " + files.size() + " selected files cannot be delivered");
    }
  }

  private static URI parseReferenceURI(IndexedFile file) {
    if (StringUtils.isBlank(file.getReferenceURL())) {
      LOGGER.warn("Reference file has no reference URL: {}", file.getUUID());
      return null;
    }
    try {
      return new URI(file.getReferenceURL());
    } catch (URISyntaxException e) {
      LOGGER.warn("Cannot convert referenceURL to URI: {}", file.getUUID());
      return null;
    }
  }

  private static String endpointKey(URI uri) {
    return StringUtils.lowerCase(uri.getScheme(), Locale.ROOT) + "://"
      + StringUtils.lowerCase(uri.getHost(), Locale.ROOT) + ":" + uri.getPort();
  }

  private static boolean isAvailable(URI uri) {
    try {
      Protocol protocol = RodaCoreFactory.getProtocol(uri);
      return Boolean.TRUE.equals(protocol.isAvailable());
    } catch (GenericException e) {
      LOGGER.warn("Content at {} is not available", endpointKey(uri));
      return false;
    }
  }

  /**
   * Builds the zip: file content only, each entry named by its path relative
   * to the representation's {@code data/} directory.
   */
  public static StreamResponse createZipStreamResponse(ModelService model, IndexService index, List<IndexedFile> files)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException {
    boolean multipleAIPs = files.stream().map(IndexedFile::getAipId).distinct().count() > 1;
    boolean multipleRepresentations = files.stream().map(IndexedFile::getRepresentationUUID).distinct().count() > 1;

    List<ZipEntryInfo> zipEntries = new ArrayList<>(files.size());
    for (IndexedFile file : files) {
      Optional<LiteRODAObject> liteFile = LiteRODAObjectFactory.get(file);
      if (liteFile.isEmpty()) {
        throw new RequestNotValidException("Couldn't retrieve file with id: " + file.getId());
      }
      Binary binary = model.getBinary(liteFile.get());
      zipEntries.add(new ZipEntryInfo(zipEntryName(file, multipleAIPs, multipleRepresentations), binary.getContent()));
    }

    return DownloadUtils.createZipStreamResponse(zipEntries, zipName(index, files));
  }

  /**
   * Path relative to the representation's {@code data/}, prefixed only when
   * the selection spans more than one representation — identical internal
   * paths in two representations would otherwise collide silently. Across AIPs
   * the representation id alone is not unique either, so the AIP id is
   * prepended as well.
   */
  static String zipEntryName(IndexedFile file, boolean multipleAIPs, boolean multipleRepresentations) {
    StringBuilder name = new StringBuilder();

    if (multipleRepresentations) {
      if (multipleAIPs) {
        name.append(file.getAipId()).append(DownloadUtils.ZIP_PATH_DELIMITER);
      }
      name.append(file.getRepresentationId()).append(DownloadUtils.ZIP_PATH_DELIMITER);
    }

    if (file.getPath() != null) {
      for (String segment : file.getPath()) {
        name.append(segment).append(DownloadUtils.ZIP_PATH_DELIMITER);
      }
    }

    return name.append(file.getId()).toString();
  }

  /**
   * Names the zip after what the recipient recognises — the AIP and
   * representation titles — falling back on the respective ids when a title is
   * missing.
   */
  static String zipName(IndexService index, List<IndexedFile> files) {
    List<String> aipIds = files.stream().map(IndexedFile::getAipId).distinct().toList();
    List<String> representationUUIDs = files.stream().map(IndexedFile::getRepresentationUUID).distinct().toList();

    if (aipIds.size() != 1) {
      return FALLBACK_ZIP_NAME;
    }

    StringBuilder name = new StringBuilder(sanitizeFileName(retrieveAIPTitle(index, aipIds.getFirst())));
    if (representationUUIDs.size() == 1) {
      name.append("_").append(sanitizeFileName(
        retrieveRepresentationTitle(index, representationUUIDs.getFirst(), files.getFirst().getRepresentationId())));
    }

    // truncate rather than abbreviate: an ellipsis in a file name is noise
    return StringUtils.left(name.toString(), MAX_ZIP_NAME_LENGTH);
  }

  private static String retrieveAIPTitle(IndexService index, String aipId) {
    try {
      IndexedAIP aip = index.retrieve(IndexedAIP.class, aipId,
        List.of(RodaConstants.INDEX_UUID, RodaConstants.AIP_TITLE));
      return StringUtils.defaultIfBlank(aip.getTitle(), aipId);
    } catch (NotFoundException | GenericException e) {
      LOGGER.warn("Could not retrieve the title of AIP {}", aipId);
      return aipId;
    }
  }

  private static String retrieveRepresentationTitle(IndexService index, String representationUUID,
    String representationId) {
    try {
      IndexedRepresentation representation = index.retrieve(IndexedRepresentation.class, representationUUID,
        List.of(RodaConstants.INDEX_UUID, RodaConstants.REPRESENTATION_TITLE));
      return StringUtils.defaultIfBlank(representation.getTitle(), representationId);
    } catch (NotFoundException | GenericException e) {
      LOGGER.warn("Could not retrieve the title of representation {}", representationUUID);
      return representationId;
    }
  }

  /**
   * Keeps the name readable — including å, ä and ö, which the response header
   * encodes per RFC 6266 — while removing what a file system or a zip tool
   * would choke on.
   */
  static String sanitizeFileName(String name) {
    String sanitized = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").replaceAll("\\s+", "_")
      .replaceAll("_{2,}", "_");
    sanitized = StringUtils.strip(sanitized, "_.");
    return StringUtils.defaultIfBlank(sanitized, FALLBACK_ZIP_NAME);
  }
}
