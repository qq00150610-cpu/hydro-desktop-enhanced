package com.hydrodesktop.video;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hydrodesktop.R;

import java.util.List;

/**
 * 视频列表适配器
 */
public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.VideoViewHolder> {

    public interface OnVideoClickListener {
        void onVideoClick(VideoItem video);
    }

    private final List<VideoItem> videos;
    private final OnVideoClickListener listener;

    public VideoListAdapter(List<VideoItem> videos, OnVideoClickListener listener) {
        this.videos = videos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem video = videos.get(position);

        holder.tvTitle.setText(video.getTitle());
        holder.tvDuration.setText(video.getFormattedDuration());
        holder.tvSize.setText(video.getFormattedSize());

        // 生成缩略图首字母
        String firstChar = video.getTitle().length() > 0 ?
                video.getTitle().substring(0, 1).toUpperCase() : "V";
        holder.tvThumbnail.setText(firstChar);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onVideoClick(video);
        });
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView tvThumbnail;
        TextView tvTitle;
        TextView tvDuration;
        TextView tvSize;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvThumbnail = itemView.findViewById(R.id.tv_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_video_title);
            tvDuration = itemView.findViewById(R.id.tv_video_duration);
            tvSize = itemView.findViewById(R.id.tv_video_size);
        }
    }
}
