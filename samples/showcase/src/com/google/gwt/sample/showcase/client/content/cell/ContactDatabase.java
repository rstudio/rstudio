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
package com.google.gwt.sample.showcase.client.content.cell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.user.client.Random;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The data source for contact information used in the sample.
 */
public class ContactDatabase {

  /**
   * A contact category.
   */
  public static class Category {

    private final String displayName;

    private Category(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  /**
   * Information about a contact.
   */
  public static class ContactInfo implements Comparable<ContactInfo> {

    /**
     * The key provider that provides the unique ID of a contact.
     */
    public static final ProvidesKey<ContactInfo> KEY_PROVIDER = new ProvidesKey<ContactInfo>() {
      @Override
      public Object getKey(ContactInfo item) {
        return item == null ? null : item.getId();
      }
    };

    private static int nextId = 0;

    private String address;
    private int age;
    private Date birthday;
    private Category category;
    private String firstName;
    private final int id;
    private String lastName;

    public ContactInfo(Category category) {
      this.id = nextId;
      nextId++;
      setCategory(category);
    }

    @Override
    public int compareTo(ContactInfo o) {
      return (o == null || o.firstName == null) ? -1 : -o.firstName.compareTo(firstName);
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof ContactInfo) {
        return id == ((ContactInfo) o).id;
      }
      return false;
    }

    /**
     * @return the contact's address
     */
    public String getAddress() {
      return address;
    }

    /**
     * @return the contact's age
     */
    public int getAge() {
      return age;
    }

    /**
     * @return the contact's birthday
     */
    public Date getBirthday() {
      return birthday;
    }

    /**
     * @return the category of the conteact
     */
    public Category getCategory() {
      return category;
    }

    /**
     * @return the contact's firstName
     */
    public String getFirstName() {
      return firstName;
    }

    /**
     * @return the contact's full name
     */
    public final String getFullName() {
      return firstName + " " + lastName;
    }

    /**
     * @return the unique ID of the contact
     */
    public int getId() {
      return this.id;
    }

    /**
     * @return the contact's lastName
     */
    public String getLastName() {
      return lastName;
    }

    @Override
    public int hashCode() {
      return id;
    }

    /**
     * Set the contact's address.
     * 
     * @param address the address
     */
    public void setAddress(String address) {
      this.address = address;
    }

    /**
     * Set the contact's birthday.
     * 
     * @param birthday the birthday
     */
    @SuppressWarnings("deprecation")
    public void setBirthday(Date birthday) {
      this.birthday = birthday;

      // Recalculate the age.
      Date today = new Date();
      this.age = today.getYear() - birthday.getYear();
      if (today.getMonth() > birthday.getMonth()
          || (today.getMonth() == birthday.getMonth() && today.getDate() > birthday.getDate())) {
        this.age--;
      }
    }

    /**
     * Set the contact's category.
     * 
     * @param category the category to set
     */
    public void setCategory(Category category) {
      assert category != null : "category cannot be null";
      this.category = category;
    }

    /**
     * Set the contact's first name.
     * 
     * @param firstName the firstName to set
     */
    public void setFirstName(String firstName) {
      this.firstName = firstName;
    }

    /**
     * Set the contact's last name.
     * 
     * @param lastName the lastName to set
     */
    public void setLastName(String lastName) {
      this.lastName = lastName;
    }
  }

  /**
   * The constants used in this Content Widget.
   */
  static interface DatabaseConstants extends Constants {
    String[] contactDatabaseCategories();
  }

