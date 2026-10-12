/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package code.name.monkey.retromusic.glide.audiocover;

import android.os.Environment;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AudioFileCoverUtils {

  public static final String[] FALLBACKS = {
    "cover.jpg", "album.jpg", "folder.jpg",
    "cover.png", "album.png", "folder.png",
    "cover.webp", "album.webp", "folder.webp",
    "Cover.jpg", "Album.jpg", "Folder.jpg",
    "Cover.png", "Album.png", "Folder.png"
  };

  private static final Set<String> GENERIC_DIR_NAMES = new HashSet<>(Arrays.asList(
      "music", "download", "downloads", "audio", "media", "bluetooth",
      "podcasts", "ringtones", "notifications", "alarms", "documents",
      "0", "sdcard", "storage", "emulated", "unknown", "unsorted", "untagged",
      "misc", "miscellaneous", "telegram", "whatsapp audio", "internal storage"
  ));

  public static boolean isGenericDirectory(File dir) {
    if (dir == null) {
      return true;
    }
    String name = dir.getName().toLowerCase(Locale.ROOT);
    if (name.isEmpty() || isStorageRoot(dir)) {
      return true;
    }
    if (GENERIC_DIR_NAMES.contains(name)) {
      File parent = dir.getParentFile();
      if (parent == null || isStorageRoot(parent)) {
        return true;
      }
      if (name.contains("download") || name.contains("bluetooth") || name.contains("telegram") || name.contains("whatsapp")) {
        return true;
      }
    }
    return false;
  }

  private static boolean isStorageRoot(File file) {
    if (file == null) {
      return true;
    }
    try {
      File extDir = Environment.getExternalStorageDirectory();
      if (extDir != null && (file.equals(extDir) || file.equals(extDir.getParentFile()))) {
        return true;
      }
    } catch (Exception ignored) {
    }
    String name = file.getName().toLowerCase(Locale.ROOT);
    if (name.equals("0") || name.equals("sdcard") || name.equals("emulated") || name.equals("storage") || name.equals("mnt")) {
      return true;
    }
    File p = file.getParentFile();
    if (p != null) {
      String pName = p.getName().toLowerCase(Locale.ROOT);
      if (pName.equals("storage") || pName.equals("mnt")) {
        return true;
      }
    }
    return false;
  }

  public static InputStream fallback(String path) throws FileNotFoundException {
    // Method 1: use embedded high resolution album art if there is any
    try {
      MP3File mp3File = new MP3File(path);
      if (mp3File.hasID3v2Tag()) {
        Artwork art = mp3File.getTag().getFirstArtwork();
        if (art != null) {
          byte[] imageData = art.getBinaryData();
          return new ByteArrayInputStream(imageData);
        }
      }
      // If there are any exceptions, we ignore them and continue to the other fallback method
    } catch (ReadOnlyFileException | InvalidAudioFrameException | TagException | IOException | CannotReadException ignored) {
    }

    // Method 2: look for album art in external files
    final File parent = new File(path).getParentFile();
    if (parent != null && !isGenericDirectory(parent)) {
      for (String fallback : FALLBACKS) {
        File cover = new File(parent, fallback);
        if (cover.exists()) {
          return new FileInputStream(cover);
        }
      }
    }
    return null;
  }
}
