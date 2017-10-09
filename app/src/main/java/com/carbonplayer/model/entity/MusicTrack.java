package com.carbonplayer.model.entity;

import com.carbonplayer.model.entity.enums.PlaySource;
import com.carbonplayer.model.entity.enums.StorageType;
import com.carbonplayer.model.entity.enums.StreamQuality;
import com.carbonplayer.model.entity.primitive.RealmLong;

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
    public static final String HAS_CACHED_FILE = "hasCachedFile";
    public static final String CACHED_FILE_QUALITY = "cachedFileQuality";
    public static final String STORAGE_TYPE = "storageType";

    @PrimaryKey
    private String id;
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
    private String artistArtURL;
    private int playCount;
    private String rating;
    private int estimatedSize;
    private String albumId;
    private RealmList<RealmString> artistId;
    private String nid;
    private RealmList<RealmLong> localPlays;
    private long localTrackSizeBytes;
    private boolean hasCachedFile;
    private int cachedFileQuality;
    private int storageType;

    public MusicTrack() {
    }

    public MusicTrack(String id, Date recentTimestamp, boolean deleted, String title, String artist, String composer,
                      String album, Integer year, String comment, Integer trackNumber, String genre, int durationMillis,
                      Integer beatsPerMinute, String albumArtURL, String artistArtURL, int playCount, String rating, int estimatedSize,
                      String albumId, RealmList<RealmString> artistId, String clientId, String nid, long localTrackSizeBytes,
                      boolean hasCachedFile, StreamQuality cachedFileQuality, StorageType storageType) {
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
        this.artistArtURL = artistArtURL;
        this.playCount = playCount;
        this.rating = rating;
        this.estimatedSize = estimatedSize;
        this.albumId = albumId;
        this.artistId = artistId;
        this.nid = nid;
        this.localTrackSizeBytes = localTrackSizeBytes;

        this.hasCachedFile = hasCachedFile;
        this.cachedFileQuality = cachedFileQuality.ordinal();
        this.storageType = storageType.ordinal();
    }


    public MusicTrack(JSONObject trackJson) throws JSONException {
        if (!trackJson.getString("kind").equals("sj#track")) return;

        if (trackJson.has("id"))
            id = trackJson.getString("id");
        else if (trackJson.has("clientId"))
            id = "client" + trackJson.getString("clientId");

        if (trackJson.has("clientId"))
            clientId = trackJson.getString("clientId");
        if (trackJson.has("recentTimestamp"))
            recentTimestamp = new Date(Long.parseLong(trackJson.getString("recentTimestamp").substring(0, 10)));
        if (trackJson.has("deleted"))
            deleted = trackJson.getBoolean("deleted");
        if (trackJson.has("title"))
            title = trackJson.getString("title");
        if (trackJson.has("artist"))
            artist = trackJson.getString("artist");
        if (trackJson.has("composer"))
            composer = trackJson.getString("composer");
        album = trackJson.getString("album");
        if (trackJson.has("year"))
            year = trackJson.getInt("year");
        if (trackJson.has("comment"))
            comment = trackJson.getString("comment");
        if (trackJson.has("trackNumber"))
            trackNumber = trackJson.getInt("trackNumber");
        if (trackJson.has("genre"))
            genre = trackJson.getString("genre");
        durationMillis = Integer.parseInt(trackJson.getString("durationMillis"));
        if (trackJson.has("beatsPerMinute"))
            beatsPerMinute = trackJson.getInt("beatsPerMinute");
        if (trackJson.has("albumArtRef"))
            albumArtURL = trackJson.getJSONArray("albumArtRef").getJSONObject(0).getString("url");
        if (trackJson.has("artistArtRef"))
            artistArtURL = trackJson.getJSONArray("artistArtRef").getJSONObject(0).getString("url");
        if (trackJson.has("playCount"))
            playCount = trackJson.getInt("playCount");
        if (trackJson.has("rating"))
            rating = trackJson.getString("rating");
        estimatedSize = Integer.parseInt(trackJson.getString("estimatedSize"));
        if (trackJson.has("albumId"))
            albumId = trackJson.getString("albumId");
        if (trackJson.has("artistId")) {
            JSONArray artist_ids = trackJson.getJSONArray("artistId");
            artistId = new RealmList<>();
            for (int i = 0; i < artist_ids.length(); i++) artistId.add(new RealmString(artist_ids.getString(i)));
        }
        if (trackJson.has("nid")) nid = trackJson.getString("nid");
        localTrackSizeBytes = 0;
    }

    public String getTrackId() {
        return id;
    }

    public void setTrackId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

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

    public String getArtistArtURL() {
        return artistArtURL;
    }

    public void setArtistArtURL(String artistArtURL) {
        this.artistArtURL = artistArtURL;
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

    public String getNid() {
        return nid;
    }

    public void setNid(String nid) {
        this.nid = nid;
    }

    public void addPlay() {
        localPlays.add(new RealmLong(System.currentTimeMillis()));
    }

    public void setLocalTrackSizeBytes(long localTrackSizeBytes) {
        this.localTrackSizeBytes = localTrackSizeBytes;
    }

    public long getLocalTrackSizeBytes() {
        return localTrackSizeBytes;
    }

    public boolean isHasCachedFile() {
        return hasCachedFile;
    }

    public void setHasCachedFile(boolean hasCachedFile) {
        this.hasCachedFile = hasCachedFile;
    }

    public StreamQuality getCachedFileQuality() {
        return StreamQuality.values()[cachedFileQuality];
    }

    public void setCachedFileQuality(StreamQuality cachedFileQuality) {
        this.cachedFileQuality = cachedFileQuality.ordinal();
    }

    public StorageType getStorageType() {
        return StorageType.values()[storageType];
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType.ordinal();
    }


    public long getCacheImportance(PlaySource source) {
        int cacheImportance;
        cacheImportance = Math.max(0, (int) Math.round(Math.log((double) playCount) * 8.0));
        for (RealmLong play : localPlays) {
            cacheImportance += Math.max(0,
                    (int) Math.round(10 - Math.pow(((
                            System.currentTimeMillis() - play.get()) / 864000000L), 2)));
        }
        switch (source) {
            case RECENTS:
                cacheImportance += 20;
                break;
            case ALBUM:
            case PLAYLIST:
                cacheImportance += 18;
                break;
            case ARTIST:
                cacheImportance += 16;
                break;
            case SONGS:
            case EXTERNAL:
                cacheImportance += 10;
                break;
            case RADIO:
            default:
                cacheImportance += 0;
                break;
        }
        return cacheImportance;
    }

    public String getMostUsefulID() {
        if (nid != null) return nid;
        if (clientId != null) return clientId;
        return id;
    }

    public ParcelableMusicTrack parcelable() {
        return new ParcelableMusicTrack(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MusicTrack: {");
        sb.append("id: ");
        sb.append(id);
        sb.append(", title: ");
        sb.append(title);
        sb.append(", album: ");
        sb.append(album);
        sb.append(", trackNumber: ");
        sb.append(trackNumber);
        sb.append(", album id: ");
        sb.append(albumId);
        sb.append("}");

        return sb.toString();
    }

}
