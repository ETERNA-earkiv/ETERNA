package org.roda.core.storage.scatteredfs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.roda.core.common.iterables.CloseableIterable;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.utils.JsonUtils;
import org.roda.core.data.v2.ip.ShallowFile;
import org.roda.core.data.v2.ip.ShallowFiles;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.storage.Binary;
import org.roda.core.storage.BinaryVersion;
import org.roda.core.storage.Container;
import org.roda.core.storage.ContentPayload;
import org.roda.core.storage.DefaultBinary;
import org.roda.core.storage.DefaultBinaryVersion;
import org.roda.core.storage.DefaultContainer;
import org.roda.core.storage.DefaultDirectory;
import org.roda.core.storage.DirectResourceAccess;
import org.roda.core.storage.Directory;
import org.roda.core.storage.EmptyClosableIterable;
import org.roda.core.storage.Entity;
import org.roda.core.storage.ExternalFileManifestContentPayload;
import org.roda.core.storage.Resource;
import org.roda.core.storage.StorageService;
import org.roda.core.storage.StorageServiceUtils;
import org.roda.core.storage.fs.FSPathContentPayload;
import org.roda.core.storage.fs.FSUtils;
import org.roda.core.storage.fs.FileStorageService;
import org.roda.core.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that persists binary files and their containers in scattered folders on the File System.
 *
 * @author Luis Faria <lfaria@keep.pt>
 * @author Hélder Silva <hsilva@keep.pt>
 * @author Filiph Schaaf <filiph.schaaf@whitered.se>
 */
