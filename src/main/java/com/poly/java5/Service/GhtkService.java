package com.poly.java5.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GhtkService {

    @Value("${ghtk.api.token}")
    private String ghtkToken;

    private final String GHTK_FEE_URL = "https://services.giaohangtietkiem.vn/services/shipment/fee";

    private String normalizeAddress(String str) {
        if (str == null) return "";
        return str.replace("Thành phố ", "")
                  .replace("TP. ", "")
                  .replace("Tỉnh ", "")
                  .replace("Quận ", "")
                  .replace("Huyện ", "")
                  .replace("Thị xã ", "")
                  .replace("Phường ", "")
                  .replace("Xã ", "")
                  .replace("Thị trấn ", "")
                  .trim();
    }

    public Double calculateShippingFee(String province, String district,String ward, Integer weight, Double value) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", ghtkToken);
            headers.set("X-Client-Source", "Bibliora");

            String cleanPickProvince = normalizeAddress("Cần Thơ");
            String cleanPickDistrict = normalizeAddress("Phường Cái Răng");
            String cleanPickWard = normalizeAddress("Phường Cái Răng");
            
            String cleanProvince = normalizeAddress(province);
            String cleanDistrict = normalizeAddress(district);
            String cleanWard = normalizeAddress(ward);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GHTK_FEE_URL)
                    .queryParam("pick_province", cleanPickProvince)
                    .queryParam("pick_district", cleanPickDistrict)
                    .queryParam("pick_ward", cleanPickWard)
                    .queryParam("province", cleanProvince)
                    .queryParam("district", cleanDistrict)
                    .queryParam("ward", cleanWard)
                    .queryParam("weight", weight)
                    .queryParam("value", 0)
                    .queryParam("deliver_option", "none");
			
			// 🌟 IN URL RA CONSOLE ĐỂ DEBUG LỖI 31K
            String finalUrl = builder.build().encode().toUri().toString();
            log.info("====== DEBUG GHTK URL ======");
            log.info(finalUrl);
            log.info("============================");

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    builder.build().encode().toUri(),
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            log.info("====== GHTK API RESPONSE ======");
            log.info("Response body: {}", body);
            log.info("===============================");
            if (body != null) {
                if (Boolean.TRUE.equals(body.get("success"))) {
                    Map<String, Object> feeInfo = (Map<String, Object>) body.get("fee");
                    if (feeInfo != null && feeInfo.get("fee") != null) {
                        return Double.valueOf(feeInfo.get("fee").toString());
                    }
                } else {
                    String ghtkMessage = body.containsKey("message") ? body.get("message").toString() : "Dữ liệu không hợp lệ";
                    throw new RuntimeException("Lỗi từ GHTK: " + ghtkMessage);
                }
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            log.error("GHTK API error: ", e);
            throw new RuntimeException("Lỗi kết nối GHTK: " + e.getMessage());
        }
        throw new RuntimeException("GHTK API không phản hồi.");
    }
}
