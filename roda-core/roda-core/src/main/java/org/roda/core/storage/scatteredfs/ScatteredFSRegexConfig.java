package org.roda.core.storage.scatteredfs;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.storage.DefaultStoragePath;
import org.roda.core.storage.fs.FSUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ScatteredFSRegexConfig implements ScatteredFSConfig {
  private Pattern pattern;

  private boolean isDirectory;

  private String replacement;

  private int folderDepth;

  public ScatteredFSRegexConfig(String fileSystemName) throws GenericException {
    String regex = RodaCoreFactory.getRodaConfigurationAsString("core.storage.filesystem", fileSystemName,
            "regex");

    String type = RodaCoreFactory.getRodaConfigurationAsString("core.storage.filesystem", fileSystemName,
            "type");

    String rule = RodaCoreFactory.getRodaConfigurationAsString("core.storage.filesystem", fileSystemName,
            "rule");

    if (regex.isEmpty()) {
      throw new GenericException("Error! Missing regex.");
    }

    if (rule.isEmpty()) {
      throw new GenericException("Error! Missing rule.");
    }

    this.pattern = Pattern.compile(regex);

    this.folderDepth = Paths.get(rule).getNameCount();

    this.isDirectory = type.equals("directory");

    this.replacement = rule;
  }

  public StoragePath getStoragePath(Path relativePath) throws RequestNotValidException {
    ArrayList<String> pathPartials = new ArrayList<>();

    int i;
    for (i = 0; i < relativePath.getNameCount(); i++) {
      String pathPartial = relativePath.getName(i).toString();
      pathPartials.add(FSUtils.decodePathPartial(pathPartial));
    }

    if(i > 1) {
      int numPathPartialsToRemove = Math.min(pathPartials.size(), this.folderDepth);
      pathPartials.subList(1, numPathPartialsToRemove + 1).clear();
    }

    return DefaultStoragePath.parse(pathPartials);
  }

  public Path getScatteredPath(String id) {
    Matcher matcher = this.pattern.matcher(id);

    Path scatteredPath = Paths.get(matcher.replaceAll(this.replacement));
    scatteredPath = scatteredPath.resolve(id);

    return scatteredPath;
  }

  public boolean isValidName(Path path) {
    Matcher matcher = this.pattern.matcher(path.getFileName().toString());
    return matcher.matches();
  }

  public boolean isDirectory() {
    return this.isDirectory;
  }

  public int getScatteredFolderDepth() {
    return this.folderDepth;
  }
}