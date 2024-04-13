package com.demo.opencv.socialMedia;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.demo.opencv.UserDataManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class TwitterAuthenticator { // used to obtain user's Twitter access token and secret
    private static final String CONSUMER_KEY = "LMBdIxtET2HNzYRdiXQcqMWqd";
    private static final String CONSUMER_SECRET = "6n68iFVBaZO6JAflA2uTIBF9m17VgeWlw2Bu3U4xZQqJvMysm9";
    private static final String CALLBACK_URL = "gazelink://oauth-callback";
    private final Twitter twitter;
    private final Context context;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public TwitterAuthenticator(Context context) {
        this.context = context;
        twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
    }

    public void startAuthentication(Context applicationContext) {
        UserDataManager userDataManager = (UserDataManager) applicationContext;
        Log.d("SocialMediaPost", "Starting Twitter Authentication");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // storing in shared preferences
                    RequestToken requestToken = twitter.getOAuthRequestToken(CALLBACK_URL);
                    userDataManager.setRequestToken(requestToken.getToken());
                    userDataManager.setRequestTokenSecret(requestToken.getTokenSecret());

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL()));
                    context.startActivity(intent);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void handleCallback(Uri uri, Context applicationContext) {
        if (uri != null && uri.toString().startsWith(CALLBACK_URL)) { // if the intent is correct
            String verifier = uri.getQueryParameter("oauth_verifier");

            UserDataManager userDataManager = (UserDataManager) applicationContext;
            String requestTokenString = userDataManager.getRequestToken();
            String requestTokenSecretString = userDataManager.getRequestTokenSecret();
            Log.d("SocialMediaPost", "Callback Request Token: " + requestTokenString);
            Log.d("SocialMediaPost", "Callback Request Token Secret: " + requestTokenSecretString);

            RequestToken requestToken = new RequestToken(requestTokenString, requestTokenSecretString);

            executorService.submit(() -> {
                AccessToken accessToken;
                try {
                    accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
                    userDataManager.setAccessToken(accessToken.getToken());
                    userDataManager.setAccessTokenSecret(accessToken.getTokenSecret());
                    Log.d("SocialMediaPost", "Authentication Access token obtained: " + accessToken);
                } catch (TwitterException e) {
                    Log.d("SocialMediaPost", "Authentication token access failed: " + e);
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
