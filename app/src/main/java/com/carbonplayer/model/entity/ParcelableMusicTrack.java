package com.carbonplayer.model.entity;


import org.parceler.Parcel;

/**
 * Simplified MusicTrack object for passing to/from background service
 */
@SuppressWarnings("unused")
@Parcel
public class ParcelableMusicTrack{
    int position;
    String id;
    String title;
    String artist;
    String album;
    int year;
    int trackNumber;
    String genre;
    int durationMillis;
    String albumArtURL;
    String rating;
    int estimatedSize;

    ParcelableMusicTrack() {}

    public ParcelableMusicTrack(int position, String id, String title,
                                String artist, String album, int year,
                                int trackNumber, String genre,
                                int durationMillis, String albumArtURL,
                                String rating, int estimatedSize) {
        this.position = position;
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.year = year;
        this.trackNumber = trackNumber;
        this.genre = genre;
        this.durationMillis = durationMillis;
        this.albumArtURL = albumArtURL;
        this.rating = rating;
        this.estimatedSize = estimatedSize;
    }

    public ParcelableMusicTrack(MusicTrack source){
        this.id = source.getTrackId();
        this.title = source.getTitle();
        this.artist = source.getArtist();
        this.album = source.getAlbum();
        this.year = source.getYear();
        this.trackNumber = source.getTrackNumber();
        this.genre = source.getGenre();
        this.durationMillis = source.getDurationMillis();
        this.albumArtURL = source.getAlbumArtURL();
        this.rating = source.getRating();
        this.estimatedSize = source.getEstimatedSize();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(int durationMillis) {
        this.durationMillis = durationMillis;
    }

    public String getAlbumArtURL() {
        return albumArtURL;
    }

    public void setAlbumArtURL(String albumArtURL) {
        this.albumArtURL = albumArtURL;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public int getEstimatedSize() {
        return estimatedSize;
    }

    public void setEstimatedSize(int estimatedSize) {
        this.estimatedSize = estimatedSize;
    }

}
