package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class Adapter_TitleViewNovelRecorded extends
        RecyclerView.Adapter<Adapter_TitleViewNovelRecorded.MyViewHolder> {
    Context context;
    ArrayList<TitleViewNovelRecorded> titles;
    private OnButtonClickListener listener;

    public Adapter_TitleViewNovelRecorded(Context context, ArrayList<TitleViewNovelRecorded> titles,
                                          OnButtonClickListener listener) {
        this.context = context;
        this.titles = titles;
        this.listener = listener;
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
        holder.bind(titles.get(position).getTitle(), listener);
    }

    @Override
    public int getItemCount() {
        //recycler view wants to know how many items we have in total
        return titles.size();
    }

    // 更新数据
    public void updateData(ArrayList<TitleViewNovelRecorded> newTitles) {
        titles = newTitles;
        notifyDataSetChanged();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        //similar to onCreate:
        //grab views from row layout
        Button title;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
        }
        public void bind(String fileName, OnButtonClickListener listener){
            title.setText(fileName);
            title.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onButtonClick(fileName);
                }
            });
        }
    }
}
