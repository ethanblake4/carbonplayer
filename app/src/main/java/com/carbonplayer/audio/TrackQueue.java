package com.carbonplayer.audio;


import android.support.v4.util.ArrayMap;

import com.carbonplayer.model.entity.MusicTrack;
import com.carbonplayer.model.entity.ParcelableMusicTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton track queue which mirrors data in {@link MusicPlayerService}
 */
public class TrackQueue {
    private static TrackQueue instance;
    private ArrayList<MusicTrack> queue;
    private int position;

    public static TrackQueue instance(){
        if(instance==null) instance = new TrackQueue();
        return instance;
    }

    public static TrackQueue instance(List<MusicTrack> tracks, boolean replace){
        if (instance==null)
            instance = new TrackQueue(tracks);
        else if (replace) instance.replace(tracks);

        return instance;
    }

    private TrackQueue(){
        queue = new ArrayList<>();
    }

    private TrackQueue(List<MusicTrack> tracks){
        replace(tracks);
    }

    public void replace(List<MusicTrack> tracks){
        if(tracks instanceof ArrayList)
            queue = (ArrayList<MusicTrack>)tracks;
        else
            queue = new ArrayList<>();
            for(MusicTrack track : tracks) queue.add(track);
    }

    public ArrayList<ParcelableMusicTrack> getParcelable(){
        ArrayList<ParcelableMusicTrack> parcelables = new ArrayList<>();
        int i = 0;
        for(MusicTrack t : queue){
            parcelables.add(new ParcelableMusicTrack(t));
            i++;
        }
        return parcelables;
    }

    public ArrayList<MusicTrack> queue() {return queue;}

    public void insertAtEnd(MusicTrack t){
        queue.add(t);
    }

    public void insertNext(MusicTrack t){
        queue.add(position+1, t);
    }

    public void reorder(int pos, int pnew){
        MusicTrack t = queue.get(pos);
        queue.remove(pos);
        queue.add(pnew, t);
    }

    public void prevtrack(){
        position--;
    }

    public void nexttrack(){
        position++;
    }

    public MusicTrack currentTrack(){
        return queue.get(position);
    }

    public int position(){
        return position;
    }
}
