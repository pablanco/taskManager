package com.artech.inappbillinglib;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import com.artech.application.MyApplication;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.inappbillinglib.util.Purchase;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;

public class StoreUtils {
    public static Entity createPurchaseResult(boolean purchaseSuccess, String productId, String transactionData) {
        Entity purchaseResult = new Entity(MyApplication.getInstance().getSDT("PurchaseResult").getStructure());
        purchaseResult.setProperty("Success", purchaseSuccess);
        purchaseResult.setProperty("ProductIdentifier", productId);
        purchaseResult.setProperty("TransactionData", transactionData);
        return purchaseResult;
    }

    public static EntityList createStoreProductCollection(List<ProductDetail> productDetails) {
        EntityList collection = new EntityList();
        for (ProductDetail productDetail : productDetails) {
            Entity item = createProductCollectionItem(productDetail);
            collection.add(item);
        }
        return collection;
    }

    private static Entity createProductCollectionItem(ProductDetail productDetail) {
        StructureDefinition sdtDefinition = MyApplication.getInstance().getSDT("StoreProductCollection").getStructure();
        Entity item = new Entity(sdtDefinition);
        item.setProperty("Identifier", productDetail.getId());
        item.setProperty("LocalizedTitle", productDetail.getTitle());
        item.setProperty("LocalizedDescription", productDetail.getDesc());
        item.setProperty("LocalizedPriceAsString", productDetail.getPrice());
        item.setProperty("Purchased", String.valueOf(productDetail.isPurchased()));
        return item;
    }

    public static EntityList createStoreRestoredTransactionCollection(List<Purchase> purchases) {
        EntityList collection = new EntityList();
        for (Purchase purchase : purchases) {
            Entity item = createRestoredTransactionCollectionItem(purchase.getSku(), purchase.getOriginalJson());
            collection.add(item);
        }
        return collection;
    }

    public static Entity createRestoredTransactionCollectionItem(String sku, String transactionData) {
        StructureDefinition sdtDefinition = MyApplication.getInstance().getSDT("StoreRestoredTransactionCollection").getStructure();
        Entity item = new Entity(sdtDefinition);
        item.setProperty("ProductIdentifier", sku);
        item.setProperty("TransactionData", transactionData);
        return item;
    }

    public static List<String> createSkusStringList(String jsonString) {
        List<String> skus = new ArrayList<>();
        try {
            JsonArray jsonArray = new Gson().fromJson(jsonString, JsonArray.class);
            for (int i = 0; i < jsonArray.size(); i++) {
                skus.add(jsonArray.get(i).getAsString());
            }
        } catch (JsonSyntaxException e) {
        }
        return skus;
    }

    public static JSONArray createSkusJsonArray(List<String> skus) {
        JSONArray jsonArray = new JSONArray();
        for (String sku : skus) {
            jsonArray.put(sku);
        }
        return jsonArray;
    }
}
