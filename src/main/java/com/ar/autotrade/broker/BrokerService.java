package com.ar.autotrade.broker;

import com.ar.autotrade.broker.models.*;
import com.ar.autotrade.broker.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
public class BrokerService {

    private String token;

    @Value("${broker.userName}")
    private String userName;

    @Value("${broker.password}")
    private String password;

    @Value("${broker.pin}")
    private String pin;

    @Value("${broker.url}")
    private String baseUrl;

    /**
     * todo Rate Limitter add multiple
     * Quote	1req/second
     * Historical candle	3req/second
     * Order placement	10req/second
     * All other endpoints	10req/second
     */
    @Autowired
    OkHttpClient client;

    private static final ObjectMapper objectMapper =
            new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    final DateTimeFormatter zoneFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    private Headers getBrokerHeaders() {
        return Headers.of(Map.ofEntries(Map.entry("authority", "kite.zerodha.com")
                ,Map.entry("pragma", "no-cache")
                ,Map.entry("cache-control", "no-cache")
                ,Map.entry("x-kite-version", "2.9.11")
                ,Map.entry("accept", "application/json, text/plain, */*")
                ,Map.entry("sec-ch-ua-mobile", "?0")
                ,Map.entry("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36")
                ,Map.entry("sec-ch-ua", "\"Chromium\";v=\"88\", \"Google Chrome\";v=\"88\", \";Not A Brand\";v=\"99\"")
                ,Map.entry("sec-fetch-site", "same-origin")
                ,Map.entry("sec-fetch-mode", "cors")
                ,Map.entry("sec-fetch-dest", "empty")
                ,Map.entry("referer", "%s/dashboard".formatted(baseUrl))
                ,Map.entry("accept-language", "en-GB,en-US;q=0.9,en;q=0.8")
                ,Map.entry("x-kite-userid", userName)));
    }

