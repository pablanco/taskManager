package com.artech.inappbillinglib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.artech.actions.ActionResult;
import com.artech.application.MyApplication;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.model.Entity;
import com.artech.base.utils.Strings;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;
import com.artech.inappbillinglib.util.IabException;
import com.artech.inappbillinglib.util.IabHelper.OnIabPurchaseFinishedListener;
import com.artech.inappbillinglib.util.IabResult;
import com.artech.inappbillinglib.util.Inventory;
import com.artech.inappbillinglib.util.Purchase;
import com.artech.inappbillinglib.util.SkuDetails;

public class StoreAPI extends ExternalApi {
	private static final String TAG = "StoreAPI";
	private static final String METHOD_GET_PRODUCTS = "GetProducts";
	private static final String METHOD_PURCHASE_PRODUCT = "purchaseProduct";
	private static final String METHOD_CONSUME_PRODUCT = "ConsumeProduct";
	private static final String METHOD_GET_PURCHASED_PRODUCTS = "GetPurchasedProducts";
	@SuppressWarnings("unused")
	private static final String METHOD_IS_PRODUCT_ENABLED = "IsEnabled";
	@SuppressWarnings("unused")
	private static final String METHOD_ENABLE_PRODUCT = "EnableProduct";
	@SuppressWarnings("unused")
	private static final String METHOD_DISABLE_PRODUCT = "DisableProduct";
	private static final String METHOD_RESTORE_TRANSACTIONS = "RestoreTransactions";

	private Entity mPurchaseResult;

	public StoreAPI()
	{
		addSimpleMethodHandler(METHOD_GET_PRODUCTS, 1, mMethodGetProducts);
		addMethodHandler(METHOD_PURCHASE_PRODUCT, 2, mMethodPurchaseProduct);
		addSimpleMethodHandler(METHOD_GET_PURCHASED_PRODUCTS, 0, mMethodGetPurchasedProducts);
		addSimpleMethodHandler(METHOD_CONSUME_PRODUCT, 1, mMethodConsumeProduct);
		addSimpleMethodHandler(METHOD_RESTORE_TRANSACTIONS, 0, mMethodRestoreTransactions);
		registerActivityLifeCycleListener();
	}

	private final ISimpleMethodInvoker mMethodGetProducts = new ISimpleMethodInvoker() {
		@Override
		public Object invoke(List<Object> parameters) {
			String jsonString = (String) parameters.get(0);
			List<String> skus = StoreUtils.createSkusStringList(jsonString);

			List<ProductDetail> result;
			try {
				result = new ArrayList<>();
				Inventory inventory = IabService.getInstance().queryInventory(true, skus);
				for (String sku : skus) {
					SkuDetails details = inventory.getSkuDetails(sku);
					ProductDetail productDetail = new ProductDetail(
							sku,
							details.getTitle(),
							details.getDescription(),
							details.getPrice(),
							inventory.hasPurchase(sku)
					);
					result.add(productDetail);
				}
			} catch (IabException | IllegalStateException e) {
				result = Collections.emptyList();
			}

			return StoreUtils.createStoreProductCollection(result);
		}
	};

	private final IMethodInvoker mMethodPurchaseProduct = new IMethodInvoker() {
		@Override
		public @NonNull ExternalApiResult invoke(List<Object> parameters) {
			String sku = (String) parameters.get(0);
			int productQty = Integer.parseInt(((String) parameters.get(1)));

			// Only productQty == 1 is supported by Google's in-app billing API.
			if (productQty != 1) {
				mPurchaseResult = StoreUtils.createPurchaseResult(false, Strings.EMPTY, Strings.EMPTY);
				return ExternalApiResult.success(mPurchaseResult);
			}

			/*
			   In case we have the sku registered as purchased in our inventory, the onPurchaseFinishedListener
			   is called directly by launchPurchaseFlow. So after it finishes we already have our PurchaseResult
			   ready and want to return the result immediately.

			   Otherwise, onPurchaseFinishedListener is called by handleActivityResult which should be called on
			   ActivityResult, so we want to defer returning our PurchaseResult to afterActivityResult.
			 */
			mPurchaseResult = null;
			try
			{
				IabService.getInstance().launchPurchaseFlow(getActivity(), sku, RequestCodes.ACTION_ALWAYS_SUCCESSFUL, mPurchaseFinishedListener);
				return ExternalApiResult.SUCCESS_WAIT;
			}
			catch (IllegalStateException e) {
				mPurchaseResult = StoreUtils.createPurchaseResult(false, Strings.EMPTY, Strings.EMPTY);
				return ExternalApiResult.success(mPurchaseResult);
			}
		}
	};