  private static final String[] FEMALE_FIRST_NAMES = {
      "Mary", "Patricia", "Linda", "Barbara", "Elizabeth", "Jennifer", "Maria", "Susan",
      "Margaret", "Dorothy", "Lisa", "Nancy", "Karen", "Betty", "Helen", "Sandra", "Donna",
      "Carol", "Ruth", "Sharon", "Michelle", "Laura", "Sarah", "Kimberly", "Deborah", "Jessica",
      "Shirley", "Cynthia", "Angela", "Melissa", "Brenda", "Amy", "Anna", "Rebecca", "Virginia",
      "Kathleen", "Pamela", "Martha", "Debra", "Amanda", "Stephanie", "Carolyn", "Christine",
      "Marie", "Janet", "Catherine", "Frances", "Ann", "Joyce", "Diane", "Alice", "Julie",
      "Heather", "Teresa", "Doris", "Gloria", "Evelyn", "Jean", "Cheryl", "Mildred", "Katherine",
      "Joan", "Ashley", "Judith", "Rose", "Janice", "Kelly", "Nicole", "Judy", "Christina",
      "Kathy", "Theresa", "Beverly", "Denise", "Tammy", "Irene", "Jane", "Lori", "Rachel",
      "Marilyn", "Andrea", "Kathryn", "Louise", "Sara", "Anne", "Jacqueline", "Wanda", "Bonnie",
      "Julia", "Ruby", "Lois", "Tina", "Phyllis", "Norma", "Paula", "Diana", "Annie", "Lillian",
      "Emily", "Robin", "Peggy", "Crystal", "Gladys", "Rita", "Dawn", "Connie", "Florence",
      "Tracy", "Edna", "Tiffany", "Carmen", "Rosa", "Cindy", "Grace", "Wendy", "Victoria", "Edith",
      "Kim", "Sherry", "Sylvia", "Josephine", "Thelma", "Shannon", "Sheila", "Ethel", "Ellen",
      "Elaine", "Marjorie", "Carrie", "Charlotte", "Monica", "Esther", "Pauline", "Emma",
      "Juanita", "Anita", "Rhonda", "Hazel", "Amber", "Eva", "Debbie", "April", "Leslie", "Clara",
      "Lucille", "Jamie", "Joanne", "Eleanor", "Valerie", "Danielle", "Megan", "Alicia", "Suzanne",
      "Michele", "Gail", "Bertha", "Darlene", "Veronica", "Jill", "Erin", "Geraldine", "Lauren",
      "Cathy", "Joann", "Lorraine", "Lynn", "Sally", "Regina", "Erica", "Beatrice", "Dolores",
      "Bernice", "Audrey", "Yvonne", "Annette", "June", "Samantha", "Marion", "Dana", "Stacy",
      "Ana", "Renee", "Ida", "Vivian", "Roberta", "Holly", "Brittany", "Melanie", "Loretta",
      "Yolanda", "Jeanette", "Laurie", "Katie", "Kristen", "Vanessa", "Alma", "Sue", "Elsie",
      "Beth", "Jeanne"};
  private static final String[] MALE_FIRST_NAMES = {
      "James", "John", "Robert", "Michael", "William", "David", "Richard", "Charles", "Joseph",
      "Thomas", "Christopher", "Daniel", "Paul", "Mark", "Donald", "George", "Kenneth", "Steven",
      "Edward", "Brian", "Ronald", "Anthony", "Kevin", "Jason", "Matthew", "Gary", "Timothy",
      "Jose", "Larry", "Jeffrey", "Frank", "Scott", "Eric", "Stephen", "Andrew", "Raymond",
      "Gregory", "Joshua", "Jerry", "Dennis", "Walter", "Patrick", "Peter", "Harold", "Douglas",
      "Henry", "Carl", "Arthur", "Ryan", "Roger", "Joe", "Juan", "Jack", "Albert", "Jonathan",
      "Justin", "Terry", "Gerald", "Keith", "Samuel", "Willie", "Ralph", "Lawrence", "Nicholas",
      "Roy", "Benjamin", "Bruce", "Brandon", "Adam", "Harry", "Fred", "Wayne", "Billy", "Steve",
      "Louis", "Jeremy", "Aaron", "Randy", "Howard", "Eugene", "Carlos", "Russell", "Bobby",
      "Victor", "Martin", "Ernest", "Phillip", "Todd", "Jesse", "Craig", "Alan", "Shawn",
      "Clarence", "Sean", "Philip", "Chris", "Johnny", "Earl", "Jimmy", "Antonio", "Danny",
      "Bryan", "Tony", "Luis", "Mike", "Stanley", "Leonard", "Nathan", "Dale", "Manuel", "Rodney",
      "Curtis", "Norman", "Allen", "Marvin", "Vincent", "Glenn", "Jeffery", "Travis", "Jeff",
      "Chad", "Jacob", "Lee", "Melvin", "Alfred", "Kyle", "Francis", "Bradley", "Jesus", "Herbert",
      "Frederick", "Ray", "Joel", "Edwin", "Don", "Eddie", "Ricky", "Troy", "Randall", "Barry",
      "Alexander", "Bernard", "Mario", "Leroy", "Francisco", "Marcus", "Micheal", "Theodore",
      "Clifford", "Miguel", "Oscar", "Jay", "Jim", "Tom", "Calvin", "Alex", "Jon", "Ronnie",
      "Bill", "Lloyd", "Tommy", "Leon", "Derek", "Warren", "Darrell", "Jerome", "Floyd", "Leo",
      "Alvin", "Tim", "Wesley", "Gordon", "Dean", "Greg", "Jorge", "Dustin", "Pedro", "Derrick",
      "Dan", "Lewis", "Zachary", "Corey", "Herman", "Maurice", "Vernon", "Roberto", "Clyde",
      "Glen", "Hector", "Shane", "Ricardo", "Sam", "Rick", "Lester", "Brent", "Ramon", "Charlie",
      "Tyler", "Gilbert", "Gene"};
  private static final String[] LAST_NAMES = {
      "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore",
      "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia",
      "Martinez", "Robinson", "Clark", "Rodriguez", "Lewis", "Lee", "Walker", "Hall", "Allen",
      "Young", "Hernandez", "King", "Wright", "Lopez", "Hill", "Scott", "Green", "Adams", "Baker",
      "Gonzalez", "Nelson", "Carter", "Mitchell", "Perez", "Roberts", "Turner", "Phillips",
      "Campbell", "Parker", "Evans", "Edwards", "Collins", "Stewart", "Sanchez", "Morris",
      "Rogers", "Reed", "Cook", "Morgan", "Bell", "Murphy", "Bailey", "Rivera", "Cooper",
      "Richardson", "Cox", "Howard", "Ward", "Torres", "Peterson", "Gray", "Ramirez", "James",
      "Watson", "Brooks", "Kelly", "Sanders", "Price", "Bennett", "Wood", "Barnes", "Ross",
      "Henderson", "Coleman", "Jenkins", "Perry", "Powell", "Long", "Patterson", "Hughes",
      "Flores", "Washington", "Butler", "Simmons", "Foster", "Gonzales", "Bryant", "Alexander",
      "Russell", "Griffin", "Diaz", "Hayes", "Myers", "Ford", "Hamilton", "Graham", "Sullivan",
      "Wallace", "Woods", "Cole", "West", "Jordan", "Owens", "Reynolds", "Fisher", "Ellis",
      "Harrison", "Gibson", "Mcdonald", "Cruz", "Marshall", "Ortiz", "Gomez", "Murray", "Freeman",
      "Wells", "Webb", "Simpson", "Stevens", "Tucker", "Porter", "Hunter", "Hicks", "Crawford",
      "Henry", "Boyd", "Mason", "Morales", "Kennedy", "Warren", "Dixon", "Ramos", "Reyes", "Burns",
      "Gordon", "Shaw", "Holmes", "Rice", "Robertson", "Hunt", "Black", "Daniels", "Palmer",
      "Mills", "Nichols", "Grant", "Knight", "Ferguson", "Rose", "Stone", "Hawkins", "Dunn",
      "Perkins", "Hudson", "Spencer", "Gardner", "Stephens", "Payne", "Pierce", "Berry",
      "Matthews", "Arnold", "Wagner", "Willis", "Ray", "Watkins", "Olson", "Carroll", "Duncan",
      "Snyder", "Hart", "Cunningham", "Bradley", "Lane", "Andrews", "Ruiz", "Harper", "Fox",
      "Riley", "Armstrong", "Carpenter", "Weaver", "Greene", "Lawrence", "Elliott", "Chavez",
      "Sims", "Austin", "Peters", "Kelley", "Franklin", "Lawson"};
  private static final String[] STREET_NAMES =
      {
          "Peachtree", "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Tenth",
          "Fourteenth", "Spring", "Techwood", "West Peachtree", "Juniper", "Cypress", "Fowler",
          "Piedmont", "Juniper", "Main", "Central", "Currier", "Courtland", "Williams",
          "Centennial", "Olympic", "Baker", "Highland", "Pryor", "Decatur", "Bell", "Edgewood",
          "Mitchell", "Forsyth", "Capital"};
  private static final String[] STREET_SUFFIX = {
      "St", "Rd", "Ln", "Blvd", "Way", "Pkwy", "Cir", "Ave"};

