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
package com.google.gwt.sample.expenses.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Class to generate random expense report data.
 */
public abstract class ReportGenerator {

  class EmployeeDTO {
    String department;
    String displayName;
    String password;
    long supervisorKey;
    String userName;
  }

  class ExpenseDTO {
    double amount;
    String approval;
    String category;
    Date created;
    String description;
    String reasonDenied;
    long reportId;
  }

  class ReportDTO {
    long approvedSupervisorKey;
    Date created;
    String department;
    String notes;
    String purpose;
    long reporterKey;
  }

  // Must be in sync with DESCRIPTIONS
  private static String[] CATEGORIES = {
      "Local Transportation", "Local Transportation", "Local Transportation",
      "Local Transportation", "Local Transportation", "Local Transportation",
      "Local Transportation", "Local Transportation", "Local Transportation",
      "Long Distance Transportation", "Long Distance Transportation",
      "Long Distance Transportation", "Long Distance Transportation",
      "Office Supplies", "Office Supplies", "Office Supplies",
      "Office Supplies", "Office Supplies", "Office Supplies",
      "Office Supplies", "Office Supplies", "Office Supplies",
      "Office Supplies", "Office Supplies", "Office Supplies",
      "Office Supplies", "Office Supplies", "Office Supplies",
      "Office Supplies", "Office Supplies", "Office Supplies",
      "Office Supplies", "Electronic Equipment", "Electronic Equipment",
      "Electronic Equipment", "Electronic Equipment", "Electronic Equipment",
      "Electronic Equipment", "Electronic Equipment", "Electronic Equipment",
      "Electronic Equipment", "Electronic Equipment", "Electronic Equipment",
      "Electronic Equipment", "Electronic Equipment", "Electronic Equipment",
      "Electronic Equipment", "Electronic Equipment", "Electronic Equipment",
      "Electronic Equipment", "Electronic Equipment", "Dues and Fees",
      "Dues and Fees", "Dues and Fees", "Dues and Fees", "Dues and Fees",
      "Dues and Fees", "Dues and Fees", "Dues and Fees", "Communications",
      "Communications", "Communications", "Communications", "Communications",
      "Communications", "Books", "Books", "Books", "Books", "Books", "Books",
      "Books", "Books", "Books", "Books", "Books", "Books", "Books", "Books",
      "Books", "Books", "Books", "Books", "Books", "Books", "Facilities",
      "Facilities", "Facilities", "Facilities", "Facilities", "Facilities",
      "Facilities", "Facilities", "Facilities", "Facilities", "Facilities",
      "Facilities", "Food Service", "Food Service", "Food Service",
      "Food Service", "Food Service", "Food Service", "Food Service",
      "Food Service", "Food Service", "Food Service", "Food Service",
      "Marketing", "Marketing", "Marketing", "Marketing", "Marketing",
      "Human Relations", "Human Relations", "Human Relations",
      "Human Relations", "Meals", "Meals", "Meals", "Lodging", "Lodging",
      "Lodging", "Lodging", "Lodging"};

