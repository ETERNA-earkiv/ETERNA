package org.roda.core.storage.scatteredfs;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.roda.core.RodaCoreFactory;
import org.roda.core.common.iterables.CloseableIterable;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.utils.JsonUtils;
import org.roda.core.data.v2.ip.ShallowFile;
import org.roda.core.data.v2.ip.ShallowFiles;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.storage.*;
import org.roda.core.storage.fs.FSPathContentPayload;
import org.roda.core.storage.fs.FSUtils;
import org.roda.core.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

/**
 * File System related utility class
 *
 * @author Luis Faria <lfaria@keep.pt>
 * @author Hélder Silva <hsilva@keep.pt>
 * @author Filiph Schaaf <filiph.schaaf@whitered.se>
 */
public class ScatteredFSUtils extends FSUtils {

  public static final Logger LOGGER = LoggerFactory.getLogger(ScatteredFSUtils.class);

  public static final char VERSION_SEP = '_';
  public static final String METADATA_SUFFIX = ".json";
  public static final String SEPARATOR = "/";
  public static final String SEPARATOR_REGEX = "/";
  public static final String SCATTERED_FS_FILESYSTEM_PROPERTY = "core.storage.filesystem";

  private static final Map<String, ScatteredFSConfig> containerNameScatteredFSConfigMap = new HashMap<>();

  public static void initialize() throws GenericException {
    List<String> scatteredFSFilesystems = RodaCoreFactory.getRodaConfigurationAsList(SCATTERED_FS_FILESYSTEM_PROPERTY);

    for(String filesystem : scatteredFSFilesystems) {
      ScatteredFSConfig config = ScatteredFSConfig.getScatteredFSConfigFromRodaConfiguration(filesystem);

      containerNameScatteredFSConfigMap.put(filesystem, config);
    }
  }

  /**
   * Get path
   *
   * @param basePath
   *          base path
   * @param storagePath
   *          storage path, related to base path, that one wants to resolve
   */
  public static Path getEntityPath(Path basePath, StoragePath storagePath) {
    String containerName = storagePath.getContainerName();
    if (storagePath.asList().size() > 1 && containerNameScatteredFSConfigMap.containsKey(containerName)) {
      String id;
      List<String> directoryPathList = storagePath.getDirectoryPath();
      String directoryPath = "";

      if(!directoryPathList.isEmpty()) {
        id = directoryPathList.get(0);
        directoryPathList = directoryPathList.subList(1, directoryPathList.size());
        directoryPathList.add(storagePath.getName());
        directoryPath = String.join(File.separator, directoryPathList);
      } else {
        id = storagePath.getName();
      }

      ScatteredFSConfig fsConfig = containerNameScatteredFSConfigMap.get(containerName);
      if(fsConfig.isValidName(Path.of(id))) {
        Path scatteredPath = fsConfig.getScatteredPath(id);
        return basePath.resolve(containerName).resolve(scatteredPath).resolve(directoryPath);
      }
    }

    return FSUtils.getEntityPath(basePath, storagePath);
  }

  public static Path getEntityPath(Path basePath, StoragePath storagePath, String version)
          throws RequestNotValidException {
    if (version.indexOf(VERSION_SEP) >= 0) {
      throw new RequestNotValidException("Cannot use '" + VERSION_SEP + "' in version " + version);
    }

    String containerName = storagePath.getContainerName();
    if (storagePath.asList().size() > 1 && containerNameScatteredFSConfigMap.containsKey(containerName)) {
      String id;
      List<String> directoryPathList = storagePath.getDirectoryPath();
      String directoryPath = "";

      if(directoryPathList.size() > 1) {
        id = directoryPathList.get(0);
        directoryPathList = directoryPathList.subList(1, directoryPathList.size());
        directoryPathList.add(storagePath.getName() + VERSION_SEP + version);
        directoryPath = String.join(File.separator, directoryPathList);
      } else {
        id = storagePath.getName();
      }

      ScatteredFSConfig fsConfig = containerNameScatteredFSConfigMap.get(containerName);
      if(fsConfig.isValidName(Path.of(id))) {
        Path scatteredPath = fsConfig.getScatteredPath(id);
        return basePath.resolve(containerName).resolve(scatteredPath).resolve(directoryPath);
      }
    }

    return FSUtils.getEntityPath(basePath, storagePath);
  }

  public static StoragePath getStoragePath(Path basePath, Path absolutePath) throws RequestNotValidException {
    return getStoragePath(basePath.relativize(absolutePath));
  }

