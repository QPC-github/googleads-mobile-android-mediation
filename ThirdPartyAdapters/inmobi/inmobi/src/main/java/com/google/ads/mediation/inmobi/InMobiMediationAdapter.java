package com.google.ads.mediation.inmobi;

import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_INVALID_SERVER_PARAMETERS;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.inmobi.InMobiInitializer.Listener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.inmobi.sdk.InMobiSdk;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * InMobi Adapter for AdMob Mediation used to load and show rewarded video ads. This class should
 * not be used directly by publishers.
 */
public class InMobiMediationAdapter extends Adapter {

    public static final String TAG = InMobiMediationAdapter.class.getSimpleName();

    // Flag to check whether the InMobi SDK has been initialized or not.
    static final AtomicBoolean isSdkInitialized = new AtomicBoolean();

    // Callback listener
    private InMobiRewardedAd inMobiRewarded;

    /**
     * {@link Adapter} implementation
     */
    @NonNull
    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.ADAPTER_VERSION;
        String[] splits = versionString.split("\\.");

        if (splits.length >= 4) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
            return new VersionInfo(major, minor, micro);
        }

        String logMessage = String
                .format("Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
                        versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @NonNull
    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = InMobiSdk.getVersion();
        String[] splits = versionString.split("\\.");

        if (splits.length >= 3) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]);
            return new VersionInfo(major, minor, micro);
        }

        String logMessage = String
                .format("Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.",
                        versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public void initialize(@NonNull Context context,
                           final @NonNull InitializationCompleteCallback initializationCompleteCallback,
                           @NonNull List<MediationConfiguration> mediationConfigurations) {

        if (isSdkInitialized.get()) {
            initializationCompleteCallback.onInitializationSucceeded();
            return;
        }

        HashSet<String> accountIDs = new HashSet<>();
        for (MediationConfiguration configuration : mediationConfigurations) {
            String serverAccountID = configuration.getServerParameters()
                    .getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);

            if (!TextUtils.isEmpty(serverAccountID)) {
                accountIDs.add(serverAccountID);
            }
        }

        int count = accountIDs.size();
        if (count <= 0) {
            AdError error = InMobiConstants.createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Missing or Invalid Account ID." + " configured for this ad source instance in the AdMob or Ad Manager UI");
            initializationCompleteCallback.onInitializationFailed(error.toString());
            return;
        }


        String accountID = accountIDs.iterator().next();

        if (count > 1) {
            String message = String.format("Multiple '%s' entries found: %s. "
                            + "Using '%s' to initialize the InMobi SDK",
                    InMobiAdapterUtils.KEY_ACCOUNT_ID, accountIDs, accountID);
            Log.w(TAG, message);
        }

        InMobiInitializer.getInstance().init(context, accountID, new Listener() {
            @Override
            public void onInitializeSuccess() {
                isSdkInitialized.set(true);
                initializationCompleteCallback.onInitializationSucceeded();
            }

            @Override
            public void onInitializeError(@NonNull AdError error) {
                initializationCompleteCallback.onInitializationFailed(error.toString());
            }
        });
    }

    @Override
    public void loadRewardedAd(
            @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            final @NonNull MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {
        inMobiRewarded = new InMobiRewardedAd(mediationRewardedAdConfiguration,
                mediationAdLoadCallback);
        inMobiRewarded.loadAd();
    }

}
