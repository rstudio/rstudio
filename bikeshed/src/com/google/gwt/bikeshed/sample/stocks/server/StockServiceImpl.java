/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.bikeshed.sample.stocks.server;

import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.AbstractListModel.DefaultRange;
import com.google.gwt.bikeshed.sample.stocks.client.StockService;
import com.google.gwt.bikeshed.sample.stocks.shared.StockQuote;
import com.google.gwt.bikeshed.sample.stocks.shared.StockQuoteList;
import com.google.gwt.bikeshed.sample.stocks.shared.StockRequest;
import com.google.gwt.bikeshed.sample.stocks.shared.StockResponse;
import com.google.gwt.bikeshed.sample.stocks.shared.Transaction;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class StockServiceImpl extends RemoteServiceServlet implements
    StockService {

  /**
   * The result of a query to the remote service that provides stock quotes.
   */
  private static class Result {
    int numRows;
    StockQuoteList quotes;

    public Result(StockQuoteList quotes, int numRows) {
      this.quotes = quotes;
      this.numRows = numRows;
    }
  }

  static HashMap<String, String> companyNamesBySymbol = new HashMap<String, String>();

  static TreeSet<String> stockTickers = new TreeSet<String>();

  private static final Pattern DATA_PATTERN = Pattern.compile("\"([^\"]*)\"\\s*:\\s*\"([^\"]*)\"");

  private static final int MAX_RESULTS_TO_RETURN = 10000;

  private static final Pattern QUOTE_PATTERN = Pattern.compile("\\{[^\\}]*\\}");

  private static final HashMap<String,Pattern> sectorPatterns =
    new HashMap<String,Pattern>();

  private static final HashMap<String,String> sectorQueries =
    new HashMap<String,String>();
  
  static {
    int num = Stocks.SYMBOLS.size();
    for (int i = 0; i < num - 1; i += 2) {
      String symbol = Stocks.SYMBOLS.get(i);
      String companyName = Stocks.SYMBOLS.get(i + 1);
      stockTickers.add(symbol);

      companyNamesBySymbol.put(symbol, companyName);
    }
  }
  
  static {
    sectorQueries.put("DOW JONES INDUSTRIALS",
        "AA|AXP|BA|BAC|CAT|CSCO|CVX|DD|DIS|GE|HD|HPQ|IBM|INTC|JNJ|JPM|KFT|KO|" +
        "MCD|MMM|MRK|MSFT|PFE|PG|T|TRV|UTX|VZ|WMT|XOM");
    sectorQueries.put("S&P 500",
        "A|AA|AAPL|ABC|ABT|ACS|ADBE|ADI|ADM|ADP|ADSK|AEE|AEP|AES|AET|AFL|AGN|" +
        "AIG|AIV|AIZ|AKAM|AKS|ALL|ALTR|AMAT|AMD|AMGN|AMP|AMT|AMZN|AN|ANF|AON|" +
        "APA|APC|APD|APOL|ASH|ATI|AVB|AVP|AVY|AXP|AYE|AZO|BA|BAC|BAX|BBBY|" +
        "BBT|BBY|BCR|BDK|BDX|BEN|BF.B|BHI|BIG|BIIB|BJS|BK|BLL|BMC|BMS|BMY|" +
        "BRCM|BRK.B|BSX|BTU|BXP|C|CA|CAG|CAH|CAM|CAT|CB|CBE|CBG|CBS|CCE|CCL|" +
        "CEG|CELG|CEPH|CF|CFN|CHK|CHRW|CI|CINF|CL|CLF|CLX|CMA|CMCSA|CME|CMI|" +
        "CMS|CNP|CNX|COF|COG|COH|COL|COP|COST|CPB|CPWR|CRM|CSC|CSCO|CSX|CTAS|" +
        "CTL|CTSH|CTXS|CVH|CVS|CVX|D|DD|DE|DELL|DF|DFS|DGX|DHI|DHR|DIS|DNB|" +
        "DNR|DO|DOV|DOW|DPS|DRI|DTE|DTV|DUK|DV|DVA|DVN|EBAY|ECL|ED|EFX|EIX|" +
        "EK|EL|EMC|EMN|EMR|EOG|EP|EQR|EQT|ERTS|ESRX|ETFC|ETN|ETR|EXC|EXPD|" +
        "EXPE|F|FAST|FCX|FDO|FDX|FE|FHN|FII|FIS|FISV|FITB|FLIR|FLR|FLS|FMC|" + 
        "FO|FPL|FRX|FSLR|FTI|FTR|GAS|GCI|GD|GE|GENZ|GILD|GIS|GLW|GME|GNW|" +
        "GOOG|GPC|GPS|GR|GS|GT|GWW|HAL|HAR|HAS|HBAN|HCBK|HCN|HCP|HD|HES|HIG|" +
        "HNZ|HOG|HON|HOT|HPQ|HRB|HRL|HRS|HSP|HST|HSY|HUM|IBM|ICE|IFF|IGT|" +
        "INTC|INTU|IP|IPG|IRM|ISRG|ITT|ITW|IVZ|JBL|JCI|JCP|JDSU|JEC|JNJ|JNPR|" +
        "JNS|JPM|JWN|K|KEY|KFT|KG|KIM|KLAC|KMB|KO|KR|KSS|L|LEG|LEN|LH|LIFE|" +
        "LLL|LLTC|LLY|LM|LMT|LNC|LO|LOW|LSI|LTD|LUK|LUV|LXK|M|MA|MAR|MAS|MAT|" +
        "MCD|MCHP|MCK|MCO|MDP|MDT|MEE|MET|MFE|MHP|MHS|MI|MIL|MJN|MKC|MMC|MMM|" +
        "MO|MOLX|MON|MOT|MRK|MRO|MS|MSFT|MTB|MU|MUR|MWV|MWW|MXB|MYL|NBL|NBR|" + 
        "NDAQ|NEM|NI|NKE|NOC|NOV|NOVL|NSC|NSM|NTAP|NTRS|NU|NUE|NVDA|NVLS|NWL|" +
        "NWSA|NYT|NYX|ODP|OI|OMC|ORCL|ORLY|OXY|PAYX|PBCT|PBG|PBI|PCAR|PCG|" +
        "PCL|PCP|PCS|PDCO|PEG|PEP|PFE|PFG|PG|PGN|PGR|PH|PHM|PKI|PLD|PLL|PM|" +
        "PNC|PNW|POM|PPG|PPL|PRU|PSA|PTV|PWR|PX|PXD|Q|QCOM|QLGC|R|RAI|RDC|RF|" +
        "RHI|RHT|RL|ROK|ROP|ROST|RRC|RRD|RSG|RSH|RTN|RX|S|SAI|SBUX|SCG|SCHW|" +
        "SE|SEE|SHLD|SHW|SIAL|SII|SJM|SLB|SLE|SLM|SNA|SNDK|SNI|SO|SPG|SPLS|" +
        "SRCL|SRE|STI|STJ|STR|STT|STZ|SUN|SVU|SWK|SWN|SWY|SYK|SYMC|SYY|T|TAP|" +
        "TDC|TE|TEG|TER|TGT|THC|TIE|TIF|TJX|TLAB|TMK|TMO|TROW|TRV|TSN|TSO|" +
        "TSS|TWC|TWX|TXN|TXT|UNH|UNM|UNP|UPS|USB|UTX|V|VAR|VFC|VIAb|VLO|VMC|" +
        "VNO|VRSN|VTR|VZ|WAG|WAT|WDC|WEC|WFC|WFMI|WFR|WHR|WIN|WLP|WM|WMB|WMT|" +
        "WPI|WPO|WU|WY|WYN|WYNN|X|XEL|XL|XLNX|XOM|XRAY|XRX|XTO|YHOO|YUM|ZION|" +
        "ZMH");
    
    // Precompile each regex
    for (Map.Entry<String,String> entry : sectorQueries.entrySet()) {
      sectorPatterns.put(entry.getKey(), compile(entry.getValue()));
    }
  }

  private static Pattern compile(String query) {
    try {
      return Pattern.compile(query);
    } catch (PatternSyntaxException e) {
      return null;
    }
  }

  /**
   * A mapping of usernames to {@link PlayerStatus}.
   */
  private Map<String, PlayerStatus> players = new HashMap<String, PlayerStatus>();
  
  public StockResponse addFavorite(String ticker, Range favoritesRange) {
    PlayerStatus player = ensurePlayer();
    player.addFavorite(ticker);
    Result favorites = query(player.getFavoritesQuery(), player.getFavoritesPattern(), favoritesRange);
    return new StockResponse(null, favorites.quotes, null,
        0, favorites.numRows, 0, player.getCash());
  }

  public Result getSectorQuotes(String sector, Range sectorRange) {
    sector = sector.toUpperCase();
    String sectorQuery = sectorQueries.get(sector);
    Pattern sectorPattern = sectorPatterns.get(sector);
    if (sectorQuery == null) {
      return null;
    }
    return query(sectorQuery, sectorPattern, sectorRange);
  }

  public StockResponse getStockQuotes(StockRequest request)
      throws IllegalArgumentException {

    String query = request.getSearchQuery();
    if (query == null || query.length() == 0) {
      query = ".*";
    }
    Range searchRange = request.getSearchRange();
    Range favoritesRange = request.getFavoritesRange();
    Range sectorRange = request.getSectorRange();
    
    PlayerStatus player = ensurePlayer();
    Result searchResults = query(query, compile(query), searchRange);
    Result favorites = query(player.getFavoritesQuery(), player.getFavoritesPattern(), favoritesRange);
    Result sector = sectorRange != null ? getSectorQuotes(request.getSector(), sectorRange) : null;

    return new StockResponse(searchResults.quotes,
        favorites.quotes,
        sector != null ? sector.quotes : null,
        searchResults.numRows,
        favorites.numRows,
        sector != null ? sector.numRows : 0,
        player.getCash());
  }
  
  public StockResponse removeFavorite(String ticker, Range favoritesRange) {
    PlayerStatus player = ensurePlayer();
    player.removeFavorite(ticker);
    Result favorites = query(player.getFavoritesQuery(), player.getFavoritesPattern(), favoritesRange);
    return new StockResponse(null, favorites.quotes, null,
        0, favorites.numRows, 0, player.getCash());
  }

  public Transaction transact(Transaction transaction)
      throws IllegalArgumentException {
    // Get the current stock price.
    String ticker = transaction.getTicker();
    Pattern tickerPattern = compile(ticker);
    if (ticker == null || ticker.length() < 0) {
      throw new IllegalArgumentException("Stock could not be found");
    }
    Result result = query(ticker, tickerPattern, new DefaultRange(0, 1));
    if (result.numRows != 1 || result.quotes.size() != 1) {
      throw new IllegalArgumentException("Could not resolve stock ticker");
    }
    StockQuote quote = result.quotes.get(0);
  
    // Perform the transaction with the user.
    int quantity = transaction.getQuantity();
    int price = quote.getPrice();
    if (transaction.isBuy()) {
      ensurePlayer().buy(ticker, quantity, price);
    } else {
      ensurePlayer().sell(ticker, quantity, price);
    }
  
    return new Transaction(true, ticker, quantity, price);
  }

  /**
   * Ensure that a {@link PlayerStatus} for the current player exists and return
   * it.
   * 
   * @return the {@link PlayerStatus} for the current player
   */
  private PlayerStatus ensurePlayer() {
    String userId = "I Am the User";
    PlayerStatus player = players.get(userId);
    if (player == null) {
      player = new PlayerStatus();
      players.put(userId, player);
    }
    return player;
  }

  private List<String> getTickers(String query, Pattern pattern) {
    Set<String> tickers = new TreeSet<String>();
    if (query.length() > 0) {
      query = query.toUpperCase();
      
      int count = 0;
      for (String ticker : stockTickers) {
        if (ticker.startsWith(query) || (pattern != null && match(ticker, pattern))) {
          tickers.add(ticker);
          count++;
          if (count > MAX_RESULTS_TO_RETURN) {
            break;
          }
        }
      }
      
      if (pattern != null) {
        for (Map.Entry<String,String> entry : companyNamesBySymbol.entrySet()) {
          if (match(entry.getValue(), pattern)) {
            tickers.add(entry.getKey());
            count++;
            if (count > MAX_RESULTS_TO_RETURN) {
              break;
            }
          }
        }
      }
    }
      
    return new ArrayList<String>(tickers);
  }

  private boolean match(String symbol, Pattern pattern) {
    Matcher m = pattern.matcher(symbol);
    return m.matches();
  }

  /**
   * Query the remote service to retrieve current stock prices.
   * 
   * @param query the query string
   * @param range the range of results requested
   * @return the stock quotes
   */
  private Result query(String query, Pattern queryPattern, Range range) {
    // Get all symbols for the query.
    PlayerStatus player = ensurePlayer();
    List<String> symbols = getTickers(query, queryPattern);

    if (symbols.size() == 0) {
      return new Result(new StockQuoteList(0), 0);
    }

    int start = range.getStart();
    int end = Math.min(start + range.getLength(), symbols.size());

    // Get the symbols that are in range.
    Set<String> symbolsInRange = new HashSet<String>();
    if (end > start) {
      symbolsInRange.addAll(symbols.subList(start, end));
    }

    // Build the URL string.
    StringBuilder sb = new StringBuilder(
        "http://www.google.com/finance/info?client=ig&q=");
    boolean first = true;
    for (String symbol : symbolsInRange) {
      if (!first) {
        sb.append(',');
      }
      sb.append(symbol);
      first = false;
    }

    if (first) {
      // No symbols
      return new Result(new StockQuoteList(0), 0);
    }

    // Send the request.
    String content = "";
    try {
      String urlString = sb.toString();
      URL url = new URL(urlString);
      InputStream urlInputStream = url.openStream();
      Scanner contentScanner = new Scanner(urlInputStream, "UTF-8");
      if (contentScanner.hasNextLine()) {
        // See
        // http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
        content = contentScanner.useDelimiter("\\A").next();
      }

      // System.out.println(content);
    } catch (MalformedURLException mue) {
      System.err.println(mue);
    } catch (IOException ioe) {
      System.err.println(ioe);
    }

    // Parse response.
    Map<String, StockQuote> priceMap = new HashMap<String, StockQuote>();
    Matcher matcher = QUOTE_PATTERN.matcher(content);
    while (matcher.find()) {
      String group = matcher.group();

      String symbol = null;
      String price = null;

      Matcher dataMatcher = DATA_PATTERN.matcher(group);
      while (dataMatcher.find()) {
        String tag = dataMatcher.group(1);
        String data = dataMatcher.group(2);
        if (tag.equals("t")) {
          symbol = data;
        } else if (tag.equals("l_cur")) {
          price = data;
        }
      }

      if (symbol != null && price != null) {
        int iprice = 0;
        try {
          iprice = (int) (Double.parseDouble(price) * 100);
          String name = companyNamesBySymbol.get(symbol);
          Integer sharesOwned = player.getSharesOwned(symbol);
          boolean favorite = player.isFavorite(symbol);
          priceMap.put(symbol, new StockQuote(symbol, name, iprice,
              sharesOwned == null ? 0 : sharesOwned.intValue(), favorite));
        } catch (NumberFormatException e) {
          System.out.println("Bad price " + price + " for symbol " + symbol);
        }
      }
    }

    // Convert the price map to a StockQuoteList.
    StockQuoteList toRet = new StockQuoteList(start);
    for (int i = start; i < end; i++) {
      String symbol = symbols.get(i);
      StockQuote quote = priceMap.get(symbol);
      if (quote == null) {
        System.out.println("Bad symbol " + symbol);
      } else {
        toRet.add(quote);
      }
    }

    return new Result(toRet, symbols.size());
  }
}
