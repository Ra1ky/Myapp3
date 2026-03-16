package com.example.sportify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sportify.db.FoodItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    private List<FoodItem> foodItems = new ArrayList<>();
    private final OnFoodDeleteListener deleteListener;

    public interface OnFoodDeleteListener {
        void onDelete(FoodItem foodItem);
    }

    public FoodAdapter(OnFoodDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setFoodItems(List<FoodItem> foodItems) {
        this.foodItems = foodItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem item = foodItems.get(position);
        holder.tvMealType.setText(item.getMealType());
        holder.tvName.setText(item.getName());
        holder.tvCalories.setText(item.getCalories() + " kcal");
        
        String macros = String.format(Locale.getDefault(), "P: %dg • C: %dg • F: %dg", 
                item.getProtein(), item.getCarbs(), item.getFat());
        holder.tvMacros.setText(macros);

        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return foodItems.size();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView tvMealType, tvName, tvCalories, tvMacros;
        ImageButton btnDelete;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMealType = itemView.findViewById(R.id.tvFoodMealType);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvCalories = itemView.findViewById(R.id.tvFoodCalories);
            tvMacros = itemView.findViewById(R.id.tvFoodMacros);
            btnDelete = itemView.findViewById(R.id.btnDeleteFood);
        }
    }
}
