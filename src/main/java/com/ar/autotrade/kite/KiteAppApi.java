package com.ar.autotrade.kite;

import com.ar.autotrade.kite.models.*;
import com.ar.autotrade.kite.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Data
@Slf4j
public class KiteAppApi {

    String token;
    String userName;
    String password;
    String pin;

    @Autowired
    OkHttpClient client;

    private static final ObjectMapper objectMapper =
            new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    final DateTimeFormatter zoneFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    public void start() throws IOException {
        String login_url = "https://kite.zerodha.com/api/login";
        String twofa_url = "https://kite.zerodha.com/api/twofa";

        FormBody formBodyBuilder = new FormBody.Builder()
                .add("user_id",userName)
                .add("password", password).build();

        Request request = new Request.Builder()
                .url(login_url)
                .method("POST", formBodyBuilder)
                .headers(Utils.getKiteHeaders(userName))
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        JsonNode res = objectMapper.readTree(myResult);
        String requestId = res.get("data").get("request_id").textValue();

        formBodyBuilder = new FormBody.Builder()
                .add("user_id", userName)
                .add("request_id", requestId)
                .add("twofa_value", pin).build();

        request = new Request.Builder()
                .url(twofa_url)
                .method("POST", formBodyBuilder)
                .headers(Utils.getKiteHeaders(userName))
                .build();
        response = client.newCall(request).execute();
        List<String> cookielist = response.headers().values("Set-Cookie");
        response.close();
        Pattern pattern = Pattern.compile("enctoken=(.*?);");
        this.token = cookielist.stream().filter(x -> x.contains("enctoken"))
                .map(x ->
                        pattern.matcher(x).results().map(mr -> mr.group(1)).findFirst().get()
                ).findFirst().get();
    }

