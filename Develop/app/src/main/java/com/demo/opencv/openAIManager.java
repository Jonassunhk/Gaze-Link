package com.demo.opencv;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class openAIManager extends AppCompatActivity {
    private static final String OPENAI_API_KEY = "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";
    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    OpenAIService service;
    Context mContext;

    public void initialize(Context context) {
        mContext = context;
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Create OkHttpClient and add interceptor
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        service = retrofit.create(OpenAIService.class);
       // speech = createTempFile(mContext, "textToSpeech", ".mp3");
    }

//    public File createTempFile(Context context, String fileName, String fileExtension) {
//        File tempFile;
//        try {
//            File cacheDir = context.getCacheDir();
//            tempFile = File.createTempFile(fileName, fileExtension, cacheDir);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        return tempFile;
//    }

    private void sendMessage(String data) {
        Log.d("TextGeneration", "Broadcasting Message");
        Intent intent = new Intent("textGenerationEvent");
        intent.putExtra("message", data);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    public File textToSpeech(String text) {
        TextToSpeechInput textToSpeechInput = new TextToSpeechInput("tts-1", "alloy", text);
        final File[] speech = new File[1];
        service.textToSpeech(textToSpeechInput).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<TextToSpeechOutput> call, @NonNull Response<TextToSpeechOutput> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("TextToSpeech", "Received voice file");

                    speech[0] = response.body().response;
                } else {
                    Log.d("TextToSpeech", "Error: response not successful");
                }
            }
            @Override
            public void onFailure(@NonNull Call<TextToSpeechOutput> call, @NonNull Throwable t) {
                Log.d("TextToSpeech", "Failure: " + t.getMessage());
            }
        });
        return speech[0];
    }

    public void generateText(String prompt) {

        // Prepare the request body
        Message messageStructure = new Message("user", prompt);
        Message[] messages = new Message[1];
        messages[0] = messageStructure;
        TextGenerationInput requestBody = new TextGenerationInput(messages,"gpt-3.5-turbo");
        service.generateText(requestBody).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<TextGenerationOutput> call, @NonNull Response<TextGenerationOutput> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && response.body().choices().size() != 0) {
                        String output = response.body().choices().get(0).message.content;
                        sendMessage(output);
                        Log.d("TextGeneration", output);
                    } else {
                        Log.d("TextGeneration", "Error: No contents");
                    }
                } else {
                    Log.d("TextGeneration", "Error: response not successful");
                }
            }
            @Override
            public void onFailure(@NonNull Call<TextGenerationOutput> call, @NonNull Throwable t) {
                Log.d("TextGeneration", "Failure: " + t.getMessage());
            }
        });
    }

    // Retrofit service interface for OpenAI
    public interface OpenAIService {
        @Headers({
                "Content-Type: application/json",
                "Authorization: Bearer " + OPENAI_API_KEY
        })
        @POST("https://api.openai.com/v1/chat/completions")
        Call<TextGenerationOutput> generateText(@Body TextGenerationInput body);
        Call<TextToSpeechOutput> textToSpeech(@Body TextToSpeechInput body);
    }

    public record Message(String role, String content) {} // container for the output from the model
    public record TextGenerationInput(Message[] messages, String model) {} // container for call request to the model
    public record Choice(String finish_reason, int index, Message message) {} // Response model for the OpenAI API
    public record TextGenerationOutput(List<Choice> choices) {} // container for callback from the model
    public record TextToSpeechInput(String model, String voice, String input) {}
    public record TextToSpeechOutput(File response) {}

}