  /**
   * The singleton instance of the database.
   */
  private static ContactDatabase instance;

  /**
   * Get the singleton instance of the contact database.
   * 
   * @return the singleton instance
   */
  public static ContactDatabase get() {
    if (instance == null) {
      instance = new ContactDatabase();
    }
    return instance;
  }

  /**
   * The provider that holds the list of contacts in the database.
   */
  private ListDataProvider<ContactInfo> dataProvider = new ListDataProvider<ContactInfo>();

  private final Category[] categories;

  /**
   * The map of contacts to her friends.
   */
  private final Map<Integer, Set<ContactInfo>> friendsMap =
      new HashMap<Integer, Set<ContactInfo>>();

  /**
   * Construct a new contact database.
   */
  private ContactDatabase() {
    // Initialize the categories.
    DatabaseConstants constants = GWT.create(DatabaseConstants.class);
    String[] catNames = constants.contactDatabaseCategories();
    categories = new Category[catNames.length];
    for (int i = 0; i < catNames.length; i++) {
      categories[i] = new Category(catNames[i]);
    }

    // Generate initial data.
    generateContacts(250);
  }

  /**
   * Add a new contact.
   * 
   * @param contact the contact to add.
   */
  public void addContact(ContactInfo contact) {
    List<ContactInfo> contacts = dataProvider.getList();
    // Remove the contact first so we don't add a duplicate.
    contacts.remove(contact);
    contacts.add(contact);
  }

