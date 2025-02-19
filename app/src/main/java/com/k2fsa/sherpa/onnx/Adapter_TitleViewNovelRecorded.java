package com.k2fsa.sherpa.onnx;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class Adapter_TitleViewNovelRecorded extends
        RecyclerView.Adapter<Adapter_TitleViewNovelRecorded.MyViewHolder> {
    Context context;
    ArrayList<TitleViewNovelRecorded> titles;
    private MainActivityCallback listener;

    public Adapter_TitleViewNovelRecorded(Context context, ArrayList<TitleViewNovelRecorded> titles,
                                          MainActivityCallback listener) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context;
        this.titles = titles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate the layout (give a looking to each row)
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.title_view_novel_recorded, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        //assign value to each row
        if (this.titles == null || this.titles.get(position) == null) {
            return;
        }

        String title = this.titles.get(position).getTitle();
        if (title == null) {
            title = "Unknown"; // 为空时提供默认值
        }
        holder.title.setText(titles.get(position).getTitle());
        holder.bind(titles.get(position).getTitle(), listener);
    }

    @Override
    public int getItemCount() {
        //recycler view wants to know how many items we have in total
        return (this.titles != null) ? this.titles.size() : 0;
    }

    // 更新数据
    public void updateData(ArrayList<TitleViewNovelRecorded> newTitles) {
        if (newTitles == null) {
            newTitles = new ArrayList<>();
        }
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
            if (this.title == null) {
                throw new NullPointerException("Button title not found! Check your XML layout.");
            }
        }
        public void bind(String fileName, MainActivityCallback listener){
            title.setText(fileName);
            title.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onButtonClick(fileName);
                }
            });
        }
    }
}
