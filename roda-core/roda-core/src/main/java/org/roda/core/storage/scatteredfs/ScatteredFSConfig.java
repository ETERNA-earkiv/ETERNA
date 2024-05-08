package org.roda.core.storage.scatteredfs;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.StoragePath;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public interface ScatteredFSConfig {
  enum ScatterMethod {
    RANGE("range"),
    REGEX("regex");

    private final String scatterMethod;

    ScatterMethod(String scatterMethod) {
      this.scatterMethod = scatterMethod;
    }

    public String toString() {
      return this.scatterMethod;
    }
  }

  static ScatteredFSConfig getScatteredFSConfigFromRodaConfiguration(String fileSystemName) throws GenericException {
    String scatterMethod = RodaCoreFactory.getRodaConfigurationAsString("core.storage.filesystem", fileSystemName,
            "scatter_method");

    String regex = RodaCoreFactory.getRodaConfigurationAsString("core.storage.filesystem", fileSystemName,
            "regex");

    String type = RodaCoreFactory.getRodaConfigurationAsString("core.storage.filesystem", fileSystemName,
            "type");

    String rule = RodaCoreFactory.getRodaConfigurationAsString("core.storage.filesystem", fileSystemName,
            "rule");

    if(scatterMethod == null || scatterMethod.isEmpty() || scatterMethod.equals("ranges")) {
      return new ScatteredFSRangeConfig(fileSystemName);
    } else if(scatterMethod.equals("regex")) {
      return new ScatteredFSRegexConfig(fileSystemName);
    } else {
      throw new GenericException("Encountered unknown scatter method.");
    }
  }

  public StoragePath getStoragePath(Path relativePath) throws RequestNotValidException;

  public Path getScatteredPath(String id);

  public boolean isValidName(Path path);

  public boolean isDirectory();

  public int getScatteredFolderDepth();
}
