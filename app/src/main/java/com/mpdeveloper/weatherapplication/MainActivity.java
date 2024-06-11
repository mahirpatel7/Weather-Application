package com.mpdeveloper.weatherapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mpdeveloper.weatherapplication.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;

    ImageButton imageButton;
    private SwipeRefreshLayout swipeRefreshLayout;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageButton = findViewById(R.id.imagebutton1);

        findViewById(R.id.button_request_location_permission).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationPermission();
            }
        });

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                fetchWeatherData("cityname");
            }
        });

        // You can also set colors for the SwipeRefreshLayout loading indicator
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        fetchWeatherData("Patan");
        // Initialize search functionality
        searchCity();
    }


    private class FetchWeatherTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            String apiKey = "fa543c9ab204dca2353b1b63b7051c6f";
            String cityName = "cityname"; // Provide city name here
            String apiUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&appid=" + apiKey;

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null; // Return null if an exception occurs
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            swipeRefreshLayout.setRefreshing(false); // Stop the refreshing animation
            if (s != null) {
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    String weatherDescription = jsonObject.getJSONArray("weather")
                            .getJSONObject(0)
                            .getString("description");
                    // Update UI using data binding
                    binding.weather.setText(weatherDescription);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void searchCity() {
        SearchView searchView = binding.searchView;
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Perform actions when user submits the query
                fetchWeatherData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Perform actions as the query text changes
                // This method will be called whenever the user types or deletes characters in the search view
                return false;
            }
        });
    }

    private void fetchWeatherData(String cityName) {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .build();

        ApiInterface apiInterface = retrofit.create(ApiInterface.class);

        Call<WeatherApp> call = apiInterface.getWeatherData(cityName, "fa543c9ab204dca2353b1b63b7051c6f", "metric");
        call.enqueue(new Callback<WeatherApp>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<WeatherApp> call, Response<WeatherApp> response) {
                swipeRefreshLayout.setRefreshing(false);
                // Handle successful response here
                WeatherApp responseBody = response.body();
                if (response.isSuccessful() && responseBody != null) {
                    String temperature = String.valueOf(responseBody.getMain().getTemp());
                    String humidity = String.valueOf(responseBody.getMain().getHumidity());
                    String windspeed = String.valueOf(responseBody.getWind().getSpeed());
                    String sunrise = convertTimestampToTime(responseBody.getSys().getSunrise());
                    String sunset = convertTimestampToTime(responseBody.getSys().getSunset());
                    String sealevel = String.valueOf(responseBody.getMain().getPressure());
                    String condition;
                    if (responseBody.getWeather() != null && !responseBody.getWeather().isEmpty()) {
                        condition = responseBody.getWeather().get(0).getMain();
                    } else {
                        condition = "unknown";
                    }

                    String minTemp = String.valueOf(responseBody.getMain().getTempMin());
                    String maxTemp = String.valueOf(responseBody.getMain().getTempMax());

                    binding.temperature.setText(temperature + "°C");
                    binding.weather.setText(condition);
                    binding.maxtemp.setText("Max Temp: " + minTemp + "°C");
                    binding.mintemp.setText("Min Temp: " + maxTemp + "°C");
                    binding.humidity.setText(humidity + "%");
                    binding.windspeed.setText(windspeed + "m/s");
                    binding.sunrise.setText(sunrise);
                    binding.sunset.setText(sunset);
                    binding.sea.setText(sealevel + "hPa");
                    binding.conditions.setText(condition);
                    binding.day.setText(dayName(responseBody.getSys().getSunrise()));
                    binding.date.setText(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
                    binding.cityname.setText(cityName);

                    changeImagesAccordingToWeatherCondition(condition);
                }
            }

            @Override
            public void onFailure(Call<WeatherApp> call, Throwable t) {
                // Handle failure here
                Log.e(TAG, "Failed to fetch weather data", t);
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, Settings.class);
                startActivity(intent);
            }
        });
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Show an explanation to the user
                    new AlertDialog.Builder(this)
                            .setTitle("Location Permission Needed")
                            .setMessage("This app needs the Location permission, please accept to use location functionality")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // Prompt the user once explanation has been shown
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                            LOCATION_PERMISSION_REQUEST_CODE);
                                }
                            })
                            .create()
                            .show();
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                }
            } else {
                // Permission has already been granted
                Toast.makeText(this, "Location permission already granted", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Handle the result of the request for location permissions
    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                // Proceed with location-related operations
                // For example, you can start retrieving the user's location
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                // Disable or hide location-related functionality
            }
        }
    }

    private void changeImagesAccordingToWeatherCondition(String conditions) {
        if (conditions.equals("Partly Clouds") || conditions.equals("Clouds") || conditions.equals("Overcast") || conditions.equals("Mist") || conditions.equals("Foggy") ||  conditions.equals("Haze") || conditions.equals("Smoke")){
            binding.getRoot().setBackgroundResource(R.drawable.colud_background);
            binding.lottieAnimationView.setAnimation(R.raw.cloud);
        } else if (conditions.equals("Clear Sky") || conditions.equals("Sunny") || conditions.equals("Clear")) {
            binding.getRoot().setBackgroundResource(R.drawable.sunny_background);
            binding.lottieAnimationView.setAnimation(R.raw.sun);
        } else if (conditions.equals("Light Rain") || conditions.equals("Drizzle") || conditions.equals("Moderate Rain") || conditions.equals("Showers") || conditions.equals("Heavy Rain")) {
            binding.getRoot().setBackgroundResource(R.drawable.rain_background);
            binding.lottieAnimationView.setAnimation(R.raw.rain);
        } else if (conditions.equals("Light Snow") || conditions.equals("Moderate Snow") || conditions.equals("Heavy Snow") || conditions.equals("Blizzard")) {
            binding.getRoot().setBackgroundResource(R.drawable.snow_background);
            binding.lottieAnimationView.setAnimation(R.raw.snow);
        }
        binding.lottieAnimationView.playAnimation();
    }

    private String dayName(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
        return sdf.format(new Date(timestamp * 1000)); // Convert seconds to milliseconds
    }

    private String convertTimestampToTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp * 1000)); // Convert seconds to milliseconds
    }
}
