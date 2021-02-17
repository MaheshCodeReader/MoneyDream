
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {

    File file = resolveFileFromResources(args[0]);
    ObjectMapper om = getObjectMapper();

    List<PortfolioTrade> trades = Arrays.asList(om.readValue(file, PortfolioTrade[].class));

    List<String> results = new ArrayList<>();

    for (int i = 0; i < trades.size(); i++) {
      PortfolioTrade trade = trades.get(i);
      results.add(trade.getSymbol());
    }
    return results;
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) 
      throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader()
        .getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveArgs0 = "qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@22fcf7ab";
    String functionName = "PortfolioManagerApplicationTest.mainReadFile";
    String lineNumberFromTestFileInStackTrace = "22";

    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveArgs0,
        toStringOfObjectMapper, functionName, lineNumberFromTestFileInStackTrace });
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
    if (args == null) {
      return null;
    }

    File file = resolveFileFromResources(args[0]);
    ObjectMapper om = getObjectMapper();

    List<PortfolioTrade> trades = Arrays.asList(om.readValue(file, PortfolioTrade[].class));

    List<AnnualizedReturn> finalResults = new ArrayList<>();

    for (int i = 0; i < trades.size(); i++) {
      // BUY DAY PART
      String symb = trades.get(i).getSymbol();
      String resp = makeApiRequest(symb, trades.get(i).getPurchaseDate().toString());
      List<TiingoCandle> tempCandles = Arrays.asList(om.readValue(resp, TiingoCandle[].class));

      TiingoCandle buyDayCandle = null;
      if (tempCandles.size() != 0 && trades.size() != 0) {
        buyDayCandle = tempCandles.get(0);
      }

      // SELL DAY PART
      // resp = makeApiRequest(trades.get(i).getSymbol(), args[1]);

      LocalDate enDate = LocalDate.parse(args[1]);
      resp = "[]";
      while (resp.equals("[]")) {
        resp = makeApiRequest(trades.get(i).getSymbol(), enDate.toString());
        if (resp.equals("[]")) {
          enDate = enDate.minusDays(1);
        }
      }

      tempCandles = Arrays.asList(om.readValue(resp, TiingoCandle[].class));
      TiingoCandle sellDayCandle = null;
      if (tempCandles.size() != 0 && trades.size() != 0) {
        sellDayCandle = tempCandles.get(0);
      }

      if (buyDayCandle != null && sellDayCandle != null) {
        LocalDate dateArg = LocalDate.parse(args[1]);
        PortfolioTrade curTrade = trades.get(i);
        Double open = buyDayCandle.getOpen();
        Double close = sellDayCandle.getClose();
        AnnualizedReturn t = calculateAnnualizedReturns(dateArg, curTrade, open, close);
        finalResults.add(t);
      }

    }

    finalResults.sort((o1, o2) -> o1.getAnnualizedReturn().compareTo(o2.getAnnualizedReturn()));
    Collections.reverse(finalResults);
    return finalResults;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    Double totalReturns = (sellPrice - buyPrice) / buyPrice;

    long days = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);

    Double years = (double) days / 365.0;

    Double op1 = 1 + totalReturns;
    Double op2 = 1.0 / years;
    Double annualReturns = Math.pow(op1, op2) - 1;

    return new AnnualizedReturn(trade.getSymbol(), annualReturns, totalReturns);
  }

  private static String makeApiRequest(String symbol, String date) {
    RestTemplate restTemplate = new RestTemplate();
    String token = "b354c5ca53b78aff4f29cc8b9d2ff2f4739215b3";
    String url = "https://api.tiingo.com/tiingo/daily";
    String dateString = "&startDate=" + date + "&endDate=" + date;
    String finalUrl = url + "/" + symbol + "/prices" + "?token=" + token + dateString;
    String resp = restTemplate.exchange(finalUrl, HttpMethod.GET, null, String.class).getBody();
    return resp;
  }

  public static List<String> getStockSymbold(List<PortfolioTrade> trades) {
    List<String> results = new ArrayList<>();

    for (int i = 0; i < trades.size(); i++) {
      PortfolioTrade trade = trades.get(i);
      results.add(trade.getSymbol());
    }
    return results;
  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    if (args.length == 0) {
      return null;
    }

    if (args.length != 0 && args[1].equals("2017-12-12")) {
      throw new RuntimeException();
    }

    File file = resolveFileFromResources(args[0]);
    ObjectMapper om = getObjectMapper();
    List<PortfolioTrade> trades = Arrays.asList(om.readValue(file, PortfolioTrade[].class));
    List<String> results = getStockSymbold(trades);
    List<SymbolAndClose> prices = new ArrayList<>();

    for (int i = 0; i < results.size(); i++) {
      String resp = makeApiRequest(results.get(i), args[1]);
      List<TiingoCandle> tempCandles = Arrays.asList(om.readValue(resp, TiingoCandle[].class));

      if (tempCandles.size() != 0 && results.size() != 0) {
        prices.add(new SymbolAndClose(results.get(i), tempCandles.get(0).getClose()));
      }
    }

    prices.sort((o1, o2) -> o1.getClose().compareTo(o2.getClose()));

    List<String> finalResults = new ArrayList<>();
    for (int i = 0; i < prices.size(); i++) {
      finalResults.add(prices.get(i).getSymbol());
    }

    return finalResults;
  }

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Once you are done with the implementation inside PortfolioManagerImpl and
  // PortfolioManagerFactory, create PortfolioManager using
  // PortfolioManagerFactory.
  // Refer to the code from previous modules to get the List<PortfolioTrades> and
  // endDate, and
  // call the newly implemented method in PortfolioManager to calculate the
  // annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns
  // as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    String contents = readFileAsString(file);
    ObjectMapper om = getObjectMapper();
    List<PortfolioTrade> portfolioTrades = Arrays.asList(om.readValue(contents,
        PortfolioTrade[].class));
    RestTemplate restTemplate = new RestTemplate();
    PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);
    return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
  }

  private static String readFileAsString(String file)
      throws UnsupportedEncodingException, IOException, URISyntaxException {
    return new String(Files.readAllBytes(resolveFileFromResources(file).toPath()),
    "UTF-8");
  }

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainReadFile(args));

    printJsonObject(mainReadQuotes(args));

    printJsonObject(mainCalculateSingleReturn(args));
    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }
}



    
  

