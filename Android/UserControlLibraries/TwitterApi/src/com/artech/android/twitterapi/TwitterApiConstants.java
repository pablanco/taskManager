package com.artech.android.twitterapi;

import com.artech.base.services.Services;

public class TwitterApiConstants {
	public final static String CONSUMER_KEY = Services.Strings.getResource(R.string.TwitterConsumerKey);
	public final static String CONSUMER_SECRET = Services.Strings.getResource(R.string.TwitterConsumerSecret);
	
	public final static String PREFERENCES_KEY = "twitter_session";
	public final static String PREF_NAME = "twitter_oauth";
	public final static String PREF_KEY_TOKEN = "twitter_oauth_token";
	public final static String PREF_KEY_TOKEN_SECRET = "twitter_oauth_token_secret";
	
	public final static String ENDPOINT_DOMAIN = "api.twitter.com";
	public final static String CALLBACK_URI = "oauth://twitter4j";
	
	public final static String AUTH_URL = "auth_url";
	public final static String OAUTH_VERIFIER = "oauth_verifier";
	public final static String PENDING_OPERATION = "pending_operation";
	public final static String AUTHORIZATION_RESULT = "authorization_result";
	
	public final static String SHOW_STATUS_URI = "twitter://status?status_id=";
	public final static String SHOW_USER_PROFILE_URI = "twitter://user?screen_name=";
	public final static String WEB_TWEET_URL = "https://twitter.com/intent/tweet?source=webclient&text=";
	public final static String MOBILE_URL = "https://mobile.twitter.com/";
	
	final static String[] twitterPackages = {"com.twitter.android", "com.twidroid", "com.handmark.tweetcaster", "com.thedeck.android"};
}
