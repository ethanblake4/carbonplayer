package com.carbonplayer.model.entity;


import android.support.v4.media.MediaMetadataCompat;

import org.parceler.Parcel;

import java.util.Objects;

/**
 * Simplified Track object for passing to/from background service
 */
@SuppressWarnings("unused")
@Parcel
public class ParcelableTrack {
    long localId;
    int position;
    String id;
    String clientId;
    String nid;
    String storeId;
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

    ParcelableTrack() {
    }

    public ParcelableTrack(int position, String id, String title,
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

    public ParcelableTrack(Track source) {
        this.localId = source.getLocalId();
        this.id = source.getId();
        this.clientId = source.getClientId();
        this.nid = source.getNid();
        this.storeId = source.getStoreId();
        this.title = source.getTitle();
        this.artist = source.getArtist();
        this.album = Objects.requireNonNull(Objects.requireNonNull(source.getAlbums()).first()).getTitle();
        this.year = orZero(source.getYear());
        this.trackNumber = orZero(source.getTrackNumber());
        this.genre = source.getGenre();
        this.durationMillis = source.getDurationMillis();
        this.albumArtURL = Objects.requireNonNull(source.getAlbums().first()).getAlbumArtRef();
        this.rating = source.getRating();
        Integer estSize = source.getEstimatedSize();
        this.estimatedSize = estSize == null ? 0 : estSize;
    }

    @Override
    public String toString() {
        return "ParcelableTrack(id: " + this.id + ", title: " + this.title + ")";
    }

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getNid() {
        return nid;
    }

    public void setNid(String nid) {
        this.nid = nid;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
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

    public MediaMetadataCompat getMediaMetadata() {
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, getId())
            /*.putString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE, getUrl(context, realm).toString())*/
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getArtist())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDurationMillis())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, getAlbumArtURL())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getTitle())
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, getTrackNumber())
             /*   .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, allTracks.size())*/
                .build();
    }

    private int orZero(Integer in) {
        if (in == null) return 0;
        return in;
    }

}