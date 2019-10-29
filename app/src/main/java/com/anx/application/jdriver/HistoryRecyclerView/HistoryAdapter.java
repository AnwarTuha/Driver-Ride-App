package com.anx.application.jdriver.HistoryRecyclerView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.anx.application.jdriver.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {

    private List<HistoryObject> itemList;
    private Context context;

    public HistoryAdapter(List<HistoryObject> itemList, Context context){
        this.itemList = itemList;
        this.context = context;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);

        HistoryViewHolder rcv = new HistoryViewHolder(layoutView);
        return rcv;
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {

        holder.rideId.setText("Ride id: "+itemList.get(position).getRideId());
        holder.time.setText("Date: "+itemList.get(position).getTime());
        holder.rideDestination.setText("Destination: " + itemList.get(position).getRideDestination());
        holder.rideCost.setText("Fare: Birr. "+ itemList.get(position).getRideCost());
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
