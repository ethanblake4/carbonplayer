package com.carbonplayer.ui.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.carbonplayer.R;
import com.carbonplayer.model.entity.MusicTrack;

import java.util.List;

/**
 * Album / playlist adapter
 */
class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.ViewHolder> {
    private List<MusicTrack> mDataset;
    private Activity context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView trackName;
        TextView trackNumber;

        ViewHolder(View v) {
            super(v);
            trackName = (TextView) v.findViewById(R.id.trackName);
            trackNumber = (TextView) v.findViewById(R.id.trackNumber);
            v.findViewById(R.id.songLayoutRoot).setOnClickListener(view -> {
                Toast.makeText(context, trackName.getText(), Toast.LENGTH_SHORT).show();
                //Intent i = new Intent(context, AlbumActivity.class);
                //startActivity(i);
            });
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    SongListAdapter(List<MusicTrack> myDataset, Activity context) {
        mDataset = myDataset;
        this.context = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SongListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_item_layout, parent, false);
        // set the view's size, margins, paddings and layout parameters

        return new SongListAdapter.ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(SongListAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        MusicTrack t = mDataset.get(position);
        holder.trackName.setText(t.getTitle());
        if(t.getTrackNumber() != null) holder.trackNumber.setText(t.getTrackNumber().toString());

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
