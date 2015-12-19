package io.github.voidc.np4ilr.model;

import android.support.annotation.Nullable;

import java.util.Date;

public class ILRTrack {
    private String artist;
    private String title;
    private String imageURI;
    private Date timestamp;

    public ILRTrack(String artist, String title, String imageURI) {
        this.artist = artist;
        this.title = title;
        this.imageURI = imageURI;
    }

    public ILRTrack(String artist, String title, String imageURI, Date timestamp) {
        this(artist, title, imageURI);
        this.timestamp = timestamp;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getImageURI() {
        return imageURI;
    }

    @Nullable
    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ILRTrack ilrTrack = (ILRTrack) o;

        if (!artist.equals(ilrTrack.artist)) return false;
        return title.equals(ilrTrack.title);

    }

    @Override
    public int hashCode() {
        int result = artist.hashCode();
        result = 31 * result + title.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return artist + " - " + title;
    }
}
