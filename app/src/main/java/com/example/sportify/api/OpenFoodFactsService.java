package com.example.sportify.api;

import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface OpenFoodFactsService {
    @GET("api/v2/product/{barcode}.json")
    Call<ProductResponse> getProduct(@Path("barcode") String barcode);

    class ProductResponse {
        @SerializedName("status")
        public int status;
        @SerializedName("product")
        public ProductData product;
    }

    class ProductData {
        @SerializedName("product_name")
        public String productName;
        @SerializedName("nutriments")
        public Nutriments nutriments;
    }

    class Nutriments {
        @SerializedName("energy-kcal_100g")
        public float calories100g;
        @SerializedName("proteins_100g")
        public float proteins100g;
        @SerializedName("carbohydrates_100g")
        public float carbohydrates100g;
        @SerializedName("fat_100g")
        public float fat100g;
    }
}
