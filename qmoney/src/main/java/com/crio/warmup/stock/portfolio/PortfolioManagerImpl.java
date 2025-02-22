
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import com.crio.warmup.stock.PortfolioManagerApplication;
import com.crio.warmup.stock.SortByAnnualizedReturns;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Collections;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {



  private StockQuotesService service;
  private RestTemplate restTemplate;
  private Future<AnnualizedReturn> submit;


  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
  protected PortfolioManagerImpl(StockQuotesService service) {
    this.service = service;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF




  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws StockQuoteServiceException
 {
        //List<Candle> tiingoCandles = new ArrayList<>();
        //String url = buildUri(symbol, from, to);
        //TiingoCandle[] tiingoCandle = restTemplate.getForObject(url, TiingoCandle[].class);
        //for(TiingoCandle candle : tiingoCandle){
          //tiingoCandles.add(candle);
        //}
        List<Candle> stocks = new ArrayList<>();
        try{
        stocks = service.getStockQuote(symbol, from, to);
        }catch(JsonProcessingException e){
          System.out.println("Error in Json parsing"+ e);
        }
        return stocks;
        
  }

  // protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
  //      String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
  //           + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
  //      String modifiedUrl = uriTemplate.replace("$SYMBOL",symbol).replace("$STARTDATE",startDate.toString()).replace("$ENDDATE",endDate.toString()).replace("$APIKEY",PortfolioManagerApplication.getToken());
  //   return modifiedUrl;
  // }



  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) throws StockQuoteServiceException {
    // TODO Auto-generated method stub
    List<AnnualizedReturn> annualReturnsOfAllTokens = new ArrayList<>();
    

    for(PortfolioTrade trade : portfolioTrades){
      // LocalDate endDate = stringToLocalDate(args[1]);
      //List<Candle> candles = PortfolioManagerApplication.fetchCandles(trade, endDate, PortfolioManagerApplication.getToken());
      
      List<Candle> candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
      double sellPrice = candles.get(candles.size()-1).getClose();
      double buyPrice =candles.get(0).getOpen();
      double totalReturns = (sellPrice - buyPrice)/buyPrice;
      double num_of_years = trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS)/365.24;
      double annualizedReturns = Math.pow(1+totalReturns, 1/num_of_years)-1;
      annualReturnsOfAllTokens.add(new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns));
    }
    Collections.sort(annualReturnsOfAllTokens,getComparator());
    return annualReturnsOfAllTokens;
  }
  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
      throws InterruptedException, StockQuoteServiceException {
    // TODO Auto-generated method stub
    List<AnnualizedReturn> userAnnualizedReturns = new ArrayList<>();
    List<Future<AnnualizedReturn>> responses = new ArrayList<>();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);

    for(PortfolioTrade trade : portfolioTrades){
      Future<AnnualizedReturn> future = executor.submit(()->{return this.calculateAnnualizedReturn(trade, endDate);});
      responses.add(future);
    }
    for(Future<AnnualizedReturn> response : responses){
      try {
          userAnnualizedReturns.add(response.get());
        } catch (ExecutionException e) {
          throw new StockQuoteServiceException("Failed to get Data from Service Provider");
        }
    }
    Collections.sort(userAnnualizedReturns,getComparator());
    executor.shutdown();
    return userAnnualizedReturns;

    // List<Future<AnnualizedReturn>> responses = portfolioTrades.stream()
    // .map(trade->executor.submit(()->this.calculateAnnualizedReturn(trade, endDate)))
    // .collect(Collectors.toList());
    //   List<AnnualizedReturn> userAnnualizedReturns = responses.stream()
    //   .map(response -> {
    //     try {
    //       return response.get();
    //     } catch (InterruptedException e) {
    //       e.printStackTrace();
    //     } catch (ExecutionException e) {
    //       e.printStackTrace();
    //     } catch(NullPointerException e){
    //       e.printStackTrace();
    //     }
    //     return null;
    //   })
    //   .collect(Collectors.toList());
      // if(userAnnualizedReturns == null){
      //   throw new StockQuoteServiceException("Unknown error occured while getting data");
      // }
      // Collections.sort(userAnnualizedReturns,getComparator());
      // executor.shutdown();
      // return userAnnualizedReturns;
  }

  public AnnualizedReturn calculateAnnualizedReturn(PortfolioTrade trade,LocalDate endDate) throws StockQuoteServiceException{
    List<Candle> candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
        double sellPrice = candles.get(candles.size()-1).getClose();
        double buyPrice =candles.get(0).getOpen();
        double totalReturns = (sellPrice - buyPrice)/buyPrice;
        double num_of_years = trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS)/365.24;
        double annualizedReturns = Math.pow(1+totalReturns, 1/num_of_years)-1;
      return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
  }


  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Modify the function #getStockQuote and start delegating to calls to
  //  stockQuoteService provided via newly added constructor of the class.
  //  You also have a liberty to completely get rid of that function itself, however, make sure
  //  that you do not delete the #getStockQuote function.

}
