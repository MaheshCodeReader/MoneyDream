
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {
  RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) 
    throws JsonProcessingException, StockQuoteServiceException {
    // TODO Auto-generated method stub

    if(to == null) {
      to = LocalDate.now();
    }

    String token = "b354c5ca53b78aff4f29cc8b9d2ff2f4739215b3";
    String url = "https://api.tiingo.com/tiingo/daily";
    String dateString = "&startDate=" + from + "&endDate=" + to;
    String finalUrl = url + "/" + symbol + "/prices" + "?token=" + token + dateString;
    TiingoCandle[] dummyCandles = this.restTemplate.getForObject(finalUrl, TiingoCandle[].class);
    System.out.println(dummyCandles == null);
    RestTemplate arrestTemplate = new RestTemplate();
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    
    try {
      String resp1 = arrestTemplate.exchange(finalUrl, HttpMethod.GET, null, String.class).getBody();
      List<Candle> tempCandles = Arrays.asList(om.readValue(resp1, TiingoCandle[].class));
      return tempCandles;
    } catch (NullPointerException e) {
      throw new StockQuoteServiceException(
        "Error occurred while requesting response from Tiingo api",
        e.getCause());
    }
  }



  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    
    
    String token = "b354c5ca53b78aff4f29cc8b9d2ff2f4739215b3";
    String url = "https://api.tiingo.com/tiingo/daily";
    String dateString = "&startDate=" + startDate + "&endDate=" + endDate;
    String finalUrl = url + "/" + symbol + "/prices" + "?token=" + token + dateString;   
    return finalUrl;
  }





  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //  1. Update the method signature to match the signature change in the interface.
  //     Start throwing new StockQuoteServiceException when you get some invalid response from
  //     Tiingo, or if Tiingo returns empty results for whatever reason, or you encounter
  //     a runtime exception during Json parsing.
  //  2. Make sure that the exception propagates all the way from
  //     PortfolioManager#calculateAnnualisedReturns so that the external user's of our API
  //     are able to explicitly handle this exception upfront.


}
