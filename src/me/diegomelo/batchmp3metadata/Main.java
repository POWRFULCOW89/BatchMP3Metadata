package me.diegomelo.batchmp3metadata;

import com.mpatric.mp3agic.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

public class Main {

    /**
     * Tool entry point.
     * <p>
     * Usage:
     * <p>
     * $ id3batcheditor [directory]
     * <p>
     * A structure where each directory contains a single album and a cover image is assumed.
     * Track files with no ID3 metadata will be assigned a default one:
     * <p>
     * - The file name as track name
     * - The directory name as album name
     * - A maximum number of one file in the working directory as album cover.
     * <p>
     * Note that numbering isn't added, as there's no reliable way of establishing the intended order.
     */
    public static void main(String[] args) throws InvalidDataException, UnsupportedTagException, IOException, NotSupportedException {

        String dirPath = args[0]; // Directory is a positional parameter

        final HashMap<String, List<String>> params = new HashMap<>();
        LinkedList<String> options = null;

        // Starting at index 1, since the first argument is the directory path.
        for (int i = 1; i < args.length; i++) {
            final String a = args[i];

            // Parsing arguments
            if (a.charAt(0) == '-') {
                if (a.length() < 2) {
                    System.err.println("Error at argument " + a);
                    return;
                }

                options = new LinkedList<>();
                params.put(a.substring(1), options);
            } else if (options != null) {
                // If no tokens found, treat space separated string as a single array parameter
                options.add(a);
            } else {
                System.err.println("Illegal parameter usage");
                return;
            }
        }

        standardFileEdit(dirPath, params);

        /*
         * TODO: 1. Get all directories in the current working directory
         * 2. For each directory, get all files
         * 3. Get the album name
         * 4. Make dummy copies
         * 5. Edit each file metadata
         * 6. Delete dummy copies
         * */
    }

    /**
     * Gets all directories in the current working directory.
     * <p>
     * For future use.
     */
    private static File[] getAllDirectoriesInCWD() {
        return new File(".").listFiles(File::isDirectory);
    }

    /**
     * Retrieves all valid files in the given directory.
     */
    private static File[] getAllFilesInDirectory(String dir) {
        return new File(dir).listFiles();
    }

    /**
     * Delete all files in the current directory that contain "_TEMP_".
     */
    private static void deleteTempFiles(String dirPath) {
        File[] files = getAllFilesInDirectory(dirPath);

        for (File file : files) {
            if (file.getName().endsWith(".mp3") && file.getName().contains("_TEMP_")) {
                System.out.println("Deleting file: " + file.getName());
                if (!file.delete()) throw new RuntimeException("Could not delete file: " + file.getName());
            }
        }
    }

    /**
     * Edits the id3v2 tags of all current files in the given directory, adding track name, album, and cover.
     *
     * @param dirPath The directory to edit.
     */
    private static void standardFileEdit(String dirPath, HashMap<String, List<String>> params) throws InvalidDataException, UnsupportedTagException, IOException, NotSupportedException {
        File[] files = getAllFilesInDirectory(dirPath);

        if (files.length == 0) return;
        String cwd = System.getProperty("user.dir");
        String albumName;

        if (dirPath.equals(".")) {
            albumName = cwd.split("\\\\")[cwd.split("\\\\").length - 1];
        } else {
            albumName = files[0].getParentFile().getName();
        }

        System.out.println("Album name: " + albumName);
        String albumCover = getCoverFile(dirPath);
        System.out.println("Album cover: " + albumCover);

        for (File file : files) {
            if (file.getName().endsWith(".mp3") && !file.getName().contains("_TEMP_")) {
                String trackName = file.getName().split("\\.")[0];
                Mp3File f = getMP3File(getTempFileName(file.getAbsolutePath()));

                System.out.println("Editing file: " + file.getName());
                editMP3FileField(f, "album", albumName);
                editMP3FileField(f, "title", trackName);
                editFileAlbumCover(f, albumCover);

                for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                    String option = entry.getKey();
                    List<String> value = entry.getValue();

                    List<String> validOptions = List.of("track",
                    "artist",
                    "title",
                    "album",
                    "year",
                    "genre",
                    "comment",
                    "lyrics" ,
                    "composer",
                    "publisher",
                    "originalArtist",
                    "albumArtist",
                    "copyright",
                    "url",
                    "encoder");

                    if (validOptions.contains(option)){
                        System.out.println("Overriding " + option + ": " + value.toString());

                        String delimiter = "";
                        if (value.size() > 1) delimiter = ", ";

                        editMP3FileField(f, option, String.join(delimiter, value));
                    } else {
                        System.out.println("Invalid option: " + option);
                    }

                    editMP3FileField(f, "album", albumName);
                }
            }
        }