    public void start() throws IOException {
        if(token!=null){
            return;
        }
        String login_url = "%s/api/login".formatted(baseUrl);
        String twofa_url = "%s/api/twofa".formatted(baseUrl);

        FormBody formBodyBuilder = new FormBody.Builder()
                .add("user_id", userName)
                .add("password", password).build();

        Request request = new Request.Builder()
                .url(login_url)
                .method("POST", formBodyBuilder)
                .headers(getBrokerHeaders())
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
                .headers(getBrokerHeaders())
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

    public List<Position> getPositions() throws IOException {
        Request request = new Request.Builder()
                .url("%s/oms/portfolio/positions".formatted(baseUrl))
                .method("GET", null)
                .headers(getBrokerHeaders())
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        System.out.println(myResult);
        return null;
    }

    public List<OrderResponse> getOrders() throws IOException {
        //The order history or the order book is transient as it only lives for a day in the system. When you retrieve orders,
        //you get all the orders for the day including open, pending, and executed ones.
        Request request = new Request.Builder()
                .url("%s/oms/orders".formatted(baseUrl))
                .method("GET", null)
                .headers(getBrokerHeaders())
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        BrokerResponse<OrderResponse[]> orders = objectMapper.readValue(myResult, new TypeReference<BrokerResponse<OrderResponse[]>>() {
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
                .add("variety", order.getVariety().getText());
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
                .url("%s/oms/orders/".formatted(baseUrl) + order.getVariety().getText())
                .method("POST", formBody)
                .headers(getBrokerHeaders())
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        BrokerResponse<OrderResponse> orderResponse = objectMapper.readValue(myResult, new TypeReference<BrokerResponse<OrderResponse>>() {
        });
        log.debug("Order response is {}", orderResponse);
        if (orderResponse.getStatus().equalsIgnoreCase("error")) {
            throw new RuntimeException(orderResponse.getMessage());
        }
        return orderResponse.getData();
    }

    public void cancelOrder(String orderId) throws IOException {
        Request request = new Request.Builder()
                .url("%s/oms/orders/regular/".formatted(baseUrl) + orderId)
                .method("DELETE", null)
                .headers(getBrokerHeaders())
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
                .url("%s/oms/quote?i=NSE:".formatted(baseUrl) + url)
                .method("GET", null)
                .headers(getBrokerHeaders())
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        BrokerResponse<Map<?, ?>> res = objectMapper.readValue(myResult, new TypeReference<BrokerResponse<Map<?, ?>>>() {
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

    public Quote getQuote(String symbol) throws IOException {
        return getQuotes(List.of(symbol)).get(0);
    }

    public String addGTTOrder(GttOrder gttOrder) throws IOException {
        String condition, orders;
        if(gttOrder.getGttType()== GttOrder.GttType.SINGLE) {
            condition = String.format("{\"exchange\":\"NSE\", \"tradingsymbol\":\"%s\", \"trigger_values\":[%.1f]," +
                    " \"last_price\": %.1f}", gttOrder.getSymbol(), gttOrder.getTriggerPrice(), gttOrder.getLtp());
            orders = String.format("[{\"exchange\":\"NSE\", \"tradingsymbol\": \"%s\", " +
                            "\"transaction_type\": \"%s\", \"quantity\": %d, " +
                            "\"order_type\": \"LIMIT\",\"product\": \"CNC\", \"price\": %.1f}]",
                    gttOrder.getSymbol(), gttOrder.getTransactionType().toString(), gttOrder.getQuantity(), gttOrder.getLimitPrice());
        } else{
            condition = String.format("{\"exchange\":\"NSE\", \"tradingsymbol\":\"%s\", \"trigger_values\":[%.1f, %.1f]," +
                    " \"last_price\": %.1f}", gttOrder.getSymbol(), gttOrder.getStopLossTriggerPrice(), gttOrder.getTriggerPrice(), gttOrder.getLtp());
            String orderStopLoss = String.format("{\"exchange\":\"NSE\", \"tradingsymbol\": \"%s\", " +
                            "\"transaction_type\": \"%s\", \"quantity\": %d, " +
                            "\"order_type\": \"LIMIT\",\"product\": \"CNC\", \"price\": %.1f}",
                    gttOrder.getSymbol(), gttOrder.getTransactionType().toString(), gttOrder.getQuantity(), gttOrder.getStopLossLimitPrice());
            String orderTarget = String.format("[{\"exchange\":\"NSE\", \"tradingsymbol\": \"%s\", " +
                            "\"transaction_type\": \"%s\", \"quantity\": %d, " +
                            "\"order_type\": \"LIMIT\",\"product\": \"CNC\", \"price\": %.1f}]",
                    gttOrder.getSymbol(), gttOrder.getTransactionType().toString(), gttOrder.getQuantity(), gttOrder.getLimitPrice());
            orders = "[".concat(orderStopLoss).concat(orderTarget).concat("]");
        }

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType,
                String.format("condition=%s&orders=%s&&type=%s", condition, orders, gttOrder.getGttType().getValue()));
        //RequestBody body = RequestBody.create(mediaType, "condition={\"exchange\":\"NSE\",\"tradingsymbol\":\"CPSEETF\",\"trigger_values\":[23.07],\"last_price\":23.07}&orders=[{\"exchange\":\"NSE\",\"tradingsymbol\":\"CPSEETF\",\"transaction_type\":\"BUY\",\"quantity\":1,\"price\":23.07,\"order_type\":\"LIMIT\",\"product\":\"CNC\"}]&type=single");
        Request request = new Request.Builder()
                .url("%s/oms/gtt/triggers".formatted(baseUrl))
                .method("POST", body)
                .headers(getBrokerHeaders())
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        log.debug("myResult {} ", myResult);
        response.close();
        BrokerResponse<Map<String, String>> orderResponse = objectMapper.readValue(myResult, new TypeReference<BrokerResponse<Map<String, String>>>() {
        });
        log.debug("Order response is {}", orderResponse);
        if (orderResponse.getStatus().equalsIgnoreCase("error")) {
            log.error("conditions {}; orders {}", condition, orders);
            throw new RuntimeException(orderResponse.getMessage());
        }
        return orderResponse.getData().get("trigger_id");
    }

    public GttOrderResponse getGTTOrder(String id) throws IOException {
        Request request = new Request.Builder()
                .url("%s/oms/gtt/triggers/".formatted(baseUrl) + id)
                .method("GET", null)
                .headers(getBrokerHeaders())
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
            return GttOrderResponse.builder().status(GttOrderResponse.Status.getFromValue("cancelled")).build(); //as the order canceled it will not get from kite
        }
        log.debug("Order response is {}", orderResponse);
        if (orderResponse.get("status").textValue().equalsIgnoreCase("error")) {
            throw new RuntimeException(orderResponse.get("message").textValue());
        }
        //BrokerResponse<GttOrderResponse> orderResponse = objectMapper.readValue(myResult, new TypeReference<BrokerResponse<GttOrderResponse>>() { }); todo use full response
        GttOrderResponse.GttOrderResponseBuilder res = GttOrderResponse.builder()
                .status(GttOrderResponse.Status.getFromValue(orderResponse.get("data").get("status").textValue()))
                .lastUpdated(LocalDateTime.parse(orderResponse.get("data").get("updated_at").textValue(), formatter));
        GttOrder.GttType gttType = orderResponse.get("data").get("orders").size() > 1 ? GttOrder.GttType.TWO_LEG : GttOrder.GttType.SINGLE;
        res.gttType(gttType);
        if (orderResponse.get("data").get("status").textValue().equalsIgnoreCase("triggered")) {
            // check if two legged and check which one is triggered
            // chk which one is triggered
            int executedOrderIndex = 0;
            if (gttType == GttOrder.GttType.TWO_LEG &&
                    orderResponse.get("data").get("orders").get(0).get("result") != null) {
                // in condition triger value 1st is stoplloss, 2nd one is target same for orders array also.
                res.triggerType(GttOrderResponse.TriggerType.STOPLOSS);
                executedOrderIndex = 0;
            } else if (gttType == GttOrder.GttType.TWO_LEG &&
                    orderResponse.get("data").get("orders").get(1).get("result") != null) {
                res.triggerType(GttOrderResponse.TriggerType.TARGET);
                executedOrderIndex = 1;
            } else {
                // single executed
                res.triggerType(GttOrderResponse.TriggerType.TARGET);
                executedOrderIndex = 0;
            }
            String orderId = orderResponse.get("data").get("orders").get(executedOrderIndex).get("result").get("order_result").get("order_id").textValue();
            if (orderId != null && !orderId.isEmpty()) {
                if (getOrder(orderId).getData().getStatus() != Enums.Status.COMPLETE) {
                    log.debug("{} is not completed for GTT id {}", orderId, id);
                    res.status(GttOrderResponse.Status.getFromValue("cancelled"));
                }
            }
            var triggeredOn = LocalDateTime.parse(orderResponse.get("data").get("orders").get(executedOrderIndex).get("result").get("timestamp").toString(), formatter);
            res.triggerStatus(orderResponse.get("data").get("orders").get(executedOrderIndex).get("result").get("order_result").get("status").textValue())
                    .price(orderResponse.get("data").get("orders").get(executedOrderIndex).get("result").get("price").floatValue())
                    .quantity(orderResponse.get("data").get("orders").get(executedOrderIndex).get("result").get("quantity").intValue())
                    .triggeredOn(triggeredOn);
        }
        return res.build();
    }

    public void cancelGTTOrder(String id) throws IOException {
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url("%s/oms/gtt/triggers/".formatted(baseUrl) + id)
                .method("DELETE", body)
                .headers(getBrokerHeaders())
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

    public BrokerResponse<OrderResponse> getOrder(String orderId) throws IOException {
        Request request = new Request.Builder()
                .url("%s/oms/orders/".formatted(baseUrl) + orderId)
                .method("GET", null)
                .headers(getBrokerHeaders())
                .addHeader("authorization", "enctoken " + this.token)
                .build();
        Response response = client.newCall(request).execute();
        String myResult = response.body().string();
        response.close();
        //JsonNode orderResponse = objectMapper.readTree(myResult);
        //todo need buy date so use orderresponse
        //return Enums.Status.valueOf(orderResponse.get("data").get(orderResponse.get("data").size() - 1).get("status").asText());

        BrokerResponse<OrderResponse[]> orderResponses = objectMapper.readValue(myResult, new TypeReference<BrokerResponse<OrderResponse[]>>() {
        });
        if (orderResponses.getStatus().equalsIgnoreCase("error")) {
            log.error("response status {} response {}", orderResponses.getStatus(), myResult);
            return null;
        }
        OrderResponse[] orders = orderResponses.getData();
        OrderResponse order = orders[orders.length - 1];
        BrokerResponse<OrderResponse> orderResponse = new BrokerResponse();
        orderResponse.setStatus(orderResponses.getStatus());
        orderResponse.setMessage(orderResponses.getMessage());
        orderResponse.setData(order);
        return orderResponse;
    }
}