  private static final String[] CITIES = {
      "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
      "Philadelphia", "San Antonio", "San Diego", "Dallas", "Detroit",
      "San Jose", "Indianapolis", "Jacksonville", "San Francisco", "Hempstead",
      "Columbus", "Austin", "Memphis", "Baltimore", "Charlotte", "Fort Worth",
      "Milwaukee", "Boston", "El Paso", "Washington", "Nashville", "Seattle",
      "Denver", "Las Vegas", "Portland", "Oklahoma City", "Tucson",
      "Albuquerque", "Atlanta", "Long Beach", "Brookhaven", "Fresno",
      "New Orleans", "Sacramento", "Cleveland", "Mesa", "Kansas City",
      "Virginia Beach", "Omaha", "Oakland", "Miami", "Tulsa", "Honolulu",
      "Minneapolis", "Colorado Springs", "Arlington", "Wichita", "St. Louis",
      "Raleigh, North Carolina", "Santa Ana", "Anaheim", "Cincinnati", "Tampa",
      "Islip", "Pittsburgh", "Toledo", "Aurora", "Oyster Bay", "Bakersfield",
      "Riverside", "Stockton", "Corpus Christi", "Buffalo", "Newark",
      "St. Paul", "Anchorage", "Lexington-Fayette", "Plano", "St. Petersburg",
      "Fort Wayne", "Glendale", "Lincoln", "Jersey City, New Jersey",
      "Greensboro", "Norfolk", "Chandler", "Henderson", "Birmingham",
      "Scottsdale", "Madison", "Baton Rouge", "North Hempstead", "Hialeah",
      "Chesapeake", "Garland", "Orlando", "Babylon", "Lubbock", "Chula Vista",
      "Akron", "Rochester", "Winston-Salem", "Durham", "Reno", "Laredo",
      "Abba", "Abbeville", "Acworth", "Adairsville", "Adel", "Adrian", "Ailey",
      "Alamo", "Atlanta", "Albany", "Alma", "Alpharetta", "Amboy", "Ambrose",
      "Americus", "Andersonville", "Appling", "Aragon", "Arcade", "Arlington",
      "Arnoldsville", "Ashburn", "Athens Clarke", "Atlanta", "Attapulgus",
      "Auburn", "Augusta", "Austell Douglas", "Avera", "Avondale Estates",
      "Axson", "Baconton", "Baden", "Bainbridge", "Baldwin", "Ball Ground",
      "Bannockburn", "Barnesville", "Barney", "Barretts", "Baxley",
      "Bellville", "Bemiss", "Berkeley Lake", "Big Canoe", "Blackshear",
      "Blairsville", "Blakely", "Bloomingdale", "Blue Ridge", "Blythe",
      "Boston", "Bostwick", "Bowdon", "Bowens Mill", "Bowman", "Box Springs",
      "Braswell", "Bremen", "Bristol", "Bronwood", "Brookfield", "Brooklet",
      "Broxton", "Brunswick", "Buchanan", "Buena Vista", "Buford", "Bushnell",
      "Butler", "Byron", "Cairo", "Calhoun", "Camilla", "Canon", "Canton",
      "Carlton", "Carnesville", "Carrollton", "Cartersville", "Cataula",
      "Cave Spring", "Cedar Springs", "Cedartown", "Centerville", "Chamblee",
      "Chatsworth", "Chattahoochee Hills", "Chickamauga", "Chula",
      "Clarkesville", "Clarkston", "Claxton", "Clayton", "Cleveland", "Climax",
      "Clyattville", "Clyo", "Cobb", "Cobbtown", "Cochran", "Cogdell",
      "Colbert", "Coleman", "Colesburg", "College Park", "Collins", "Colquitt",
      "Columbus", "Comer", "Commerce", "Conyers", "Coolidge", "Cordele",
      "Cornelia", "Council", "Country Club Estate", "Coverdale", "Covington",
      "Cox", "Crawford", "Crawfordville", "Crescent", "Culloden", "Cumming",
      "Cusseta", "Cuthbert", "Dacula", "Dahlonega", "Daisy", "Dakota",
      "Dallas", "Dalton", "Danielsville", "Darien", "Davisboro", "Dawson",
      "Dawsonville", "Dearing", "Decatur", "Demorest", "Denmark", "Denton",
      "De Soto", "Dillard", "Dixie", "Dock Junction", "Doerun",
      "Donalsonville", "Doraville", "Douglas", "Douglasville", "Dover",
      "Dover Bluff", "Dry Branch", "Dublin", "Dudley", "Duluth", "Dunwoody",
      "East Dublin", "East Ellijay", "East Point", "Eastanollee", "Eastman",
      "Eatonton", "Ebenezer", "Edge Hill", "Edison", "Edith", "Egypt",
      "Elberton", "Eldorado", "Ellabell", "Ellaville", "Ellerslie", "Ellijay",
      "Emerson", "Eton", "Euharlee", "Eulonia", "Evans", "Everitt", "Fairburn",
      "Fairmount", "Fargo", "Fitzgerald", "Flemington", "Flovilla",
      "Flowery Branch", "Folkston", "Forest Park", "Forsyth", "Fort Benning",
      "Fort Gaines", "Fort Gordon", "Fort McPherson", "Fort Oglethorpe",
      "Fort Stewart", "Fort Valley", "Fortson", "Franklin", "Fruitland",
      "Gainesville", "Garden City", "Georgetown", "Gibson", "Gillsville",
      "Glennville", "Glenwood", "Glory", "Gordon", "Graham", "Grantville",
      "Gray", "Grayson", "Greensboro", "Greenville", "Griffin", "Grooverville",
      "Groveland", "Grovetown", "Gumbranch", "Guyton", "Hagan", "Hahira",
      "Hamilton", "Hampton", "Hapeville", "Harding", "Hardwick", "Harlem",
      "Harrietts Bluff", "Harrison", "Hartwell", "Hawkinsville", "Hazlehurst",
      "Helen", "Helena", "Hephzibah", "Hickox", "Hiltonia", "Hinesville",
      "Hiram", "Hoboken", "Hogansville", "Holly Springs", "Holt", "Homeland",
      "Homerville", "Hopeulikit", "Hortense", "Hoschton", "Howard", "Howell",
      "Hull", "Ideal", "Ila", "Inaha", "Irwinton", "Irwinville",
      "Isle of Hope", "Jackson", "Jakin", "Jasper", "Jefferson",
      "Jeffersonville", "Jekyll Island", "Jesup", "Johns Creek", "Jonesboro",
      "Keller", "Kennesaw", "Kinderlou", "Kings Bay", "Kingsland", "Kingston",
      "Kirkland", "La Fayette", "LaGrange", "Lake City", "Lake Park",
      "Lakeland", "Lakemont", "Lavonia", "Lawrenceville", "Lax", "Leary",
      "Leefield", "Leesburg", "Lenox", "Leslie", "Lexington", "Lilburn",
      "Lilly", "Lincolnton", "Lithonia", "Locust Grove", "Loganville",
      "Lookout Mountain", "Louisville", "Lovejoy", "Ludowici", "Lula",
      "Lulaton", "Lumber City", "Lumpkin", "Luthersville", "Lyons", "Macon",
      "Madison", "Manassas", "Manchester", "Mansfield", "Marietta",
      "Marshallville", "Martinez", "Matthews", "Mauk", "Mayday", "McCaysville",
      "McDonough", "McRae", "Meansville", "Meigs", "Meldrim", "Menlo",
      "Mershon", "Metter", "Midville", "Midway", "Milledgeville", "Millen",
      "Milner", "Milton", "Mineral Bluff", "Molena", "Moniac", "Monroe",
      "Montezuma", "Montgomery", "Monticello", "Mora", "Morgan", "Morrow",
      "Morven", "Moultrie", "Mount Berry", "Mount Vernon", "Mount Zion",
      "Mountain Park", "Mystic", "Nahunta", "Nankin", "Nashville", "Needmore",
      "Nelson", "Nevils", "Newnan", "New Rock Hill", "Newton", "Nicholls",
      "Nicholson", "Norcross", "Norman Park", "Norwood", "Oakwood", "Ocilla",
      "Oconee", "Offerman", "Oglethorpe", "Oliver", "Omaha", "Omega",
      "Osterfield", "Ousley", "Oxford", "Palmetto", "Patterson", "Pavo",
      "Payne", "Peachtree City", "Pearson", "Pelham", "Pembroke",
      "Pendergrass", "Perry", "Phillipsburg", "Philomath", "Pine Lake",
      "Pine Mountain Valley", "Pineora", "Pinehurst", "Pitts", "Plains",
      "Plainville", "Pooler", "Port Wentworth", "Potter", "Poulan",
      "Powder Springs", "Preston", "Pridgen", "Queensland", "Quitman",
      "Race Pond", "Ray City", "Rebecca", "Red Oak", "Reidsville", "Remerton",
      "Resaca", "Retreat", "Riceboro", "Richland", "Richmond Hill",
      "Ridgeville", "Rincon", "Ringgold", "Rising Fawn", "Riverdale",
      "Roberta", "Rochelle", "Rock Spring", "Rockingham", "Rockmart",
      "Rocky Face", "Rome", "Roopville", "Roosterville", "Rossville",
      "Roswell", "Royston", "Rutledge", "Saint George", "Saint Marys",
      "Saint Simons Island", "Sandersville", "Sandy Springs", "Santa Claus",
      "Sapelo Island", "Sargent", "Savannah", "Scotland", "Screven", "Senoia",
      "Sessoms", "Sharon", "Shawnee", "Shellman", "Shellman Bluff", "Shiloh",
      "Sirmans", "Skidaway Island", "Smithville", "Smyrna", "Snellville",
      "Social Circle", "Soperton", "South Newport", "Sparta", "Springfield",
      "Stapleton", "Statenville", "Statesboro", "Statham", "Stephens",
      "Sterling", "Stillwell", "Stilson", "Stockbridge", "Stockton",
      "Stone Mountain", "Sugar Hill", "Summerville", "Sunbury", "Sunny Side",
      "Sunsweet", "Suwanee", "Swainsboro", "Sycamore", "Sylvania", "Sylvester",
      "Talbotton", "Tallapoosa", "Tarboro", "Tarver", "Tate", "Temple",
      "Tennille", "Thalman", "Thelma", "Thomaston", "Thomasville", "Thomson",
      "Tifton", "Toccoa", "Townsend", "Trenton", "Trudie", "Tucker",
      "Tunnel Hill", "Twin City", "Twin Lakes", "Tybee Island", "Ty Ty",
      "Unadilla", "Union City", "Union Point", "Unionville", "Upton", "Uvalda",
      "Valdosta", "Valona", "Varnell", "Vidalia", "Vidette", "Vienna",
      "Villa Rica", "Waco", "Wadley", "Waleska", "Walnut Grove",
      "Walthourville", "Warm Springs", "Warner Robins", "Warrenton", "Warthen",
      "Warwick", "Washington", "Waterloo", "Watkinsville", "Waverly",
      "Waverly Hall", "Waycross", "Waynesboro", "Waynesville", "Weber",
      "West Green", "West Point", "Westwood", "Whigham", "White", "White Oak",
      "White Plains", "Whitemarsh Island", "Wildwood", "Willacoochee",
      "Wilmington Island", "Winder", "Winokur", "Winterville", "Withers",
      "Woodbine", "Woodbury", "Woodland", "Woodstock", "Woodville", "Wray",
      "Wrens", "Wrightsville", "Young Harris", "Yatesville", "Zebulon"};

