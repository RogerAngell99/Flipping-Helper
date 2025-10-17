package flippinghelper;

import com.google.gson.Gson;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
public class FlippingApiClient {

    private static final String API_URL = "https://www.gielinorgains.com/api/items";
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public List<FlippingItem> getItems() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.debug("API Response: {}", response.body());

        ApiResponse apiResponse = gson.fromJson(response.body(), ApiResponse.class);

        List<FlippingItem> items = apiResponse.getData();
        for (FlippingItem item : items) {
            item.setPredictedAction("buy"); // Default action is to buy
        }

        return items;
    }

    @Data
    private static class ApiResponse {
        private List<FlippingItem> data;
        private int totalItems;
    }
}
