package com.fintech.reconcile.service;

import java.io.File;
import java.nio.file.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileLifecycleService {
  private static final Logger log = LoggerFactory.getLogger(FileLifecycleService.class);

  @Value("${app.reconciliation.incoming-dir}")
  private String incomingDir;

  @Value("${app.reconciliation.archive-dir}")
  private String archiveDir;

  public Optional<File> getNextAvailableFile() {
    File dir = new File(incomingDir);
    if (!dir.exists()) {
      dir.mkdirs();
      return Optional.empty();
    }

    File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv"));
    if (files == null || files.length == 0) {
      return Optional.empty();
    }

    return Optional.of(files[0]);
  }

  public void archiveFile(File file) {
    try {
      String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      Path targetDirPath = Paths.get(archiveDir, today);
      if (!Files.exists(targetDirPath)) {
        Files.createDirectories(targetDirPath);
      }

      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
      String archivedFileName = file.getName().replace(".csv", "_" + timestamp + ".csv");
      Path targetFilePath = targetDirPath.resolve(archivedFileName);

      Files.move(file.toPath(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
      log.info("File successfully archived: {} -> {}", file.getName(), targetFilePath);
    } catch (IOException e) {
      log.error("Failed to archive file: {}", file.getAbsolutePath());
    }
  }
}