  private static final String[] COMPANIES = {
      "Wal-Mart Stores", "Exxon Mobil", "Chevron", "General Motors",
      "ConocoPhillips", "General Electric", "Ford Motor", "Citigroup",
      "Bank of America Corp.", "AT&T", "Berkshire Hathaway",
      "J.P. Morgan Chase & Co.", "American Intl. Group", "Hewlett-Packard",
      "Intl. Business Machines", "Valero Energy", "Verizon Communications",
      "McKesson", "Cardinal Health", "Goldman Sachs", "Morgan Stanley",
      "Home Depot ", "Procter & Gamble", "CVS/Caremark", "UnitedHealth Group",
      "Kroger", "Boeing", "AmerisourceBergen", "Costco Wholesale",
      "Merrill Lynch", "Target", "State Farm Insurance", "Wellpoint", "Dell",
      "Johnson & Johnson", "Marathon Oil", "Lehman Brothers", "Wachovia Corp.",
      "United Technologies", "Walgreen", "Wells Fargo", "Dow Chemical",
      "MetLife", "Microsoft", "Sears Holdings", "United Parcel Service",
      "Pfizer", "Lowe's", "Time Warner", "Caterpillar",
      "Medco Health Solutions", "Archer Daniels Midland", "Fannie Mae",
      "Freddie Mac", "Safeway", "Sunoco", "Lockheed Martin", "Sprint Nextel",
      "PepsiCo", "Intel", "Altria Group", "SuperValu", "Kraft Foods",
      "Allstate", "Motorola", "Best Buy", "Walt Disney", "FedEx",
      "Ingram Micro", "Sysco", "Cisco Systems", "Johnson Controls",
      "Honeywell ", "Prudential Financial", "American Express",
      "Northrop Grumman", "Hess", "GMAC", "Comcast", "Alcoa", "DuPont",
      "New York Life Insurance", "Coca-Cola", "News Corp.", "Aetna",
      "TIAA-CREF", "General Dynamics", "Tyson Foods", "HCA",
      "Enterprise GP Holdings", "Macy's", "Delphi", "Travelers Cos.",
      "Liberty Mutual Ins. Group", "Hartford Financial Services",
      "Abbott Laboratories", "Washington Mutual", "Humana",
      "Massachusetts Mutual Life Insurance"};

