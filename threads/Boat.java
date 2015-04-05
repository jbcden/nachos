package nachos.threads;
import nachos.ag.BoatGrader;
import java.util.ArrayList;

public class Boat
{
  static BoatGrader bg;

  static Island boatLocation = Island.OAHU;

  static int totalChildren;
  static int totalAdults;

  static int oahuChildren = 0;
  static int oahuAdults = 0;

  static int molokaiChildren = 0;
  static int molokaiAdults = 0;

  static ArrayList<Person> boatPeople = new ArrayList<Person>();

  static enum Island {
    OAHU, MOLOKAI
  }

  static enum Person {
    CHILD, ADULT
  }

  static int getNumberOfChildren(Island current) {
    if(current == Island.MOLOKAI) {
      return molokaiChildren;
    } else {
      return oahuChildren;
    }
  }

  static int getNumberOfAdults(Island current) {
    if(current == Island.MOLOKAI) {
      return molokaiAdults;
    } else {
      return oahuAdults;
    }
  }

  static int getTotalNumberOfPeople(Island current) {
    return getNumberOfChildren(current) + getNumberOfAdults(current);
  }

  static boolean isBoatHere(Island current) {
    return boatLocation == current;
  }

  public static void selfTest()
  {
    BoatGrader b = new BoatGrader();

    // assume >= 2 children

    System.out.println("\n ***Testing Boats with only 2 children***");
    begin(0, 2, b);

    //	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
    //  	begin(1, 2, b);

    //  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
    //  	begin(3, 3, b);
  }

  public static void begin( int adults, int children, BoatGrader b )
  {
    // Store the externally generated autograder in a class
    // variable to be accessible by children.
    bg = b;

    // Instantiate global variables here
    totalChildren = children;
    totalAdults = adults;

    // Create threads here. See section 3.4 of the Nachos for Java
    // Walkthrough linkied from the projects page.

    Runnable a = new Runnable() {
      public void run() {
        AdultItinerary();
      }
    };

    for (int i = 0; i < adults; i++) {
      KThread t = new KThread(a);
      t.setName("adult thread " + i);
      t.fork();
    }

    Runnable c = new Runnable() {
      public void run() {
        ChildItinerary();
      }
    };

    for (int i = 0; i < children; i++) {
      KThread t = new KThread(c);
      t.setName("child thread " + i);
      t.fork();
    }

    KThread.currentThread().yield();
  }

  /* This is where you should put your solutions. Make calls
     to the BoatGrader to show that it is synchronized. For
        example:
        bg.AdultRowToMolokai();
        indicates that an adult has rowed the boat across to Molokai
  */
  static void AdultItinerary() {
    oahuAdults += 1; // an adult has arrived on Oahu

    Person personType = Person.ADULT;
    Island currentIsland = Island.OAHU;
  }

  static void ChildItinerary() {
    oahuChildren += 1; // a child has arrived on Oahu

    Person personType = Person.CHILD;
    Island currentIsland = Island.OAHU;
  }

  static boolean isDone() { // assume correct for now
    return (molokaiChildren == totalChildren) && (molokaiAdults == totalAdults);
  }

  static void SampleItinerary() {
    // Please note that this isn't a valid solution (you can't fit
    // all of them on the boat). Please also note that you may not
    // have a single thread calculate a solution and then just play
    // it back at the autograder -- you will be caught.
    System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
    bg.AdultRowToMolokai();
    bg.ChildRideToMolokai();
    //bg.AdultRideToMolokai(); // this case is invalid
    bg.ChildRideToMolokai();
  }

  // Tells if we can add another person to the boat
  private static boolean isBoatValid(Person p) {
    if( p == Person.CHILD) {
      return (!boatPeople.contains(Person.ADULT) && boatPeople.size() < 2);
    } else {
      return boatPeople.size() == 0;
    }
  }

  private static void AdultRowToMolokai() {
    oahuAdults -= 1;
    molokaiAdults += 1;

    boatLocation = Island.MOLOKAI;
    bg.AdultRowToMolokai();
  }

  private static void ChildrenRowToMolokai() {
    oahuChildren -= 1;
    molokaiChildren += 1;

    boatLocation = Island.MOLOKAI;
    bg.ChildRowToMolokai();
  }

  private static void ChildrenRideToMolokai() {
    oahuChildren -= 1;
    molokaiChildren += 1;

    bg.ChildRideToMolokai();
  }

  // this may be superfluous
  private static void AdultRowToOahu() {
    oahuAdults += 1;
    molokaiAdults -= 1;

    boatLocation = Island.OAHU;
    bg.AdultRowToOahu();
  }

  private static void ChildrenRowToOahu() {
    oahuChildren += 1;
    molokaiChildren -= 1;

    boatLocation = Island.OAHU;
    bg.ChildRowToOahu();
  }

  // This may be superfluous
  private static void ChildrenRideToOahu() {
    oahuChildren += 1;
    molokaiChildren -= 1;

    bg.ChildRideToOahu();
  }
}
