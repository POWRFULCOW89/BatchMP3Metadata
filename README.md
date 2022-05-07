# BatchMP3Metadata
Batch metadata edit of music files

## Usage

```bash
batchmp3metadata directory [option...]
```
A directory structure containing a single album and a cover image is assumed. Each song will be assigned a new [ID3v2](https://es.wikipedia.org/wiki/ID3) tag, 
if not present, and assigned:
- The file name as track name
- The directory name as album name
- A single PNG or JPEG as album cover in the directory.

## Examples

- Batch edit the current directory
  ```bash
  batchmp3metadata .
  ```
- Batch edit a relative directory and assign an artist
  ```bash
  batchmp3metadata ../Album -artist Artist
  ```
  
- Batch edit an absolute path and assign several genres:
  ```bash
  batchmp3metadata "C:\Users\user\Music\My Album" -genre "Hip Hop"
  ```