  private static final double COST_AIRFARE = 600;

  private static final double COST_BREAKFAST = 15;

  private static final double COST_DINNER = 60;

  private static final double COST_HOTEL = 300;

  private static final double COST_LUNCH = 25;

  private static final double COST_SUNDRY = 100;

  private static final String[] DEPARTMENTS = {
      "Operations", "Engineering", "Finance", "Marketing", "Sales"};

  // Must be in sync with CATEGORIES
  private static String[] DESCRIPTIONS = {
      "Train Fare", "Taxi Fare", "Monorail", "Water Taxi", "Bus Fare",
      "Bicycle Rental", "Car Rental", "Limousine Service", "Helicopter",
      "Airplane Ticket", "Bus Ticket", "Train Ticket", "Car Rental",
      "Paperclips", "Stapler", "Scissors", "Paste", "Notebooks", "Pencils",
      "Whiteboard Markers", "Tissues", "Pens", "Copier Paper", "Legal Pad",
      "Rubber Bands", "Binder Clips", "Scotch Tape", "Masking Tape",
      "Tape Dispenser", "Highlighter", "Staples", "File Folders", "Headphones",
      "Workstation", "Laptop", "USB Cable", "Electronic Plunger",
      "Serial Cable", "KVM", "Video Cable", "Docking Station", "Headset",
      "Speakers", "Keyboard", "Mouse", "UPS", "Hard Drive", "CD-ROM Drive",
      "Power Cord", "Extension Cord", "Surge Protector", "ACM Membership",
      "IEEE Membership", "Google I/O Ticket", "Parking Ticket",
      "Other Professional Association", "Conference Fee", "Trade Show Fee",
      "Bank Fee", "Telephone", "Internet", "Mobile", "Phone Card",
      "Satellite Phone", "Cable TV", "AJAX", "Java", "C++", "C#", "Python",
      "Google Go", "Perl", "Visual Basic", "Software Engineering", "Windows",
      "UNIX", "Linux", "Apple", "Android", "iPhone", "Blackberry", "Mobile",
      "Software Design", "Marketing", "Management", "Toilet Paper",
      "Paper Towels", "Cleaning Supplies", "Cleaning Contractor", "Repairs",
      "Updates", "Exterminator", "Plant Care", "Decoration", "Furniture ",
      "Reading Material", "Trash Bags", "Coffee Cups", "Coffee Stirrers",
      "Coffee Lids", "Condiments", "Coffee Maker Supplies",
      "Coffee Maker Maintenance", "Coffee Beans", "Tea", "Bottled Drinks",
      "Snacks", "Straws", "Flyers", "Posters", "Booth", "Meeting  Expenses",
      "Design Consultant", "Candidate Travel", "Recruiting Expenses",
      "Outreach", "Training", "Self", "Co-Workers", "Customers", "Hotel",
      "Motel ", "Holiday Inn", "Private Apartment", "Corporate Apartment"};