        deleteTempFiles(dirPath);
    }

    /**
     * Creates a temporary copy of the file.
     * <p>
     * Since mp3agic doesn't support file overwriting, we need to create a temporary file and then delete it.
     */
    private static String getTempFileName(String filePath) {
        String tempName = Instant.now().toEpochMilli() + ".mp3";
        File f = new File(filePath);
        String newName = f.getParent() + "/" + f.getName() + "_TEMP_" + tempName;

        if (f.renameTo(new File(newName))) return newName;
        return null;
    }

    /**
     * Gets the MP3 file from the given file path as {@Mp3File}.
     */
    private static Mp3File getMP3File(String filePath) throws InvalidDataException, UnsupportedTagException, IOException {
        return new Mp3File(filePath);
    }


    /**
     * Prints the ID3v2 tag of the file.
     * <p>
     * Supported fields: track, artist, title, album, year, genre, comment, lyrics, original artist,
     * album artist, composer, copyright, publisher, encoder, album cover data.
     */
    private static void getMP3FileInfo(String filePath) throws InvalidDataException, UnsupportedTagException, IOException {
        Mp3File mp3file = new Mp3File(filePath);
        if (mp3file.hasId3v2Tag()) {
            ID3v2 id3v2Tag = mp3file.getId3v2Tag();
            System.out.println("Track: " + id3v2Tag.getTrack());
            System.out.println("Artist: " + id3v2Tag.getArtist());
            System.out.println("Title: " + id3v2Tag.getTitle());
            System.out.println("Album: " + id3v2Tag.getAlbum());
            System.out.println("Year: " + id3v2Tag.getYear());
            System.out.println("Genre: " + id3v2Tag.getGenre() + " (" + id3v2Tag.getGenreDescription() + ")");
            System.out.println("Comment: " + id3v2Tag.getComment());
            System.out.println("Lyrics: " + id3v2Tag.getLyrics());
            System.out.println("Composer: " + id3v2Tag.getComposer());
            System.out.println("Publisher: " + id3v2Tag.getPublisher());
            System.out.println("Original artist: " + id3v2Tag.getOriginalArtist());
            System.out.println("Album artist: " + id3v2Tag.getAlbumArtist());
            System.out.println("Copyright: " + id3v2Tag.getCopyright());
            System.out.println("URL: " + id3v2Tag.getUrl());
            System.out.println("Encoder: " + id3v2Tag.getEncoder());
            byte[] albumImageData = id3v2Tag.getAlbumImage();
            if (albumImageData != null) {
                System.out.println("Have album image data, length: " + albumImageData.length + " bytes");
                System.out.println("Album image mime type: " + id3v2Tag.getAlbumImageMimeType());
            }
        }
    }


    /**
     * Sets the value of a single field in the ID3v2 tag of the MP3 file.
     *
     * @param mp3File The MP3 file to edit.
     * @param field   The name of the field to edit.
     * @param value   The value to set the field to.
     */
    private static void editMP3FileField(Mp3File mp3File, String field, String value) throws IOException, NotSupportedException {

        // Before manipulating the file, we check or add a ID3v2 tag
        ID3v2 id3v2Tag;
        if (mp3File.hasId3v2Tag()) {
            id3v2Tag = mp3File.getId3v2Tag();
        } else {
            id3v2Tag = new ID3v24Tag();
            mp3File.setId3v2Tag(id3v2Tag);
        }

        switch (field) {
            case "track" -> id3v2Tag.setTrack(String.valueOf(Integer.parseInt(value)));
            case "artist" -> id3v2Tag.setArtist(value);
            case "title" -> id3v2Tag.setTitle(value);
            case "album" -> id3v2Tag.setAlbum(value);
            case "year" -> id3v2Tag.setYear(value);
            case "genre" -> id3v2Tag.setGenre(Integer.parseInt(value));
            case "comment" -> id3v2Tag.setComment(value);
            case "lyrics" -> id3v2Tag.setLyrics(value);
            case "composer" -> id3v2Tag.setComposer(value);
            case "publisher" -> id3v2Tag.setPublisher(value);
            case "originalArtist" -> id3v2Tag.setOriginalArtist(value);
            case "albumArtist" -> id3v2Tag.setAlbumArtist(value);
            case "copyright" -> id3v2Tag.setCopyright(value);
            case "url" -> id3v2Tag.setUrl(value);
            case "encoder" -> id3v2Tag.setEncoder(value);
        }

        saveWithOriginalFilename(mp3File);
    }

    /**
     * Saves the file with the original filename.
     * <p>
     * The mp3agic library does not allow overwriting files, so we manually make copies
     * and strip a temporary suffix from the current filename.
     *
     * @throws NotSupportedException If the file cannot be saved.
     */

    private static void saveWithOriginalFilename(Mp3File mp3File) throws IOException, NotSupportedException {
        String filename = mp3File.getFilename();
        filename = filename.split("_TEMP_")[0];

        mp3File.save(filename);
    }

    /**
     * Changes the album image of the MP3 file.
     *
     * @param mp3File  The MP3 file to edit.
     * @param filePath The image to set as the album image.
     * @throws NotSupportedException If the file is not valid.
     */
    private static void editFileAlbumCover(Mp3File mp3File, String filePath) throws IOException, NotSupportedException {

        if (mp3File.hasId3v2Tag()) {
            ID3v2 id3v2Tag = mp3File.getId3v2Tag();

            if (filePath != null) {
                byte[] albumImageData = Files.readAllBytes(Paths.get(filePath));
                String mimeType = Files.probeContentType(Paths.get(filePath));
                id3v2Tag.setAlbumImage(albumImageData, mimeType);
            }
        }

        saveWithOriginalFilename(mp3File);
    }

    /**
     * Returns the first cover album in the directory, if any.
     *
     * @param dirPath The directory to search for a cover album.
     * @return The path to the cover album, or null if none found.
     */
    private static String getCoverFile(String dirPath) {
        File[] files = getAllFilesInDirectory(dirPath);

        for (File file : files) {
            if (file.getName().endsWith(".jpg") || file.getName().endsWith(".png")) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }
}