    public boolean isStarted() {
        if (this.token == null || this.token.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<Position> getPositions() throws IOException {
        Request request = new Request.Builder()
                .url("https://kite.zerodha.com/oms/portfolio/positions")
                .method("GET", null)
                .headers(Utils.getKiteHeaders(userName))
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        System.out.println(myResult);
        return null;
    }

    public List<OrderResponse> getOrders() throws IOException {
        Request request = new Request.Builder()
                .url("https://kite.zerodha.com/oms/orders")
                .method("GET", null)
                .headers(Utils.getKiteHeaders(userName))
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        KiteResponse<OrderResponse[]> orders = objectMapper.readValue(myResult, new TypeReference<KiteResponse<OrderResponse[]>>() {
        });
        if (orders.getStatus().equalsIgnoreCase("error")) {
            log.error("response status {} response {}", orders.getStatus(), myResult);
            return List.of();
        }
        return Arrays.asList(orders.getData());
    }

    public OrderResponse addOrder(Order order) throws IOException {
        order.validate();
        FormBody.Builder formBodyBuilder = new FormBody.Builder()
                .add("tradingsymbol", order.getSymbol())
                .add("exchange", "NSE")
                .add("transaction_type", order.getTransactionType().name())
                .add("order_type", order.getOrderType().getText())
                .add("product", order.getProduct().name())
                .add("quantity", String.valueOf(order.getQuantity()))
                .add("user_id", userName)
                .add("validity", "DAY")
                .add("variety", "regular");
        if (order.getTag() != null) {
            formBodyBuilder.add("tag", order.getTag());
        }
        if (order.getOrderType() == Enums.OrderType.SL || order.getOrderType() == Enums.OrderType.SLM) {
            formBodyBuilder.add("trigger_price", Utils.getPriceAsString(order.getTriggerPrice()));
        }
        if (order.getOrderType() == Enums.OrderType.LIMIT || order.getOrderType() == Enums.OrderType.SL) {
            formBodyBuilder.add("price", Utils.getPriceAsString(order.getLimitPrice()));
        }
        FormBody formBody = formBodyBuilder.build();
        Request request = new Request.Builder()
                .url("https://kite.zerodha.com/oms/orders/" + order.getVariety().getText())
                .method("POST", formBody)
                .headers(Utils.getKiteHeaders(userName))
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        KiteResponse<OrderResponse> orderResponse = objectMapper.readValue(myResult, new TypeReference<KiteResponse<OrderResponse>>() {
        });
        log.debug("Order response is {}", orderResponse);
        if (orderResponse.getStatus().equalsIgnoreCase("error")) {
            throw new RuntimeException(orderResponse.getMessage());
        }
        return orderResponse.getData();
    }

    public void cancelOrder(String orderId) throws IOException {
        Request request = new Request.Builder()
                .url("https://kite.zerodha.com/oms/orders/regular/" + orderId)
                .method("DELETE", null)
                .headers(Utils.getKiteHeaders(userName))
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        log.debug("response for id {} is {}", orderId, myResult);
    }

    public List<Quote> getQuotes(List<String> symbols) throws IOException {
        String url = symbols.stream().reduce((s, s2) -> {
            //url encode variable
            s2 = s2.replace(" ", "+").replace("&", "%26");
            return s.concat("&i=NSE:").concat(s2);
        }).get();
        //log.debug("url for ticks {}", url);
        Request request = new Request.Builder()
                .url("https://kite.zerodha.com/oms/quote?i=NSE:" + url)
                .method("GET", null)
                .headers(Utils.getKiteHeaders(userName))
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        KiteResponse<Map<?, ?>> res = objectMapper.readValue(myResult, new TypeReference<KiteResponse<Map<?, ?>>>() {
        });
        if (res.getStatus().equalsIgnoreCase("error")) {
            log.error("response status {} response {}", res.getStatus(), myResult);
            return List.of();
        }
        List<Quote> list = res.getData().entrySet().stream().map(entry -> {
            Quote quote = null;
            try {
                String symbol = entry.getKey().toString().split(":")[1];
                Map val = (Map) entry.getValue();
                //log.debug("each value {}:: {}", symbol, val);
                Quote.QuoteBuilder tickBuilder = Quote.builder();
                Float ltp = Float.parseFloat(val.get("last_price").toString());
                Float prevClose = Float.parseFloat(((Map) val.get("ohlc")).get("close").toString());
                Float per = (ltp - prevClose) * 100 / prevClose;
                Optional.ofNullable(val.get("volume")).map(x -> tickBuilder.volume(Float.parseFloat(x.toString())));
                LocalDateTime dt = LocalDateTime.parse(val.get("timestamp").toString(), formatter);
                tickBuilder.instrumentToken((Integer) val.get("instrument_token"))
                        .symbol(symbol).ltp(ltp)
                        .index(symbol.equals("NIFTY 50"))
                        .perChange(per).dateTime(dt);
                quote = tickBuilder.build();
            } catch (Exception e) {
                log.error(entry.toString(), e);
            }
            return quote;
        }).collect(Collectors.toList());
        //log.debug(list.toString());
        return list;
    }

    public Integer addGTTOrder(String symbol, Float triggerValue, Float ltp,
                                Enums.TransactionType type,
                                Integer quantity, Float price) throws IOException {
        String condition = String.format("{\"exchange\":\"NSE\", \"tradingsymbol\":\"%s\", \"trigger_values\":[%.1f]," +
                " \"last_price\": %.1f}", symbol, triggerValue, ltp);
        String orders = String.format("[{\"exchange\":\"NSE\", \"tradingsymbol\": \"%s\", " +
                        "\"transaction_type\": \"%s\", \"quantity\": %d, " +
                        "\"order_type\": \"LIMIT\",\"product\": \"CNC\", \"price\": %.1f}]",
                symbol, type.toString(), quantity, price);
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType,
                String.format("condition=%s&orders=%s&&type=single", condition, orders));
        //RequestBody body = RequestBody.create(mediaType, "condition={\"exchange\":\"NSE\",\"tradingsymbol\":\"CPSEETF\",\"trigger_values\":[23.07],\"last_price\":23.07}&orders=[{\"exchange\":\"NSE\",\"tradingsymbol\":\"CPSEETF\",\"transaction_type\":\"BUY\",\"quantity\":1,\"price\":23.07,\"order_type\":\"LIMIT\",\"product\":\"CNC\"}]&type=single");
        Request request = new Request.Builder()
                .url("https://kite.zerodha.com/oms/gtt/triggers")
                .method("POST", body)
                .headers(Utils.getKiteHeaders(userName))
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        log.debug("myResult {} ", myResult);
        response.close();
        KiteResponse<Map<String, Integer>> orderResponse = objectMapper.readValue(myResult, new TypeReference<KiteResponse<Map<String, Integer>>>() {
        });
        log.debug("Order response is {}", orderResponse);
        if (orderResponse.getStatus().equalsIgnoreCase("error")) {
            log.error("conditions {}; orders {}", condition, orders);
            throw new RuntimeException(orderResponse.getMessage());
        }
        return orderResponse.getData().get("trigger_id");
    }

    public GTTOrderResponse getGTTOrder(Integer id) throws IOException {
        Request request = new Request.Builder()
                .url("https://kite.zerodha.com/oms/gtt/triggers/" + id)
                .method("GET", null)
                .headers(Utils.getKiteHeaders(userName))
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        log.debug("myResult {} ", myResult);
        response.close();
        JsonNode orderResponse = null;
        try {
            orderResponse = objectMapper.readTree(myResult);
        } catch (JsonProcessingException e) {
            return GTTOrderResponse.builder().status("cancelled").build(); //as the order canceled it will not get from kite
        }
        log.debug("Order response is {}", orderResponse);
        if (orderResponse.get("status").textValue().equalsIgnoreCase("error")) {
            throw new RuntimeException(orderResponse.get("message").textValue());
        }
        GTTOrderResponse.GTTOrderResponseBuilder res = GTTOrderResponse.builder()
                .status(orderResponse.get("data").get("status").textValue())
                .lastUpdated(LocalDateTime.parse(orderResponse.get("data").get("updated_at").textValue(), formatter));
        if (orderResponse.get("data").get("status").textValue().equalsIgnoreCase("triggered")) {
            String orderId = orderResponse.get("data").get("orders").get(0).get("result").get("order_result").get("order_id").textValue();
            if (orderId != null && !orderId.isEmpty()) {
                if (getOrderStatus(orderId) != Enums.Status.COMPLETE) {
                    log.debug("{} is not completed for GTT id {}", orderId, id);
                    res.status("cancelled");
                }
            }
            res.orderStatus(orderResponse.get("data").get("orders").get(0).get("result").get("order_result").get("status").textValue())
                    .price(orderResponse.get("data").get("orders").get(0).get("result").get("price").floatValue())
                    .quantity(orderResponse.get("data").get("orders").get(0).get("result").get("quantity").intValue());
        }
        return res.build();
    }

    public void cancelGTTOrder(Integer id) throws IOException {
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url("https://kite.zerodha.com/oms/gtt/triggers/" + id)
                .method("DELETE", body)
                .headers(Utils.getKiteHeaders(userName))
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        log.debug("myResult {} ", myResult);
        response.close();
        JsonNode orderResponse = objectMapper.readTree(myResult);
        log.debug("Order response is {}", orderResponse);
        if (orderResponse.get("status").textValue().equalsIgnoreCase("error")) {
            throw new RuntimeException(orderResponse.get("message").textValue());
        }
    }

    public Enums.Status getOrderStatus(String orderId) throws IOException {
        Request request = new Request.Builder()
                .url("https://kite.zerodha.com/oms/orders/" + orderId)
                .method("GET", null)
                .headers(Utils.getKiteHeaders(userName))
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        JsonNode orderResponse = objectMapper.readTree(myResult);
        return Enums.Status.valueOf(orderResponse.get("data").get(orderResponse.get("data").size() - 1).get("status").asText());
    }
}