  // 11% of females hyphenate their last names
  private static final double FEMALE_HYPHENATE = 0.11;

  private static final String[] FULLCITIES = {
      "New York, New York", "Los Angeles, California", "Chicago, Illinois",
      "Houston, Texas", "Phoenix, Arizona", "Philadelphia, Pennsylvania",
      "San Antonio, Texas", "San Diego, California", "Dallas, Texas",
      "Detroit, Michigan", "San Jose, California", "Indianapolis, Indiana",
      "Jacksonville, Florida", "San Francisco, California",
      "Hempstead, New York", "Columbus, Ohio", "Austin, Texas",
      "Memphis, Tennessee", "Baltimore, Maryland", "Charlotte, North Carolina",
      "Fort Worth, Texas", "Milwaukee, Wisconsin", "Boston, Massachusetts",
      "El Paso, Texas", "Washington, District of Columbia",
      "Nashville-Davidson, Tennessee", "Seattle, Washington",
      "Denver, Colorado", "Las Vegas, Nevada", "Portland, Oregon",
      "Oklahoma City, Oklahoma", "Tucson, Arizona", "Albuquerque, New Mexico",
      "Atlanta, Georgia", "Long Beach, California", "Brookhaven, New York",
      "Fresno, California", "New Orleans, Louisiana", "Sacramento, California",
      "Cleveland, Ohio", "Mesa, Arizona", "Kansas City, Missouri",
      "Virginia Beach, Virginia", "Omaha, Nebraska", "Oakland, California",
      "Miami, Florida", "Tulsa, Oklahoma", "Honolulu, Hawaii",
      "Minneapolis, Minnesota", "Colorado Springs, Colorado",
      "Arlington, Texas", "Wichita, Kansas", "St. Louis, Missouri",
      "Raleigh, North Carolina", "Santa Ana, California",
      "Anaheim, California", "Cincinnati, Ohio", "Tampa, Florida",
      "Islip, New York", "Pittsburgh, Pennsylvania", "Toledo, Ohio",
      "Aurora, Colorado", "Oyster Bay, New York", "Bakersfield, California",
      "Riverside, California", "Stockton, California", "Corpus Christi, Texas",
      "Buffalo, New York", "Newark, New Jersey", "St. Paul, Minnesota",
      "Anchorage, Alaska", "Lexington-Fayette, Kentucky", "Plano, Texas",
      "St. Petersburg, Florida", "Fort Wayne, Indiana", "Glendale, Arizona",
      "Lincoln, Nebraska", "Jersey City, New Jersey",
      "Greensboro, North Carolina", "Norfolk, Virginia", "Chandler, Arizona",
      "Henderson, Nevada", "Birmingham, Alabama", "Scottsdale, Arizona",
      "Madison, Wisconsin", "Baton Rouge, Louisiana",
      "North Hempstead, New York", "Hialeah, Florida", "Chesapeake, Virginia",
      "Garland, Texas", "Orlando, Florida", "Babylon, New York",
      "Lubbock, Texas", "Chula Vista, California", "Akron, Ohio",
      "Rochester, New York", "Winston-Salem, North Carolina",
      "Durham, North Carolina", "Reno, Nevada", "Laredo, Texas"};

