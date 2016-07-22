package com.carbonplayer.model.entity;

import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Album object
 */
public class Album extends RealmObject {
    public static final String ID = "id";
    public static final String TITLE = "title";

    @PrimaryKey private String id;
    private Date recentTimestamp;
    private String title;
    private String artist;
    private String composer;
    private Integer year;
    private String genre;
    private String albumArtURL;
    private RealmList<RealmString> artistId;
    private RealmList<RealmString> songIds;

    public Album(){}

    public Album(String id, Date recentTimestamp, String title, String artist,
                 String composer, Integer year, String genre, String albumArtURL,
                 RealmList<RealmString> artistId, RealmList<RealmString> songIds) {
        this.id = id;
        this.recentTimestamp = recentTimestamp;
        this.title = title;
        this.artist = artist;
        this.composer = composer;
        this.year = year;
        this.genre = genre;
        this.albumArtURL = albumArtURL;
        this.artistId = artistId;
        this.songIds = songIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getRecentTimestamp() {
        return recentTimestamp;
    }

    public void setRecentTimestamp(Date recentTimestamp) {
        this.recentTimestamp = recentTimestamp;
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

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getAlbumArtURL() {
        return albumArtURL;
    }

    public void setAlbumArtURL(String albumArtURL) {
        this.albumArtURL = albumArtURL;
    }

    public RealmList<RealmString> getArtistId() {
        return artistId;
    }

    public void setArtistId(RealmList<RealmString> artistId) {
        this.artistId = artistId;
    }

    public RealmList<RealmString> getSongIds() {
        return songIds;
    }

    public void setSongIds(RealmList<RealmString> songIds) {
        this.songIds = songIds;
    }

    public void addSongId(String songId){
        this.songIds.add(new RealmString(songId));
    }
}
