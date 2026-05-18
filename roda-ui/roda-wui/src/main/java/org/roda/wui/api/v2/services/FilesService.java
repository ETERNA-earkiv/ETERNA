/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.api.v2.services;

import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.roda.core.RodaCoreFactory;
import org.roda.core.common.Messages;
import org.roda.core.common.PremisV3Utils;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.exceptions.TechnicalMetadataNotFoundException;
import org.roda.core.data.utils.URNUtils;
import org.roda.core.data.v2.ConsumesOutputStream;
import org.roda.core.data.v2.DefaultConsumesOutputStream;
import org.roda.core.data.v2.LiteRODAObject;
import org.roda.core.data.v2.StreamResponse;
import org.roda.core.data.v2.file.CreateFolderRequest;
import org.roda.core.data.v2.file.MoveFilesRequest;
import org.roda.core.data.v2.generics.DeleteRequest;
import org.roda.core.data.v2.index.select.SelectedItems;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.core.data.v2.ip.Representation;
import org.roda.core.data.v2.ip.metadata.LinkingIdentifier;
import org.roda.core.data.v2.ip.metadata.TechnicalMetadata;
import org.roda.core.data.v2.ip.metadata.TechnicalMetadataInfo;
import org.roda.core.data.v2.ip.metadata.TechnicalMetadataInfos;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.synchronization.central.DistributedInstance;
import org.roda.core.data.v2.user.User;
import org.roda.core.index.IndexService;
import org.roda.core.model.LiteRODAObjectFactory;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.UserUtility;
import org.roda.core.plugins.PluginHelper;
import org.roda.core.plugins.base.characterization.SiegfriedPlugin;
import org.roda.core.plugins.base.maintenance.DeleteRODAObjectPlugin;
import org.roda.core.plugins.base.maintenance.MovePlugin;
import org.roda.core.protocols.Protocol;
import org.roda.core.storage.Binary;
import org.roda.core.storage.BinaryConsumesOutputStream;
import org.roda.core.storage.BinaryVersion;
import org.roda.core.storage.ContentPayload;
import org.roda.core.storage.DirectResourceAccess;
import org.roda.core.storage.RangeConsumesOutputStream;
import org.roda.core.storage.utils.RODAInstanceUtils;
import org.roda.core.util.IdUtils;
import org.roda.wui.api.v2.utils.CommonServicesUtils;
import org.roda.wui.common.HTMLUtils;
import org.roda.wui.common.model.RequestContext;
import org.roda.wui.common.server.ServerTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.storage.Resource;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.common.iterables.CloseableIterable;