  // 2% of males hyphenate their last names
  private static final double MALE_HYPHENATE_PROBABILITY = 0.02;

  private static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;

  private static final long MILLIS_PER_HOUR = 60L * 60L * 1000L;

  private static final String[] NOTES = {
      // Some entries do not have notes.
      "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
      "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
      "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
      "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
      "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
      "", "", "", "", "", "", "Need approval by Monday",
      "Bill to widgets project", "High priority", "Review A.S.A.P."};

  private static final String[] PURPOSES = {
      "Spending lots of money", "Team building diamond cutting offsite",
      "Visit to Istanbul", "ISDN modem for telecommuting", "Sushi offsite",
      "Baseball card research", "Potato chip cooking offsite",
      "Money laundering", "Donut day", "Approved by FULLNAME", "For FULLNAME",
      "Customer meeting with FULLNAME", "Customer meeting with COMPANY",
      "See FULLNAME", "Met with FIRSTNAME and FIRSTNAME",
      "Discussed with FULLNAME", "w/ FULLNAME", "w/ FIRSTNAME",
      "Buy supplies for FULLNAME", "Met COMPANY in CITY",
      "Met with COMPANY in CITY", "Met w/ COMPANY in CITY",
      "Met COMPANY in CITY with FULLNAME",
      "Met with COMPANY in FULLCITY with FIRSTNAME", "COMPANY lunch",
      "Conference", "Sales opportunity", "Sales opportunity with COMPANY",
      "Maintenance", "Supplies", "Emergency", "Backup", "Research",
      "CITY breakfast", "CITY lunch", "CITY dinner", "CITY conference",
      "Conference in CITY", "Conference in FULLCITY", "Meals with FIRSTNAME",
      "Meet with FULLNAME", "Meet with FIRSTNAME",
      "Meet with FIRSTNAME in CITY", "Meet with FIRSTNAME in FULLCITY",
      "FULLCITY meetings", "Visit to FULLCITY", "Visit to CITY",
      "COMPANY offsite", "CITY offsite", "FULLCITY offsite",
      "Brainstorm with FIRSTNAME's team", "Visit FIRSTNAME",
      "Visit FIRSTNAME at CITY airport", "Visit FIRSTNAME and FIRSTNAME",
      "Visit FIRSTNAME, FIRSTNAME, and FIRSTNAME"};

  public static void readFile(String filename, List<String> names,
      List<Double> frequencies) throws IOException {
    Reader reader = new FileReader(filename);
    BufferedReader br = new BufferedReader(reader);

    names.clear();
    frequencies.clear();

    String s;
    while ((s = br.readLine()) != null) {
      String[] split = s.split("\\s+");
      String name = split[0];
      if (name.startsWith("MC") && name.length() > 2) {
        name = "Mc" + name.charAt(2) + name.substring(3).toLowerCase();
      } else {
        name = "" + name.charAt(0) + name.substring(1).toLowerCase();
      }
      names.add(name);
      frequencies.add(Double.parseDouble(split[2]));
    }

    // Disambiguate names with equal cumulative frequencies
    double lastFreq = 0;
    int count = 1;
    int len = frequencies.size();
    for (int i = 0; i < len; i++) {
      Double freq = frequencies.get(i);
      if (freq == lastFreq) {
        count++;
        continue;
      } else {
        if (count > 1) {
          for (int c = 0; c < count; c++) {
            double newFreq = lastFreq + (.001 * c) / count;
            frequencies.set(i - count + c, newFreq);
          }
          count = 1;
        }

        lastFreq = freq;
      }
    }
  }