  public static StoragePath getStoragePath(Path relativePath) throws RequestNotValidException {
    String containerName = String.valueOf(relativePath.getName(0));

    if (containerNameScatteredFSConfigMap.containsKey(containerName)) {
      ScatteredFSConfig fsConfig = containerNameScatteredFSConfigMap.get(containerName);

      if (relativePath.getNameCount() > fsConfig.getScatteredFolderDepth() + 1) {
        Path scatteredRoot = Path.of("");
        for (int i = 1; i < fsConfig.getScatteredFolderDepth() + 2; i++) {
          scatteredRoot = scatteredRoot.resolve(relativePath.getName(i));
        }

        String id = scatteredRoot.getFileName().toString();
        Path scatteredPath = fsConfig.getScatteredPath(id);
        if(scatteredPath.equals(scatteredRoot) && fsConfig.isValidName(scatteredRoot)) {
          return fsConfig.getStoragePath(relativePath);
        }
      }
    }

    return FSUtils.getStoragePath(relativePath);
  }

  /**
   * List content of the certain folder
   *
   * @param basePath
   *          base path
   * @param path
   *          relative path to base path
   * @throws NotFoundException
   * @throws GenericException
   */
  public static CloseableIterable<Resource> listPath(final Path basePath, final Path path)
          throws NotFoundException, GenericException {
    CloseableIterable<Resource> resourceIterable;
    try {
      final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);
      final Iterator<Path> pathIterator = directoryStream.iterator();
      resourceIterable = new CloseableIterable<Resource>() {

        @Override
        public Iterator<Resource> iterator() {
          return new Iterator<Resource>() {

            @Override
            public boolean hasNext() {
              return pathIterator.hasNext();
            }

            @Override
            public Resource next() {
              Path next = pathIterator.next();
              Resource ret;
              try {
                ret = convertPathToResource(basePath, next);
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
      throw new NotFoundException("Could not list contents of entity because it doesn't exist: " + path, e);
    } catch (IOException e) {
      throw new GenericException("Could not list contents of entity at: " + path, e);
    }

    return resourceIterable;
  }

  public static CloseableIterable<Resource> listPathUnderFile(final Path basePath, final Path path)
          throws NotFoundException, GenericException {
    CloseableIterable<Resource> resourceIterable;
    try {
      LineIterator lineIterator = FileUtils.lineIterator(path.toFile());
      resourceIterable = new CloseableIterable<Resource>() {

        @Override
        public Iterator<Resource> iterator() {
          return new Iterator<Resource>() {

            @Override
            public boolean hasNext() {
              return lineIterator.hasNext();
            }

            @Override
            public Resource next() {
              String json = lineIterator.next();
              Resource ret;
              try {
                JsonContentPayload content = new JsonContentPayload(json);
                StoragePath storagePath = getStoragePath(basePath, path);
                ret = new DefaultBinary(storagePath, content, 0L, true, new HashMap<>());
              } catch (RequestNotValidException e) {
                LOGGER.error("Error while list path " + basePath + " while parsing resource " + json, e);
                ret = null;
              }

              return ret;
            }

          };
        }

        @Override
        public void close() throws IOException {
          lineIterator.close();
        }
      };

    } catch (NoSuchFileException e) {
      throw new NotFoundException("Could not list contents of entity because it doesn't exist: " + path, e);
    } catch (IOException e) {
      throw new GenericException("Could not list contents of entity at: " + path, e);
    }

    return resourceIterable;
  }

  public static CloseableIterable<Resource> recursivelyListPath(final Path basePath, final Path path)
          throws NotFoundException, GenericException {
    CloseableIterable<Resource> resourceIterable;
    try {
      final Stream<Path> walk = Files.walk(path, FileVisitOption.FOLLOW_LINKS);
      final Iterator<Path> pathIterator = walk.iterator();

      // skip root
      if (pathIterator.hasNext()) {
        pathIterator.next();
      }

      resourceIterable = new CloseableIterable<Resource>() {

        @Override
        public Iterator<Resource> iterator() {
          return new Iterator<Resource>() {

            @Override
            public boolean hasNext() {
              return pathIterator.hasNext();
            }

            @Override
            public Resource next() {
              Path next = pathIterator.next();
              Resource ret;
              try {
                ret = convertPathToResource(basePath, next);
              } catch (GenericException | NotFoundException | RequestNotValidException e) {
                LOGGER.error("Error while list path " + basePath + " while parsing resource " + next, e);
                ret = null;
              }

              return ret;
            }

          };
        }

        @Override
        public void close() {
          walk.close();
        }
      };

    } catch (NoSuchFileException e) {
      throw new NotFoundException("Could not list contents of entity because it doesn't exist: " + path, e);
    } catch (IOException e) {
      throw new GenericException("Could not list contents of entity at: " + path, e);
    }

    return resourceIterable;
  }

  /**
   * List containers
   *
   * @param basePath
   *          base path
   * @throws GenericException
   */
  public static CloseableIterable<Container> listContainers(final Path basePath) throws GenericException {
    CloseableIterable<Container> containerIterable;
    try {
      final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(basePath);
      final Iterator<Path> pathIterator = directoryStream.iterator();
      containerIterable = new CloseableIterable<Container>() {

        @Override
        public Iterator<Container> iterator() {
          return new Iterator<Container>() {

            @Override
            public boolean hasNext() {
              return pathIterator.hasNext();
            }

            @Override
            public Container next() {
              Path next = pathIterator.next();
              Container ret;
              try {
                ret = convertPathToContainer(basePath, next);
              } catch (NoSuchElementException | GenericException | RequestNotValidException e) {
                LOGGER.error("Error while listing containers, while parsing resource " + next, e);
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

    } catch (IOException e) {
      throw new GenericException("Could not list contents of entity at: " + basePath, e);
    }

    return containerIterable;
  }

  /**
   * Converts a path into a resource
   *
   * @param basePath
   *          base path
   * @param path
   *          relative path to base path
   * @throws RequestNotValidException
   * @throws NotFoundException
   * @throws GenericException
   */
  public static Resource convertPathToResource(Path basePath, Path path)
          throws RequestNotValidException, NotFoundException, GenericException {
    Resource resource;

    // TODO support binary reference

    if (!exists(path)) {
      throw new NotFoundException("Cannot find file or directory at " + path);
    }

    // storage path
    StoragePath storagePath = getStoragePath(basePath, path);

    // construct
    if (FSUtils.isDirectory(path)) {
      resource = new DefaultDirectory(storagePath);
    } else {
      ContentPayload content = null;
      long sizeInBytes;
      try {
        if (FSUtils.isManifestOfExternalFiles(path)) {
          List<String> allLines = Files.readAllLines(path);
          ShallowFiles shallowFiles = new ShallowFiles();
          for (String line : allLines) {
            shallowFiles.addObject(JsonUtils.getObjectFromJson(line, ShallowFile.class));
          }
          content = new ExternalFileManifestContentPayload(shallowFiles);
        } else {
          content = new FSPathContentPayload(path);
        }
        sizeInBytes = Files.size(path);
        Map<String, String> contentDigest = null;
        resource = new DefaultBinary(storagePath, content, sizeInBytes, false, contentDigest);
      } catch (IOException e) {
        throw new GenericException("Could not get file size", e);
      }
    }
    return resource;
  }

  public static BinaryVersion convertPathToBinaryVersion(Path historyDataPath, Path historyMetadataPath, Path path)
          throws RequestNotValidException, NotFoundException, GenericException {
    DefaultBinaryVersion ret;

    if (!FSUtils.exists(path)) {
      throw new NotFoundException("Cannot find file version at " + path);
    }

    // storage path
    Path relativePath = historyDataPath.relativize(path);
    String fileName = relativePath.getFileName().toString();
    int lastIndexOfDot = fileName.lastIndexOf(VERSION_SEP);

    if (lastIndexOfDot <= 0 || lastIndexOfDot == fileName.length() - 1) {
      throw new RequestNotValidException("Bad name for versioned file: " + path);
    }

    String id = fileName.substring(lastIndexOfDot + 1);
    String realFileName = fileName.substring(0, lastIndexOfDot);
    Path realFilePath = relativePath.getParent().resolve(realFileName);
    Path metadataPath = historyMetadataPath.resolve(relativePath.getParent().resolve(fileName + METADATA_SUFFIX));

    StoragePath storagePath = ScatteredFSUtils.getStoragePath(realFilePath);

    // construct
    ContentPayload content = new FSPathContentPayload(path);
    long sizeInBytes;
    try {
      sizeInBytes = Files.size(path);
      Map<String, String> contentDigest = null;
      Binary binary = new DefaultBinary(storagePath, content, sizeInBytes, false, contentDigest);

      if (FSUtils.exists(metadataPath)) {
        ret = JsonUtils.readObjectFromFile(metadataPath, DefaultBinaryVersion.class);
        ret.setBinary(binary);
      } else {
        Date createdDate = new Date(Files.readAttributes(path, BasicFileAttributes.class).creationTime().toMillis());
        Map<String, String> defaultProperties = new HashMap<>();
        ret = new DefaultBinaryVersion(binary, id, createdDate, defaultProperties);
      }

    } catch (IOException e) {
      throw new GenericException("Could not get file size", e);
    }

    return ret;
  }

  /**
   * Converts a path into a container
   *
   * @param basePath
   *          base path
   * @param path
   *          relative path to base path
   * @throws GenericException
   * @throws RequestNotValidException
   */
  public static Container convertPathToContainer(Path basePath, Path path)
          throws GenericException, RequestNotValidException {
    Container resource;

    // storage path
    StoragePath storagePath = ScatteredFSUtils.getStoragePath(basePath, path);

    // construct
    if (FSUtils.isDirectory(path)) {
      resource = new DefaultContainer(storagePath);
    } else {
      throw new GenericException("A file is not a container!");
    }
    return resource;
  }

  public static Path createRandomDirectory(Path parent) throws IOException {
    String containerName = parent.getFileName().toString();

    if(containerNameScatteredFSConfigMap.containsKey(containerName)) {
      String id = IdUtils.createUUID();
      Path scatteredPath = containerNameScatteredFSConfigMap.get(containerName).getScatteredPath(id);

      return Files.createDirectories(parent.resolve(scatteredPath));
    }

    return FSUtils.createRandomDirectory(parent);
  }

  public static CloseableIterable<BinaryVersion> listBinaryVersions(final Path historyDataPath,
                                                                    final Path historyMetadataPath, final StoragePath storagePath) throws GenericException, NotFoundException {
    Path fauxPath = getEntityPath(historyDataPath, storagePath);
    final Path parent = fauxPath.getParent();
    final String baseName = fauxPath.getFileName().toString();

    CloseableIterable<BinaryVersion> iterable;

    try {
      final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parent,
              new DirectoryStream.Filter<Path>() {

                @Override
                public boolean accept(Path entry) {
                  String fileName = entry.getFileName().toString();
                  int lastIndexOfDot = fileName.lastIndexOf(VERSION_SEP);

                  return lastIndexOfDot > 0 ? fileName.substring(0, lastIndexOfDot).equals(baseName) : false;
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
              try {
                return convertPathToBinaryVersion(historyDataPath, historyMetadataPath, next);
              } catch (GenericException | NotFoundException | RequestNotValidException e) {
                LOGGER.error("Error while list path " + parent + " while parsing resource " + next, e);
                return null;
              }
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

  public static CloseableIterable<Resource> listResourcesUnderContainer(final Path storagePath, String containerName)
          throws NotFoundException, GenericException {
    CloseableIterable<Resource> resourceIterable;

    Path containerPath = storagePath.resolve(containerName);

    if(containerNameScatteredFSConfigMap.containsKey(containerName)) {
      ScatteredFSConfig scatteredFSConfig = containerNameScatteredFSConfigMap.get(containerName);

      try {
        final Stream<Path> directoryStream =
                Files.walk(
                        containerPath,
                        scatteredFSConfig.getScatteredFolderDepth() + 1,
                         FileVisitOption.FOLLOW_LINKS
                        )
                        .filter(p -> {
                          Path relativePath = containerPath.relativize(p);
                          int folderDepth = relativePath.getNameCount();

                          if (scatteredFSConfig.getScatteredFolderDepth() + 1 != folderDepth) {
                            return false;
                          }

                          String id = p.getFileName().toString();
                          Path scatteredPath = scatteredFSConfig.getScatteredPath(id);

                          if (!relativePath.equals(scatteredPath)) {
                            return false;
                          }

                          if (!scatteredFSConfig.isValidName(relativePath)) {
                            return false;
                          }

                          return Files.isDirectory(p) == scatteredFSConfig.isDirectory();
                        });

        final Iterator<Path> pathIterator = directoryStream.iterator();

        resourceIterable = new CloseableIterable<Resource>() {

          @Override
          public Iterator<Resource> iterator() {
            return new Iterator<Resource>() {

              @Override
              public boolean hasNext() {
                return pathIterator.hasNext();
              }

              @Override
              public Resource next() {
                Path next = pathIterator.next();
                try {
                  return convertPathToResource(storagePath, next);
                } catch (GenericException | NotFoundException | RequestNotValidException e) {
                  LOGGER.error("Error while list path " + storagePath + " while parsing resource " + next, e);
                  return null;
                }
              }

            };
          }

          @Override
          public void close() throws IOException {
            directoryStream.close();
          }
        };


      } catch (NoSuchFileException e) {
        throw new NotFoundException("Could not list contents of entity because it doesn't exist: " + containerPath, e);
      } catch (IOException e) {
        throw new GenericException("Could not list contents of entity at: " + containerPath, e);
      }
    } else {
      resourceIterable = FSUtils.listPath(storagePath, containerPath);
    }

    return resourceIterable;
  }
}