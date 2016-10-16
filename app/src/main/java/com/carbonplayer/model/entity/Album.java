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
    public static final String ARTIST = "artist";

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

    @SuppressWarnings("unused")
    public Album(String id, Date recentTimestamp, String title, String artist,
                 String composer, Integer year, String genre, String albumArtURL,
                 RealmList<RealmString> artistId, RealmList<RealmString> songIds) {
        this.id = id;
        this.recentTimestamp = recentTimestamp;
        this.title = title;
        if(this.title.equals("")) this.title = "Unknown album";
        this.artist = artist;
        this.composer = composer;
        this.year = year;
        this.genre = genre;
        this.albumArtURL = albumArtURL;
        this.artistId = artistId;
        this.songIds = songIds;
    }

    public Album(MusicTrack track){
        this.id = track.getAlbumId();
        this.recentTimestamp = track.getRecentTimestamp();
        this.title = track.getAlbum();
        if(this.title.equals("")) this.title = "Unknown album";
        this.artist = track.getArtist();
        this.composer = track.getComposer();
        this.year = track.getYear();
        this.genre = track.getGenre();
        this.albumArtURL = track.getAlbumArtURL();
        this.artistId = track.getArtistId();
        this.songIds = new RealmList<>( new RealmString(track.getTrackId()) );
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
        if(this.title.equals("")) this.title = "Unknown album";
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
