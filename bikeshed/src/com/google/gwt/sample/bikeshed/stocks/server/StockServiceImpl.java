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
package com.google.gwt.sample.bikeshed.stocks.server;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.AbstractListModel.DefaultRange;
import com.google.gwt.sample.bikeshed.stocks.client.StockService;
import com.google.gwt.sample.bikeshed.stocks.shared.PlayerInfo;
import com.google.gwt.sample.bikeshed.stocks.shared.StockQuote;
import com.google.gwt.sample.bikeshed.stocks.shared.StockQuoteList;
import com.google.gwt.sample.bikeshed.stocks.shared.StockRequest;
import com.google.gwt.sample.bikeshed.stocks.shared.StockResponse;
import com.google.gwt.sample.bikeshed.stocks.shared.Transaction;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
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

  static class Quote {
    String change;
    long createdTime;
    int price;

    public Quote(int price, String change) {
      this.price = price;
      this.change = change;
      this.createdTime = System.currentTimeMillis();
    }

    public String getChange() {
      return change;
    }

    public long getCreatedTime() {
      return createdTime;
    }

    public int getPrice() {
      return price;
    }
  }

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

  private static final int MAX_RESULTS_TO_RETURN = 10000;

  private static final Map<String, Quote> QUOTES = new HashMap<String, Quote>();

  private static final HashMap<String,Pattern> sectorPatterns =
    new HashMap<String,Pattern>();

  private static final HashMap<String,String> sectorQueries =
    new HashMap<String,String>();

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
    for (Map.Entry<String, String> entry : sectorQueries.entrySet()) {
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
  private Map<String, PlayerStatus> players =
    new HashMap<String, PlayerStatus>();

  public StockResponse addFavorite(String ticker, Range favoritesRange) {
    PlayerStatus player = ensurePlayer();
    player.addFavorite(ticker);
    Result favorites = queryFavorites(favoritesRange);
    player.addStatus(player.getDisplayName() + " added " + ticker
        + " to favorites");
    return createStockResponse(player, null, null, favorites, null, null);
  }

  public Result getSectorQuotes(String sector, Range sectorRange) {
    sector = sector.toUpperCase();
    String sectorQuery = sectorQueries.get(sector);
    if (sectorQuery == null) {
      return null;
    }
    Pattern sectorPattern = sectorPatterns.get(sector);
    return queryTickerRegex(sectorPattern, sectorRange);
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
    Result searchResults = getSearchQuotes(query, searchRange);
    Result favorites = queryFavorites(favoritesRange);
    String sectorName = request.getSector();
    Result sector = sectorRange != null ? getSectorQuotes(sectorName,
        sectorRange) : null;

    return createStockResponse(player, request.getSearchQuery(), searchResults,
        favorites, sectorName, sector);
  }

  public StockResponse removeFavorite(String ticker, Range favoritesRange) {
    PlayerStatus player = ensurePlayer();
    player.removeFavorite(ticker);
    Result favorites = queryFavorites(favoritesRange);
    player.addStatus(player.getDisplayName() + " removed " + ticker
        + " from favorites");
    return createStockResponse(player, null, null, favorites, null, null);
  }

  public Transaction transact(Transaction transaction)
      throws IllegalArgumentException {
    // Get the current stock price.
    String ticker = transaction.getTicker();
    if (ticker == null || ticker.length() < 0) {
      throw new IllegalArgumentException("Stock could not be found");
    }

    Result result = queryExactTicker(ticker);
    if (result.numRows != 1 || result.quotes.size() != 1) {
      throw new IllegalArgumentException("Could not resolve stock ticker");
    }
    StockQuote quote = result.quotes.get(0);

    // Perform the transaction with the user.
    int quantity = transaction.getQuantity();
    int price = quote.getPrice();
    
    PlayerStatus player = ensurePlayer();
    if (transaction.isBuy()) {
      player.buy(ticker, quantity, price);
      player.addStatus(player.getDisplayName() + " bought " + quantity +
          " share" + ((quantity == 1) ? "" : "s") + " of " + ticker);
    } else {
      player.sell(ticker, quantity, price);
      player.addStatus(player.getDisplayName() + " sold " + quantity +
          " share" + ((quantity == 1) ? "" : "s") + " of " + ticker);
    }

    return new Transaction(transaction.isBuy(), ticker, quantity, price);
  }

  /**
   * Create a stock response, updating the current user's net worth.
   * 
   * @param player the player info
   * @param searchQuery the original search query
   * @param searchResults the search results
   * @param favorites the users favorites
   * @param sectorName the name of the sector
   * @param sectorResults the sector results
   * @return a {@link StockResponse}
   */
  private StockResponse createStockResponse(PlayerStatus player,
      String searchQuery, Result searchResults, Result favorites,
      String sectorName, Result sectorResults) {
    // Default to no search results.
    if (searchResults == null) {
      searchResults = new Result(null, 0);
    }

    // Default to no sector results.
    if (sectorResults == null) {
      sectorResults = new Result(null, 0);
    }

    // Store the new stock value.
    player.setStockValue(favorites.quotes.getValue());

    // Create a stock response.
    List<PlayerInfo> playerInfo = new ArrayList<PlayerInfo>();
    for (PlayerStatus curPlayer : players.values()) {
      playerInfo.add(curPlayer.copy());
    }
    Collections.sort(playerInfo, new Comparator<PlayerInfo>() {
      public int compare(PlayerInfo o1, PlayerInfo o2) {
        // Reverse sort so top player is first.
        return o2.getNetWorth() - o1.getNetWorth();
      }
    });
    StockResponse response = new StockResponse(player.copy(), searchQuery,
        searchResults.quotes, favorites.quotes, sectorName != null ? sectorName
            : null, sectorResults.quotes, searchResults.numRows,
        favorites.numRows, sectorResults.numRows, playerInfo);

    return response;
  }

  /**
   * Ensure that a {@link PlayerStatus} for the current player exists and return
   * it.
   * 
   * @return the {@link PlayerStatus} for the current player
   */
  private PlayerStatus ensurePlayer() {
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    String userId = user.getUserId();
    PlayerStatus player = players.get(userId);
    if (player == null) {
      player = new PlayerStatus(user.getNickname());
      players.put(userId, player);
    }
    return player;
  }

  private Result getQuotes(SortedSet<String> symbols, Range range) {
    int start = range.getStart();
    int end = Math.min(start + range.getLength(), symbols.size());

    if (end <= start) {
      return new Result(new StockQuoteList(0), 0);
    }

    // Get the symbols that are in range.
    SortedSet<String> symbolsInRange = new TreeSet<String>();
    int idx = 0;
    for (String symbol : symbols) {
      if (idx >= start && idx < end) {
        symbolsInRange.add(symbol);
      }
      idx++;
    }

    // If we already have a price that is less than 5 seconds old,
    // don't re-request the data from the server

    SortedSet<String> symbolsToQuery = new TreeSet<String>();
    long now = System.currentTimeMillis();
    for (String symbol : symbolsInRange) {
      Quote quote = QUOTES.get(symbol);
      if (quote == null || now - quote.getCreatedTime() >= 5000) {
        symbolsToQuery.add(symbol);
        // System.out.println("retrieving new value of " + symbol);
      } else {
        // System.out.println("Using cached value of " + symbol + " (" + (now -
        // quote.getCreatedTime()) + "ms old)");
      }
    }

    if (symbolsToQuery.size() > 0) {
      GoogleFinance.queryServer(symbolsToQuery, QUOTES);
    }

    // Create and return a StockQuoteList containing the quotes
    StockQuoteList toRet = new StockQuoteList(start);
    for (String symbol : symbolsInRange) {
      Quote quote = QUOTES.get(symbol);

      if (quote == null) {
        System.out.println("Bad symbol " + symbol);
      } else {
        String name = Stocks.companyNamesBySymbol.get(symbol);
        PlayerStatus player = ensurePlayer();
        Integer sharesOwned = player.getSharesOwned(symbol);
        boolean favorite = player.isFavorite(symbol);
        int totalPaid = player.getAverageCostBasis(symbol);

        toRet.add(new StockQuote(symbol, name, quote.getPrice(),
            quote.getChange(),
            sharesOwned == null ? 0 : sharesOwned.intValue(), favorite,
            totalPaid));
      }
    }

    return new Result(toRet, toRet.size());
  }

  // If a query is alpha-only ([A-Za-z]+), return stocks for which:
  // 1a) a prefix of the ticker symbol matches the query
  // 2) any substring of the stock name matches the query
  //
  // If a query is non-alpha, consider it as a regex and return stocks for
  // which:
  // 1b) any portion of the stock symbol matches the regex
  // 2) any portion of the stock name matches the regex
  private Result getSearchQuotes(String query, Range searchRange) {
    SortedSet<String> symbols = new TreeSet<String>();

    boolean queryIsAlpha = true;
    for (int i = 0; i < query.length(); i++) {
      char c = query.charAt(i);
      if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z')) {
        queryIsAlpha = false;
        break;
      }
    }

    // Canonicalize case
    query = query.toUpperCase(Locale.US);

    // (1a)
    if (queryIsAlpha) {
      getTickersByPrefix(query, symbols);
    }

    // Use Unicode case-insensitive matching, allow matching of a substring
    Pattern pattern = compile("(?iu).*(" + query + ").*");
    if (pattern != null) {
      // (1b)
      if (!queryIsAlpha) {
        getTickersBySymbolRegex(pattern, symbols);
      }

      // (2)
      if (query.length() > 2) {
        getTickersByNameRegex(pattern, symbols);
      }
    }

    return getQuotes(symbols, searchRange);
  }

  // Assume pattern is upper case
  private void getTickersByNameRegex(Pattern pattern, Set<String> tickers) {
    if (pattern == null) {
      return;
    }

    for (Map.Entry<String, String> entry : Stocks.companyNamesBySymbol.entrySet()) {
      if (tickers.size() >= MAX_RESULTS_TO_RETURN) {
        return;
      }

      if (match(entry.getValue(), pattern)) {
        tickers.add(entry.getKey());
      }
    }
  }

  // Assume prefix is upper case
  private void getTickersByPrefix(String prefix, Set<String> tickers) {
    if (prefix == null || prefix.length() == 0) {
      return;
    }

    for (String ticker : Stocks.stockTickers) {
      if (tickers.size() >= MAX_RESULTS_TO_RETURN) {
        break;
      }

      if (ticker.startsWith(prefix)) {
        tickers.add(ticker);
      }
    }
  }

  // Assume pattern is upper case
  private void getTickersBySymbolRegex(Pattern pattern, Set<String> tickers) {
    if (pattern == null) {
      return;
    }

    for (String ticker : Stocks.stockTickers) {
      if (tickers.size() >= MAX_RESULTS_TO_RETURN) {
        return;
      }
      if (match(ticker, pattern)) {
        tickers.add(ticker);
      }
    }
  }

  private boolean match(String symbol, Pattern pattern) {
    Matcher m = pattern.matcher(symbol);
    return m.matches();
  }

  private Result queryExactTicker(String ticker) {
    SortedSet<String> symbols = new TreeSet<String>();
    symbols.add(ticker);
    return getQuotes(symbols, new DefaultRange(0, 1));
  }

  private Result queryFavorites(Range favoritesRange) {
    PlayerStatus player = ensurePlayer();
    SortedSet<String> symbols = new TreeSet<String>();

    Pattern favoritesPattern = player.getFavoritesPattern();
    if (favoritesPattern != null) {
      getTickersBySymbolRegex(favoritesPattern, symbols);
    }

    return getQuotes(symbols, favoritesRange);
  }

  private Result queryTickerRegex(Pattern pattern, Range range) {
    SortedSet<String> symbols = new TreeSet<String>();
    getTickersBySymbolRegex(pattern, symbols);
    return getQuotes(symbols, range);
  }
}
