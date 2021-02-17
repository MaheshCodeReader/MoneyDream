
package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.quotes.StockQuoteServiceFactory;
import com.crio.warmup.stock.quotes.StockQuotesService;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerFactory {

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Implement the method to return new instance of PortfolioManager.
  //  Remember, pass along the RestTemplate argument that is provided to the new instance.

  public static PortfolioManager getPortfolioManager(RestTemplate restTemplate) {
    return new PortfolioManagerImpl(restTemplate);
  }


  public static PortfolioManager getPortfolioManager(String provider,
     RestTemplate restTemplate) {
      StockQuotesService service = StockQuoteServiceFactory.INSTANCE.getService(provider, restTemplate);
      
    return new PortfolioManagerImpl(service);
  }

}