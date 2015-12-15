package com.artech.controls.ads;
import com.artech.base.utils.NameMap;

/**
 * Created by gmilano on 8/3/15.
 *
 * Register Ads providers here, the Ads.addProvider must be called at app creation time, taking into account the controls used in the app
 */
public class Ads {

        private static NameMap<IAdsProvider> sAdsProviders;

        static
        {
            sAdsProviders = new NameMap<IAdsProvider>();
        }

        public static void addProvider(IAdsProvider provider)
        {
            sAdsProviders.put(provider.getId(), provider);
        }

        static IAdsProvider getProvider(String id)
        {
            return sAdsProviders.get(id);
        }
}