	private ISimpleMethodInvoker mMethodGetPurchasedProducts = new ISimpleMethodInvoker() {
		@Override
		public Object invoke(List<Object> parameters) {
			List<String> result;
			try {
				Inventory inventory = IabService.getInstance().queryInventory(false, null);
				result = inventory.getAllOwnedSkus();
			} catch (IabException | IllegalStateException e) {
				result = Collections.emptyList();
			}
			return StoreUtils.createSkusJsonArray(result).toString();
		}
	};

	private ISimpleMethodInvoker mMethodConsumeProduct = new ISimpleMethodInvoker() {
		@Override
		public Object invoke(List<Object> parameters) {
			String sku = (String) parameters.get(0);

			boolean success = true;
			try {
				Inventory inventory = IabService.getInstance().queryInventory(true, null);
				Purchase purchase = inventory.getPurchase(sku);
				if (purchase != null) {
					IabService.getInstance().consume(purchase);
				} else {
					Log.d(TAG, "No purchase for product '" + sku + "' found.");
					success = false;
				}
			} catch (IabException | IllegalStateException e) {
				success = false;
			}

			return success;
		}
	};

	private final ISimpleMethodInvoker mMethodRestoreTransactions = new ISimpleMethodInvoker() {
		@Override
		public Object invoke(List<Object> parameters) {
			List<Purchase> result;
			try {
				Inventory inventory = IabService.getInstance().queryInventory(true, null, null);
				result = inventory.getAllPurchases();
			} catch (IabException e) {
				result = Collections.emptyList();
			}
			return StoreUtils.createStoreRestoredTransactionCollection(result);
		}
	};

	@Override
	public ExternalApiResult afterActivityResult(int requestCode, int resultCode, Intent data, String method) {
		boolean handled;
		try {
			handled = IabService.getInstance().handleActivityResult(requestCode, resultCode, data);
		} catch (IllegalStateException e) {
			handled = false;
		}

		if (handled) {
			Log.d(TAG, "onActivityResult handled by IABHelper.");
		} else {
			super.afterActivityResult(requestCode, resultCode, data, method);
		}

		return new ExternalApiResult(ActionResult.SUCCESS_CONTINUE_NO_REFRESH, mPurchaseResult);
	}

	// This listener might be called from handleActivityResult, i.e. it might use the UI thread, so it should NOT take long.
	private OnIabPurchaseFinishedListener mPurchaseFinishedListener = new OnIabPurchaseFinishedListener() {
		@Override
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			boolean purchaseSuccess = !result.isFailure();
			String productId = purchaseSuccess ? purchase.getSku() : Strings.EMPTY;
			String transactionData = purchaseSuccess ? purchase.getOriginalJson() : Strings.EMPTY;
			Log.d(TAG, purchaseSuccess ? "Purchase successful." : String.format("Purchase failed. Error: %s", result));
			mPurchaseResult = StoreUtils.createPurchaseResult(purchaseSuccess, productId, transactionData);
		}
	};

	@SuppressLint("NewApi")
	private ActivityLifecycleCallbacks mActivityLifecycleListener = new ActivityLifecycleCallbacks() {
		@Override
		public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
		}

		@Override
		public void onActivityStarted(Activity activity) {
		}

		@Override
		public void onActivityStopped(Activity activity) {
		}

		@Override
		public void onActivityResumed(Activity activity) {
		}

		@Override
		public void onActivityPaused(Activity activity) {
		}

		@Override
		public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
		}

		@Override
		public void onActivityDestroyed(Activity activity) {
			if (activity == getActivity()) {
				IabService.dispose();
			}
		}
	};

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void registerActivityLifeCycleListener() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			MyApplication.getInstance().registerActivityLifecycleCallbacks(mActivityLifecycleListener);
		}
	}
}