  /**
   * Add a display to the database. The current range of interest of the display
   * will be populated with data.
   * 
   * @param display a {@Link HasData}.
   */
  public void addDataDisplay(HasData<ContactInfo> display) {
    dataProvider.addDataDisplay(display);
  }

  /**
   * Generate the specified number of contacts and add them to the data
   * provider.
   * 
   * @param count the number of contacts to generate.
   */
  public void generateContacts(int count) {
    List<ContactInfo> contacts = dataProvider.getList();
    for (int i = 0; i < count; i++) {
      contacts.add(createContactInfo());
    }
  }

  public ListDataProvider<ContactInfo> getDataProvider() {
    return dataProvider;
  }

  /**
   * Get the categories in the database.
   * 
   * @return the categories in the database
   */
  public Category[] queryCategories() {
    return categories;
  }

  /**
   * Query all contacts for the specified category.
   * 
   * @param category the category
   * @return the list of contacts in the category
   */
  public List<ContactInfo> queryContactsByCategory(Category category) {
    List<ContactInfo> matches = new ArrayList<ContactInfo>();
    for (ContactInfo contact : dataProvider.getList()) {
      if (contact.getCategory() == category) {
        matches.add(contact);
      }
    }
    return matches;
  }

  /**
   * Query all contacts for the specified category that begin with the specified
   * first name prefix.
   * 
   * @param category the category
   * @param firstNamePrefix the prefix of the first name
   * @return the list of contacts in the category
   */
  public List<ContactInfo> queryContactsByCategoryAndFirstName(Category category,
      String firstNamePrefix) {
    List<ContactInfo> matches = new ArrayList<ContactInfo>();
    for (ContactInfo contact : dataProvider.getList()) {
      if (contact.getCategory() == category && contact.getFirstName().startsWith(firstNamePrefix)) {
        matches.add(contact);
      }
    }
    return matches;
  }

  /**
   * Query the list of friends for the specified contact.
   * 
   * @param contact the contact
   * @return the friends of the contact
   */
  public Set<ContactInfo> queryFriends(ContactInfo contact) {
    Set<ContactInfo> friends = friendsMap.get(contact.getId());
    if (friends == null) {
      // Assign some random friends.
      friends = new HashSet<ContactInfo>();
      int numContacts = dataProvider.getList().size();
      int friendCount = 2 + Random.nextInt(8);
      for (int i = 0; i < friendCount; i++) {
        friends.add(dataProvider.getList().get(Random.nextInt(numContacts)));
      }
      friendsMap.put(contact.getId(), friends);
    }
    return friends;
  }

  /**
   * Refresh all displays.
   */
  public void refreshDisplays() {
    dataProvider.refresh();
  }

  /**
   * Create a new random {@link ContactInfo}.
   * 
   * @return the new {@link ContactInfo}.
   */
  @SuppressWarnings("deprecation")
  private ContactInfo createContactInfo() {
    ContactInfo contact = new ContactInfo(nextValue(categories));
    contact.setLastName(nextValue(LAST_NAMES));
    if (Random.nextBoolean()) {
      // Male.
      contact.setFirstName(nextValue(MALE_FIRST_NAMES));
    } else {
      // Female.
      contact.setFirstName(nextValue(FEMALE_FIRST_NAMES));
    }

    // Create a birthday between 20-80 years ago.
    int year = (new Date()).getYear() - 21 - Random.nextInt(61);
    contact.setBirthday(new Date(year, Random.nextInt(12), 1 + Random.nextInt(31)));

    // Create an address.
    int addrNum = 1 + Random.nextInt(999);
    String addrStreet = nextValue(STREET_NAMES);
    String addrSuffix = nextValue(STREET_SUFFIX);
    contact.setAddress(addrNum + " " + addrStreet + " " + addrSuffix);
    return contact;
  }

  /**
   * Get the next random value from an array.
   * 
   * @param array the array
   * @return a random value in the array
   */
  private <T> T nextValue(T[] array) {
    return array[Random.nextInt(array.length)];
  }

}
