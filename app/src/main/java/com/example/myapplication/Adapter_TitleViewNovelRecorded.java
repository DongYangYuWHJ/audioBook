package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class Adapter_TitleViewNovelRecorded extends
        RecyclerView.Adapter<Adapter_TitleViewNovelRecorded.MyViewHolder> {
    Context context;
    ArrayList<TitleViewNovelRecorded> titles;

    public Adapter_TitleViewNovelRecorded(Context context, ArrayList<TitleViewNovelRecorded> titles) {
        this.context = context;
        this.titles = titles;
    }

    @NonNull
    @Override
    public Adapter_TitleViewNovelRecorded.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate the layout (give a looking to each row)
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.title_view_novel_recorded, parent, false);
        return new Adapter_TitleViewNovelRecorded.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Adapter_TitleViewNovelRecorded.MyViewHolder holder, int position) {
        //assign value to each row
        holder.title.setText(titles.get(position).getTitle());
    }

    @Override
    public int getItemCount() {
        //recycler view wants to know how many items we have in total
        return titles.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        //similar to onCreate:
        //grab views from row layout
        TextView title;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
        }
    }
}
