package com.hydrodesktop.video;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hydrodesktop.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频列表 - 浏览本地视频文件
 */
public class VideoListActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 2001;

    private RecyclerView rvVideos;
    private VideoListAdapter adapter;
    private List<VideoItem> videoList;
    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        initViews();
        checkPermissionAndLoad();
    }

    private void initViews() {
        rvVideos = findViewById(R.id.rv_videos);
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvEmpty = findViewById(R.id.tv_empty);

        tvTitle.setText(R.string.video_list_title);
        btnBack.setOnClickListener(v -> finish());

        videoList = new ArrayList<>();
        adapter = new VideoListAdapter(videoList, video -> {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra("uri", video.getUri().toString());
            intent.putExtra("title", video.getTitle());
            startActivity(intent);
        });

        rvVideos.setLayoutManager(new GridLayoutManager(this, 3));
        rvVideos.setAdapter(adapter);
    }

    private void checkPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.READ_MEDIA_VIDEO
                    }, PERMISSION_REQUEST);
        } else {
            loadVideos();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadVideos();
            } else {
                tvEmpty.setText(R.string.video_permission_denied);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 加载本地视频列表
     */
    private void loadVideos() {
        videoList.clear();

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.RESOLUTION
        };

        String sortOrder = MediaStore.Video.Media.DATE_MODIFIED + " DESC";

        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));

                Uri uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));

                VideoItem item = new VideoItem(uri, name, duration, size, path);
                videoList.add(item);
            }
            cursor.close();
        }

        adapter.notifyDataSetChanged();

        if (videoList.isEmpty()) {
            tvEmpty.setText(R.string.video_empty);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }
}
