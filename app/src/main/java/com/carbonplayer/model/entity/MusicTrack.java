package com.carbonplayer.model.entity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Music track object
 */
@SuppressWarnings("unused")
public class MusicTrack extends RealmObject {

    public static final String ID = "id";

    @PrimaryKey private String id;
    private String clientId;
    private Date recentTimestamp;
    private boolean deleted; //?
    private String title;
    private String artist;
    private String composer;
    private String album;
    private Integer year;
    private String comment;
    private Integer trackNumber;
    private String genre;
    private int durationMillis;
    private Integer beatsPerMinute;
    private String albumArtURL;
    private int playCount;
    private String rating;
    private int estimatedSize;
    private String albumId;
    private RealmList<RealmString> artistId;
    private String nid;

    public MusicTrack(){}

    public MusicTrack(String id, Date recentTimestamp, boolean deleted, String title, String artist, String composer,
                      String album, Integer year, String comment, Integer trackNumber, String genre, int durationMillis,
                      Integer beatsPerMinute, String albumArtURL, int playCount, String rating, int estimatedSize,
                      String albumId, RealmList<RealmString> artistId, String clientId, String nid) {
        this.id = id;
        this.recentTimestamp = recentTimestamp;
        this.clientId = clientId;
        this.deleted = deleted;
        this.title = title;
        this.artist = artist;
        this.composer = composer;
        this.album = album;
        this.year = year;
        this.comment = comment;
        this.trackNumber = trackNumber;
        this.genre = genre;
        this.durationMillis = durationMillis;
        this.beatsPerMinute = beatsPerMinute;
        this.albumArtURL = albumArtURL;
        this.playCount = playCount;
        this.rating = rating;
        this.estimatedSize = estimatedSize;
        this.albumId = albumId;
        this.artistId = artistId;
        this.nid = nid;
    }


    public MusicTrack(JSONObject trackJson) throws JSONException{
        if(!trackJson.getString("kind").equals("sj#track")) return;

        id = trackJson.getString("id");
        if(trackJson.has("clientId"))
            clientId = trackJson.getString("clientId");
        recentTimestamp = new Date(Long.parseLong(trackJson.getString("recentTimestamp").substring(0,10)));
        deleted = trackJson.getBoolean("deleted");
        title = trackJson.getString("title");
        artist = trackJson.getString("artist");
        if(trackJson.has("composer"))
            composer = trackJson.getString("composer");
        album = trackJson.getString("album");
        if(trackJson.has("year"))
            year = trackJson.getInt("year");
        if(trackJson.has("comment"))
            comment = trackJson.getString("comment");
        trackNumber = trackJson.getInt("trackNumber");
        if(trackJson.has("genre"))
            genre = trackJson.getString("genre");
        durationMillis = Integer.parseInt(trackJson.getString("durationMillis"));
        if(trackJson.has("beatsPerMinute"))
            beatsPerMinute = trackJson.getInt("beatsPerMinute");
        if(trackJson.has("albumArtRef"))
            albumArtURL = trackJson.getJSONArray("albumArtRef").getJSONObject(0).getString("url");
        if(trackJson.has("playCount"))
            playCount = trackJson.getInt("playCount");
        if(trackJson.has("rating"))
            rating = trackJson.getString("rating");
        estimatedSize = Integer.parseInt(trackJson.getString("estimatedSize"));
        if(trackJson.has("albumId"))
            albumId = trackJson.getString("albumId");
        if(trackJson.has("artistID")) {
            JSONArray artist_ids = trackJson.getJSONArray("artistId");
            artistId = new RealmList<>();
            for (int i = 0; i < artist_ids.length(); i++) artistId.add(new RealmString(artist_ids.getString(i)));
        }
        if(trackJson.has("nid")) nid = trackJson.getString("nid");
    }

    public String getTrackId() {
        return id;
    }

    public void setTrackId(String id) {
        this.id = id;
    }

    public String getClientId() {return clientId;}

    public void setClientId(String clientId) {this.clientId = clientId;}

    public Date getRecentTimestamp() {
        return recentTimestamp;
    }

    public void setRecentTimestamp(Date recentTimestamp) {
        this.recentTimestamp = recentTimestamp;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
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

    public Integer getBeatsPerMinute() {
        return beatsPerMinute;
    }

    public void setBeatsPerMinute(Integer beatsPerMinute) {
        this.beatsPerMinute = beatsPerMinute;
    }

    public String getAlbumArtURL() {
        return albumArtURL;
    }

    public void setAlbumArtURL(String albumArtURL) {
        this.albumArtURL = albumArtURL;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
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

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public RealmList<RealmString> getArtistId() {
        return artistId;
    }

    public void setArtistId(RealmList<RealmString> artistId) {
        this.artistId = artistId;
    }

    public String getNid() {return nid;}

    public void setNid(String nid) { this.nid = nid; }

    public String getMostUsefulID(){
        if(nid != null) return nid;
        if(clientId != null) return clientId;
        return id;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("MusicTrack: {");
        sb.append("id: "); sb.append(id);
        sb.append(", title: "); sb.append(title);
        sb.append(", album: "); sb.append(album);
        sb.append(", trackNumber: "); sb.append(trackNumber);
        sb.append(", album id: "); sb.append(albumId);
        sb.append("}");

        return sb.toString();
    }

}