public class ScatteredFileStorageService extends FileStorageService  {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScatteredFileStorageService.class);

  public static final String HISTORY_SUFFIX = "-history";
  private static final String HISTORY_DATA_FOLDER = "data";
  private static final String HISTORY_METADATA_FOLDER = "metadata";

  private final Path rodaDataPath;
  private final Path basePath;
  private final Path historyDataPath;
  private final Path historyMetadataPath;
  private final Path trashPath;

  public ScatteredFileStorageService(Path basePath, boolean createTrash, String trashDirName, boolean createHistory)
          throws GenericException {
    super(basePath, createTrash, trashDirName, createHistory);

    this.basePath = basePath;
    rodaDataPath = this.basePath.getParent();
    Path historyPath = rodaDataPath.resolve(basePath.getFileName() + HISTORY_SUFFIX);
    historyDataPath = historyPath.resolve(HISTORY_DATA_FOLDER);
    historyMetadataPath = historyPath.resolve(HISTORY_METADATA_FOLDER);
    trashPath = rodaDataPath.resolve(trashDirName == null ? RodaConstants.TRASH_CONTAINER : trashDirName);

    try {
      ScatteredFSUtils.initialize();
    } catch (GenericException exception) {
      LOGGER.error("Error! Could not initialize scattered fs.");
      throw exception;
    }
  }

  public ScatteredFileStorageService(Path basePath, String trashDirName) throws GenericException {
    this(basePath, true, trashDirName, true);
  }

  public ScatteredFileStorageService(Path basePath) throws GenericException {
    this(basePath, null);
  }

  @Override
  public boolean exists(StoragePath storagePath) {
    return FSUtils.exists(ScatteredFSUtils.getEntityPath(basePath, storagePath));
  }

  @Override
  public CloseableIterable<Container> listContainers() throws GenericException {
    return ScatteredFSUtils.listContainers(basePath);
  }

  @Override
  public Container createContainer(StoragePath storagePath) throws GenericException, AlreadyExistsException {
    Path containerPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    Path directory = null;
    try {
      directory = Files.createDirectory(containerPath);
      return new DefaultContainer(storagePath);
    } catch (FileAlreadyExistsException e) {
      // cleanup
      try {
        FSUtils.deletePath(directory);
      } catch (NotFoundException e1) {
        LOGGER.warn("Error while trying to clean up", e1);
      }

      throw new AlreadyExistsException("Could not create container at " + containerPath, e);
    } catch (IOException e) {
      // cleanup
      try {
        FSUtils.deletePath(directory);
      } catch (NotFoundException e1) {
        LOGGER.warn("Error while trying to clean up", e1);
      }

      throw new GenericException("Could not create container at " + containerPath, e);
    }
  }

  @Override
  public Container getContainer(StoragePath storagePath) throws RequestNotValidException, NotFoundException {
    if (!storagePath.isFromAContainer()) {
      throw new RequestNotValidException("Storage path is not from a container");
    }

    Path containerPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    Container container;
    if (FSUtils.exists(containerPath)) {
      container = new DefaultContainer(storagePath);
    } else {
      throw new NotFoundException("Container not found: " + storagePath);
    }
    return container;
  }

  @Override
  public void deleteContainer(StoragePath storagePath) throws NotFoundException, GenericException {
    Path containerPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    trash(containerPath);

    // cleanup history
    deleteAllBinaryVersionsUnder(storagePath);

  }

  private void trash(Path fromPath) throws GenericException, NotFoundException {
    if (trashPath == null) {
      LOGGER.warn("Skipping trash '{}' because no trash folder is defined!", fromPath);
      return;
    }
    try {
      Path toPath = trashPath.resolve(rodaDataPath.relativize(fromPath));
      LOGGER.debug("Moving to trash: {} to {}", fromPath, toPath);
      FSUtils.move(fromPath, toPath, true);
    } catch (AlreadyExistsException e) {
      String unique = IdUtils.createUUID();
      Path uniqueToPath = trashPath.resolve(unique).resolve(rodaDataPath.relativize(fromPath));
      try {
        LOGGER.debug("Re-trying to move to trash: {} to {}", fromPath, uniqueToPath);
        FSUtils.move(fromPath, uniqueToPath, true);
      } catch (AlreadyExistsException e1) {
        LOGGER.error("Error moving to trash: {} to {}", fromPath, uniqueToPath, e1);
        throw new GenericException("Unexpected exception while moving to trash", e1);
      }
    }
  }

  @Override
  public CloseableIterable<Resource> listResourcesUnderContainer(StoragePath storagePath, boolean recursive)
          throws NotFoundException, GenericException {
    Path path = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    if (recursive) {
      return ScatteredFSUtils.recursivelyListPath(basePath, path);
    } else {
      return ScatteredFSUtils.listResourcesUnderContainer(basePath, path.getFileName().toString());
    }
  }

  @Override
  public Long countResourcesUnderContainer(StoragePath storagePath, boolean recursive)
          throws NotFoundException, GenericException {
    Path path = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    if (recursive) {
      return FSUtils.recursivelyCountPath(path);
    } else {
      return FSUtils.countPath(path);
    }
  }

  @Override
  public Directory createDirectory(StoragePath storagePath) throws AlreadyExistsException, GenericException {
    Path dirPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    Path directory = null;

    if (FSUtils.exists(dirPath)) {
      throw new AlreadyExistsException("Could not create directory at " + dirPath);
    }

    try {
      directory = Files.createDirectories(dirPath);
      return new DefaultDirectory(storagePath);
    } catch (IOException e) {
      // cleanup
      try {
        FSUtils.deletePath(directory);
      } catch (NotFoundException | GenericException e1) {
        LOGGER.warn("Error while cleaning up", e1);
      }

      throw new GenericException("Could not create directory at " + dirPath, e);
    }
  }

  @Override
  public Directory createRandomDirectory(StoragePath parentStoragePath)
          throws RequestNotValidException, GenericException, NotFoundException, AlreadyExistsException {
    Path parentDirPath = ScatteredFSUtils.getEntityPath(basePath, parentStoragePath);
    Path directory = null;

    try {
      directory = ScatteredFSUtils.createRandomDirectory(parentDirPath);

      return new DefaultDirectory(ScatteredFSUtils.getStoragePath(basePath, directory));
    } catch (FileAlreadyExistsException e) {
      // cleanup
      FSUtils.deletePath(directory);

      throw new AlreadyExistsException("Could not create random directory under " + parentDirPath, e);
    } catch (IOException e) {
      // cleanup
      FSUtils.deletePath(directory);

      throw new GenericException("Could not create random directory under " + parentDirPath, e);
    }
  }

  @Override
  public Directory getDirectory(StoragePath storagePath)
          throws RequestNotValidException, NotFoundException, GenericException {
    if (storagePath.isFromAContainer()) {
      throw new RequestNotValidException("Invalid storage path for a directory: " + storagePath);
    }

    Path directoryPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    Resource resource = ScatteredFSUtils.convertPathToResource(basePath, directoryPath);

    if (resource instanceof Directory) {
      return (Directory) resource;
    } else {
      throw new RequestNotValidException("Looking for a directory but found something else: " + storagePath);
    }
  }

  @Override
  public CloseableIterable<Resource> listResourcesUnderDirectory(StoragePath storagePath, boolean recursive)
          throws NotFoundException, GenericException {
    Path directoryPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    if (recursive) {
      return ScatteredFSUtils.recursivelyListPath(basePath, directoryPath);
    } else {
      return ScatteredFSUtils.listPath(basePath, directoryPath);
    }
  }

  @Override
  public CloseableIterable<Resource> listResourcesUnderFile(StoragePath storagePath, boolean recursive)
          throws NotFoundException, GenericException {
    Path directoryPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    return ScatteredFSUtils.listPathUnderFile(basePath, directoryPath);
  }

  @Override
  public Long countResourcesUnderDirectory(StoragePath storagePath, boolean recursive)
          throws NotFoundException, GenericException {
    Path directoryPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    if (recursive) {
      return FSUtils.recursivelyCountPath(directoryPath);
    } else {
      return FSUtils.countPath(directoryPath);
    }
  }

  @Override
  public Binary createBinary(StoragePath storagePath, ContentPayload payload, boolean asReference)
          throws GenericException, AlreadyExistsException {
    if (asReference) {
      Path binPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
      try {
        if (FSUtils.exists(binPath)) {
          if (payload instanceof ExternalFileManifestContentPayload) {
            ShallowFile shallowFile = ((ExternalFileManifestContentPayload) payload).getShallowFiles().getObjects()
                    .get(0);
            ShallowFiles manifestContent = FSUtils.retrieveManifestFileContent(binPath);

            for (ShallowFile manifestFileShallow : manifestContent.getObjects()) {
              if (manifestFileShallow.getName().equals(shallowFile.getName())) {
                throw new AlreadyExistsException("Binary already exists: " + binPath);
              }
            }
            manifestContent.addObject(shallowFile);
            payload = new ExternalFileManifestContentPayload(manifestContent);
          } else {
            throw new GenericException("Looking for a manifest external file but found something else: " + storagePath);
          }
        }
        // ensuring parent exists
        Path parent = binPath.getParent();
        if (!FSUtils.exists(parent)) {
          Files.createDirectories(parent);
        }

        // writing file
        payload.writeToPath(binPath);
        Long sizeInBytes = Files.size(binPath);
        return new DefaultBinary(storagePath, payload, sizeInBytes, true, new HashMap<>());
      } catch (IOException e) {
        throw new GenericException("Could not create binary", e);
      }
    } else {
      Path binPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
      if (FSUtils.exists(binPath)) {
        throw new AlreadyExistsException("Binary already exists: " + binPath);
      } else {
        try {
          // ensuring parent exists
          Path parent = binPath.getParent();
          if (!FSUtils.exists(parent)) {
            Files.createDirectories(parent);
          }

          // writing file
          payload.writeToPath(binPath);
          ContentPayload newPayload = new FSPathContentPayload(binPath);
          Long sizeInBytes = Files.size(binPath);
          boolean isReference = false;
          Map<String, String> contentDigest = null;

          return new DefaultBinary(storagePath, newPayload, sizeInBytes, isReference, contentDigest);
        } catch (FileAlreadyExistsException e) {
          throw new AlreadyExistsException("Binary already exists: " + binPath);
        } catch (IOException e) {
          throw new GenericException("Could not create binary", e);
        }
      }
    }
  }

  @Override
  public Binary createRandomBinary(StoragePath parentStoragePath, ContentPayload payload, boolean asReference)
          throws GenericException, RequestNotValidException {
    if (asReference) {
      throw new GenericException("Method not yet implemented");
    } else {
      Path parent = ScatteredFSUtils.getEntityPath(basePath, parentStoragePath);
      try {
        // ensure parent exists
        if (!FSUtils.exists(parent)) {
          Files.createDirectories(parent);
        }

        // create file
        Path binPath = FSUtils.createRandomFile(parent);

        // writing file
        payload.writeToPath(binPath);
        StoragePath storagePath = ScatteredFSUtils.getStoragePath(basePath, binPath);
        ContentPayload newPayload = new FSPathContentPayload(binPath);
        Long sizeInBytes = Files.size(binPath);
        boolean isReference = false;
        Map<String, String> contentDigest = null;

        return new DefaultBinary(storagePath, newPayload, sizeInBytes, isReference, contentDigest);
      } catch (IOException e) {
        throw new GenericException("Could not create binary", e);
      }
    }
  }

  @Override
  public Binary updateBinaryContent(StoragePath storagePath, ContentPayload payload, boolean asReference,
                                    boolean createIfNotExists) throws GenericException, NotFoundException, RequestNotValidException {
    if (asReference) {
      Path binaryPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
      boolean fileExists = ScatteredFSUtils.exists(binaryPath);
      if (!fileExists && !createIfNotExists) {
        throw new NotFoundException("Binary does not exist: " + binaryPath);
      } else {
        try {
          ShallowFile shallowFile = ((ExternalFileManifestContentPayload) payload).getShallowFiles().getObjects()
                  .get(0);
          ShallowFiles manifestFileContent = FSUtils.retrieveManifestFileContent(binaryPath);
          manifestFileContent.getObjects()
                  .replaceAll(sf -> sf.getName().equals(shallowFile.getName()) ? shallowFile : sf);
          ExternalFileManifestContentPayload newPayload = new ExternalFileManifestContentPayload(manifestFileContent);
          newPayload.writeToPath(binaryPath);

          Path sfPath = binaryPath.getParent().resolve(shallowFile.getName());
          StoragePath sfStoragePath = ScatteredFSUtils.getStoragePath(basePath, sfPath);
          Resource resource = FSUtils.convertReferenceToResource(sfStoragePath, shallowFile.getLocation().toString(),
                  false);
          if (resource instanceof Binary) {
            return (DefaultBinary) resource;
          } else {
            throw new GenericException("Looking for a binary but found something else");
          }
        } catch (IOException e) {
          throw new GenericException("Could not update binary content", e);
        }
      }
    } else {
      Path binaryPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
      boolean fileExists = FSUtils.exists(binaryPath);

      if (!fileExists && !createIfNotExists) {
        throw new NotFoundException("Binary does not exist: " + binaryPath);
      } else if (fileExists && !FSUtils.isFile(binaryPath)) {
        throw new GenericException("Looking for a binary but found something else");
      } else {
        try {
          binaryPath.getParent().toFile().mkdirs();
          payload.writeToPath(binaryPath);
        } catch (IOException e) {
          throw new GenericException("Could not update binary content", e);
        }
      }

      Resource resource = ScatteredFSUtils.convertPathToResource(basePath, binaryPath);
      if (resource instanceof Binary) {
        return (DefaultBinary) resource;
      } else {
        throw new GenericException("Looking for a binary but found something else");
      }
    }
  }

  @Override
  public Binary getBinary(StoragePath storagePath)
          throws RequestNotValidException, NotFoundException, GenericException {
    Path binaryPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    if (FSUtils.exists(binaryPath)) {
      Resource resource = ScatteredFSUtils.convertPathToResource(basePath, binaryPath);
      if (resource instanceof Binary) {
        return (Binary) resource;
      } else {
        throw new RequestNotValidException("Looking for a binary but found something else");
      }
    } else {
      ShallowFile shallowFile = FSUtils.isResourcePresentOnManifestFile(binaryPath);
      if (shallowFile != null) {
        Resource resource = FSUtils.convertReferenceToResource(storagePath, shallowFile.getLocation().toString(),
                false);
        if (resource instanceof DefaultBinary) {
          ((DefaultBinary) resource).setSizeInBytes(shallowFile.getSize());
          return (Binary) resource;
        } else {
          throw new RequestNotValidException("Looking for a binary but found something else");
        }
      }
    }
    throw new NotFoundException("Cannot find file or directory at " + binaryPath);
  }

  @Override
  public void deleteResource(StoragePath storagePath) throws NotFoundException, GenericException {
    Path resourcePath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    if (FSUtils.exists(resourcePath)) {
      trash(resourcePath);

      // cleanup history
      deleteAllBinaryVersionsUnder(storagePath);
    } else {
      ShallowFile shallowFile = FSUtils.isResourcePresentOnManifestFile(resourcePath);
      if (shallowFile != null) {
        FSUtils.removeResourceFromManifestFile(resourcePath);
      }
    }
  }

  public Path resolve(StoragePath storagePath) {
    return ScatteredFSUtils.getEntityPath(basePath, storagePath);
  }

  @Override
  public void copy(StorageService fromService, StoragePath fromStoragePath, StoragePath toStoragePath)
          throws AlreadyExistsException, GenericException, RequestNotValidException, NotFoundException,
          AuthorizationDeniedException {
    if (fromService instanceof FileStorageService) {
      Path sourcePath = ((FileStorageService) fromService).resolve(fromStoragePath);
      Path targetPath = ScatteredFSUtils.getEntityPath(basePath, toStoragePath);
      FSUtils.copy(sourcePath, targetPath, false);
    } else {
      Class<? extends Entity> rootEntity = fromService.getEntity(fromStoragePath);
      StorageServiceUtils.copyBetweenStorageServices(fromService, fromStoragePath, this, toStoragePath, rootEntity);
    }
  }

  @Override
  public void copy(StorageService fromService, StoragePath fromStoragePath, Path toPath, String resource)
          throws AlreadyExistsException, GenericException {
    Path sourcePath = null;
    if (StringUtils.isNotBlank(resource)) {
      sourcePath = ScatteredFSUtils.getEntityPath(basePath, fromStoragePath).resolve(resource);
    } else {
      sourcePath = ScatteredFSUtils.getEntityPath(basePath, fromStoragePath);
    }
    if (FSUtils.exists(sourcePath)) {
      FSUtils.copy(sourcePath, toPath, false);
    }
  }

  @Override
  public void move(StorageService fromService, StoragePath fromStoragePath, StoragePath toStoragePath)
          throws AlreadyExistsException, GenericException, RequestNotValidException, NotFoundException,
          AuthorizationDeniedException {
    if (fromService instanceof ScatteredFileStorageService) {
      Path sourcePath = ((ScatteredFileStorageService) fromService).resolve(fromStoragePath);
      Path targetPath = ScatteredFSUtils.getEntityPath(basePath, toStoragePath);
      FSUtils.move(sourcePath, targetPath, false);
    } else {
      Class<? extends Entity> rootEntity = fromService.getEntity(fromStoragePath);
      StorageServiceUtils.moveBetweenStorageServices(fromService, fromStoragePath, this, toStoragePath, rootEntity);
    }
  }

  @Override
  public Class<? extends Entity> getEntity(StoragePath storagePath) throws NotFoundException {
    Path entity = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    if (FSUtils.exists(entity)) {
      return getEntityClass(storagePath, entity);
    } else {
      return getEntityExternalFile(storagePath);
    }
  }

  public Class<? extends Entity> getEntityExternalFile(StoragePath storagePath) throws NotFoundException {
    Optional<String> aipId = ModelUtils.extractAipId(storagePath);
    Optional<String> representationId = ModelUtils.extractRepresentationId(storagePath);
    List<String> path = ModelUtils.extractFilePathFromRepresentationData(storagePath);
    try {
      StoragePath externalFile = ModelUtils.getFileStoragePath(aipId.get(), representationId.get(), path,
              RodaConstants.RODA_MANIFEST_EXTERNAL_FILES);
      Path entity = ScatteredFSUtils.getEntityPath(basePath, externalFile);
      if (FSUtils.exists(entity)) {
        return getEntityClass(externalFile, entity);
      } else {
        throw new NotFoundException("Entity was not found: " + storagePath);
      }
    } catch (RequestNotValidException e) {
      throw new NotFoundException("Entity was not found: " + storagePath);
    }
  }

  @Override
  public DirectResourceAccess getDirectAccess(final StoragePath storagePath) {
    return new DirectResourceAccess() {

      @Override
      public Path getPath() {
        // TODO disable write access to resource
        // for UNIX programs using user with read-only permissions
        // for Java programs using SecurityManager and Policy
        return ScatteredFSUtils.getEntityPath(basePath, storagePath);
      }

      @Override
      public void close() {
        // nothing to do
      }
    };
  }

  @Override
  public CloseableIterable<BinaryVersion> listBinaryVersions(StoragePath storagePath)
          throws GenericException, NotFoundException {
    if (historyDataPath == null) {
      LOGGER.warn("Skipping list binary versions because no history folder is defined, so returning empty list!");
      return new EmptyClosableIterable<>();
    }

    Path fauxPath = ScatteredFSUtils.getEntityPath(historyDataPath, storagePath);
    Path parent = fauxPath.getParent();
    final String baseName = fauxPath.getFileName().toString();

    CloseableIterable<BinaryVersion> iterable;

    if (!FSUtils.exists(parent)) {
      return new EmptyClosableIterable<>();
    }

    try {
      final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parent,
              new DirectoryStream.Filter<Path>() {

                @Override
                public boolean accept(Path entry) {
                  return entry.getFileName().toString().startsWith(baseName);
                }
              });

      final Iterator<Path> pathIterator = directoryStream.iterator();
      iterable = new CloseableIterable<BinaryVersion>() {

        @Override
        public Iterator<BinaryVersion> iterator() {
          return new Iterator<BinaryVersion>() {

            @Override
            public boolean hasNext() {
              return pathIterator.hasNext();
            }

            @Override
            public BinaryVersion next() {
              Path next = pathIterator.next();
              BinaryVersion ret;
              try {
                ret = ScatteredFSUtils.convertPathToBinaryVersion(historyDataPath, historyMetadataPath, next);
              } catch (GenericException | NotFoundException | RequestNotValidException e) {
                LOGGER.error("Error while list path " + basePath + " while parsing resource " + next, e);
                ret = null;
              }

              return ret;
            }

          };
        }

        @Override
        public void close() throws IOException {
          directoryStream.close();
        }
      };

    } catch (NoSuchFileException e) {
      throw new NotFoundException("Could not find versions of " + storagePath, e);
    } catch (IOException e) {
      throw new GenericException("Error finding version of " + storagePath, e);
    }

    return iterable;
  }

  @Override
  public BinaryVersion getBinaryVersion(StoragePath storagePath, String version)
          throws RequestNotValidException, NotFoundException, GenericException {
    if (historyDataPath == null) {
      throw new GenericException("Skipping get binary version because no history folder is defined!");
    }
    Path binVersionPath = ScatteredFSUtils.getEntityPath(historyDataPath, storagePath, version);
    return ScatteredFSUtils.convertPathToBinaryVersion(historyDataPath, historyMetadataPath, binVersionPath);
  }

  @Override
  public BinaryVersion createBinaryVersion(StoragePath storagePath, Map<String, String> properties)
          throws RequestNotValidException, NotFoundException, GenericException {
    if (historyDataPath == null) {
      throw new GenericException("Skipping create binary version because no history folder is defined!");
    }

    Path binPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);

    String id = IdUtils.createUUID();
    Path dataPath = ScatteredFSUtils.getEntityPath(historyDataPath, storagePath, id);
    Path metadataPath = FSUtils.getBinaryHistoryMetadataPath(historyDataPath, historyMetadataPath, dataPath);

    if (!FSUtils.exists(binPath)) {
      throw new NotFoundException("Binary does not exist: " + binPath);
    }

    if (!FSUtils.isFile(binPath)) {
      throw new RequestNotValidException("Not a regular file: " + binPath);
    }

    if (FSUtils.exists(dataPath)) {
      throw new GenericException("Binary version id collided: " + dataPath);
    }

    try {
      // ensuring parent exists
      Path parent = dataPath.getParent();
      if (!FSUtils.exists(parent)) {
        Files.createDirectories(parent);
      }

      // writing file
      Files.copy(binPath, dataPath);

      // Creating metadata
      DefaultBinaryVersion b = new DefaultBinaryVersion();
      b.setId(id);
      b.setProperties(properties);
      b.setCreatedDate(new Date());
      Files.createDirectories(metadataPath.getParent());
      JsonUtils.writeObjectToFile(b, metadataPath);

      return ScatteredFSUtils.convertPathToBinaryVersion(historyDataPath, historyMetadataPath, dataPath);
    } catch (IOException e) {
      throw new GenericException("Could not create binary", e);
    }
  }

  @Override
  public void revertBinaryVersion(StoragePath storagePath, String version)
          throws NotFoundException, RequestNotValidException, GenericException {
    if (historyDataPath == null) {
      LOGGER.warn("Skipping revert binary version because no history folder is defined!");
      return;
    }

    Path binPath = ScatteredFSUtils.getEntityPath(basePath, storagePath);
    Path binVersionPath = ScatteredFSUtils.getEntityPath(historyDataPath, storagePath, version);

    if (!FSUtils.exists(binPath)) {
      throw new NotFoundException("Binary does not exist: " + binPath);
    }

    if (!FSUtils.isFile(binPath)) {
      throw new RequestNotValidException("Not a regular file: " + binPath);
    }

    if (!FSUtils.exists(binVersionPath)) {
      throw new NotFoundException("Binary version does not exist: " + binVersionPath);
    }

    try {
      // writing file
      Files.copy(binVersionPath, binPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new GenericException("Could not create binary", e);
    }
  }

  @Override
  public void deleteBinaryVersion(StoragePath storagePath, String version)
          throws NotFoundException, GenericException, RequestNotValidException {
    if (historyDataPath == null) {
      LOGGER.warn("Skipping delete binary version because no history folder is defined!");
      return;
    }

    Path dataPath = ScatteredFSUtils.getEntityPath(historyDataPath, storagePath, version);
    Path metadataPath = FSUtils.getBinaryHistoryMetadataPath(historyDataPath, historyMetadataPath, dataPath);

    trash(dataPath);
    trash(metadataPath);

    // cleanup created parents
    FSUtils.deleteEmptyAncestorsQuietly(dataPath, historyDataPath);
    FSUtils.deleteEmptyAncestorsQuietly(metadataPath, historyMetadataPath);
  }

  private void deleteAllBinaryVersionsUnder(StoragePath storagePath) {
    if (historyDataPath == null) {
      LOGGER.warn("Skipping delete all binary versions because no history folder is defined!");
      return;
    }

    Path resourcePath = ScatteredFSUtils.getEntityPath(basePath, storagePath);

    Path relativePath = basePath.relativize(resourcePath);
    Path resourceHistoryDataPath = historyDataPath.resolve(relativePath);

    if (FSUtils.isDirectory(resourceHistoryDataPath)) {
      try {
        Path resourceHistoryMetadataPath = historyMetadataPath
                .resolve(historyDataPath.relativize(resourceHistoryDataPath));

        trash(resourceHistoryDataPath);
        trash(resourceHistoryMetadataPath);

        FSUtils.deleteEmptyAncestorsQuietly(resourceHistoryDataPath, historyDataPath);
        FSUtils.deleteEmptyAncestorsQuietly(resourceHistoryMetadataPath, historyMetadataPath);
      } catch (GenericException | NotFoundException e) {
        LOGGER.warn("Could not delete history under " + resourceHistoryDataPath, e);
      }
    } else {
      Path parent = resourceHistoryDataPath.getParent();
      final String baseName = resourceHistoryDataPath.getFileName().toString();

      if (FSUtils.exists(parent)) {
        DirectoryStream<Path> directoryStream = null;
        try {
          directoryStream = Files.newDirectoryStream(parent, new DirectoryStream.Filter<Path>() {

            @Override
            public boolean accept(Path entry) {
              return entry.getFileName().toString().startsWith(baseName);
            }
          });

          for (Path p : directoryStream) {
            trash(p);

            Path pMetadata = FSUtils.getBinaryHistoryMetadataPath(historyDataPath, historyMetadataPath, p);
            trash(pMetadata);

            FSUtils.deleteEmptyAncestorsQuietly(p, historyDataPath);
            FSUtils.deleteEmptyAncestorsQuietly(pMetadata, historyMetadataPath);
          }
        } catch (IOException | GenericException | NotFoundException e) {
          LOGGER.warn("Could not delete history under " + resourceHistoryDataPath, e);
        } finally {
          IOUtils.closeQuietly(directoryStream);
        }
      }
    }
  }

  @Override
  public boolean hasDirectory(StoragePath storagePath) {
    try {
      this.getDirectory(storagePath);
      return true;
    } catch (NotFoundException | RequestNotValidException | GenericException e) {
      return false;
    }
  }

  @Override
  public boolean hasBinary(StoragePath storagePath) {
    try {
      this.getBinary(storagePath);
      return true;
    } catch (NotFoundException | RequestNotValidException | GenericException e) {
      return false;
    }
  }

  @Override
  public List<StoragePath> getShallowFiles(StoragePath storagePath) throws NotFoundException, GenericException {
    ArrayList<StoragePath> storagePaths = new ArrayList<>();
    for (Resource resource : listResourcesUnderContainer(storagePath, true)) {
      if (resource instanceof Binary) {
        if (((Binary) resource).getContent() instanceof ExternalFileManifestContentPayload) {
          storagePaths.add(resource.getStoragePath());
        }
      }
    }
    return storagePaths;
  }
}
