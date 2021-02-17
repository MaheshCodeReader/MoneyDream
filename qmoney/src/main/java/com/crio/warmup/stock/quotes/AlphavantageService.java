
package com.crio.warmup.stock.quotes;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

  RestTemplate restTemplate;

  protected AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
    throws JsonProcessingException, StockQuoteServiceException, RuntimeException {
    // TODO Auto-generated method stub

    if(to == null) {
      to = LocalDate.now();
    }

    String finalUrl = buildUri(symbol, from, to);
    AlphavantageDailyResponse entireResponse1 = this.restTemplate.getForObject(finalUrl, AlphavantageDailyResponse.class);
    System.out.println(entireResponse1 == null);
    
    RestTemplate arrestTemplate = new RestTemplate();
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    
    try {
      String resp1 = arrestTemplate.exchange(finalUrl, HttpMethod.GET, null, String.class).getBody();
      AlphavantageDailyResponse entireResponse2 = om.readValue(resp1, AlphavantageDailyResponse.class);
      
      List<Candle> relevantCandles = new ArrayList<>();

      

      for(LocalDate date = from; !date.isEqual(to); date = date.plusDays(1)) {
        AlphavantageCandle temp = entireResponse2.getCandles().get(date);
        if(temp != null) {
          temp.setDate(date);
          relevantCandles.add(temp);
        }
      }

      AlphavantageCandle lastDayCandle = entireResponse2.getCandles().get(to);
      if(lastDayCandle != null) {
        lastDayCandle.setDate(to);
        relevantCandles.add(lastDayCandle);
      }

      return relevantCandles;
    } catch(NullPointerException e) {
      throw new StockQuoteServiceException("Alphavantage returned invalid response");
    }
  }




  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    
    String apikey = "V8XYX6K8JPNZY41B";
    String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&outputsize=full";
    String finalUrl = url + "&symbol=" + symbol + "&apikey=" + apikey;
      
    return finalUrl;
  }
  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //   1. Update the method signature to match the signature change in the interface.
  //   2. Start throwing new StockQuoteServiceException when you get some invalid response from
  //      Alphavantage, or you encounter a runtime exception during Json parsing.
  //   3. Make sure that the exception propagates all the way from PortfolioManager, so that the
  //      external user's of our API are able to explicitly handle this exception upfront.
  //CHECKSTYLE:OFF

}

