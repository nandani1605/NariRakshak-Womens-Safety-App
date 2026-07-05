package com.example.narirakshak;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    private List<PlaceModel> placeList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(PlaceModel place);
    }

    public PlaceAdapter(List<PlaceModel> placeList, OnItemClickListener listener) {
        this.placeList = placeList;
        this.listener = listener;
    }

    public void updateList(List<PlaceModel> newList) {
        this.placeList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceModel place = placeList.get(position);
        holder.tvPlaceName.setText(place.name);

        // FIX: Ab 'km away' text bhi aapki select ki gayi bhasha ke hisaab se aayega
        String formattedDistance = holder.itemView.getContext().getString(R.string.map_distance_format, place.distance);
        holder.tvPlaceDistance.setText(formattedDistance);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(place);
            }
        });
    }

    @Override
    public int getItemCount() {
        return placeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaceName, tvPlaceDistance;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tvPlaceName);
            tvPlaceDistance = itemView.findViewById(R.id.tvPlaceDistance);
        }
    }
}