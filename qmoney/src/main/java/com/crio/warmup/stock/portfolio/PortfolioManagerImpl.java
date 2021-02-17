
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuoteServiceFactory;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


  RestTemplate restTemplate;
  StockQuotesService stockQuotesService;

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  protected PortfolioManagerImpl(StockQuotesService service) {
    this.stockQuotesService = service;
  }


  //CHECKSTYLE:OFF




  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {

    if(to == null) {
      to = LocalDate.now();
    }
    List<Candle> resp = null;
    try {
      this.stockQuotesService = StockQuoteServiceFactory.INSTANCE.getService("tiingo", restTemplate);
      resp = this.stockQuotesService.getStockQuote(symbol, from, to);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    } catch (StockQuoteServiceException e) {
      e.printStackTrace();
    }
    return resp;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> trades,
      LocalDate endDate) {
    
    if (trades == null) {
      return null;
    }

    if (endDate == null || endDate.toString().equals("")) {
      endDate = LocalDate.now();
    }

    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    // List<PortfolioTrade> trades = Arrays.asList(om.readValue(file, PortfolioTrade[].class));
    this.stockQuotesService = StockQuoteServiceFactory.INSTANCE.getService("tiingo", restTemplate);

    List<AnnualizedReturn> finalResults = new ArrayList<>();

    for (int i = 0; i < trades.size(); i++) {
      List<Candle> resp2 = null;
      try {
        
        resp2 = this.stockQuotesService.getStockQuote(trades.get(i).getSymbol(), 
        trades.get(i).getPurchaseDate(), endDate);
        
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      } catch (StockQuoteServiceException e) {
        e.printStackTrace();
      }

      if(resp2 != null) {
        LocalDate dateArg = resp2.get(resp2.size() - 1).getDate();
        PortfolioTrade curTrade = trades.get(i);
        Double open = resp2.get(0).getOpen();
        Double close = resp2.get(resp2.size() - 1).getClose();
        AnnualizedReturn t = getAnnualizedReturns(dateArg, curTrade, open, close);
        finalResults.add(t);
      }
      
    }

    finalResults.sort((o1, o2) -> o1.getAnnualizedReturn().compareTo(o2.getAnnualizedReturn()));
    Collections.reverse(finalResults);
    return finalResults;
  }


  public static AnnualizedReturn getAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {
    Double totalReturns = (sellPrice - buyPrice) / buyPrice;

    long days = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);

    Double years = (double) days / 365.0;

    Double op1 = 1 + totalReturns;
    Double op2 = 1.0 / years;
    Double annualReturns = Math.pow(op1, op2) - 1;

    return new AnnualizedReturn(trade.getSymbol(), annualReturns, totalReturns);
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate, int numThreads) throws InterruptedException, StockQuoteServiceException {
    // TODO Auto-generated method stub
    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
    List<Future<AnnualizedReturn>> futureReturnsList = new ArrayList<Future<AnnualizedReturn>>();
    final ExecutorService pool = Executors.newFixedThreadPool(numThreads);
    for (int i = 0; i < portfolioTrades.size(); i++) {
      PortfolioTrade trade = portfolioTrades.get(i);
      Callable<AnnualizedReturn> callableTask = () -> {
        return getAnnualizedReturn(trade, endDate);
      };
      Future<AnnualizedReturn> futureReturns = pool.submit(callableTask);
      futureReturnsList.add(futureReturns);
    }
  
    for (int i = 0; i < portfolioTrades.size(); i++) {
      Future<AnnualizedReturn> futureReturns = futureReturnsList.get(i);
      try {
        AnnualizedReturn returns = futureReturns.get();
        annualizedReturns.add(returns);
      } catch (ExecutionException e) {
        throw new StockQuoteServiceException("Error when calling the API", e);
  
      }
    }
    Collections.sort(annualizedReturns, Collections.reverseOrder());
    return annualizedReturns;
  }
  

  public AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade, LocalDate endDate)
    throws StockQuoteServiceException {
  LocalDate startDate = trade.getPurchaseDate();
  String symbol = trade.getSymbol(); 
 
  Double buyPrice = 0.0, sellPrice = 0.0;
 
  try {
    LocalDate startLocalDate = trade.getPurchaseDate();
    List<Candle> stocksStartToEndFull = getStockQuote(symbol, startLocalDate, endDate);
 
    Collections.sort(stocksStartToEndFull, (candle1, candle2) -> { 
      return candle1.getDate().compareTo(candle2.getDate()); 
    });
    
    Candle stockStartDate = stocksStartToEndFull.get(0);
    Candle stocksLatest = stocksStartToEndFull.get(stocksStartToEndFull.size() - 1);
 
    buyPrice = stockStartDate.getOpen();
    sellPrice = stocksLatest.getClose();
    endDate = stocksLatest.getDate();
 
  } catch (JsonProcessingException e) {
    throw new RuntimeException();
  }
  Double totalReturn = (sellPrice - buyPrice) / buyPrice;
 
  long daysBetweenPurchaseAndSelling = ChronoUnit.DAYS.between(startDate, endDate);
  Double totalYears = (double) (daysBetweenPurchaseAndSelling) / 365;
 
  Double annualizedReturn = Math.pow((1 + totalReturn), (1 / totalYears)) - 1;
  return new AnnualizedReturn(symbol, annualizedReturn, totalReturn);
 
}

}