  private List<Double> femaleNameFreqs = new ArrayList<Double>();

  private List<String> femaleNames = new ArrayList<String>();

  private List<Double> lastNameFreqs = new ArrayList<Double>();

  private List<String> lastNames = new ArrayList<String>();

  private List<Double> maleNameFreqs = new ArrayList<Double>();

  private List<String> maleNames = new ArrayList<String>();

  private int numReports = 0;

  private Random rand = new Random();

  public int getDepartment() {
    return rand.nextInt(DEPARTMENTS.length);
  }

  public int getNumReports() {
    return numReports;
  }

  public void init(String last, String female, String male) throws IOException {
    if (lastNames.size() == 0) {
      readFile(last, lastNames, lastNameFreqs);
    }
    if (femaleNames.size() == 0) {
      readFile(female, femaleNames, femaleNameFreqs);
    }
    if (maleNames.size() == 0) {
      readFile(male, maleNames, maleNameFreqs);
    }
  }

  public long makeEmployee(int department, long supervisorKey) {
    if (!shouldContinue()) {
      return -1;
    }

    String[] firstLast = new String[2];
    getFullName(firstLast);

    String displayName = firstLast[0] + " " + firstLast[1];
    String userName = userName(firstLast[0], firstLast[1]);

    EmployeeDTO employee = new EmployeeDTO();
    employee.userName = userName;
    employee.displayName = displayName;
    employee.supervisorKey = supervisorKey;
    employee.department = DEPARTMENTS[department];
    employee.password = "";
    long id = storeEmployee(employee);

    int numExpenseReports = rand.nextInt(96) + 5;
    for (int i = 0; i < numExpenseReports; i++) {
      makeExpenseReport(id, department, supervisorKey);
    }

    return id;
  }

  public void reset() {
    this.numReports = 0;
  }

  public abstract boolean shouldContinue();

  public abstract long storeEmployee(EmployeeDTO employee);

  public abstract long storeExpense(ExpenseDTO expense);

  public abstract long storeReport(ReportDTO report);

  private double amount(double max) {
    double x = (1.0 + rand.nextDouble()) * max * 0.5;
    x *= 100;
    x = Math.floor(x);
    x /= 100;
    return x;
  }

  private String chooseName(List<String> names, List<Double> freqs) {
    double lastFreq = freqs.get(freqs.size() - 1);
    double freq = rand.nextDouble() * lastFreq;

    int index = Collections.binarySearch(freqs, freq);
    if (index < 0) {
      index = -index - 1;
    }
    String name = names.get(index);
    return name;
  }

  private String get(String[] data) {
    return data[rand.nextInt(data.length)];
  }

  private void getFullName(String[] firstLast) {
    firstLast[1] = chooseName(lastNames, lastNameFreqs);
    if (rand.nextInt(2) == 0) {
      firstLast[0] = chooseName(femaleNames, femaleNameFreqs);
      if (rand.nextDouble() < FEMALE_HYPHENATE) {
        firstLast[1] += "-" + chooseName(lastNames, lastNameFreqs);
      }
    } else {
      firstLast[0] = chooseName(maleNames, maleNameFreqs);
      if (rand.nextDouble() < MALE_HYPHENATE_PROBABILITY) {
        firstLast[1] += "-" + chooseName(lastNames, lastNameFreqs);
      }
    }
  }

  private String getPurpose() {
    String purpose = get(PURPOSES);
    while (purpose.contains("FULLCITY")) {
      purpose = purpose.replaceFirst("FULLCITY", get(FULLCITIES));
    }
    while (purpose.contains("CITY")) {
      purpose = purpose.replaceFirst("CITY", get(CITIES));
    }
    while (purpose.contains("COMPANY")) {
      purpose = purpose.replaceFirst("COMPANY", get(COMPANIES));
    }
    while (purpose.contains("FULLNAME")) {
      String[] firstLast = new String[2];
      getFullName(firstLast);
      purpose = purpose.replaceFirst("FULLNAME", firstLast[0] + " "
          + firstLast[1]);
    }
    while (purpose.contains("FIRSTNAME")) {
      String[] firstLast = new String[2];
      getFullName(firstLast);
      purpose = purpose.replaceFirst("FIRSTNAME", firstLast[0]);
    }

    return purpose;
  }

