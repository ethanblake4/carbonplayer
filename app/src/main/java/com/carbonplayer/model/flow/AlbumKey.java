package com.carbonplayer.model.flow;

/**
 * Album key
 */
public class AlbumKey {
    public final String albumId;

    public AlbumKey(String albumId) {
        this.albumId = albumId;
    }

    public boolean equals(Object o) {
        return o instanceof AlbumKey
                && albumId.equals(((AlbumKey) o).albumId);
    }

    public int hashCode() {
        return albumId.hashCode();
    }
}