@Service
public class FilesService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FilesService.class);
  private static final String HTML_EXT = ".html";

  public IndexedFile renameFolder(RequestContext requestContext, IndexedFile indexedFolder, String newName,
    String details) throws GenericException, RequestNotValidException, AlreadyExistsException, NotFoundException,
    AuthorizationDeniedException {
    String eventDescription = "The process of updating an object of the repository.";

    User user = requestContext.getUser();
    ModelService model = requestContext.getModelService();
    IndexService index = requestContext.getIndexService();
    String oldName = indexedFolder.getId();

    try {
      File folder = model.retrieveFile(indexedFolder.getAipId(), indexedFolder.getRepresentationId(),
        indexedFolder.getPath(), indexedFolder.getId());
      File newFolder = model.renameFolder(folder, newName, true);
      String outcomeText = "The folder '" + oldName + "' has been manually renamed to '" + newName + "'.";
      model.createUpdateAIPEvent(indexedFolder.getAipId(), indexedFolder.getRepresentationId(), null, null,
        RodaConstants.PreservationEventType.UPDATE, eventDescription, PluginState.SUCCESS, outcomeText, details,
        user.getName(), true, null);

      index.commitAIPs();
      return index.retrieve(IndexedFile.class, IdUtils.getFileId(newFolder), RodaConstants.FILE_FIELDS_TO_RETURN);
    } catch (RequestNotValidException | NotFoundException | GenericException | AuthorizationDeniedException e) {
      String outcomeText = "The folder '" + oldName + "' has not been manually renamed to '" + newName + "'.";

      model.createUpdateAIPEvent(indexedFolder.getAipId(), indexedFolder.getRepresentationId(), null, null,
        RodaConstants.PreservationEventType.UPDATE, eventDescription, PluginState.FAILURE, outcomeText, details,
        user.getName(), true, null);

      throw e;
    }
  }

  public Job createFormatIdentificationJob(User user, SelectedItems<?> selected)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException {
    return CommonServicesUtils.createAndExecuteJob("Format identification using Siegfried", selected,
      SiegfriedPlugin.class, PluginType.MISC, user, Collections.emptyMap(),
      "Could not execute format identification using Siegfrid action");
  }

  public Job deleteFiles(User user, DeleteRequest request)
    throws AuthorizationDeniedException, GenericException, RequestNotValidException, NotFoundException {
    Map<String, String> pluginParameters = new HashMap<>();
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_DETAILS, request.getDetails());
    return CommonServicesUtils.createAndExecuteInternalJob("Delete files",
      CommonServicesUtils.convertSelectedItems(request.getItemsToDelete(), IndexedFile.class),
      DeleteRODAObjectPlugin.class, user, pluginParameters, "Could not execute file delete action");
  }

  public Job moveFiles(RequestContext requestContext, MoveFilesRequest request)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException {
    User user = requestContext.getUser();
    IndexService indexService = requestContext.getIndexService();
    IndexedFile fileToMove = null;
    if (request.getFileUUIDtoMove() != null) {
      fileToMove = indexService.retrieve(IndexedFile.class, request.getFileUUIDtoMove(),
        RodaConstants.FILE_FIELDS_TO_RETURN);
    }

    if (fileToMove != null && (!fileToMove.getAipId().equals(request.getAipId())
      || !fileToMove.getRepresentationId().equals(request.getRepresentationId()))) {
      throw new RequestNotValidException("Cannot move to a file outside defined representation");
    }

    Map<String, String> pluginParameters = new HashMap<>();
    if (fileToMove != null) {
      pluginParameters.put(RodaConstants.PLUGIN_PARAMS_ID, fileToMove.getUUID());
    }
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_DETAILS, request.getDetails());

    return CommonServicesUtils.createAndExecuteInternalJob("Move files", request.getItemsToMove(), MovePlugin.class,
      user, pluginParameters, "Could not execute move job");
  }

  public File createFile(RequestContext requestContext, String aipId, String representationId,
    List<String> directoryPath, String fileId, ContentPayload content, String details) throws GenericException,
    AuthorizationDeniedException, RequestNotValidException, NotFoundException, AlreadyExistsException {
    String eventDescription = "The process of creating an object of the repository.";

    User user = requestContext.getUser();
    ModelService model = requestContext.getModelService();

    try {
      File file = model.createFile(aipId, representationId, directoryPath, fileId, content, user.getId());

      List<LinkingIdentifier> targets = new ArrayList<>();
      targets.add(PluginHelper.getLinkingIdentifier(aipId, file.getRepresentationId(), file.getPath(), file.getId(),
        RodaConstants.PRESERVATION_LINKING_OBJECT_OUTCOME));

      String outcomeText = "The file '" + file.getId() + "' has been manually created.";
      model.createEvent(aipId, representationId, null, null, RodaConstants.PreservationEventType.CREATION,
        eventDescription, null, targets, PluginState.SUCCESS, outcomeText, details, user.getName(), true, null);

      requestContext.getIndexService().commit(IndexedFile.class);
      return file;
    } catch (RequestNotValidException | NotFoundException | GenericException | AuthorizationDeniedException
      | AlreadyExistsException e) {
      String outcomeText = "The file '" + fileId + "' has not been manually created.";
      model.createUpdateAIPEvent(aipId, representationId, null, null, RodaConstants.PreservationEventType.CREATION,
        eventDescription, PluginState.FAILURE, outcomeText, details, user.getName(), true, null);

      throw e;
    }
  }

  public IndexedFile createFolder(RequestContext requestContext, IndexedRepresentation indexedRepresentation,
    CreateFolderRequest request) throws GenericException, RequestNotValidException, AlreadyExistsException,
    NotFoundException, AuthorizationDeniedException {
    String eventDescription = "The process of creating an object of the repository.";

    User user = requestContext.getUser();
    ModelService model = requestContext.getModelService();
    IndexService index = requestContext.getIndexService();
    File newFolder;

    String folderUUID = request.getFolderUUID();
    String folderName = request.getName();
    String details = request.getDetails();

    try {
      if (folderUUID != null) {
        IndexedFile indexedFile = index.retrieve(IndexedFile.class, folderUUID, RodaConstants.FILE_FIELDS_TO_RETURN);
        newFolder = model.createFile(indexedFile.getAipId(), indexedFile.getRepresentationId(), indexedFile.getPath(),
          indexedFile.getId(), folderName, user.getId(), true);
      } else {
        newFolder = model.createFile(indexedRepresentation.getAipId(), indexedRepresentation.getId(), null, null,
          folderName, user.getId(), true);
      }

      String outcomeText = "The folder '" + folderName + "' has been manually created.";
      model.createUpdateAIPEvent(indexedRepresentation.getAipId(), indexedRepresentation.getId(), null, null,
        RodaConstants.PreservationEventType.CREATION, eventDescription, PluginState.SUCCESS, outcomeText, details,
        user.getName(), true, null);

      index.commit(IndexedFile.class);
      return index.retrieve(IndexedFile.class, IdUtils.getFileId(newFolder), new ArrayList<>());
    } catch (RequestNotValidException | NotFoundException | GenericException | AuthorizationDeniedException e) {
      String outcomeText = "The folder '" + folderName + "' has not been manually created.";
      model.createUpdateAIPEvent(indexedRepresentation.getAipId(), indexedRepresentation.getId(), null, null,
        RodaConstants.PreservationEventType.CREATION, eventDescription, PluginState.FAILURE, outcomeText, details,
        user.getName(), true, null);

      throw e;
    }
  }

  public RangeConsumesOutputStream retrieveAIPRepresentationRangeStream(RequestContext requestContext,
    IndexedFile indexedFile)
    throws AuthorizationDeniedException, RequestNotValidException, NotFoundException, GenericException {
    ModelService model = requestContext.getModelService();
    if (!indexedFile.isDirectory()) {
      final RangeConsumesOutputStream stream;
      DirectResourceAccess directFileAccess = model.getDirectAccess(indexedFile);
      if (indexedFile.getFileFormat() != null && StringUtils.isNotBlank(indexedFile.getFileFormat().getMimeType())) {
        stream = new RangeConsumesOutputStream(directFileAccess.getPath(), indexedFile.getFileFormat().getMimeType());
      } else {
        stream = new RangeConsumesOutputStream(directFileAccess.getPath());
      }
      return stream;
    } else {
      throw new RequestNotValidException("Range stream for directory unsupported");
    }
  }

  public StreamResponse retrieveAIPRepresentationFile(RequestContext requestContext, IndexedFile indexedFile)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException {
    ModelService model = requestContext.getModelService();
    Optional<LiteRODAObject> liteFile = LiteRODAObjectFactory.get(indexedFile);
    if (liteFile.isEmpty()) {
      throw new RequestNotValidException("Couldn't retrieve file with id: " + indexedFile.getId());
    }
    if (!indexedFile.isDirectory()) {
      final ConsumesOutputStream stream;
      Binary representationFileBinary = model.getBinary(liteFile.get());
      if (indexedFile.getFileFormat() != null && StringUtils.isNotBlank(indexedFile.getFileFormat().getMimeType())) {
        stream = new BinaryConsumesOutputStream(representationFileBinary, indexedFile.getFileFormat().getMimeType());
      } else {
        stream = new BinaryConsumesOutputStream(representationFileBinary);
      }
      return new StreamResponse(stream);
    } else {
      ConsumesOutputStream download = model.exportObjectToStream(liteFile.get());
      return new StreamResponse(download);
    }
  }

  public Optional<String> retrieveDistributedInstanceName(RequestContext requestContext, String instanceId,
    boolean isLocalInstance) {
    try {
      ModelService model = requestContext.getModelService();
      RodaConstants.DistributedModeType distributedModeType = RodaCoreFactory.getDistributedModeType();

      if (RodaConstants.DistributedModeType.CENTRAL.equals(distributedModeType)) {
        if (isLocalInstance) {
          return Optional.of(RodaCoreFactory.getProperty(RodaConstants.CENTRAL_INSTANCE_NAME_PROPERTY,
            RodaConstants.DEFAULT_CENTRAL_INSTANCE_NAME));
        } else {
          DistributedInstance distributedInstance = model.retrieveDistributedInstance(instanceId);
          return Optional.of(distributedInstance.getName());
        }
      }
    } catch (GenericException | AuthorizationDeniedException | RequestNotValidException | NotFoundException e) {
      LOGGER.warn("Could not retrieve the distributed instance", e);
      return Optional.empty();
    }

    return Optional.empty();
  }

  public boolean isShallowFileAvailable(IndexedFile indexedFile) {
    try {
      if (indexedFile.isReference()) {
        String referenceURL = indexedFile.getReferenceURL();
        final Protocol protocol = RodaCoreFactory.getProtocol(new URI(referenceURL));
        return protocol.isAvailable();
      }
    } catch (URISyntaxException e) {
      LOGGER.warn("Cannot convert referenceURL to URI: {}", indexedFile.getUUID());
    } catch (GenericException e) {
      LOGGER.warn("File is not available: {}", indexedFile.getUUID());
    }
    return false;
  }

  public List<String> getConfigurationFileRules(User user) {
    if (UserUtility.hasPermissions(user, RodaConstants.PERMISSION_METHOD_FIND_REPRESENTATION_INFORMATION)) {
      return RodaCoreFactory.getRodaConfigurationAsList("ui.ri.rule.File").stream()
        .map(r -> RodaCoreFactory.getRodaConfigurationAsString(r, RodaConstants.SEARCH_FIELD_FIELDS)).toList();
    } else {
      return Collections.emptyList();
    }
  }

  public StreamResponse retrieveFilePreservationHTML(RequestContext requestContext, IndexedFile file, String language)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException,
    TechnicalMetadataNotFoundException {

    final String filename;
    final ConsumesOutputStream stream;
    StreamResponse ret;
    ModelService model = requestContext.getModelService();
    Binary preservationMetadataBinary = model.retrievePreservationFile(file.getAipId(), file.getRepresentationId(),
      file.getAncestorsPath(), file.getId());
    filename = preservationMetadataBinary.getStoragePath().getName() + HTML_EXT;
    List<String> parameters = PremisV3Utils.getApplicationTechnicalMetadataParameters(model, file.getAipId(),
      file.getRepresentationId(), file.getAncestorsPath(), file.getId());
    // PremisV3Utils
    StringBuilder htmlTechnical = new StringBuilder();
    for (int i = 0; i < parameters.size(); i += 2) {
      htmlTechnical.append(HTMLUtils.technicalMetadataToHtml(preservationMetadataBinary, parameters.get(i),
        parameters.get(i + 1), ServerTools.parseLocale(language)));
    }
    stream = new DefaultConsumesOutputStream(filename, RodaConstants.MEDIA_TYPE_TEXT_HTML, out -> {
      PrintStream printStream = new PrintStream(out);
      printStream.print(htmlTechnical);
      printStream.close();
    });

    ret = new StreamResponse(stream);

    return ret;
  }

  public StreamResponse retrieveFilePreservationFile(RequestContext requestContext, IndexedFile file)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException,
    TechnicalMetadataNotFoundException {

    final ConsumesOutputStream stream;
    StreamResponse ret;
    ModelService model = requestContext.getModelService();
    Binary preservationMetadataBinary = model.retrievePreservationFile(file.getAipId(), file.getRepresentationId(),
      file.getAncestorsPath(), file.getId());
    stream = new BinaryConsumesOutputStream(preservationMetadataBinary, RodaConstants.MEDIA_TYPE_TEXT_XML);

    ret = new StreamResponse(stream);

    return ret;
  }

  public TechnicalMetadataInfos retrieveFileTechnicalMetadataInfos(RequestContext requestContext, IndexedFile file,
    String localeString)
    throws AuthorizationDeniedException, RequestNotValidException, NotFoundException, GenericException {
    TechnicalMetadataInfos technicalMetadataInfos = new TechnicalMetadataInfos();

    ModelService model = requestContext.getModelService();

    Locale locale = ServerTools.parseLocale(localeString);
    Messages messages = RodaCoreFactory.getI18NMessages(locale);

    Representation representation = model.retrieveRepresentation(file.getAipId(), file.getRepresentationId());

    for (TechnicalMetadata technicalMetadata : representation.getTechnicalMetadata()) {
      String type = technicalMetadata.getType();
      String label = messages.getTranslation(
        RodaConstants.I18N_UI_BROWSE_METADATA_TECHNICAL_TYPE_PREFIX + type.toLowerCase(), technicalMetadata.getId());
      technicalMetadataInfos.addObject(new TechnicalMetadataInfo(type, label));
    }

    return technicalMetadataInfos;
  }

  public StreamResponse retrieveFileTechnicalMetadataHTML(RequestContext requestContext, IndexedFile file, String type,
    String versionID, String localeString) throws RequestNotValidException, AuthorizationDeniedException,
    NotFoundException, GenericException, TechnicalMetadataNotFoundException {
    ModelService model = requestContext.getModelService();
    Representation representation = model.retrieveRepresentation(file.getAipId(), file.getRepresentationId());
    String techMDURN = URNUtils.createRodaTechnicalMetadataURN(file.getId(),
      RODAInstanceUtils.getLocalInstanceIdentifier(), type.toLowerCase());
    Binary metadataBinary;
    if (versionID != null) {
      BinaryVersion binaryVersion = model.getBinaryVersion(representation, versionID,
        List.of(RodaConstants.STORAGE_DIRECTORY_METADATA, RodaConstants.STORAGE_DIRECTORY_TECHNICAL, type,
          techMDURN + RodaConstants.REPRESENTATION_INFORMATION_FILE_EXTENSION));
      metadataBinary = binaryVersion.getBinary();
    } else {
      metadataBinary = model.getBinary(representation, RodaConstants.STORAGE_DIRECTORY_METADATA,
        RodaConstants.STORAGE_DIRECTORY_TECHNICAL, type,
        techMDURN + RodaConstants.REPRESENTATION_INFORMATION_FILE_EXTENSION);
    }
    String filename = metadataBinary.getStoragePath().getName() + HTML_EXT;
    String htmlDescriptive = HTMLUtils.technicalMetadataToHtml(metadataBinary, type, versionID,
      ServerTools.parseLocale(localeString));

    ConsumesOutputStream stream = new DefaultConsumesOutputStream(filename, RodaConstants.MEDIA_TYPE_APPLICATION_XML,
      out -> {
        PrintStream printStream = new PrintStream(out);
        printStream.print(htmlDescriptive);
        printStream.close();
      });

    return new StreamResponse(stream);
  }

  public StreamResponse retrieveFileTechnicalMetadata(RequestContext requestContext, IndexedFile file, String type,
    String versionID)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException {

    final ConsumesOutputStream stream;
    StreamResponse ret;
    ModelService model = requestContext.getModelService();
    Representation representation = model.retrieveRepresentation(file.getAipId(), file.getRepresentationId());
    String techMDURN = URNUtils.createRodaTechnicalMetadataURN(file.getId(),
      RODAInstanceUtils.getLocalInstanceIdentifier(), type.toLowerCase());
    Binary metadataBinary;
    if (versionID != null) {
      BinaryVersion binaryVersion = model.getBinaryVersion(representation, versionID,
        List.of(RodaConstants.STORAGE_DIRECTORY_METADATA, RodaConstants.STORAGE_DIRECTORY_TECHNICAL, type,
          techMDURN + RodaConstants.REPRESENTATION_INFORMATION_FILE_EXTENSION));
      metadataBinary = binaryVersion.getBinary();
    } else {
      metadataBinary = model.getBinary(representation, RodaConstants.STORAGE_DIRECTORY_METADATA,
        RodaConstants.STORAGE_DIRECTORY_TECHNICAL, type,
        techMDURN + RodaConstants.REPRESENTATION_INFORMATION_FILE_EXTENSION);
    }
    stream = new BinaryConsumesOutputStream(metadataBinary, RodaConstants.MEDIA_TYPE_TEXT_XML);

    ret = new StreamResponse(stream);

    return ret;
  }

  public StreamResponse retrieveFileContentHTML(RequestContext requestContext, IndexedFile indexedFile,
    String localeString) throws GenericException, RequestNotValidException, NotFoundException,
    AuthorizationDeniedException {
    return retrieveFileContentHTML(requestContext, indexedFile, localeString, null);
  }

  public StreamResponse retrieveFileContentHTML(RequestContext requestContext, IndexedFile indexedFile,
    String localeString, String selectedXsltId) throws GenericException, RequestNotValidException, NotFoundException,
    AuthorizationDeniedException {

    ModelService model = requestContext.getModelService();
    Optional<LiteRODAObject> liteFile = LiteRODAObjectFactory.get(indexedFile);
    if (liteFile.isEmpty()) {
      throw new RequestNotValidException("Couldn't retrieve file with id: " + indexedFile.getId());
    }

    Binary binary = model.getBinary(liteFile.get());

    // Detect XML namespace from the file content
    String namespace = detectXmlNamespace(binary);
    if (namespace == null) {
      throw new RequestNotValidException("File is not XML or has no namespace: " + indexedFile.getId());
    }

    Locale locale = ServerTools.parseLocale(localeString);

    // Resolve the candidate XSLTs once. Both listAvailableXslts() and this
    // method share the same resolver so a stylesheet that appears in the
    // dropdown is guaranteed to be renderable.
    List<XsltSource> sources = resolveXsltSources(model, indexedFile, binary, namespace);
    if (sources.isEmpty()) {
      throw new NotFoundException("No XSLT stylesheet available for file: " + indexedFile.getId()
        + " (namespace: " + namespace + ")");
    }

    XsltSource chosen = chooseXsltSource(sources, selectedXsltId);
    if (chosen == null) {
      throw new NotFoundException("Requested XSLT '" + selectedXsltId
        + "' is not available for file: " + indexedFile.getId());
    }
    LOGGER.info("Rendering file {} with XSLT '{}'", indexedFile.getId(), chosen.id);
    String html = chosen.render(binary, locale);

    String filename = indexedFile.getId() + HTML_EXT;
    final String finalHtml = html;
    ConsumesOutputStream stream = new DefaultConsumesOutputStream(filename, RodaConstants.MEDIA_TYPE_TEXT_HTML,
      out -> {
        PrintStream printStream = new PrintStream(out);
        printStream.print(finalHtml);
        printStream.close();
      });

    return new StreamResponse(stream);
  }

  public List<Map<String, String>> listAvailableXslts(RequestContext requestContext, IndexedFile indexedFile)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException {

    ModelService model = requestContext.getModelService();
    Optional<LiteRODAObject> liteFile = LiteRODAObjectFactory.get(indexedFile);
    if (liteFile.isEmpty()) {
      throw new RequestNotValidException("Couldn't retrieve file with id: " + indexedFile.getId());
    }

    Binary binary = model.getBinary(liteFile.get());
    String namespace = detectXmlNamespace(binary);

    List<XsltSource> sources = resolveXsltSources(model, indexedFile, binary, namespace);

    List<Map<String, String>> result = new ArrayList<>(sources.size());
    for (XsltSource source : sources) {
      Map<String, String> entry = new HashMap<>();
      entry.put("id", source.id);
      entry.put("label", source.label);
      result.add(entry);
    }
    return result;
  }


  public StreamResponse retrieveFileContentHTMLWithCustomXslt(RequestContext requestContext,
    IndexedFile indexedFile, InputStream xsltInputStream, String localeString)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException {

    ModelService model = requestContext.getModelService();
    Optional<LiteRODAObject> liteFile = LiteRODAObjectFactory.get(indexedFile);
    if (liteFile.isEmpty()) {
      throw new RequestNotValidException("Couldn't retrieve file with id: " + indexedFile.getId());
    }

    Binary binary = model.getBinary(liteFile.get());
    Locale locale = ServerTools.parseLocale(localeString);
    String html = HTMLUtils.representationFileToHtmlWithCustomXslt(binary, xsltInputStream, locale);

    String filename = indexedFile.getId() + HTML_EXT;
    ConsumesOutputStream stream = new DefaultConsumesOutputStream(filename, RodaConstants.MEDIA_TYPE_TEXT_HTML,
      out -> {
        PrintStream printStream = new PrintStream(out);
        printStream.print(html);
        printStream.close();
      });

    return new StreamResponse(stream);
  }



  /**
   * Single source of truth for the candidate XSLT list for a given file. Both
   * listAvailableXslts() (dropdown) and retrieveFileContentHTML() (rendering)
   * call this so a stylesheet that appears in the UI can always be rendered.
   *
   * Lookup order (first non-empty layer wins):
   *  1. directly beside the XML file (same folder under representations/rep_X/data/...),
   *     but only when there is a filename-matched stylesheet (Foo.xml + Foo.xslt).
   *     Unrelated .xslt siblings (e.g. B.xslt next to A.xml) do NOT block lower
   *     layers — a wrongly-named neighbour would otherwise hide a properly-named
   *     stylesheet that lives in documentation/ or in the global config.
   *  2. representation-level documentation (representations/rep_X/documentation/)
   *  3. AIP-root documentation (documentation/)
   *  4. global namespace mapping from configuration
   */
  private List<XsltSource> resolveXsltSources(ModelService model, IndexedFile indexedFile, Binary binary,
    String namespace) {
    List<XsltSource> result = new ArrayList<>();

    // Layer 1: beside the XML file — filename match required
    if (indexedFile.getRepresentationId() != null) {
      result.addAll(toXsltSources(searchXsltsBesideXmlFile(model, indexedFile)));
    }
    if (!result.isEmpty()) {
      return result;
    }

    // Layer 2: representation-level documentation
    if (indexedFile.getRepresentationId() != null) {
      result.addAll(toXsltSources(searchAllXsltsInDocumentation(model, indexedFile.getAipId(),
        indexedFile.getRepresentationId(), indexedFile.getId())));
    }
    if (!result.isEmpty()) {
      return result;
    }

    // Layer 3: AIP-root documentation
    result.addAll(toXsltSources(searchAllXsltsInDocumentation(model, indexedFile.getAipId(),
      null, indexedFile.getId())));
    if (!result.isEmpty()) {
      return result;
    }

    // Layer 4: global namespace mapping
    if (namespace != null) {
      String xsltName = resolveXsltForNamespace(namespace);
      if (xsltName != null) {
        result.add(XsltSource.global(xsltName));
      }
    }
    return result;
  }

  private static List<XsltSource> toXsltSources(List<Binary> binaries) {
    List<XsltSource> out = new ArrayList<>(binaries.size());
    for (Binary b : binaries) {
      String name = b.getStoragePath().getName();
      out.add(XsltSource.bundled(name, b));
    }
    return out;
  }

  // XSLT files can use either of the two W3C-recognized extensions. Treat both
  // identically across discovery, filename matching, and label extraction.
  private static boolean isXsltFilename(String name) {
    if (name == null) {
      return false;
    }
    String lower = name.toLowerCase(Locale.ROOT);
    return lower.endsWith(".xslt") || lower.endsWith(".xsl");
  }

  private static String stripXsltExtension(String name) {
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

  private static boolean isXsltMatchForXml(String xsltName, String xmlBaseName) {
    if (xsltName == null || xmlBaseName == null) {
      return false;
    }
    // Normalize both sides through stripXsltExtension (case-insensitive on the
    // extension) and compare the bare base name case-insensitively, so Foo.XSL
    // matches Foo.xml and FOO.xslt matches foo.xml.
    String xsltBase = stripXsltExtension(xsltName);
    return xsltBase != null && xsltBase.equalsIgnoreCase(xmlBaseName);
  }

  private static XsltSource chooseXsltSource(List<XsltSource> sources, String requestedId) {
    if (requestedId == null || requestedId.isEmpty()) {
      return sources.isEmpty() ? null : sources.get(0);
    }
    for (XsltSource s : sources) {
      if (requestedId.equals(s.id)) {
        return s;
      }
    }
    return null;
  }

  /**
   * Internal value type representing a single XSLT candidate. Two flavors:
   *  - bundled: a Binary from the AIP storage (rep or AIP documentation)
   *  - global:  an XSLT name configured in roda-wui.properties, resolved by HTMLUtils
   *
   * The id field is what the client sends back in the xslt query parameter;
   * the label is the user-visible string in the dropdown.
   */
  private static final class XsltSource {
    final String id;
    final String label;
    private final Binary bundledBinary;
    private final String globalXsltName;

    private XsltSource(String id, String label, Binary bundledBinary, String globalXsltName) {
      this.id = id;
      this.label = label;
      this.bundledBinary = bundledBinary;
      this.globalXsltName = globalXsltName;
    }

    static XsltSource bundled(String filename, Binary binary) {
      return new XsltSource(filename, stripXsltExtension(filename), binary, null);
    }

    static XsltSource global(String xsltName) {
      return new XsltSource("global:" + xsltName, xsltName, null, xsltName);
    }

    String render(Binary inputBinary, Locale locale) throws GenericException {
      if (globalXsltName != null) {
        return HTMLUtils.representationFileToHtml(inputBinary, globalXsltName, locale);
      }
      try (InputStream xsltStream = bundledBinary.getContent().createInputStream()) {
        return HTMLUtils.representationFileToHtmlWithCustomXslt(inputBinary, xsltStream, locale);
      } catch (java.io.IOException e) {
        throw new GenericException("Failed to read bundled XSLT '" + id + "'", e);
      }
    }
  }

  /**
   * Look for XSLT files in the SAME folder as the supplied XML file inside
   * the representation data tree. Non-recursive: only direct siblings.
   *
   * Returns ONLY filename-matched stylesheets (Foo.xml → Foo.xslt or Foo.xsl,
   * case-insensitive). Unrelated .xslt siblings are intentionally ignored so
   * a B.xslt sitting next to A.xml cannot hide a correctly-named A.xslt in
   * documentation/ or in the global namespace mapping.
   */
  private List<Binary> searchXsltsBesideXmlFile(ModelService model, IndexedFile indexedFile) {
    try {
      // getFileStoragePath with a null fileId resolves to the parent directory
      // (data/ + the file's path components), matching the existing storage
      // pattern used by ModelUtils elsewhere.
      StoragePath parentDir = ModelUtils.getFileStoragePath(indexedFile.getAipId(),
        indexedFile.getRepresentationId(), indexedFile.getPath(), null);
      CloseableIterable<Resource> resources = model.getStorage().listResourcesUnderDirectory(parentDir, false);
      try {
        String xmlBaseName = xmlBaseName(indexedFile.getId());
        return collectXsltBinaries(model, resources, xmlBaseName, true);
      } finally {
        resources.close();
      }
    } catch (Exception e) {
      LOGGER.debug("Could not list XSLT siblings for file {} in rep {}: {}", indexedFile.getId(),
        indexedFile.getRepresentationId(), e.getMessage());
    }
    return Collections.emptyList();
  }

  private List<Binary> searchAllXsltsInDocumentation(ModelService model, String aipId, String representationId,
    String xmlFileName) {
    try {
      StoragePath docPath = representationId != null
        ? ModelUtils.getDocumentationStoragePath(aipId, representationId)
        : ModelUtils.getDocumentationStoragePath(aipId);
      CloseableIterable<Resource> resources = model.getStorage().listResourcesUnderDirectory(docPath, true);
      try {
        return collectXsltBinaries(model, resources, xmlBaseName(xmlFileName), false);
      } finally {
        resources.close();
      }
    } catch (Exception e) {
      LOGGER.debug("Could not list all XSLTs in documentation for aip={}, rep={}: {}", aipId, representationId,
        e.getMessage());
    }
    return Collections.emptyList();
  }

  private static String xmlBaseName(String xmlFileName) {
    if (xmlFileName != null && xmlFileName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
      return xmlFileName.substring(0, xmlFileName.length() - 4);
    }
    return null;
  }

  /**
   * Walk an iterable of storage resources and partition .xsl/.xslt files into
   * "filename matches the XML base" and "everything else". Matched files come
   * out first so the dropdown and the default-rendered stylesheet line up.
   *
   * When {@code matchedOnly} is true, only filename-matched stylesheets are
   * returned and unrelated siblings are dropped — used by the beside-the-XML
   * lookup so an unrelated neighbour cannot block lower discovery layers.
   */
  private static List<Binary> collectXsltBinaries(ModelService model, CloseableIterable<Resource> resources,
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
    // Filename-matched stylesheet wins regardless of order, the rest sort
    // alphabetically so the dropdown is deterministic across runs.
    others.sort((a, b) -> a.getStoragePath().getName().compareToIgnoreCase(b.getStoragePath().getName()));
    matched.addAll(others);
    return matched;
  }

  private String resolveXsltForNamespace(String namespace) {
    List<String> rules = RodaCoreFactory.getRodaConfigurationAsList("ui", "viewer", "xslt", "representation",
      "rules");
    for (String rule : rules) {
      String ruleNamespace = RodaCoreFactory.getRodaConfigurationAsString("ui", "viewer", "xslt", "representation",
        "rule", rule, "namespace");
      if (ruleNamespace != null && namespace.equals(ruleNamespace)) {
        return RodaCoreFactory.getRodaConfigurationAsString("ui", "viewer", "xslt", "representation", "rule", rule,
          "xslt");
      }
    }
    return null;
  }

  private String detectXmlNamespace(Binary binary) {
    try (InputStream is = binary.getContent().createInputStream()) {
      byte[] header = new byte[4096];
      int read = is.read(header);
      if (read <= 0) return null;

      XMLInputFactory factory = XMLInputFactory.newInstance();
      factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(header, 0, read));
      while (reader.hasNext()) {
        int event = reader.next();
        if (event == XMLStreamReader.START_ELEMENT) {
          String ns = reader.getNamespaceURI();
          reader.close();
          return ns;
        }
      }
      reader.close();
    } catch (Exception e) {
      LOGGER.debug("Could not detect XML namespace", e);
    }
    return null;
  }

}
