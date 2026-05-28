package com.example.sportify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class DrinkPhotoAdapter extends RecyclerView.Adapter<DrinkPhotoAdapter.PhotoViewHolder> {

    private final List<File> photos;

    public DrinkPhotoAdapter(List<File> photos) {
        this.photos = photos;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        File photoFile = photos.get(position);
        Glide.with(holder.itemView.getContext())
                .load(photoFile)
                .centerCrop()
                .into(holder.ivGalleryPhoto);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGalleryPhoto;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGalleryPhoto = itemView.findViewById(R.id.ivGalleryPhoto);
        }
    }
}