  private void makeExpenseDetail(long reportId, Date created, String category,
      String description, double amount) {
    ExpenseDTO expense = new ExpenseDTO();
    expense.reportId = reportId;
    expense.created = created;
    expense.category = category;
    expense.description = description;
    expense.amount = amount;
    storeExpense(expense);
  }

  private void makeExpenseReport(long employeeId, int department,
      long supervisorId) {
    if (!shouldContinue()) {
      return;
    }

    long offset = rand.nextInt(60 * 60 * 24 * 90) * 1000L;
    long millis = new Date().getTime() - offset;
    Date created = new Date(millis);

    boolean travel = rand.nextInt(10) == 0;
    int days = 1;
    String purpose, notes;

    if (travel) {
      days = rand.nextInt(10) + 1;

      if (rand.nextInt(5) == 0) {
        int index1 = rand.nextInt(FULLCITIES.length);
        int index2 = index1;
        while (index2 == index1) {
          index2 = rand.nextInt(FULLCITIES.length);
        }
        purpose = "Travel from " + FULLCITIES[index1] + " to "
            + FULLCITIES[index2];
      } else {
        int index1 = rand.nextInt(CITIES.length);
        int index2 = index1;
        while (index2 == index1) {
          index2 = rand.nextInt(CITIES.length);
        }
        purpose = "Travel from " + CITIES[index1] + " to " + CITIES[index2];
      }

      switch (rand.nextInt(10)) {
        case 0:
          notes = "Travel for " + days + " days";
          break;
        case 1:
          notes = days + " nights";
          break;
        case 2:
          notes = days + " day trip";
          break;
        default:
          notes = "";
          break;
      }
    } else {
      purpose = getPurpose();
      notes = NOTES[rand.nextInt(NOTES.length)];
    }

    ReportDTO report = new ReportDTO();
    report.approvedSupervisorKey = supervisorId;
    report.created = created;
    report.department = DEPARTMENTS[department];
    report.notes = notes;
    report.purpose = purpose;
    report.reporterKey = employeeId;

    long id = storeReport(report);

    if (travel) {
      days = rand.nextInt(4) + 1;
      int index1 = rand.nextInt(CITIES.length);
      int index2 = index1;
      while (index2 == index1) {
        index2 = rand.nextInt(CITIES.length);
      }

      makeExpenseDetail(id, new Date(millis - days * MILLIS_PER_DAY),
          "Air Travel", "Outbound flight", amount(COST_AIRFARE));
      makeExpenseDetail(id, new Date(millis - MILLIS_PER_DAY / 2),
          "Air Travel", "Return flight", amount(COST_AIRFARE));
      for (int i = 0; i < days; i++) {
        makeExpenseDetail(id, new Date(millis - (days - i) * MILLIS_PER_DAY
            - 10 * MILLIS_PER_HOUR), "Dining", "Breakfast",
            amount(COST_BREAKFAST));
        makeExpenseDetail(id, new Date(millis - (days - i) * MILLIS_PER_DAY - 6
            * MILLIS_PER_HOUR), "Dining", "Lunch", amount(COST_LUNCH));
        makeExpenseDetail(id, new Date(millis - (days - i) * MILLIS_PER_DAY - 2
            * MILLIS_PER_HOUR), "Dining", "Dinner", amount(COST_DINNER));
        makeExpenseDetail(id, new Date(millis - (days - i) * MILLIS_PER_DAY),
            "Lodging", "Hotel", amount(COST_HOTEL));
      }
    } else {
      int numExpenses = rand.nextInt(3) + 1;
      for (int i = 0; i < numExpenses; i++) {
        int index = rand.nextInt(CATEGORIES.length);
        long detailOffset = rand.nextInt(60 * 60 * 24 * days) * 1000L;
        Date date = new Date(created.getTime() - detailOffset);
        makeExpenseDetail(id, date, CATEGORIES[index], DESCRIPTIONS[index],
            amount(COST_SUNDRY));
      }
    }

    ++numReports;
    if ((numReports % 10000) == 0) {
      System.out.println("Emitted " + numReports + " reports");
    }
  }

  private String userName(String firstName, String lastName) {
    return ("" + firstName.charAt(0) + lastName + rand.nextInt(100)).toLowerCase();
  }
}
